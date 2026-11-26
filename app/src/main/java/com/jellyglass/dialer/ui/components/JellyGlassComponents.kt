package com.jellyglass.dialer.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jellyglass.dialer.ui.theme.GlassThemeStyle

val LocalLiquidGlassIntensity = compositionLocalOf { 0.70f }
val LocalJellyBounceIntensity = compositionLocalOf { 0.75f }

fun Modifier.jellyBounce(
    pressedScale: Float = 0.88f,
    enableHaptic: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val bounceIntensity = LocalJellyBounceIntensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val latestOnClick by rememberUpdatedState(onClick)

    val damping = (0.88f - (bounceIntensity * 0.56f)).coerceIn(0.30f, 0.92f)
    val stiffness = Spring.StiffnessMediumLow + (bounceIntensity * 260f)
    val dynamicPressedScale = 1f - ((1f - pressedScale) * (0.35f + bounceIntensity * 0.95f))

    val scale by animateFloatAsState(
        targetValue = if (isPressed) dynamicPressedScale else 1f,
        animationSpec = spring(
            dampingRatio = damping,
            stiffness = stiffness
        ),
        label = "jellyScale"
    )

    val stretchX by animateFloatAsState(
        targetValue = if (isPressed) (1f + 0.06f * bounceIntensity) else 1f,
        animationSpec = spring(
            dampingRatio = damping,
            stiffness = Spring.StiffnessLow
        ),
        label = "jellyStretchX"
    )

    this
        .graphicsLayer {
            scaleX = scale * stretchX
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                if (enableHaptic) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                latestOnClick()
            }
        )
}

@Composable
fun FrostedGlassCard(
    theme: GlassThemeStyle,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    elevation: Dp = 8.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val glassIntensity = LocalLiquidGlassIntensity.current
    val alphaScale = (0.45f + glassIntensity * 0.85f).coerceIn(0.35f, 1.35f)
    val borderWidth = (0.75f + glassIntensity * 0.95f).dp

    val topBorderAlpha = (theme.glassBorderTop.alpha * alphaScale).coerceIn(0.12f, 0.95f)
    val bottomBorderAlpha = (theme.glassBorderBottom.alpha * alphaScale).coerceIn(0.06f, 0.65f)
    val surfaceAlpha = (theme.glassSurface.alpha * alphaScale).coerceIn(0.10f, 0.88f)

    val specularBrush = Brush.verticalGradient(
        colors = listOf(
            theme.glassBorderTop.copy(alpha = topBorderAlpha),
            theme.glassBorderBottom.copy(alpha = bottomBorderAlpha)
        )
    )
    val innerSheen = Brush.linearGradient(
        colors = listOf(
            theme.glassSurface.copy(alpha = surfaceAlpha),
            theme.glassSurface.copy(alpha = surfaceAlpha * 0.52f),
            theme.accent.copy(alpha = 0.06f * glassIntensity)
        )
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = (elevation.value * (0.5f + glassIntensity * 0.7f)).dp,
                shape = shape,
                ambientColor = theme.accent.copy(alpha = 0.16f * glassIntensity),
                spotColor = Color.Black.copy(alpha = if (theme.isDark) 0.45f else 0.12f)
            )
            .clip(shape)
            .background(innerSheen)
            .border(BorderStroke(borderWidth, specularBrush), shape)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun WhatsAppBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF25D366).copy(alpha = 0.18f))
            .border(1.dp, Color(0xFF25D366).copy(alpha = 0.65f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF25D366))
        )
        Text(
            text = "WhatsApp",
            color = Color(0xFF16A34A),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
