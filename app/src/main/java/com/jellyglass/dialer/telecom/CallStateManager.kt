package com.jellyglass.dialer.telecom

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import com.jellyglass.dialer.CallActivity
import com.jellyglass.dialer.ai.SpamAndAiEngine
import com.jellyglass.dialer.ai.SpamVerdict
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveCallUiState(
    val hasActiveCall: Boolean = false,
    val callerName: String = "",
    val callerNumber: String = "",
    val callState: Int = Call.STATE_DISCONNECTED,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isOnHold: Boolean = false,
    val connectTimeMillis: Long = 0L,
    val spamVerdict: SpamVerdict? = null,
    val isDemoPreview: Boolean = false
)

object CallStateManager {
    private var currentCall: Call? = null
    private var inCallService: JellyInCallService? = null

    private val _uiState = MutableStateFlow(ActiveCallUiState())
    val uiState: StateFlow<ActiveCallUiState> = _uiState.asStateFlow()

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            syncFromCall(call, state)
            if (state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) {
                clearCallState()
            }
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            syncFromCall(call, call.state)
        }
    }

    fun attachService(service: JellyInCallService) {
        inCallService = service
    }

    fun detachService() {
        inCallService = null
        clearCallState()
    }

    fun onCallAdded(context: Context, call: Call) {
        currentCall?.unregisterCallback(callCallback)
        currentCall = call
        call.registerCallback(callCallback)
        syncFromCall(call, call.state)

        val intent = Intent(context, CallActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
        runCatching { context.startActivity(intent) }
    }

    fun onCallRemoved(call: Call) {
        if (currentCall == call) {
            call.unregisterCallback(callCallback)
            currentCall = null
            clearCallState()
        }
    }

    private fun syncFromCall(call: Call, state: Int) {
        if (state == Call.STATE_DISCONNECTED) {
            clearCallState()
            return
        }
        val handle = call.details?.handle
        val number = handle?.schemeSpecificPart.orEmpty()
        val displayName = call.details?.callerDisplayName
            ?.takeIf { it.isNotBlank() }
            ?: call.details?.contactDisplayName
                ?.takeIf { it.isNotBlank() }
            ?: number.ifBlank { "Unknown Caller" }

        val verdict = SpamAndAiEngine.analyzeOfflineSpam(number, displayName)
        val connectTime = call.details?.connectTimeMillis ?: 0L

        _uiState.value = _uiState.value.copy(
            hasActiveCall = true,
            callerName = displayName,
            callerNumber = number,
            callState = state,
            isOnHold = state == Call.STATE_HOLDING,
            connectTimeMillis = if (connectTime > 0L) connectTime else System.currentTimeMillis(),
            spamVerdict = verdict,
            isDemoPreview = false
        )
    }

    fun clearCallState() {
        currentCall = null
        _uiState.value = ActiveCallUiState(hasActiveCall = false)
    }

    fun answerCall() {
        if (_uiState.value.isDemoPreview) {
            _uiState.value = _uiState.value.copy(
                callState = Call.STATE_ACTIVE,
                connectTimeMillis = System.currentTimeMillis()
            )
            return
        }
        currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun endOrRejectCall() {
        val call = currentCall
        if (call != null) {
            if (call.state == Call.STATE_RINGING) {
                call.reject(false, null)
            } else {
                call.disconnect()
            }
        }
        clearCallState()
    }

    fun toggleMute() {
        val next = !_uiState.value.isMuted
        inCallService?.setMuted(next)
        _uiState.value = _uiState.value.copy(isMuted = next)
    }

    fun toggleSpeaker() {
        val next = !_uiState.value.isSpeakerOn
        val route = if (next) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
        inCallService?.setAudioRoute(route)
        _uiState.value = _uiState.value.copy(isSpeakerOn = next)
    }

    fun toggleHold() {
        val call = currentCall ?: return
        if (_uiState.value.isOnHold) {
            call.unhold()
        } else {
            call.hold()
        }
    }

    fun playDtmf(digit: Char) {
        currentCall?.playDtmfTone(digit)
        currentCall?.stopDtmfTone()
    }

    fun placeCall(context: Context, rawNumber: String) {
        val cleaned = rawNumber.trim()
        if (cleaned.isBlank()) return
        val uri = Uri.fromParts("tel", cleaned, null)
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        runCatching {
            telecom?.placeCall(uri, Bundle())
        }.onFailure {
            val fallback = Intent(Intent.ACTION_CALL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }
        }
    }

    fun launchPreviewCall(context: Context, name: String, number: String, isIncoming: Boolean) {
        val verdict = SpamAndAiEngine.analyzeOfflineSpam(number, name)
        _uiState.value = ActiveCallUiState(
            hasActiveCall = true,
            callerName = name,
            callerNumber = number,
            callState = if (isIncoming) Call.STATE_RINGING else Call.STATE_ACTIVE,
            connectTimeMillis = System.currentTimeMillis(),
            spamVerdict = verdict,
            isDemoPreview = true
        )
        val intent = Intent(context, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(intent)
    }
}
