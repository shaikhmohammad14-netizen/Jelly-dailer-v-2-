package com.jellyglass.dialer.ui.screens

import android.telecom.Call
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jellyglass.dialer.ai.SpamAndAiEngine
import com.jellyglass.dialer.data.AiEngineMode
import com.jellyglass.dialer.telecom.ActiveCallUiState
import com.jellyglass.dialer.telecom.CallStateManager
import com.jellyglass.dialer.ui.components.FrostedGlassCard
import com.jellyglass.dialer.ui.components.PremiumIcons
import com.jellyglass.dialer.ui.components.jellyBounce
import com.jellyglass.dialer.ui.theme.CallScreenStyle
import com.jellyglass.dialer.ui.theme.GlassThemeStyle
import com.jellyglass.dialer.ui.theme.IconPackStyle
import kotlinx.coroutines.delay

@Composable
fun InCallScreen(
    callState: ActiveCallUiState,
    theme: GlassThemeStyle,
    iconPack: IconPackStyle,
    callStyle: CallScreenStyle,
    aiMode: AiEngineMode,
    apiKey: String,
    onDismissScreen: () -> Unit
) {
    LaunchedEffect(callState.hasActiveCall, callState.callState) {
        if (!callState.hasActiveCall || callState.callState == Call.STATE_DISCONNECTED) {
            CallStateManager.clearCallState()
            onDismissScreen()
        }
    }

    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(callState.callState, callState.connectTimeMillis) {
        while (callState.callState == Call.STATE_ACTIVE) {
            elapsedSeconds = ((System.currentTimeMillis() - callState.connectTimeMillis) / 1000L).coerceAtLeast(0L)
            delay(1000L)
        }
    }

    var showInCallKeypad by remember { mutableStateOf(false) }
    var showAiScriptSheet by remember { mutableStateOf(false) }
    var aiScripts by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedAiBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(callState.callerName, showAiScriptSheet) {
        if (showAiScriptSheet) {
            val scenario = if (callState.spamVerdict?.isSpam == true) "Unknown / Spam Screen" else "Delivery / Courier"
            aiScripts = SpamAndAiEngine.generateCallScripts(scenario, callState.callerName, aiMode, apiKey)
        }
    }

    val isSpam = callState.spamVerdict?.isSpam == true
    val bgBrush = if (isSpam) {
        Brush.verticalGradient(
            listOf(Color(0xFF3B0712), Color(0xFF1F091D), Color(0xFF090D16))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFF081229), Color(0xFF132A4A), Color(0xFF070F1E))
        )
    }

    val pulseTransition = rememberInfiniteTransition(label = "avatarPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val statusText = when (callState.callState) {
        Call.STATE_RINGING -> if (isSpam) "AI Spam Shield • Incoming Alert" else "Incoming Liquid HD Call"
        Call.STATE_DIALING, Call.STATE_CONNECTING -> "Dialing via iGlass..."
        Call.STATE_HOLDING -> "Call On Hold"
        Call.STATE_ACTIVE -> String.format("%02d:%02d • HD Voice", elapsedSeconds / 60, elapsedSeconds % 60)
        else -> "Ending call..."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (callStyle == CallScreenStyle.DYNAMIC_ISLAND_PRO || callStyle == CallScreenStyle.COLOROS_17_FLUX) {
                    FrostedGlassCard(
                        theme = GlassThemeStyle.LIQUID_AURORA,
                        shape = RoundedCornerShape(32.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF38BDF8), Color(0xFF818CF8))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = callState.callerName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = callState.callerName,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${callState.callerNumber} • $statusText",
                                    color = Color(0xFFBAE6FD),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(16.dp))
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(124.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    (if (isSpam) Color(0xFFF43F5E) else Color(0xFF38BDF8)).copy(alpha = 0.22f)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        if (isSpam) listOf(Color(0xFFE11D48), Color(0xFF9F1239))
                                        else listOf(Color(0xFF0EA5E9), Color(0xFF6366F1))
                                    )
                                )
                                .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSpam) {
                                Icon(
                                    Icons.Rounded.GppBad,
                                    contentDescription = "Spam",
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            } else {
                                Text(
                                    text = callState.callerName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = callState.callerName,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = callState.callerNumber,
                        color = Color(0xFFCBD5E1),
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = if (isSpam) Color(0xFFFDA4AF) else Color(0xFF7DD3FC),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                callState.spamVerdict?.let { verdict ->
                    FrostedGlassCard(
                        theme = GlassThemeStyle.LIQUID_AURORA,
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (verdict.isSpam) Icons.Rounded.WarningAmber else Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = if (verdict.isSpam) Color(0xFFFB7185) else Color(0xFF34D399)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${verdict.category} (${verdict.sourceEngine})",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = verdict.reason,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.22f))
                            .border(1.dp, Color(0xFF7DD3FC).copy(alpha = 0.6f), CircleShape)
                            .jellyBounce { showAiScriptSheet = !showAiScriptSheet }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color(0xFF7DD3FC), modifier = Modifier.size(16.dp))
                            Text(
                                text = if (showAiScriptSheet) "Hide AI Call Script" else "AI Call Script & Assist",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = showAiScriptSheet) {
                    FrostedGlassCard(
                        theme = GlassThemeStyle.LIQUID_AURORA,
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Tap a live script to display/use:",
                                color = Color(0xFF7DD3FC),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(aiScripts) { script ->
                                    Box(
                                        modifier = Modifier
                                            .width(230.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.White.copy(alpha = 0.14f))
                                            .jellyBounce { selectedAiBanner = script }
                                            .padding(10.dp)
                                    ) {
                                        Text(text = script, color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
                                    }
                                }
                            }
                            selectedAiBanner?.let { activeScript ->
                                Text(
                                    text = "Active Prompt: “$activeScript”",
                                    color = Color(0xFF34D399),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showInCallKeypad) {
                FrostedGlassCard(
                    theme = GlassThemeStyle.LIQUID_AURORA,
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("*", "0", "#")
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rows.forEach { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                row.forEach { key ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.White.copy(alpha = 0.14f))
                                            .jellyBounce { CallStateManager.playDtmf(key[0]) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(key, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (callState.callState == Call.STATE_RINGING) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        GlassCallActionPill(
                            icon = Icons.Rounded.AutoAwesome,
                            label = "AI Screen",
                            active = showAiScriptSheet,
                            modifier = Modifier.weight(1f),
                            onClick = { showAiScriptSheet = !showAiScriptSheet }
                        )
                        GlassCallActionPill(
                            icon = Icons.Rounded.Sms,
                            label = "Quick Reply",
                            active = false,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                CallStateManager.endOrRejectCall()
                                onDismissScreen()
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(68.dp)
                                .clip(RoundedCornerShape(34.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626)))
                                )
                                .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(34.dp))
                                .jellyBounce {
                                    CallStateManager.endOrRejectCall()
                                    onDismissScreen()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(PremiumIcons.callEnd(iconPack), contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(26.dp))
                                Text("Decline", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(68.dp)
                                .clip(RoundedCornerShape(34.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
                                )
                                .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(34.dp))
                                .jellyBounce { CallStateManager.answerCall() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(PremiumIcons.callAnswer(iconPack), contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(26.dp))
                                Text("Accept", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCallActionPill(
                            icon = if (callState.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            label = if (callState.isMuted) "Unmute" else "Mute",
                            active = callState.isMuted,
                            modifier = Modifier.weight(1f),
                            onClick = { CallStateManager.toggleMute() }
                        )
                        GlassCallActionPill(
                            icon = Icons.Rounded.Dialpad,
                            label = "Keypad",
                            active = showInCallKeypad,
                            modifier = Modifier.weight(1f),
                            onClick = { showInCallKeypad = !showInCallKeypad }
                        )
                        GlassCallActionPill(
                            icon = if (callState.isSpeakerOn) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeDown,
                            label = "Speaker",
                            active = callState.isSpeakerOn,
                            modifier = Modifier.weight(1f),
                            onClick = { CallStateManager.toggleSpeaker() }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCallActionPill(
                            icon = Icons.Rounded.Pause,
                            label = if (callState.isOnHold) "Resume" else "Hold",
                            active = callState.isOnHold,
                            modifier = Modifier.weight(1f),
                            onClick = { CallStateManager.toggleHold() }
                        )
                        GlassCallActionPill(
                            icon = Icons.Rounded.RecordVoiceOver,
                            label = "AI Script",
                            active = showAiScriptSheet,
                            modifier = Modifier.weight(1f),
                            onClick = { showAiScriptSheet = !showAiScriptSheet }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(66.dp)
                            .clip(RoundedCornerShape(33.dp))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFF43F5E), Color(0xFFDC2626)))
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(33.dp))
                            .jellyBounce {
                                CallStateManager.endOrRejectCall()
                                onDismissScreen()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(PremiumIcons.callEnd(iconPack), contentDescription = "End Call", tint = Color.White, modifier = Modifier.size(28.dp))
                            Text("End Call", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassCallActionPill(
    icon: ImageVector,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (active) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.14f)
    val contentColor = if (active) Color(0xFF0F172A) else Color.White

    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.42f)), RoundedCornerShape(24.dp))
            .jellyBounce(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(22.dp))
            Text(label, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
