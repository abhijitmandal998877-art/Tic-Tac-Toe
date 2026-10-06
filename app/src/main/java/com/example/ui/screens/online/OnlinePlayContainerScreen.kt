package com.example.ui.screens.online

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.online.model.OnlineSubScreen
import com.example.online.repository.FirebaseManager
import com.example.online.viewmodel.OnlineGameViewModel

@Composable
fun OnlinePlayContainerScreen(
    onNavigateBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnlineGameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showJoinDialog by remember { mutableStateOf(false) }

    val myUid = FirebaseManager.getCurrentUid() ?: ""

    BackHandler {
        when (uiState.subScreen) {
            OnlineSubScreen.MENU -> onNavigateBackToHome()
            OnlineSubScreen.CREATE_LOBBY -> viewModel.leaveRoom()
            OnlineSubScreen.IN_GAME -> viewModel.leaveRoom()
            OnlineSubScreen.JOIN_INPUT -> viewModel.navigateToSubScreen(OnlineSubScreen.MENU)
            OnlineSubScreen.MATCHMAKING_SEARCHING -> viewModel.cancelMatchmaking()
            OnlineSubScreen.MATCH_FOUND -> viewModel.leaveRoom()
        }
    }

    when (uiState.subScreen) {
        OnlineSubScreen.MENU, OnlineSubScreen.JOIN_INPUT -> {
            OnlineMenuScreen(
                user = uiState.currentUser,
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                onQuickMatch = { viewModel.startQuickMatch() },
                onCreateRoom = { viewModel.createRoom() },
                onOpenJoinDialog = { showJoinDialog = true },
                onUpdateDisplayName = { viewModel.updateDisplayName(it) },
                onBackToHome = onNavigateBackToHome,
                onDismissError = { viewModel.dismissError() },
                modifier = modifier
            )
        }
        OnlineSubScreen.MATCHMAKING_SEARCHING -> {
            MatchmakingSearchingScreen(
                onCancelSearch = { viewModel.cancelMatchmaking() },
                modifier = modifier
            )
        }
        OnlineSubScreen.MATCH_FOUND -> {
            MatchFoundScreen(
                opponentName = viewModel.matchedOpponentName,
                onCountdownFinished = { viewModel.navigateToSubScreen(OnlineSubScreen.IN_GAME) },
                modifier = modifier
            )
        }
        OnlineSubScreen.CREATE_LOBBY -> {
            CreateRoomLobbyScreen(
                room = uiState.currentRoom,
                onLeaveRoom = { viewModel.leaveRoom() },
                modifier = modifier
            )
        }
        OnlineSubScreen.IN_GAME -> {
            OnlineGameScreen(
                room = uiState.currentRoom,
                myUid = myUid,
                opponentLeft = uiState.opponentLeft,
                onCellClicked = { viewModel.onCellClicked(it) },
                onRequestRematch = { viewModel.requestRematch() },
                onLeaveRoom = { viewModel.leaveRoom() },
                modifier = modifier
            )
        }
    }

    if (showJoinDialog) {
        JoinRoomDialog(
            isLoading = uiState.isLoading,
            onDismiss = { showJoinDialog = false },
            onJoin = { code ->
                showJoinDialog = false
                viewModel.joinRoom(code)
            }
        )
    }
}
