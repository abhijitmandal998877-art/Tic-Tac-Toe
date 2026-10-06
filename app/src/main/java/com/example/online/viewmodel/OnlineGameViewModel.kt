package com.example.online.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.online.model.OnlineRoom
import com.example.online.model.OnlineSubScreen
import com.example.online.model.OnlineUser
import com.example.online.repository.FirebaseManager
import com.example.util.HapticManager
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnlineUiState(
    val subScreen: OnlineSubScreen = OnlineSubScreen.MENU,
    val currentUser: OnlineUser? = null,
    val currentRoom: OnlineRoom? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSubmittingMove: Boolean = false,
    val opponentLeft: Boolean = false,
    val lastMoveCell: Int? = null
)

class OnlineGameViewModel(application: Application) : AndroidViewModel(application) {

    private val soundManager = SoundManager()
    private val hapticManager = HapticManager(application)

    private val _uiState = MutableStateFlow(OnlineUiState())
    val uiState: StateFlow<OnlineUiState> = _uiState.asStateFlow()

    private var roomObserveJob: Job? = null
    private var profileObserveJob: Job? = null
    private var lastRecordedWinner: String = ""

    init {
        initializeOnline()
    }

    fun initializeOnline() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val uid = FirebaseManager.ensureAuthenticated()
            if (uid != null) {
                observeProfile(uid)
                _uiState.update { it.copy(isLoading = false) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not connect to Firebase online services. Please check your internet connection."
                    )
                }
            }
        }
    }

    private fun observeProfile(uid: String) {
        profileObserveJob?.cancel()
        profileObserveJob = viewModelScope.launch {
            FirebaseManager.observeUserProfile(uid).collect { profile ->
                _uiState.update { it.copy(currentUser = profile) }
            }
        }
    }

    fun updateDisplayName(newName: String) {
        val uid = FirebaseManager.getCurrentUid() ?: return
        viewModelScope.launch {
            FirebaseManager.updateDisplayName(uid, newName)
        }
    }

    fun createRoom() {
        val user = _uiState.value.currentUser
        val displayName = user?.displayName ?: "Player"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = FirebaseManager.createRoom(displayName)
            if (result != null) {
                val (roomId, _) = result
                soundManager.playClick()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        subScreen = OnlineSubScreen.CREATE_LOBBY,
                        opponentLeft = false
                    )
                }
                observeRoom(roomId)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to create room. Please check your internet connection."
                    )
                }
            }
        }
    }

    fun joinRoom(code: String) {
        val user = _uiState.value.currentUser
        val displayName = user?.displayName ?: "Player"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val joinResult = FirebaseManager.joinRoom(code, displayName)
            joinResult.fold(
                onSuccess = { roomId ->
                    soundManager.playClick()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            subScreen = OnlineSubScreen.IN_GAME,
                            opponentLeft = false
                        )
                    }
                    observeRoom(roomId)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to join room."
                        )
                    }
                }
            )
        }
    }

    private fun observeRoom(roomId: String) {
        roomObserveJob?.cancel()
        lastRecordedWinner = ""

        roomObserveJob = viewModelScope.launch {
            FirebaseManager.observeRoom(roomId).collect { room ->
                if (room == null) {
                    return@collect
                }

                val myUid = FirebaseManager.getCurrentUid() ?: ""
                val opponent = room.getOpponent(myUid)

                // Detect opponent disconnect / leaving
                val isOpponentGone = (room.status == "ABANDONED" || (opponent != null && !opponent.online && room.status == "PLAYING"))

                // Sound & haptic triggers on game end
                if (room.winner.isNotEmpty() && room.winner != lastRecordedWinner) {
                    lastRecordedWinner = room.winner
                    val myRole = room.getMyRole(myUid)
                    if (room.winner == "DRAW") {
                        soundManager.playDraw()
                        hapticManager.vibrateDraw()
                    } else if (room.winner == myRole) {
                        soundManager.playWin()
                        hapticManager.vibrateWin()
                    } else {
                        soundManager.playDraw()
                        hapticManager.vibrateMove()
                    }
                }

                // Auto-transition from LOBBY to IN_GAME when opponent joins
                val newSubScreen = when {
                    _uiState.value.subScreen == OnlineSubScreen.CREATE_LOBBY && room.status == "PLAYING" -> {
                        soundManager.playWin()
                        OnlineSubScreen.IN_GAME
                    }
                    else -> _uiState.value.subScreen
                }

                _uiState.update {
                    it.copy(
                        currentRoom = room,
                        subScreen = newSubScreen,
                        opponentLeft = isOpponentGone
                    )
                }
            }
        }
    }

    fun onCellClicked(index: Int) {
        val state = _uiState.value
        val room = state.currentRoom ?: return
        val myUid = FirebaseManager.getCurrentUid() ?: return

        if (state.isSubmittingMove) return
        if (!room.isMyTurn(myUid)) return
        if (index !in 0..8 || room.board[index].isNotEmpty()) return

        val myRole = room.getMyRole(myUid) ?: return

        // Audio & vibration immediate feedback
        if (myRole == "X") soundManager.playXMove() else soundManager.playOMove()
        hapticManager.vibrateMove()

        _uiState.update { it.copy(isSubmittingMove = true, lastMoveCell = index) }

        viewModelScope.launch {
            val success = FirebaseManager.makeMove(room.roomId, index, room)
            _uiState.update { it.copy(isSubmittingMove = false) }
            if (!success) {
                _uiState.update { it.copy(errorMessage = "Move failed. Check connection.") }
            }
        }
    }

    fun requestRematch() {
        val room = _uiState.value.currentRoom ?: return
        soundManager.playClick()
        viewModelScope.launch {
            FirebaseManager.requestRematch(room.roomId, room)
        }
    }

    fun leaveRoom() {
        soundManager.playClick()
        val room = _uiState.value.currentRoom
        if (room != null) {
            viewModelScope.launch {
                FirebaseManager.leaveRoom(room.roomId, room)
            }
        }
        roomObserveJob?.cancel()
        roomObserveJob = null
        _uiState.update {
            it.copy(
                subScreen = OnlineSubScreen.MENU,
                currentRoom = null,
                opponentLeft = false
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private var matchmakingListener: ValueEventListener? = null
    var matchedOpponentName: String = "Opponent"
        private set

    fun startQuickMatch() {
        val user = _uiState.value.currentUser
        val displayName = user?.displayName ?: "Player"
        soundManager.playClick()
        _uiState.update { it.copy(subScreen = OnlineSubScreen.MATCHMAKING_SEARCHING, errorMessage = null) }
        viewModelScope.launch {
            matchmakingListener = FirebaseManager.enterMatchmakingQueue(
                displayName = displayName,
                onMatched = { roomId, opponentName ->
                    matchedOpponentName = opponentName
                    soundManager.playWin()
                    _uiState.update { it.copy(subScreen = OnlineSubScreen.MATCH_FOUND) }
                    observeRoom(roomId)
                },
                onError = { err ->
                    _uiState.update {
                        it.copy(
                            subScreen = OnlineSubScreen.MENU,
                            errorMessage = err
                        )
                    }
                }
            )
        }
    }

    fun cancelMatchmaking() {
        soundManager.playClick()
        viewModelScope.launch {
            FirebaseManager.cancelMatchmaking(matchmakingListener)
            matchmakingListener = null
            _uiState.update { it.copy(subScreen = OnlineSubScreen.MENU) }
        }
    }

    fun navigateToSubScreen(sub: OnlineSubScreen) {
        soundManager.playClick()
        _uiState.update { it.copy(subScreen = sub, errorMessage = null) }
    }
}
