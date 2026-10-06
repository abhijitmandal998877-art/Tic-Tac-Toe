package com.example.online.repository

import com.example.logic.GameEngine
import com.example.model.Player
import com.example.online.model.OnlinePlayerData
import com.example.online.model.OnlineRoom
import com.example.online.model.OnlineUser
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

object FirebaseManager {

    private val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (_: Throwable) {
            null
        }

    private val database: FirebaseDatabase?
        get() = try {
            FirebaseDatabase.getInstance()
        } catch (_: Throwable) {
            null
        }

    fun isAvailable(): Boolean {
        return try {
            auth != null && database != null
        } catch (_: Throwable) {
            false
        }
    }

    fun isAuthenticated(): Boolean {
        return auth?.currentUser != null
    }

    fun isAnonymous(): Boolean {
        return auth?.currentUser?.isAnonymous == true
    }

    fun getCurrentUid(): String? {
        return auth?.currentUser?.uid
    }

    fun getCurrentEmail(): String? {
        return auth?.currentUser?.email
    }

    fun getCurrentDisplayName(): String? {
        return auth?.currentUser?.displayName
    }

    suspend fun signInAnonymously(): Result<String> {
        val currentAuth = auth ?: return Result.failure(Exception("Firebase is unavailable"))
        return try {
            val result = currentAuth.signInAnonymously().await()
            val uid = result.user?.uid ?: return Result.failure(Exception("Failed to obtain UID"))
            initializeUserProfile(uid, isGoogle = false)
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun linkOrSignInWithGoogle(idToken: String): Result<String> {
        val currentAuth = auth ?: return Result.failure(Exception("Firebase is unavailable"))
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val currentUser = currentAuth.currentUser

        return try {
            if (currentUser != null && currentUser.isAnonymous) {
                // Link anonymous user to preserve existing stats/UID
                try {
                    val linkResult = currentUser.linkWithCredential(credential).await()
                    val uid = linkResult.user?.uid ?: currentUser.uid
                    upgradeUserProfileToGoogle(uid)
                    Result.success(uid)
                } catch (collision: FirebaseAuthUserCollisionException) {
                    // Google account already exists on another UID
                    // Sign in to that Google account directly without destroying anything
                    val signInResult = currentAuth.signInWithCredential(credential).await()
                    val uid = signInResult.user?.uid ?: return Result.failure(collision)
                    initializeUserProfile(uid, isGoogle = true)
                    Result.success(uid)
                }
            } else {
                // Regular Google Sign-In
                val signInResult = currentAuth.signInWithCredential(credential).await()
                val uid = signInResult.user?.uid ?: return Result.failure(Exception("No user after Google sign in"))
                initializeUserProfile(uid, isGoogle = true)
                Result.success(uid)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            val uid = getCurrentUid()
            if (uid != null) {
                updateUserStatus(uid, "offline")
            }
            auth?.signOut()
        } catch (_: Throwable) { }
    }

    suspend fun ensureAuthenticated(): String? {
        val currentAuth = auth ?: return null
        val existing = currentAuth.currentUser
        if (existing != null) {
            return existing.uid
        }

        return try {
            val result = currentAuth.signInAnonymously().await()
            val uid = result.user?.uid ?: return null
            initializeUserProfile(uid, isGoogle = false)
            uid
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun initializeUserProfile(uid: String, isGoogle: Boolean = false) {
        val db = database ?: return
        val userRef = db.reference.child("users").child(uid)
        try {
            val snapshot = userRef.get().await()
            if (!snapshot.exists()) {
                val googleName = auth?.currentUser?.displayName
                val defaultName = if (!googleName.isNullOrBlank()) {
                    googleName
                } else {
                    "Player${Random.nextInt(1000, 9999)}"
                }

                val initialUser = OnlineUser(
                    uid = uid,
                    displayName = defaultName,
                    authType = if (isGoogle) "google" else "guest",
                    email = auth?.currentUser?.email,
                    wins = 0,
                    losses = 0,
                    draws = 0,
                    onlineStatus = true,
                    currentStatus = "online",
                    lastSeen = System.currentTimeMillis()
                )
                userRef.setValue(initialUser.toMap()).await()
            } else {
                // If user exists, update last seen and online status
                val updates = mapOf(
                    "onlineStatus" to true,
                    "currentStatus" to "online",
                    "lastSeen" to ServerValue.TIMESTAMP
                )
                userRef.updateChildren(updates).await()
            }

            // Set up onDisconnect status
            userRef.child("onlineStatus").onDisconnect().setValue(false)
            userRef.child("currentStatus").onDisconnect().setValue("offline")
            userRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP)
        } catch (_: Throwable) { }
    }

    private suspend fun upgradeUserProfileToGoogle(uid: String) {
        val db = database ?: return
        val googleName = auth?.currentUser?.displayName
        val email = auth?.currentUser?.email
        val updates = mutableMapOf<String, Any>(
            "authType" to "google",
            "onlineStatus" to true,
            "lastSeen" to ServerValue.TIMESTAMP
        )
        if (!googleName.isNullOrBlank()) updates["displayName"] = googleName
        if (!email.isNullOrBlank()) updates["email"] = email

        try {
            db.reference.child("users").child(uid).updateChildren(updates).await()
        } catch (_: Throwable) { }
    }

    fun updateUserStatus(uid: String, status: String) {
        val db = database ?: return
        try {
            db.reference.child("users").child(uid).child("currentStatus").setValue(status)
        } catch (_: Throwable) { }
    }

    fun updateFcmToken(uid: String, token: String) {
        val db = database ?: return
        try {
            db.reference.child("users").child(uid).child("fcmToken").setValue(token)
        } catch (_: Throwable) { }
    }

    fun observeUserProfile(uid: String): Flow<OnlineUser?> = callbackFlow {
        val db = database
        if (db == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val userRef = db.reference.child("users").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }

                val name = snapshot.child("displayName").getValue(String::class.java) ?: "Player"
                val authType = snapshot.child("authType").getValue(String::class.java) ?: "guest"
                val email = snapshot.child("email").getValue(String::class.java)
                val wins = snapshot.child("wins").getValue(Long::class.java)?.toInt() ?: 0
                val losses = snapshot.child("losses").getValue(Long::class.java)?.toInt() ?: 0
                val draws = snapshot.child("draws").getValue(Long::class.java)?.toInt() ?: 0
                val online = snapshot.child("onlineStatus").getValue(Boolean::class.java) ?: true
                val currentStatus = snapshot.child("currentStatus").getValue(String::class.java) ?: "online"
                val fcm = snapshot.child("fcmToken").getValue(String::class.java) ?: ""
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L

                val profile = OnlineUser(
                    uid = uid,
                    displayName = name,
                    authType = authType,
                    email = email,
                    wins = wins,
                    losses = losses,
                    draws = draws,
                    onlineStatus = online,
                    currentStatus = currentStatus,
                    fcmToken = fcm,
                    lastSeen = lastSeen
                )
                trySend(profile)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }

        userRef.addValueEventListener(listener)
        awaitClose { userRef.removeEventListener(listener) }
    }

    suspend fun updateDisplayName(uid: String, newName: String): Boolean {
        val db = database ?: return false
        val cleanName = newName.trim().take(20)
        if (cleanName.isBlank()) return false
        return try {
            db.reference.child("users").child(uid).child("displayName").setValue(cleanName).await()
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun generateRoomCode(): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        return (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }

    suspend fun createRoom(displayName: String): Pair<String, String>? {
        val db = database ?: return null
        val uid = getCurrentUid() ?: return null

        val roomsRef = db.reference.child("rooms")
        val newRoomRef = roomsRef.push()
        val roomId = newRoomRef.key ?: return null
        val roomCode = generateRoomCode()

        val playerX = OnlinePlayerData(
            uid = uid,
            displayName = displayName,
            ready = true,
            rematchRequested = false,
            online = true
        )

        val roomData = mapOf(
            "roomId" to roomId,
            "roomCode" to roomCode,
            "status" to "WAITING",
            "playerX" to playerX.toMap(),
            "playerO" to null,
            "board" to List(9) { "" },
            "currentTurn" to "X",
            "winner" to "",
            "winningLine" to emptyList<Int>(),
            "createdAt" to ServerValue.TIMESTAMP,
            "updatedAt" to ServerValue.TIMESTAMP
        )

        return try {
            newRoomRef.setValue(roomData).await()
            newRoomRef.child("playerX").child("online").onDisconnect().setValue(false)
            updateUserStatus(uid, "playing")
            Pair(roomId, roomCode)
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun joinRoom(inputCode: String, displayName: String): Result<String> {
        val db = database ?: return Result.failure(Exception("Firebase is unavailable"))
        val uid = getCurrentUid() ?: return Result.failure(Exception("Authentication required"))

        val cleanCode = inputCode.trim().uppercase()
        if (cleanCode.length != 6) {
            return Result.failure(Exception("Room code must be 6 characters"))
        }

        return try {
            val query = db.reference.child("rooms")
                .orderByChild("roomCode")
                .equalTo(cleanCode)
                .get()
                .await()

            if (!query.exists() || !query.hasChildren()) {
                return Result.failure(Exception("Room not found. Check the code and try again."))
            }

            var targetRoomId: String? = null
            var targetSnapshot: DataSnapshot? = null

            for (child in query.children) {
                val status = child.child("status").getValue(String::class.java)
                val playerO = child.child("playerO").value
                val playerXUid = child.child("playerX").child("uid").getValue(String::class.java)

                if (playerXUid == uid) {
                    return Result.success(child.key ?: "")
                }

                if (status == "WAITING" && playerO == null) {
                    targetRoomId = child.key
                    targetSnapshot = child
                    break
                }
            }

            if (targetRoomId == null || targetSnapshot == null) {
                return Result.failure(Exception("Room is already full or game has started."))
            }

            val playerO = OnlinePlayerData(
                uid = uid,
                displayName = displayName,
                ready = true,
                rematchRequested = false,
                online = true
            )

            val roomRef = db.reference.child("rooms").child(targetRoomId)
            val updates = mapOf<String, Any>(
                "playerO" to playerO.toMap(),
                "status" to "PLAYING",
                "updatedAt" to ServerValue.TIMESTAMP
            )
            roomRef.updateChildren(updates).await()

            roomRef.child("playerO").child("online").onDisconnect().setValue(false)
            updateUserStatus(uid, "playing")

            Result.success(targetRoomId)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to join room: ${e.message}"))
        }
    }

    // ==========================================
    // AUTO MATCHMAKING (QUICK MATCH)
    // ==========================================
    suspend fun enterMatchmakingQueue(
        displayName: String,
        onMatched: (roomId: String, opponentName: String) -> Unit,
        onError: (String) -> Unit
    ): ValueEventListener? {
        val db = database ?: run {
            onError("Firebase unavailable")
            return null
        }
        val myUid = getCurrentUid() ?: run {
            onError("Authentication required")
            return null
        }

        val queueRef = db.reference.child("matchmaking")
        val myEntryRef = queueRef.child(myUid)

        try {
            val entry = mapOf(
                "uid" to myUid,
                "displayName" to displayName,
                "status" to "searching",
                "timestamp" to ServerValue.TIMESTAMP
            )
            myEntryRef.setValue(entry).await()
            myEntryRef.onDisconnect().removeValue()
            updateUserStatus(myUid, "searching")

            // Listen on own entry for "matched" status
            val ownListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.child("status").getValue(String::class.java)
                    val matchedRoomId = snapshot.child("matchedRoomId").getValue(String::class.java)
                    val oppName = snapshot.child("opponentName").getValue(String::class.java) ?: "Opponent"

                    if (status == "matched" && !matchedRoomId.isNullOrBlank()) {
                        myEntryRef.removeEventListener(this)
                        myEntryRef.removeValue()
                        updateUserStatus(myUid, "playing")
                        onMatched(matchedRoomId, oppName)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    onError("Matchmaking error: ${error.message}")
                }
            }
            myEntryRef.addValueEventListener(ownListener)

            // Look for another compatible searching player
            val allQueueSnap = queueRef.get().await()
            for (child in allQueueSnap.children) {
                val candidateUid = child.child("uid").getValue(String::class.java) ?: continue
                val candidateStatus = child.child("status").getValue(String::class.java)
                val candidateName = child.child("displayName").getValue(String::class.java) ?: "Player"

                if (candidateUid != myUid && candidateStatus == "searching") {
                    // Tie-breaker: lexicographically smaller UID creates the room to avoid race conditions
                    if (myUid < candidateUid) {
                        createMatchmakingRoom(
                            myUid = myUid,
                            myName = displayName,
                            opponentUid = candidateUid,
                            opponentName = candidateName,
                            onMatched = onMatched,
                            onError = onError
                        )
                        break
                    }
                }
            }

            return ownListener
        } catch (e: Exception) {
            onError("Matchmaking failed: ${e.message}")
            return null
        }
    }

    private suspend fun createMatchmakingRoom(
        myUid: String,
        myName: String,
        opponentUid: String,
        opponentName: String,
        onMatched: (roomId: String, opponentName: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val db = database ?: return
        val roomsRef = db.reference.child("rooms")
        val newRoomRef = roomsRef.push()
        val roomId = newRoomRef.key ?: return
        val roomCode = generateRoomCode()

        val playerX = OnlinePlayerData(uid = myUid, displayName = myName, ready = true, online = true)
        val playerO = OnlinePlayerData(uid = opponentUid, displayName = opponentName, ready = true, online = true)

        val roomData = mapOf(
            "roomId" to roomId,
            "roomCode" to roomCode,
            "status" to "PLAYING",
            "playerX" to playerX.toMap(),
            "playerO" to playerO.toMap(),
            "board" to List(9) { "" },
            "currentTurn" to "X",
            "winner" to "",
            "winningLine" to emptyList<Int>(),
            "createdAt" to ServerValue.TIMESTAMP,
            "updatedAt" to ServerValue.TIMESTAMP
        )

        try {
            newRoomRef.setValue(roomData).await()
            newRoomRef.child("playerX").child("online").onDisconnect().setValue(false)
            newRoomRef.child("playerO").child("online").onDisconnect().setValue(false)

            // Notify candidate
            val candidateUpdates = mapOf(
                "status" to "matched",
                "matchedRoomId" to roomId,
                "opponentName" to myName
            )
            db.reference.child("matchmaking").child(opponentUid).updateChildren(candidateUpdates).await()

            // Remove self and update status
            db.reference.child("matchmaking").child(myUid).removeValue().await()
            updateUserStatus(myUid, "playing")

            onMatched(roomId, opponentName)
        } catch (e: Exception) {
            onError("Failed creating match: ${e.message}")
        }
    }

    suspend fun cancelMatchmaking(listener: ValueEventListener? = null) {
        val db = database ?: return
        val uid = getCurrentUid() ?: return
        try {
            val myEntryRef = db.reference.child("matchmaking").child(uid)
            if (listener != null) {
                myEntryRef.removeEventListener(listener)
            }
            myEntryRef.removeValue().await()
            updateUserStatus(uid, "online")
        } catch (_: Throwable) { }
    }

    fun observeRoom(roomId: String): Flow<OnlineRoom?> = callbackFlow {
        val db = database
        if (db == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val roomRef = db.reference.child("rooms").child(roomId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }

                try {
                    val code = snapshot.child("roomCode").getValue(String::class.java) ?: ""
                    val status = snapshot.child("status").getValue(String::class.java) ?: "WAITING"

                    val pxSnap = snapshot.child("playerX")
                    val playerX = if (pxSnap.exists()) {
                        OnlinePlayerData(
                            uid = pxSnap.child("uid").getValue(String::class.java) ?: "",
                            displayName = pxSnap.child("displayName").getValue(String::class.java) ?: "Player X",
                            ready = pxSnap.child("ready").getValue(Boolean::class.java) ?: true,
                            rematchRequested = pxSnap.child("rematchRequested").getValue(Boolean::class.java) ?: false,
                            online = pxSnap.child("online").getValue(Boolean::class.java) ?: true
                        )
                    } else null

                    val poSnap = snapshot.child("playerO")
                    val playerO = if (poSnap.exists()) {
                        OnlinePlayerData(
                            uid = poSnap.child("uid").getValue(String::class.java) ?: "",
                            displayName = poSnap.child("displayName").getValue(String::class.java) ?: "Player O",
                            ready = poSnap.child("ready").getValue(Boolean::class.java) ?: true,
                            rematchRequested = poSnap.child("rematchRequested").getValue(Boolean::class.java) ?: false,
                            online = poSnap.child("online").getValue(Boolean::class.java) ?: true
                        )
                    } else null

                    val boardList = mutableListOf<String>()
                    val boardSnap = snapshot.child("board")
                    for (i in 0..8) {
                        val cell = boardSnap.child(i.toString()).getValue(String::class.java) ?: ""
                        boardList.add(cell)
                    }

                    val turn = snapshot.child("currentTurn").getValue(String::class.java) ?: "X"
                    val winner = snapshot.child("winner").getValue(String::class.java) ?: ""

                    val winLineList = mutableListOf<Int>()
                    val winSnap = snapshot.child("winningLine")
                    for (child in winSnap.children) {
                        child.getValue(Long::class.java)?.toInt()?.let { winLineList.add(it) }
                    }

                    val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                    val updatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: 0L

                    val room = OnlineRoom(
                        roomId = roomId,
                        roomCode = code,
                        status = status,
                        playerX = playerX,
                        playerO = playerO,
                        board = boardList,
                        currentTurn = turn,
                        winner = winner,
                        winningLine = winLineList,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                    trySend(room)
                } catch (_: Throwable) {
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }

        roomRef.addValueEventListener(listener)
        awaitClose { roomRef.removeEventListener(listener) }
    }

    suspend fun makeMove(roomId: String, cellIndex: Int, currentRoom: OnlineRoom): Boolean {
        val db = database ?: return false
        val uid = getCurrentUid() ?: return false

        if (currentRoom.status != "PLAYING") return false
        val myRole = currentRoom.getMyRole(uid) ?: return false
        if (myRole != currentRoom.currentTurn) return false
        if (cellIndex !in 0..8 || currentRoom.board[cellIndex].isNotEmpty()) return false

        val newBoard = currentRoom.board.toMutableList()
        newBoard[cellIndex] = myRole

        val playerList: List<Player?> = newBoard.map {
            when (it) {
                "X" -> Player.X
                "O" -> Player.O
                else -> null
            }
        }

        val winnerPair = GameEngine.checkWinner(playerList)
        val isFull = GameEngine.isBoardFull(playerList)

        val updates = mutableMapOf<String, Any>()
        updates["board/$cellIndex"] = myRole
        updates["updatedAt"] = ServerValue.TIMESTAMP

        if (winnerPair != null) {
            val (winnerPlayer, winCombo) = winnerPair
            updates["winner"] = winnerPlayer.symbol
            updates["winningLine"] = winCombo
            updates["status"] = "FINISHED"

            val winnerUid = if (winnerPlayer == Player.X) currentRoom.playerX?.uid else currentRoom.playerO?.uid
            val loserUid = if (winnerPlayer == Player.X) currentRoom.playerO?.uid else currentRoom.playerX?.uid

            if (!winnerUid.isNullOrEmpty()) incrementUserStat(winnerUid, "wins")
            if (!loserUid.isNullOrEmpty()) incrementUserStat(loserUid, "losses")
        } else if (isFull) {
            updates["winner"] = "DRAW"
            updates["winningLine"] = emptyList<Int>()
            updates["status"] = "FINISHED"

            currentRoom.playerX?.uid?.let { incrementUserStat(it, "draws") }
            currentRoom.playerO?.uid?.let { incrementUserStat(it, "draws") }
        } else {
            updates["currentTurn"] = if (myRole == "X") "O" else "X"
        }

        return try {
            db.reference.child("rooms").child(roomId).updateChildren(updates).await()
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun incrementUserStat(uid: String, statField: String) {
        val db = database ?: return
        try {
            val statRef = db.reference.child("users").child(uid).child(statField)
            statRef.setValue(ServerValue.increment(1))
        } catch (_: Throwable) { }
    }

    suspend fun requestRematch(roomId: String, currentRoom: OnlineRoom): Boolean {
        val db = database ?: return false
        val uid = getCurrentUid() ?: return false
        val isPlayerX = currentRoom.playerX?.uid == uid
        val isPlayerO = currentRoom.playerO?.uid == uid
        if (!isPlayerX && !isPlayerO) return false

        val otherRematch = if (isPlayerX) {
            currentRoom.playerO?.rematchRequested == true
        } else {
            currentRoom.playerX?.rematchRequested == true
        }

        val updates = mutableMapOf<String, Any>()
        if (isPlayerX) {
            updates["playerX/rematchRequested"] = true
        } else {
            updates["playerO/rematchRequested"] = true
        }

        if (otherRematch) {
            updates["board"] = List(9) { "" }
            updates["winner"] = ""
            updates["winningLine"] = emptyList<Int>()
            updates["status"] = "PLAYING"
            updates["currentTurn"] = if (currentRoom.currentTurn == "X") "O" else "X"
            updates["playerX/rematchRequested"] = false
            updates["playerO/rematchRequested"] = false
            updates["updatedAt"] = ServerValue.TIMESTAMP
        }

        return try {
            db.reference.child("rooms").child(roomId).updateChildren(updates).await()
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun leaveRoom(roomId: String, currentRoom: OnlineRoom) {
        val db = database ?: return
        val uid = getCurrentUid() ?: return
        try {
            val roomRef = db.reference.child("rooms").child(roomId)
            val isPlayerX = currentRoom.playerX?.uid == uid

            if (currentRoom.status == "WAITING" || currentRoom.status == "PLAYING") {
                val onlineKey = if (isPlayerX) "playerX/online" else "playerO/online"
                val updates = mapOf(
                    "status" to "ABANDONED",
                    onlineKey to false,
                    "updatedAt" to ServerValue.TIMESTAMP
                )
                roomRef.updateChildren(updates).await()
            } else {
                val onlineKey = if (isPlayerX) "playerX/online" else "playerO/online"
                roomRef.child(onlineKey).setValue(false).await()
            }
            updateUserStatus(uid, "online")
        } catch (_: Throwable) { }
    }
}
