package com.jellyglass.dialer.ai

import com.jellyglass.dialer.data.AiEngineMode
import com.jellyglass.dialer.data.RecentCallItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class SpamVerdict(
    val isSpam: Boolean,
    val score: Int,
    val category: String,
    val reason: String,
    val sourceEngine: String
)

object SpamAndAiEngine {

    private val indianTelemarketerPrefixes = listOf(
        "140", "160", "+91140", "+91160", "0140", "0160",
        "1800", "1860", "+911800"
    )

    private val spamKeywords = listOf(
        "spam", "loan", "credit card", "insurance", "offer",
        "telemarketer", "scam", "fraud", "kyc", "lottery", "bajaj", "policy"
    )

    fun analyzeOfflineSpam(rawNumber: String, callerName: String? = null): SpamVerdict {
        val cleaned = rawNumber.filterNot { it.isWhitespace() || it == '-' }
        val digitsOnly = cleaned.filter { it.isDigit() }
        val lowerName = callerName.orEmpty().lowercase()

        if (cleaned.isBlank() || cleaned.equals("unknown", true) || cleaned.equals("private", true)) {
            return SpamVerdict(
                isSpam = true,
                score = 78,
                category = "Hidden / Private Caller",
                reason = "Caller ID withheld — high telemarketing risk",
                sourceEngine = "On-Device Shield"
            )
        }

        val matchedKeyword = spamKeywords.firstOrNull { lowerName.contains(it) }
        if (matchedKeyword != null) {
            return SpamVerdict(
                isSpam = true,
                score = 92,
                category = "Telemarketing / Financial Spam",
                reason = "Flagged by carrier/ID keyword: '$matchedKeyword'",
                sourceEngine = "On-Device Shield"
            )
        }

        if (indianTelemarketerPrefixes.any { cleaned.startsWith(it) }) {
            val isService = cleaned.startsWith("160") || cleaned.startsWith("+91160")
            return SpamVerdict(
                isSpam = !isService,
                score = if (isService) 35 else 95,
                category = if (isService) "TRAI 160 Service Call" else "TRAI 140 Promotional Spam",
                reason = if (isService) "Registered banking/service line (160 series)" else "Registered telemarketer series (140)",
                sourceEngine = "On-Device Shield"
            )
        }

        if (digitsOnly.length in 1..6 && !digitsOnly.startsWith("19") && !digitsOnly.startsWith("112")) {
            return SpamVerdict(
                isSpam = true,
                score = 72,
                category = "Shortcode / Automated Bot",
                reason = "Automated shortcode dialer pattern",
                sourceEngine = "On-Device Shield"
            )
        }

        val last10 = digitsOnly.takeLast(10)
        if (last10.length == 10) {
            val uniqueDigits = last10.toSet().size
            if (uniqueDigits <= 2) {
                return SpamVerdict(
                    isSpam = true,
                    score = 88,
                    category = "Robocall / Spoofed Number",
                    reason = "Synthetic repeating digit sequence detected",
                    sourceEngine = "On-Device Shield"
                )
            }
            if (last10.first() in '0'..'5') {
                return SpamVerdict(
                    isSpam = true,
                    score = 84,
                    category = "Invalid / Spoofed Mobile Series",
                    reason = "Non-standard Indian mobile series (${last10.take(2)}xx)",
                    sourceEngine = "On-Device Shield"
                )
            }
        }

        return SpamVerdict(
            isSpam = false,
            score = 8,
            category = "Verified Clean Pattern",
            reason = "Standard mobile subscriber series — safe to answer",
            sourceEngine = "On-Device Shield"
        )
    }

    suspend fun evaluateNumber(
        number: String,
        callerName: String?,
        mode: AiEngineMode,
        apiKey: String
    ): SpamVerdict = withContext(Dispatchers.IO) {
        val offline = analyzeOfflineSpam(number, callerName)
        if (mode == AiEngineMode.ON_DEVICE || apiKey.isBlank()) {
            return@withContext if (mode == AiEngineMode.ONLINE && apiKey.isBlank()) {
                offline.copy(sourceEngine = "Auto-Fallback Offline (No API Key)")
            } else {
                offline
            }
        }

        runCatching {
            val prompt = "Analyze phone number '$number' (caller name: '${callerName ?: "Unknown"}') in India. " +
                "Reply in 1 short sentence: is it likely spam/telemarketing or normal, and why?"
            val onlineReply = callGeminiQuick(apiKey, prompt)
            if (onlineReply.isNotBlank()) {
                val aiFlagsSpam = onlineReply.contains("spam", true) ||
                    onlineReply.contains("telemarket", true) ||
                    onlineReply.contains("scam", true)
                SpamVerdict(
                    isSpam = aiFlagsSpam || offline.isSpam,
                    score = if (aiFlagsSpam) maxOf(offline.score, 85) else offline.score,
                    category = if (aiFlagsSpam) "AI Cloud Flagged" else "AI Cloud Verified",
                    reason = onlineReply.take(140),
                    sourceEngine = if (mode == AiEngineMode.HYBRID) "Hybrid (Offline + Gemini)" else "Online Gemini AI"
                )
            } else offline
        }.getOrDefault(offline)
    }

