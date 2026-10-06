package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.Player
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPink

@Composable
fun BoardCell(
    index: Int,
    player: Player?,
    isWinningCell: Boolean,
    isBoardDisabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth entry scale animation for symbol
    val symbolScale = remember { Animatable(0f) }

    LaunchedEffect(player) {
        if (player != null) {
            symbolScale.snapTo(0f)
            symbolScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 450f)
            )
        } else {
            symbolScale.snapTo(0f)
        }
    }

    // Winning cell pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "win_cell_pulse")
    val winPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "win_pulse"
    )

    val currentScale = when {
        isWinningCell -> winPulse
        isPressed && player == null && !isBoardDisabled -> 0.94f
        else -> 1f
    }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isWinningCell -> Color(0x3510B981)
            player != null -> Color(0xFF131D33)
            else -> DarkSurface.copy(alpha = 0.85f)
        },
        animationSpec = tween(300),
        label = "cell_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isWinningCell -> NeonEmerald
            player == Player.X -> NeonCyan.copy(alpha = 0.5f)
            player == Player.O -> NeonPink.copy(alpha = 0.5f)
            else -> Color(0xFF283854)
        },
        animationSpec = tween(300),
        label = "cell_border"
    )

    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(6.dp)
            .scale(currentScale)
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = if (isWinningCell) 2.5.dp else 1.2.dp,
                color = borderColor,
                shape = shape
            )
            .testTag("cell_$index")
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = if (player == null) Color.White else Color.Transparent),
                enabled = !isBoardDisabled && player == null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (player != null) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .scale(symbolScale.value)
            ) {
                val w = size.width
                val h = size.height
                val strokeW = w * 0.14f

                if (player == Player.X) {
                    // Outer soft glow
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.35f),
                        start = Offset(0f, 0f),
                        end = Offset(w, h),
                        strokeWidth = strokeW * 1.6f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.35f),
                        start = Offset(w, 0f),
                        end = Offset(0f, h),
                        strokeWidth = strokeW * 1.6f,
                        cap = StrokeCap.Round
                    )
                    // Crisp main stroke
                    drawLine(
                        color = NeonCyan,
                        start = Offset(0f, 0f),
                        end = Offset(w, h),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = NeonCyan,
                        start = Offset(w, 0f),
                        end = Offset(0f, h),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                } else {
                    // Outer soft glow
                    drawCircle(
                        color = NeonPink.copy(alpha = 0.35f),
                        radius = (w / 2f) - (strokeW / 2f),
                        center = Offset(w / 2f, h / 2f),
                        style = Stroke(width = strokeW * 1.6f)
                    )
                    // Crisp main ring
                    drawCircle(
                        color = NeonPink,
                        radius = (w / 2f) - (strokeW / 2f),
                        center = Offset(w / 2f, h / 2f),
                        style = Stroke(width = strokeW)
                    )
                }
            }
        }
    }
}
