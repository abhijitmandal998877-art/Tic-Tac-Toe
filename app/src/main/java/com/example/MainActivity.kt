package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.GameMode
import com.example.model.Screen
import com.example.ui.screens.DeveloperWebViewScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HowToPlayScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.online.OnlinePlayContainerScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TicTacToeApp()
            }
        }
    }
}

@Composable
fun TicTacToeApp(
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        when (uiState.currentScreen) {
            Screen.SPLASH -> {
                SplashScreen(
                    modifier = Modifier.padding(innerPadding)
                )
            }
            Screen.AUTH -> {
                AuthScreen(
                    isLoading = uiState.isAuthLoading,
                    errorMessage = uiState.authError,
                    onContinueAsGuest = {
                        viewModel.continueAsGuest()
                    },
                    onContinueWithGoogle = {
                        viewModel.continueWithGoogle(context)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            Screen.HOME -> {
                HomeScreen(
                    selectedDifficulty = uiState.difficulty,
                    hasSavedGame = uiState.hasSavedGame,
                    onStartTwoPlayer = {
                        viewModel.startNewGame(GameMode.TwoPlayer)
                    },
                    onStartVsAi = { difficulty ->
                        viewModel.setDifficulty(difficulty)
                        viewModel.startNewGame(GameMode.VsAi(difficulty))
                    },
                    onContinueTapped = {
                        viewModel.onContinueTapped()
                    },
                    onOpenOnlinePlay = {
                        viewModel.navigateTo(Screen.ONLINE)
                    },
                    onOpenSettings = {
                        viewModel.navigateTo(Screen.SETTINGS)
                    },
                    onOpenHowToPlay = {
                        viewModel.navigateTo(Screen.HOW_TO_PLAY)
                    },
                    onOpenDeveloperSection = {
                        viewModel.openDeveloperSection()
                    },
                    currentUser = uiState.currentUser,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            Screen.GAME -> {
                GameScreen(
                    board = uiState.board,
                    gameStatus = uiState.gameStatus,
                    gameMode = uiState.gameMode,
                    score = uiState.score,
                    onCellClicked = { index ->
                        viewModel.onCellClicked(index)
                    },
                    onPlayAgain = {
                        viewModel.playAgain()
                    },
                    onNavigateHome = {
                        viewModel.navigateTo(Screen.HOME)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            Screen.SETTINGS -> {
                SettingsScreen(
                    isMusicEnabled = uiState.isMusicEnabled,
                    isSoundEnabled = uiState.isSoundEnabled,
                    isVibrationEnabled = uiState.isVibrationEnabled,
                    onToggleMusic = { viewModel.setMusicEnabled(it) },
                    onToggleSound = { viewModel.setSoundEnabled(it) },
                    onToggleVibration = { viewModel.setVibrationEnabled(it) },
                    onResetScore = { viewModel.resetScore() },
                    onNavigateHowToPlay = { viewModel.navigateTo(Screen.HOW_TO_PLAY) },
                    onNavigateBack = { viewModel.navigateBack() },
                    currentUser = uiState.currentUser,
                    onSignOut = { viewModel.signOut() },
                    onLinkGoogle = { viewModel.continueWithGoogle(context) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            Screen.HOW_TO_PLAY -> {
                HowToPlayScreen(
                    onNavigateBack = { viewModel.navigateBack() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            Screen.DEVELOPER_WEBVIEW -> {
                DeveloperWebViewScreen(
                    onNavigateBack = { viewModel.navigateBack() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            Screen.ONLINE -> {
                OnlinePlayContainerScreen(
                    onNavigateBackToHome = { viewModel.navigateTo(Screen.HOME) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
