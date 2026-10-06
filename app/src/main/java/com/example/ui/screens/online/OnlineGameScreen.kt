package com.example.ui.screens.online

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Player
import com.example.online.model.OnlinePlayerData
import com.example.online.model.OnlineRoom
import com.example.ui.components.BoardCell
import com.example.ui.components.GameBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OnlineGameScreen(
    room: OnlineRoom?,
    myUid: String,
    opponentLeft: Boolean,
    onCellClicked: (Int) -> Unit,
    onRequestRematch: () -> Unit,
    onLeaveRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }

    BackHandler {
        showLeaveConfirmDialog = true
    }

    val myRole = room?.getMyRole(myUid) ?: "X"
    val isMyTurn = room?.isMyTurn(myUid) == true
    val isGameOver = room?.status == "FINISHED" || (room?.winner?.isNotEmpty() == true)

    val playerX = room?.playerX
    val playerO = room?.playerO
    val opponent = room?.getOpponent(myUid)

    val amIPlayerX = playerX?.uid == myUid
    val iRequestedRematch = if (amIPlayerX) playerX?.rematchRequested == true else playerO?.rematchRequested == true
    val opponentRequestedRematch = if (amIPlayerX) playerO?.rematchRequested == true else playerX?.rematchRequested == true

    GameBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { showLeaveConfirmDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .testTag("online_leave_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Leave Game",
                        tint = TextPrimary
                    )
                }

                // Room Code pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF142238))
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "ROOM: ${room?.roomCode ?: "------"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                }

                // Opponent presence indicator
                val isOpponentOnline = opponent?.online == true && !opponentLeft
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOpponentOnline) NeonEmerald else NeonPink)
                        )
                        Text(
                            text = if (isOpponentOnline) "Online" else "Disconnected",
                            fontSize = 11.sp,
                            color = if (isOpponentOnline) NeonEmerald else NeonPink,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Players Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlayerMatchCard(
                    role = "X",
                    player = playerX,
                    isMe = amIPlayerX,
                    isCurrentTurn = room?.currentTurn == "X" && !isGameOver,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                PlayerMatchCard(
                    role = "O",
                    player = playerO,
                    isMe = !amIPlayerX,
                    isCurrentTurn = room?.currentTurn == "O" && !isGameOver,
                    accentColor = NeonPink,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Turn Indicator Banner
            val turnText: String
            val turnColor: Color
            when {
                isGameOver -> {
                    turnText = "MATCH CONCLUDED"
                    turnColor = NeonEmerald
                }
                isMyTurn -> {
                    turnText = "YOUR TURN ($myRole)"
                    turnColor = if (myRole == "X") NeonCyan else NeonPink
                }
                else -> {
                    val oppName = opponent?.displayName ?: "Opponent"
                    turnText = "$oppName's TURN (${if (myRole == "X") "O" else "X"})"
                    turnColor = TextSecondary
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface.copy(alpha = 0.9f))
                    .border(1.dp, turnColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = turnText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = turnColor,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3x3 Board
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp),
                contentAlignment = Alignment.Center
            ) {
                val boardShape = RoundedCornerShape(24.dp)
                val boardList = room?.board ?: List(9) { "" }
                val winningLine = room?.winningLine ?: emptyList()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(boardShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0F182A), Color(0xFF0C1322))
                            )
                        )
                        .border(1.5.dp, Color(0xFF233550), boardShape)
                        .padding(10.dp)
                        .testTag("online_game_board"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (r in 0..2) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (c in 0..2) {
                                    val idx = r * 3 + c
                                    val cellStr = boardList.getOrElse(idx) { "" }
                                    val cellPlayer = when (cellStr) {
                                        "X" -> Player.X
                                        "O" -> Player.O
                                        else -> null
                                    }
                                    val isWinningCell = idx in winningLine

                                    BoardCell(
                                        index = idx,
                                        player = cellPlayer,
                                        isWinningCell = isWinningCell,
                                        isBoardDisabled = !isMyTurn || isGameOver || opponentLeft,
                                        onClick = { onCellClicked(idx) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Opponent Left Alert Card
            if (opponentLeft) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF2E1018))
                        .border(1.5.dp, NeonPink, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = NeonPink)
                            Text(text = "Opponent left the game", fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Button(
                            onClick = onLeaveRoom,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF090D16)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("back_to_menu_button")
                        ) {
                            Text("BACK TO ONLINE MENU", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Game End Rematch Card
            if (isGameOver && !opponentLeft) {
                OnlineEndGameCard(
                    winner = room?.winner ?: "",
                    myRole = myRole,
                    iRequestedRematch = iRequestedRematch,
                    opponentRequestedRematch = opponentRequestedRematch,
                    onRequestRematch = onRequestRematch,
                    onLeaveRoom = onLeaveRoom,
                    modifier = Modifier.widthIn(max = 420.dp)
                )
            }
        }
    }

    // Leave Game Confirmation Dialog
    if (showLeaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = false },
            title = { Text(text = "Leave Game?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    text = "Are you sure you want to exit this online match? Your opponent will be notified.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirmDialog = false
                        onLeaveRoom()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_leave_game_button")
                ) {
                    Text("LEAVE")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLeaveConfirmDialog = false }) {
                    Text("STAY")
                }
            },
            containerColor = Color(0xFF162136),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun PlayerMatchCard(
    role: String,
    player: OnlinePlayerData?,
    isMe: Boolean,
    isCurrentTurn: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val borderCol = if (isCurrentTurn) accentColor else Color(0xFF283854)

    Box(
        modifier = modifier
            .clip(shape)
            .background(DarkSurface.copy(alpha = 0.85f))
            .border(if (isCurrentTurn) 2.dp else 1.dp, borderCol, shape)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = role,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor
                )
                Text(
                    text = if (isMe) "(YOU)" else "",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }
            Text(
                text = player?.displayName ?: "Waiting...",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OnlineEndGameCard(
    winner: String,
    myRole: String,
    iRequestedRematch: Boolean,
    opponentRequestedRematch: Boolean,
    onRequestRematch: () -> Unit,
    onLeaveRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(22.dp)
    val isWin = winner == myRole
    val isDraw = winner == "DRAW"

    val (title, titleColor) = when {
        isDraw -> Pair("MATCH DRAW!", NeonAmber)
        isWin -> Pair("🏆 YOU WON!", NeonCyan)
        else -> Pair("DEFEAT", NeonPink)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, cardShape, spotColor = titleColor)
            .clip(cardShape)
            .background(DarkSurface.copy(alpha = 0.95f))
            .border(1.5.dp, titleColor.copy(alpha = 0.5f), cardShape)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = titleColor,
                letterSpacing = 1.sp
            )

            // Rematch status message
            if (opponentRequestedRematch && !iRequestedRematch) {
                Text(
                    text = "Opponent wants a rematch! Tap Rematch to play.",
                    fontSize = 13.sp,
                    color = NeonEmerald,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            } else if (iRequestedRematch && !opponentRequestedRematch) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                    Text(text = "Waiting for opponent to accept...", fontSize = 13.sp, color = TextSecondary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRequestRematch,
                    enabled = !iRequestedRematch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF06151A)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("online_rematch_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (iRequestedRematch) "WAITING..." else "REMATCH", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onLeaveRoom,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("online_exit_button")
                ) {
                    Text(text = "LEAVE ROOM", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
