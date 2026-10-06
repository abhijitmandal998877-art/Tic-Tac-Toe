package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun HowToPlayScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }

    GameBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant.copy(alpha = 0.8f))
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "HOW TO PLAY",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = 1.5.sp
                )
            }

            // Rule 1: Turn-Based Gameplay
            RuleCard(
                step = "1",
                icon = Icons.Default.TouchApp,
                iconColor = NeonCyan,
                title = "Take Turns",
                description = "Players alternate turns placing their symbol on the 3×3 grid. Player X always moves first, followed by Player O. Tap any unoccupied cell to claim it."
            )

            // Rule 2: Winning Condition
            RuleCard(
                step = "2",
                icon = Icons.Default.EmojiEvents,
                iconColor = NeonEmerald,
                title = "3 In A Row Wins",
                description = "The first player to align three of their symbols consecutively in a horizontal row, vertical column, or diagonal line wins the round immediately."
            )

            // Rule 3: Tie Game / Draw
            RuleCard(
                step = "3",
                icon = Icons.Default.Handshake,
                iconColor = NeonAmber,
                title = "Draw Condition",
                description = "If all 9 cells on the board become filled and neither player has achieved 3 in a row, the match concludes as a Draw."
            )

            // AI Difficulty Explanations
            RuleCard(
                step = "🤖",
                icon = Icons.Default.SmartToy,
                iconColor = NeonPurple,
                title = "AI Difficulty Levels",
                description = "• Easy: Makes random moves for light, casual gameplay.\n• Medium: Blocks opponent wins and looks for immediate winning opportunities.\n• Hard: Employs the minimax algorithm — highly intelligent and mathematically unbeatable."
            )

            // Pro Tip
            RuleCard(
                step = "💡",
                icon = Icons.Default.Lightbulb,
                iconColor = NeonPink,
                title = "Strategy Pro Tip",
                description = "Claiming the center cell (cell 5) or corner cells early gives you the maximum number of potential winning pathways. Always watch your opponent's moves to block early traps!"
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun RuleCard(
    step: String,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DarkSurface.copy(alpha = 0.85f))
            .border(1.dp, Color(0xFF283854), shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.15f))
                    .border(1.dp, iconColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Step $step",
                    fontSize = 12.sp,
                    color = iconColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            text = description,
            fontSize = 14.sp,
            color = TextSecondary,
            lineHeight = 20.sp
        )
    }
}
