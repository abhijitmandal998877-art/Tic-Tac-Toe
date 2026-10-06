package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameMode
import com.example.model.GameStatus
import com.example.model.Player
import com.example.model.Score
import com.example.ui.components.BoardCell
import com.example.ui.components.GameBackground
import com.example.ui.components.ScoreBoard
import com.example.ui.components.WinningOverlay
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GameScreen(
    board: List<Player?>,
    gameStatus: GameStatus,
    gameMode: GameMode,
    score: Score,
    onCellClicked: (Int) -> Unit,
    onPlayAgain: () -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateHome()
    }

    val winningIndices: List<Int> = when (gameStatus) {
        is GameStatus.Victory -> gameStatus.winningLine
        else -> emptyList()
    }

    val isGameOver = gameStatus is GameStatus.Victory || gameStatus is GameStatus.Draw
    val isAiThinking = (gameStatus as? GameStatus.InProgress)?.isAiThinking == true

    GameBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            GameHeader(
                gameMode = gameMode,
                onBack = onNavigateHome,
                onRestart = onPlayAgain
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Scoreboard
            ScoreBoard(
                score = score,
                modifier = Modifier.widthIn(max = 420.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Turn & Status Banner
            TurnBanner(
                gameStatus = gameStatus,
                gameMode = gameMode,
                modifier = Modifier.widthIn(max = 420.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3x3 Game Board
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp),
                contentAlignment = Alignment.Center
            ) {
                val boardShape = RoundedCornerShape(24.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(boardShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF0F182A),
                                    Color(0xFF0C1322)
                                )
                            )
                        )
                        .border(1.5.dp, Color(0xFF233550), boardShape)
                        .padding(10.dp)
                        .testTag("game_board"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (row in 0..2) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (col in 0..2) {
                                    val index = row * 3 + col
                                    BoardCell(
                                        index = index,
                                        player = board[index],
                                        isWinningCell = index in winningIndices,
                                        isBoardDisabled = isGameOver || isAiThinking,
                                        onClick = { onCellClicked(index) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Winning / Draw Result Overlay Card
            WinningOverlay(
                gameStatus = gameStatus,
                onPlayAgain = onPlayAgain,
                onHome = onNavigateHome,
                modifier = Modifier.widthIn(max = 420.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GameHeader(
    gameMode: GameMode,
    onBack: () -> Unit,
    onRestart: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant.copy(alpha = 0.8f))
                .testTag("back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Return to Home",
                tint = TextPrimary
            )
        }

        // Mode Pill
        val (modeText, modeColor) = when (gameMode) {
            is GameMode.TwoPlayer -> Pair("2-PLAYER LOCAL", NeonCyan)
            is GameMode.VsAi -> Pair("VS AI (${gameMode.difficulty.label.uppercase()})", NeonPurple)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(modeColor.copy(alpha = 0.15f))
                .border(1.dp, modeColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (gameMode is GameMode.VsAi) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = modeColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = modeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = modeColor,
                    letterSpacing = 1.sp
                )
            }
        }

        IconButton(
            onClick = onRestart,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant.copy(alpha = 0.8f))
                .testTag("restart_button")
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Restart Game",
                tint = TextPrimary
            )
        }
    }
}

@Composable
private fun TurnBanner(
    gameStatus: GameStatus,
    gameMode: GameMode,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(pillShape)
            .background(DarkSurface.copy(alpha = 0.9f))
            .border(1.dp, Color(0xFF283854), pillShape)
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .testTag("turn_indicator"),
        contentAlignment = Alignment.Center
    ) {
        when (gameStatus) {
            is GameStatus.InProgress -> {
                if (gameStatus.isAiThinking) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = NeonPurple,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "AI is thinking...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                    }
                } else {
                    val turn = gameStatus.currentTurn
                    val (textColor, symbolText) = if (turn == Player.X) {
                        Pair(NeonCyan, "X's Turn")
                    } else {
                        Pair(NeonPink, if (gameMode is GameMode.VsAi) "AI's Turn (O)" else "O's Turn")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(textColor)
                        )
                        Text(
                            text = symbolText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            is GameStatus.Victory -> {
                Text(
                    text = "${gameStatus.winner.symbol} WINS!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = if (gameStatus.winner == Player.X) NeonCyan else NeonPink,
                    letterSpacing = 1.sp
                )
            }
            is GameStatus.Draw -> {
                Text(
                    text = "MATCH DRAW!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF59E0B),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
