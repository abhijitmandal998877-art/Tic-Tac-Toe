package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.DarkBackground

@Composable
fun GameBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Background subtle gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF090D16),
                        Color(0xFF10172A),
                        Color(0xFF060A12)
                    )
                )
            )

            // Subtle glowing orb top-left (Neon Cyan glow)
            val orb1Radius = width * 0.55f + (pulseAnim * 30f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x1800F0FF),
                        Color(0x0600F0FF),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.15f, height * 0.2f),
                    radius = orb1Radius
                ),
                radius = orb1Radius,
                center = Offset(width * 0.15f, height * 0.2f)
            )

            // Subtle glowing orb bottom-right (Neon Pink glow)
            val orb2Radius = width * 0.6f - (pulseAnim * 30f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x15FF2A85),
                        Color(0x05FF2A85),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.85f, height * 0.75f),
                    radius = orb2Radius
                ),
                radius = orb2Radius,
                center = Offset(width * 0.85f, height * 0.75f)
            )

            // Subtle geometric gaming grid lines in background
            val spacing = 72f
            var x = 0f
            while (x < width) {
                drawLine(
                    color = Color(0x07FFFFFF),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += spacing
            }

            var y = 0f
            while (y < height) {
                drawLine(
                    color = Color(0x07FFFFFF),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += spacing
            }
        }

        content()
    }
}