    suspend fun generateCallScripts(
        scenario: String,
        callerName: String,
        mode: AiEngineMode,
        apiKey: String
    ): List<String> = withContext(Dispatchers.IO) {
        val offlineScripts = when (scenario) {
            "Delivery / Courier" -> listOf(
                "Hi, please leave the package at the main gate with security. Thank you!",
                "I'm at work right now — please call me back in 15 minutes or drop it at the reception.",
                "Please share the OTP on SMS/WhatsApp, I will collect the parcel shortly."
            )
            "Unknown / Spam Screen" -> listOf(
                "Hello, this call is being screened by iGlass Assistant. Please state your name and purpose.",
                "I'm not interested in any promotional offers, loans, or credit cards. Please remove my number.",
                "Please send the details on WhatsApp or SMS instead of calling."
            )
            "Busy / In Meeting" -> listOf(
                "Hey $callerName, I'm currently on the store floor / in a meeting. Will call you right back!",
                "Can't talk right now — text me if it's urgent.",
                "Give me 20 minutes, wrapping up something important and I'll ring you."
            )
            else -> listOf(
                "Hi $callerName, calling you back in just a few minutes!",
                "Hey! Good to hear from you — let me know what's up.",
                "I'm stepping out right now, let's catch up on a quick call soon."
            )
        }

        if (mode == AiEngineMode.ON_DEVICE || apiKey.isBlank()) {
            return@withContext offlineScripts
        }

        runCatching {
            val prompt = "Write 3 short, natural Indian-English phone call scripts for scenario: '$scenario' with caller '$callerName'. Separate each with a newline, no numbering."
            val text = callGeminiQuick(apiKey, prompt)
            val lines = text.lines().map { it.trim().removePrefix("-").removePrefix("•").trim() }.filter { it.length > 8 }
            if (lines.size >= 2) lines.take(3) else offlineScripts
        }.getOrDefault(offlineScripts)
    }

    suspend fun summarizeRecents(
        recents: List<RecentCallItem>,
        mode: AiEngineMode,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        if (recents.isEmpty()) return@withContext "No recent calls found yet. Once calls come in, AI insights will appear here."

        val missed = recents.filter { it.type == 3 }
        val spamCount = recents.count { it.spamRiskScore >= 70 }
        val topCaller = recents.groupBy { it.name }.maxByOrNull { it.value.size }

        val offlineSummary = buildString {
            append("• Total recent calls analyzed: ${recents.size}
")
            append("• Missed calls needing callback: ${missed.size}")
            if (missed.isNotEmpty()) {
                append(" (${missed.take(3).joinToString { it.name }})")
            }
            append("
• High-risk spam calls blocked/flagged: $spamCount
")
            if (topCaller != null) {
                append("• Most frequent contact: ${topCaller.key} (${topCaller.value.size} calls)")
            }
        }

        if (mode == AiEngineMode.ON_DEVICE || apiKey.isBlank()) {
            return@withContext offlineSummary
        }

        runCatching {
            val digest = recents.take(15).joinToString("; ") { "${it.name} (${it.number}, type=${it.type})" }
            val prompt = "Summarize these recent calls concisely in 3 bullet points highlighting who needs a callback and any spam: $digest"
            val res = callGeminiQuick(apiKey, prompt)
            res.ifBlank { offlineSummary }
        }.getOrDefault(offlineSummary)
    }

    private fun callGeminiQuick(apiKey: String, prompt: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 6000
            readTimeout = 8000
            doOutput = true
        }
        val body = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            ))
        }
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        if (conn.responseCode !in 200..299) return ""
        val responseStr = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(responseStr)
        return json.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")
            .orEmpty()
    }
}
