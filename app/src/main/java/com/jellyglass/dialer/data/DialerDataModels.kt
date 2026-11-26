package com.jellyglass.dialer.data

data class ContactItem(
    val id: String,
    val name: String,
    val phone: String,
    val hasWhatsApp: Boolean = false,
    val isFavorite: Boolean = false,
    val accountSource: String = "Device"
)

data class RecentCallItem(
    val id: String,
    val name: String,
    val number: String,
    val type: Int,
    val timestamp: Long,
    val durationSec: Long,
    val hasWhatsApp: Boolean = false,
    val spamRiskScore: Int = 0,
    val spamLabel: String? = null
)

enum class AiEngineMode(val id: String, val label: String, val desc: String) {
    ON_DEVICE("on_device", "On-Device", "100% offline heuristic + ML pattern engine (No API needed)"),
    ONLINE("online", "Online AI", "Uses cloud LLM / API key for deep caller intelligence"),
    HYBRID("hybrid", "Hybrid Pro", "Instant offline spam shield + cloud AI script enhancement")
}
