package com.jellyglass.dialer

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jellyglass.dialer.data.AiEngineMode
import com.jellyglass.dialer.data.SettingsStore
import com.jellyglass.dialer.telecom.CallStateManager
import com.jellyglass.dialer.ui.components.LocalJellyBounceIntensity
import com.jellyglass.dialer.ui.components.LocalLiquidGlassIntensity
import com.jellyglass.dialer.ui.screens.InCallScreen
import com.jellyglass.dialer.ui.theme.CallScreenStyle
import com.jellyglass.dialer.ui.theme.GlassThemeStyle
import com.jellyglass.dialer.ui.theme.IconPackStyle

class CallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = SettingsStore(this)

        setContent {
            val callState by CallStateManager.uiState.collectAsState()
            val theme by store.themeFlow.collectAsState(initial = GlassThemeStyle.LIQUID_AURORA)
            val iconPack by store.iconPackFlow.collectAsState(initial = IconPackStyle.IOS_LIQUID)
            val callStyle by store.callStyleFlow.collectAsState(initial = CallScreenStyle.IOS_26_LIQUID)
            val aiMode by store.aiModeFlow.collectAsState(initial = AiEngineMode.HYBRID)
            val apiKey by store.apiKeyFlow.collectAsState(initial = "")
            val liquidGlass by store.liquidGlassFlow.collectAsState(initial = 0.70f)
            val jellyBounce by store.jellyBounceFlow.collectAsState(initial = 0.75f)
            val highRefresh by store.highRefreshFlow.collectAsState(initial = true)

            LaunchedEffect(highRefresh) {
                applyPreferredRefreshRate(this@CallActivity, highRefresh)
            }

            CompositionLocalProvider(
                LocalLiquidGlassIntensity provides liquidGlass,
                LocalJellyBounceIntensity provides jellyBounce
            ) {
                InCallScreen(
                    callState = callState,
                    theme = theme,
                    iconPack = iconPack,
                    callStyle = callStyle,
                    aiMode = aiMode,
                    apiKey = apiKey,
                    onDismissScreen = {
                        CallStateManager.clearCallState()
                        finishAndRemoveTask()
                    }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!CallStateManager.uiState.value.hasActiveCall) {
            finishAndRemoveTask()
        }
    }
}

fun applyPreferredRefreshRate(activity: Activity, enableHighHz: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        } ?: return

        val modes = currentDisplay.supportedModes
        val targetMode = if (enableHighHz) {
            modes.maxWithOrNull(compareBy({ it.physicalWidth * it.physicalHeight }, { it.refreshRate }))
        } else {
            modes.minByOrNull { it.refreshRate }
        }

        targetMode?.let { mode ->
            val params = activity.window.attributes
            params.preferredDisplayModeId = mode.modeId
            activity.window.attributes = params
        }
    }
}
