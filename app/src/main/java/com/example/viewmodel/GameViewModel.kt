package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.auth.GoogleAuthHelper
import com.example.data.GamePreferences
import com.example.logic.AiEngine
import com.example.logic.GameEngine
import com.example.model.Difficulty
import com.example.model.GameMode
import com.example.model.GameStatus
import com.example.model.Player
import com.example.model.Score
import com.example.model.Screen
import com.example.online.model.OnlineUser
import com.example.online.repository.FirebaseManager
import com.example.util.DeveloperDetector
import com.example.util.HapticManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val currentScreen: Screen = Screen.SPLASH,
    val previousScreen: Screen = Screen.SPLASH,
    val gameMode: GameMode = GameMode.TwoPlayer,
    val board: List<Player?> = List(9) { null },
    val gameStatus: GameStatus = GameStatus.InProgress(Player.X),
    val score: Score = Score(),
    val isMusicEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val hasSavedGame: Boolean = false,
    val aiPlayer: Player = Player.O,
    val isAuthLoading: Boolean = false,
    val authError: String? = null,
    val currentUser: OnlineUser? = null
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = GamePreferences(application)
    private val soundManager = SoundManager()
    private val hapticManager = HapticManager(application)
    private val devDetector = DeveloperDetector()

    private val _uiState = MutableStateFlow(
        GameUiState(
            score = prefs.getScore(),
            isMusicEnabled = prefs.isMusicEnabled,
            isSoundEnabled = prefs.isSoundEnabled,
            isVibrationEnabled = prefs.isVibrationEnabled,
            difficulty = prefs.selectedDifficulty,
            hasSavedGame = prefs.hasSavedGameSession()
        )
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var aiJob: Job? = null
    private var profileJob: Job? = null

    init {
        soundManager.isSoundEnabled = prefs.isSoundEnabled
        soundManager.isMusicEnabled = prefs.isMusicEnabled
        hapticManager.isVibrationEnabled = prefs.isVibrationEnabled

        checkInitialAuthState()
    }

    private fun checkInitialAuthState() {
        viewModelScope.launch {
            delay(800L) // Smooth splash branding experience
            FirebaseManager.initialize(getApplication())

            if (FirebaseManager.isAuthenticated()) {
                val uid = FirebaseManager.getCurrentUid()
                if (uid != null) {
                    observeUserProfile(uid)
                }
                _uiState.update { it.copy(currentScreen = Screen.HOME) }
            } else if (prefs.hasUserSession()) {
                val saved = prefs.loadUserSession()
                _uiState.update { it.copy(currentUser = saved, currentScreen = Screen.HOME) }
            } else {
                _uiState.update { it.copy(currentScreen = Screen.AUTH) }
            }
        }
    }

    private fun observeUserProfile(uid: String) {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            FirebaseManager.observeUserProfile(uid).collect { profile ->
                if (profile != null) {
                    _uiState.update { it.copy(currentUser = profile) }
                }
            }
        }
    }

    fun continueAsGuest() {
        soundManager.playClick()
        _uiState.update { it.copy(isAuthLoading = true, authError = null) }
        viewModelScope.launch {
            // Attempt Firebase Anonymous authentication first
            val firebaseResult = FirebaseManager.signInAnonymously(getApplication())
            val uid = firebaseResult.getOrNull()

            val guestProfile = if (uid != null) {
                val name = "Player${(1000..9999).random()}"
                prefs.saveUserSession(uid = uid, displayName = name, authType = "guest")
                observeUserProfile(uid)
                OnlineUser(uid = uid, displayName = name, authType = "guest", onlineStatus = true)
            } else {
                // Graceful offline fallback - never block the user from playing
                prefs.getOrCreateGuestUser()
            }

            soundManager.playWin()
            _uiState.update {
                it.copy(
                    currentUser = guestProfile,
                    isAuthLoading = false,
                    currentScreen = Screen.HOME,
                    authError = null
                )
            }
        }
    }

    fun continueWithGoogle(context: Context) {
        soundManager.playClick()
        _uiState.update { it.copy(isAuthLoading = true, authError = null) }
        viewModelScope.launch {
            val result = GoogleAuthHelper.signInWithGoogle(context)
            result.fold(
                onSuccess = { googleResult ->
                    prefs.saveUserSession(
                        uid = googleResult.uid,
                        displayName = googleResult.displayName,
                        authType = "google",
                        email = googleResult.email
                    )
                    observeUserProfile(googleResult.uid)
                    soundManager.playWin()
                    _uiState.update {
                        it.copy(
                            currentUser = OnlineUser(
                                uid = googleResult.uid,
                                displayName = googleResult.displayName,
                                authType = "google",
                                email = googleResult.email,
                                onlineStatus = true
                            ),
                            isAuthLoading = false,
                            currentScreen = Screen.HOME,
                            authError = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authError = error.message ?: "Google Sign-In failed."
                        )
                    }
                }
            )
        }
    }

    fun dismissAuthError() {
        _uiState.update { it.copy(authError = null) }
    }

    fun signOut() {
        soundManager.playClick()
        profileJob?.cancel()
        profileJob = null
        prefs.clearUserSession()
        FirebaseManager.signOut()
        _uiState.update {
            it.copy(
                currentUser = null,
                currentScreen = Screen.AUTH,
                authError = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.stopMusic()
    }

    fun navigateTo(screen: Screen) {
        soundManager.playClick()
        _uiState.update { current ->
            current.copy(
                previousScreen = current.currentScreen,
                currentScreen = screen
            )
        }
    }

    fun navigateBack() {
        soundManager.playClick()
        _uiState.update { current ->
            val target = when (current.currentScreen) {
                Screen.GAME, Screen.SETTINGS, Screen.HOW_TO_PLAY, Screen.DEVELOPER_WEBVIEW, Screen.ONLINE -> Screen.HOME
                Screen.HOME, Screen.AUTH, Screen.SPLASH -> current.currentScreen
            }
            current.copy(currentScreen = target)
        }
    }

    fun openDeveloperSection() {
        soundManager.playWin()
        hapticManager.vibrateWin()
        _uiState.update { it.copy(currentScreen = Screen.DEVELOPER_WEBVIEW) }
    }

    fun onContinueTapped() {
        devDetector.recordTap(
            onEasterEggTriggered = {
                openDeveloperSection()
            },
            onNormalTap = {
                soundManager.playClick()
                continueSavedOrStartGame()
            }
        )
    }

    private fun continueSavedOrStartGame() {
        val saved = prefs.loadSavedGameSession()
        if (saved != null) {
            val (savedBoard, savedTurn, modeStr) = saved
            val mode: GameMode = if (modeStr.startsWith("VS_AI")) {
                val diffName = modeStr.substringAfter("VS_AI_")
                val diff = try {
                    Difficulty.valueOf(diffName)
                } catch (_: Exception) {
                    _uiState.value.difficulty
                }
                GameMode.VsAi(diff)
            } else {
                GameMode.TwoPlayer
            }

            _uiState.update {
                it.copy(
                    board = savedBoard,
                    gameMode = mode,
                    gameStatus = GameStatus.InProgress(savedTurn),
                    currentScreen = Screen.GAME
                )
            }
        } else {
            // Start fresh game with current mode
            startNewGame(_uiState.value.gameMode)
        }
    }

    fun startNewGame(mode: GameMode) {
        soundManager.playClick()
        aiJob?.cancel()

        val initialBoard = List(9) { null }
        val initialStatus = GameStatus.InProgress(Player.X)

        _uiState.update {
            it.copy(
                gameMode = mode,
                board = initialBoard,
                gameStatus = initialStatus,
                currentScreen = Screen.GAME,
                hasSavedGame = true
            )
        }

        saveCurrentSession(initialBoard, Player.X, mode)
    }

    fun playAgain() {
        soundManager.playClick()
        aiJob?.cancel()

        val initialBoard = List(9) { null }
        val initialStatus = GameStatus.InProgress(Player.X)

        _uiState.update {
            it.copy(
                board = initialBoard,
                gameStatus = initialStatus,
                hasSavedGame = true
            )
        }

        saveCurrentSession(initialBoard, Player.X, _uiState.value.gameMode)
    }

    fun onCellClicked(index: Int) {
        val state = _uiState.value

        // Validate board index
        if (index !in 0..8) return

        // Check if game is in progress
        val inProgressStatus = state.gameStatus as? GameStatus.InProgress ?: return

        // Prevent moves while AI is thinking
        if (inProgressStatus.isAiThinking) return

        // Prevent selecting an already occupied cell
        if (state.board[index] != null) return

        val currentPlayer = inProgressStatus.currentTurn

        // Sound & haptic for player move
        if (currentPlayer == Player.X) {
            soundManager.playXMove()
        } else {
            soundManager.playOMove()
        }
        hapticManager.vibrateMove()

        // Apply move
        val updatedBoard = state.board.toMutableList().also { it[index] = currentPlayer }
        val nextTurn = currentPlayer.opponent()

        val newStatus = GameEngine.evaluateGameStatus(updatedBoard, nextTurn)
        handleStatusChange(updatedBoard, newStatus, state.gameMode)

        // If in VS_AI mode and game is still in progress, trigger AI move
        if (newStatus is GameStatus.InProgress && state.gameMode is GameMode.VsAi) {
            triggerAiMove(updatedBoard, state.gameMode.difficulty)
        }
    }

    private fun triggerAiMove(currentBoard: List<Player?>, difficulty: Difficulty) {
        aiJob?.cancel()
        _uiState.update {
            it.copy(gameStatus = GameStatus.InProgress(currentTurn = Player.O, isAiThinking = true))
        }

        aiJob = viewModelScope.launch {
            // Short realistic thinking delay so user can observe turns
            delay(450)

            val move = AiEngine.determineMove(
                board = currentBoard,
                difficulty = difficulty,
                aiPlayer = Player.O
            )

            if (move != null && move in 0..8 && currentBoard[move] == null) {
                soundManager.playAiMove()
                hapticManager.vibrateMove()

                val newBoard = currentBoard.toMutableList().also { it[move] = Player.O }
                val nextTurn = Player.X
                val evalStatus = GameEngine.evaluateGameStatus(newBoard, nextTurn)

                handleStatusChange(newBoard, evalStatus, _uiState.value.gameMode)
            } else {
                // Failsafe in case of any impossible state
                _uiState.update {
                    it.copy(gameStatus = GameStatus.InProgress(currentTurn = Player.X, isAiThinking = false))
                }
            }
        }
    }

    private fun handleStatusChange(board: List<Player?>, status: GameStatus, mode: GameMode) {
        when (status) {
            is GameStatus.Victory -> {
                soundManager.playWin()
                hapticManager.vibrateWin()

                if (status.winner == Player.X) {
                    prefs.incrementXWin()
                } else {
                    prefs.incrementOWin()
                }
                prefs.clearSavedGameSession()

                _uiState.update {
                    it.copy(
                        board = board,
                        gameStatus = status,
                        score = prefs.getScore(),
                        hasSavedGame = false
                    )
                }
            }
            is GameStatus.Draw -> {
                soundManager.playDraw()
                hapticManager.vibrateDraw()

                prefs.incrementDraw()
                prefs.clearSavedGameSession()

                _uiState.update {
                    it.copy(
                        board = board,
                        gameStatus = status,
                        score = prefs.getScore(),
                        hasSavedGame = false
                    )
                }
            }
            is GameStatus.InProgress -> {
                saveCurrentSession(board, status.currentTurn, mode)
                _uiState.update {
                    it.copy(
                        board = board,
                        gameStatus = status,
                        hasSavedGame = true
                    )
                }
            }
        }
    }

    private fun saveCurrentSession(board: List<Player?>, turn: Player, mode: GameMode) {
        val modeStr = when (mode) {
            is GameMode.TwoPlayer -> "TWO_PLAYER"
            is GameMode.VsAi -> "VS_AI_${mode.difficulty.name}"
        }
        prefs.saveGameSession(board, turn, modeStr)
    }

    fun resetScore() {
        soundManager.playClick()
        prefs.resetScore()
        _uiState.update { it.copy(score = prefs.getScore()) }
    }

    fun setMusicEnabled(enabled: Boolean) {
        soundManager.playClick()
        prefs.isMusicEnabled = enabled
        soundManager.isMusicEnabled = enabled
        _uiState.update { it.copy(isMusicEnabled = enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.isSoundEnabled = enabled
        soundManager.isSoundEnabled = enabled
        if (enabled) soundManager.playClick()
        _uiState.update { it.copy(isSoundEnabled = enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        soundManager.playClick()
        prefs.isVibrationEnabled = enabled
        hapticManager.isVibrationEnabled = enabled
        if (enabled) hapticManager.vibrateMove()
        _uiState.update { it.copy(isVibrationEnabled = enabled) }
    }

    fun setDifficulty(difficulty: Difficulty) {
        soundManager.playClick()
        prefs.selectedDifficulty = difficulty
        _uiState.update { it.copy(difficulty = difficulty) }
    }
}
