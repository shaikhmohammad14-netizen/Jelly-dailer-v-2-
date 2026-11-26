package com.jellyglass.dialer.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jellyglass.dialer.ui.theme.GlassThemeStyle

fun Modifier.jellyBounce(
    pressedScale: Float = 0.88f,
    enableHaptic: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.42f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "jellyScale"
    )

    val stretchX by animateFloatAsState(
        targetValue = if (isPressed) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = 0.40f,
            stiffness = Spring.StiffnessLow
        ),
        label = "jellyStretchX"
    )

    this
        .graphicsLayer {
            scaleX = scale * stretchX
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    if (enableHaptic) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = {
                    onClick()
                }
            )
        }
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
    val specularBrush = Brush.verticalGradient(
        colors = listOf(
            theme.glassBorderTop,
            theme.glassBorderBottom
        )
    )
    val innerSheen = Brush.linearGradient(
        colors = listOf(
            theme.glassSurface,
            theme.glassSurface.copy(alpha = theme.glassSurface.alpha * 0.55f)
        )
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = theme.accent.copy(alpha = 0.14f),
                spotColor = Color.Black.copy(alpha = if (theme.isDark) 0.45f else 0.12f)
            )
            .clip(shape)
            .background(innerSheen)
            .border(BorderStroke(1.15.dp, specularBrush), shape)
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
