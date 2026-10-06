package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Difficulty
import com.example.model.GameMode
import com.example.online.model.OnlineUser
import com.example.ui.components.GameBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    selectedDifficulty: Difficulty,
    hasSavedGame: Boolean,
    onStartTwoPlayer: () -> Unit,
    onStartVsAi: (Difficulty) -> Unit,
    onContinueTapped: () -> Unit,
    onOpenOnlinePlay: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHowToPlay: () -> Unit,
    onOpenDeveloperSection: () -> Unit,
    modifier: Modifier = Modifier,
    currentUser: OnlineUser? = null
) {
    var showAiDifficultyOptions by remember { mutableStateOf(false) }
    var currentAiDifficulty by remember { mutableStateOf(selectedDifficulty) }
    val coroutineScope = rememberCoroutineScope()
    val settingsHoldProgress = remember { Animatable(0f) }
    var isSettingsPressed by remember { mutableStateOf(false) }

    GameBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Player Profile Mini Badge
            if (currentUser != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceVariant.copy(alpha = 0.7f))
                        .border(1.dp, Color(0xFF283854), RoundedCornerShape(20.dp))
                        .clickable { onOpenSettings() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("home_user_chip"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(NeonPurple.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = currentUser.displayName.ifBlank { "Player" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (currentUser.authType == "google") "• Google" else "• Guest",
                        fontSize = 11.sp,
                        color = if (currentUser.authType == "google") NeonCyan else TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Hero Game Title & Stylized Logo
            HeroLogo()

            Spacer(modifier = Modifier.height(36.dp))

            // Main Actions Container
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Two Player Button
                Button(
                    onClick = onStartTwoPlayer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("play_button")
                        .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = NeonCyan),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF06151A)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "PLAY (TWO PLAYER)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                // PLAY ONLINE (Multiplayer with Friend) Button
                Button(
                    onClick = onOpenOnlinePlay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("play_online_button")
                        .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = Color(0xFF00E676)),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E676),
                        contentColor = Color(0xFF061A0F)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "PLAY ONLINE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                // Play Vs AI Button with expandable difficulty
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            showAiDifficultyOptions = !showAiDifficultyOptions
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .testTag("play_ai_button")
                            .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = NeonPurple),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "PLAY VS AI (${currentAiDifficulty.label.uppercase()})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Difficulty selector options
                    AnimatedVisibility(
                        visible = showAiDifficultyOptions,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurface.copy(alpha = 0.95f))
                                .border(1.dp, NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "SELECT AI DIFFICULTY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonPurple,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Difficulty.values().forEach { diff ->
                                    val isSelected = currentAiDifficulty == diff
                                    val tag = when (diff) {
                                        Difficulty.EASY -> "difficulty_easy"
                                        Difficulty.MEDIUM -> "difficulty_medium"
                                        Difficulty.HARD -> "difficulty_hard"
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) NeonPurple else Color(0xFF1B2538)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) Color.White else Color(0xFF2C3C58),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .testTag(tag)
                                            .clickable {
                                                currentAiDifficulty = diff
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = diff.label,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }

                            // Start match vs AI button
                            Button(
                                onClick = {
                                    onStartVsAi(currentAiDifficulty)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .padding(top = 4.dp)
                                    .testTag("start_ai_match_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00E676),
                                    contentColor = Color(0xFF061A0F)
                                )
                            ) {
                                Text(
                                    text = "START VS ${currentAiDifficulty.label.uppercase()} AI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // CONTINUE Button (Handles normal continue or 10-tap Developer easter egg)
                OutlinedButton(
                    onClick = onContinueTapped,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("continue_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                        ),
                        width = 1.5.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color(0xFF60A5FA)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasSavedGame) "CONTINUE SAVED GAME" else "CONTINUE",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // Settings & How To Play row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Settings Button (Tap for Settings, Press for 5 sec for Developer Section)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSettingsPressed) DarkSurfaceVariant else DarkSurface.copy(alpha = 0.6f)
                            )
                            .border(
                                width = if (settingsHoldProgress.value > 0.05f) 2.dp else 1.dp,
                                brush = if (settingsHoldProgress.value > 0.05f) {
                                    Brush.horizontalGradient(
                                        listOf(
                                            NeonCyan,
                                            Color(0xFF38BDF8)
                                        )
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF283854), Color(0xFF1E2B40))
                                    )
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .testTag("settings_button")
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    isSettingsPressed = true
                                    var developerTriggered = false

                                    val timerJob = coroutineScope.launch {
                                        settingsHoldProgress.animateTo(
                                            targetValue = 1f,
                                            animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
                                        )
                                        developerTriggered = true
                                        onOpenDeveloperSection()
                                    }

                                    val up = waitForUpOrCancellation()
                                    timerJob.cancel()
                                    coroutineScope.launch {
                                        settingsHoldProgress.snapTo(0f)
                                    }
                                    isSettingsPressed = false

                                    if (!developerTriggered && up != null) {
                                        onOpenSettings()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Subtle progress fill indicator when holding
                        if (settingsHoldProgress.value > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                NeonCyan.copy(alpha = 0.30f * settingsHoldProgress.value),
                                                NeonCyan.copy(alpha = 0.10f * settingsHoldProgress.value)
                                            )
                                        )
                                    )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(20.dp),
                                tint = if (settingsHoldProgress.value > 0.3f) NeonCyan else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SETTINGS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (settingsHoldProgress.value > 0.3f) TextPrimary else TextSecondary
                            )
                        }
                    }

                    // How To Play Button
                    OutlinedButton(
                        onClick = onOpenHowToPlay,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("how_to_play_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF283854), Color(0xFF1E2B40))
                            )
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "How To Play",
                            modifier = Modifier.size(20.dp),
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "HOW TO PLAY",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Modern Mobile Gaming Edition",
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HeroLogo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing Neon Symbol Badge
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NeonCyan.copy(alpha = 0.15f))
                    .border(2.dp, NeonCyan, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "X",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonCyan
                )
            }

            Text(
                text = "VS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NeonPink.copy(alpha = 0.15f))
                    .border(2.dp, NeonPink, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "O",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonPink
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "TIC TAC TOE",
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            letterSpacing = 3.sp
        )

        Text(
            text = "Classic Board • Smart AI • Offline Play",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
