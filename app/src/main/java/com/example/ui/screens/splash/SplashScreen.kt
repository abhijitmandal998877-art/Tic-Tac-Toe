package com.example.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GameBackground
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.7f) }
    val glowAlpha = remember { Animatable(0.4f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        glowAlpha.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    GameBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("splash_screen"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Stylized Animated Logo Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.scale(scale.value)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(2.5.dp, NeonCyan, RoundedCornerShape(20.dp))
                        .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = NeonCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "X",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan
                    )
                }

                Text(
                    text = "VS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NeonPink.copy(alpha = 0.15f))
                        .border(2.5.dp, NeonPink, RoundedCornerShape(20.dp))
                        .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = NeonPink),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "O",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonPink
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "TIC TAC TOE",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "MODERN ESPORTS EDITION",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan.copy(alpha = glowAlpha.value),
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = NeonCyan,
                strokeWidth = 2.5.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Loading game session...",
                fontSize = 12.sp,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
        }
    }
}
