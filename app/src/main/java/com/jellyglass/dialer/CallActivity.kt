package com.jellyglass.dialer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jellyglass.dialer.data.AiEngineMode
import com.jellyglass.dialer.data.SettingsStore
import com.jellyglass.dialer.telecom.CallStateManager
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

    override fun onStop() {
        super.onStop()
        if (!CallStateManager.uiState.value.hasActiveCall) {
            finishAndRemoveTask()
        }
    }
}
