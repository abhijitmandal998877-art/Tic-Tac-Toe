package com.example.online.model

data class OnlineUser(
    val uid: String = "",
    val displayName: String = "",
    val authType: String = "guest", // "guest" or "google"
    val email: String? = null,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val onlineStatus: Boolean = true,
    val currentStatus: String = "online", // "online", "searching", "playing", "offline"
    val fcmToken: String = "",
    val lastSeen: Long = 0L
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "displayName" to displayName,
            "authType" to authType,
            "email" to email,
            "wins" to wins,
            "losses" to losses,
            "draws" to draws,
            "onlineStatus" to onlineStatus,
            "currentStatus" to currentStatus,
            "fcmToken" to fcmToken,
            "lastSeen" to lastSeen
        )
    }
}

data class OnlinePlayerData(
    val uid: String = "",
    val displayName: String = "",
    val ready: Boolean = true,
    val rematchRequested: Boolean = false,
    val online: Boolean = true
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "displayName" to displayName,
            "ready" to ready,
            "rematchRequested" to rematchRequested,
            "online" to online
        )
    }
}

data class OnlineRoom(
    val roomId: String = "",
    val roomCode: String = "",
    val status: String = "WAITING", // WAITING, PLAYING, FINISHED, CLOSED, ABANDONED
    val playerX: OnlinePlayerData? = null,
    val playerO: OnlinePlayerData? = null,
    val board: List<String> = List(9) { "" },
    val currentTurn: String = "X",
    val winner: String = "", // "X", "O", "DRAW", or ""
    val winningLine: List<Int> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun isMyTurn(myUid: String): Boolean {
        if (status != "PLAYING") return false
        val myRole = if (playerX?.uid == myUid) "X" else if (playerO?.uid == myUid) "O" else null
        return myRole == currentTurn
    }

    fun getMyRole(myUid: String): String? {
        return when (myUid) {
            playerX?.uid -> "X"
            playerO?.uid -> "O"
            else -> null
        }
    }

    fun getOpponent(myUid: String): OnlinePlayerData? {
        return if (playerX?.uid == myUid) playerO else playerX
    }
}

data class MatchmakingQueueEntry(
    val uid: String = "",
    val displayName: String = "",
    val status: String = "searching", // "searching", "matched", "cancelled"
    val timestamp: Long = 0L,
    val matchedRoomId: String? = null
)

enum class OnlineSubScreen {
    MENU,
    CREATE_LOBBY,
    JOIN_INPUT,
    MATCHMAKING_SEARCHING,
    MATCH_FOUND,
    IN_GAME
}
