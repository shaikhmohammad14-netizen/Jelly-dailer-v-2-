package com.jellyglass.dialer.telecom

import android.telecom.Call
import android.telecom.InCallService

class JellyInCallService : InCallService() {
    override fun onCreate() {
        super.onCreate()
        CallStateManager.attachService(this)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallStateManager.onCallAdded(this, call)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallStateManager.onCallRemoved(call)
    }

    override fun onDestroy() {
        CallStateManager.detachService()
        super.onDestroy()
    }
}
