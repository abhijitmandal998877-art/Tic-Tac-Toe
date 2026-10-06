package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Difficulty
import com.example.model.Player
import com.example.model.Score
import com.example.online.model.OnlineUser
import java.util.UUID
import kotlin.random.Random

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tictactoe_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_X_WINS = "x_wins"
        private const val KEY_O_WINS = "o_wins"
        private const val KEY_DRAWS = "draws"

        private const val KEY_MUSIC_ENABLED = "music_enabled"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_DIFFICULTY = "difficulty"

        private const val KEY_SAVED_BOARD = "saved_board"
        private const val KEY_SAVED_TURN = "saved_turn"
        private const val KEY_SAVED_MODE = "saved_mode"

        private const val KEY_USER_UID = "user_uid"
        private const val KEY_USER_DISPLAY_NAME = "user_display_name"
        private const val KEY_USER_AUTH_TYPE = "user_auth_type"
        private const val KEY_USER_EMAIL = "user_email"
    }

    fun getScore(): Score {
        return Score(
            xWins = prefs.getInt(KEY_X_WINS, 0),
            oWins = prefs.getInt(KEY_O_WINS, 0),
            draws = prefs.getInt(KEY_DRAWS, 0)
        )
    }

    fun incrementXWin() {
        val current = prefs.getInt(KEY_X_WINS, 0)
        prefs.edit().putInt(KEY_X_WINS, current + 1).apply()
    }

    fun incrementOWin() {
        val current = prefs.getInt(KEY_O_WINS, 0)
        prefs.edit().putInt(KEY_O_WINS, current + 1).apply()
    }

    fun incrementDraw() {
        val current = prefs.getInt(KEY_DRAWS, 0)
        prefs.edit().putInt(KEY_DRAWS, current + 1).apply()
    }

    fun resetScore() {
        prefs.edit()
            .putInt(KEY_X_WINS, 0)
            .putInt(KEY_O_WINS, 0)
            .putInt(KEY_DRAWS, 0)
            .apply()
    }

    var isMusicEnabled: Boolean
        get() = prefs.getBoolean(KEY_MUSIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MUSIC_ENABLED, value).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()

    var selectedDifficulty: Difficulty
        get() {
            val name = prefs.getString(KEY_DIFFICULTY, Difficulty.MEDIUM.name)
            return try {
                Difficulty.valueOf(name ?: Difficulty.MEDIUM.name)
            } catch (_: Exception) {
                Difficulty.MEDIUM
            }
        }
        set(value) = prefs.edit().putString(KEY_DIFFICULTY, value.name).apply()

    fun saveGameSession(board: List<Player?>, turn: Player, modeStr: String) {
        val boardStr = board.joinToString(",") { it?.name ?: "E" }
        prefs.edit()
            .putString(KEY_SAVED_BOARD, boardStr)
            .putString(KEY_SAVED_TURN, turn.name)
            .putString(KEY_SAVED_MODE, modeStr)
            .apply()
    }

    fun clearSavedGameSession() {
        prefs.edit()
            .remove(KEY_SAVED_BOARD)
            .remove(KEY_SAVED_TURN)
            .remove(KEY_SAVED_MODE)
            .apply()
    }

    fun hasSavedGameSession(): Boolean {
        return prefs.contains(KEY_SAVED_BOARD)
    }

    fun loadSavedGameSession(): Triple<List<Player?>, Player, String>? {
        val boardStr = prefs.getString(KEY_SAVED_BOARD, null) ?: return null
        val turnStr = prefs.getString(KEY_SAVED_TURN, Player.X.name) ?: Player.X.name
        val modeStr = prefs.getString(KEY_SAVED_MODE, "TWO_PLAYER") ?: "TWO_PLAYER"

        val board = boardStr.split(",").map {
            when (it) {
                "X" -> Player.X
                "O" -> Player.O
                else -> null
            }
        }
        if (board.size != 9) return null

        val turn = if (turnStr == "O") Player.O else Player.X
        return Triple(board, turn, modeStr)
    }

    fun hasUserSession(): Boolean {
        return prefs.contains(KEY_USER_UID)
    }

    fun saveUserSession(uid: String, displayName: String, authType: String, email: String? = null) {
        prefs.edit()
            .putString(KEY_USER_UID, uid)
            .putString(KEY_USER_DISPLAY_NAME, displayName)
            .putString(KEY_USER_AUTH_TYPE, authType)
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    fun loadUserSession(): OnlineUser? {
        val uid = prefs.getString(KEY_USER_UID, null) ?: return null
        val displayName = prefs.getString(KEY_USER_DISPLAY_NAME, "Player") ?: "Player"
        val authType = prefs.getString(KEY_USER_AUTH_TYPE, "guest") ?: "guest"
        val email = prefs.getString(KEY_USER_EMAIL, null)

        val score = getScore()
        return OnlineUser(
            uid = uid,
            displayName = displayName,
            authType = authType,
            email = email,
            wins = score.xWins,
            draws = score.draws,
            onlineStatus = true
        )
    }

    fun getOrCreateGuestUser(): OnlineUser {
        val existing = loadUserSession()
        if (existing != null) {
            return existing
        }
        val guestUid = "guest_" + UUID.randomUUID().toString().take(8)
        val defaultName = "Player${Random.nextInt(1000, 9999)}"
        saveUserSession(guestUid, defaultName, "guest", null)
        return OnlineUser(
            uid = guestUid,
            displayName = defaultName,
            authType = "guest",
            onlineStatus = true
        )
    }

    fun clearUserSession() {
        prefs.edit()
            .remove(KEY_USER_UID)
            .remove(KEY_USER_DISPLAY_NAME)
            .remove(KEY_USER_AUTH_TYPE)
            .remove(KEY_USER_EMAIL)
            .apply()
    }
}
