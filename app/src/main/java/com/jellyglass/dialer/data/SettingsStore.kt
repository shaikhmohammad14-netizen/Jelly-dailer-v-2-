package com.jellyglass.dialer.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jellyglass.dialer.ui.theme.CallScreenStyle
import com.jellyglass.dialer.ui.theme.GlassThemeStyle
import com.jellyglass.dialer.ui.theme.IconPackStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "iglass_dialer_prefs")

class SettingsStore(private val context: Context) {
    companion object {
        val KEY_THEME = stringPreferencesKey("theme_style")
        val KEY_ICON_PACK = stringPreferencesKey("icon_pack")
        val KEY_CALL_STYLE = stringPreferencesKey("call_screen_style")
        val KEY_AI_MODE = stringPreferencesKey("ai_engine_mode")
        val KEY_API_KEY = stringPreferencesKey("ai_api_key")
        val KEY_AUTO_SPAM_BLOCK = booleanPreferencesKey("auto_spam_block")
        val KEY_GOOGLE_EMAIL = stringPreferencesKey("google_account_email")
        val KEY_GOOGLE_NAME = stringPreferencesKey("google_account_name")
        val KEY_LIQUID_GLASS = floatPreferencesKey("liquid_glass_intensity")
        val KEY_JELLY_BOUNCE = floatPreferencesKey("jelly_bounce_intensity")
        val KEY_HIGH_REFRESH = booleanPreferencesKey("high_refresh_rate_120hz")
    }

    val themeFlow: Flow<GlassThemeStyle> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_THEME] ?: GlassThemeStyle.LIQUID_AURORA.id
        GlassThemeStyle.entries.find { it.id == id } ?: GlassThemeStyle.LIQUID_AURORA
    }

    val iconPackFlow: Flow<IconPackStyle> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_ICON_PACK] ?: IconPackStyle.IOS_LIQUID.id
        IconPackStyle.entries.find { it.id == id } ?: IconPackStyle.IOS_LIQUID
    }

    val callStyleFlow: Flow<CallScreenStyle> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_CALL_STYLE] ?: CallScreenStyle.IOS_26_LIQUID.id
        CallScreenStyle.entries.find { it.id == id } ?: CallScreenStyle.IOS_26_LIQUID
    }

    val aiModeFlow: Flow<AiEngineMode> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_AI_MODE] ?: AiEngineMode.HYBRID.id
        AiEngineMode.entries.find { it.id == id } ?: AiEngineMode.HYBRID
    }

    val apiKeyFlow: Flow<String> = context.dataStore.data.map { it[KEY_API_KEY] ?: "" }
    val autoSpamBlockFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_SPAM_BLOCK] ?: false }
    val googleEmailFlow: Flow<String> = context.dataStore.data.map { it[KEY_GOOGLE_EMAIL] ?: "" }
    val googleNameFlow: Flow<String> = context.dataStore.data.map { it[KEY_GOOGLE_NAME] ?: "" }
    val liquidGlassFlow: Flow<Float> = context.dataStore.data.map { it[KEY_LIQUID_GLASS] ?: 0.70f }
    val jellyBounceFlow: Flow<Float> = context.dataStore.data.map { it[KEY_JELLY_BOUNCE] ?: 0.75f }
    val highRefreshFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_HIGH_REFRESH] ?: true }

    suspend fun setTheme(style: GlassThemeStyle) {
        context.dataStore.edit { it[KEY_THEME] = style.id }
    }

    suspend fun setIconPack(pack: IconPackStyle) {
        context.dataStore.edit { it[KEY_ICON_PACK] = pack.id }
    }

    suspend fun setCallStyle(style: CallScreenStyle) {
        context.dataStore.edit { it[KEY_CALL_STYLE] = style.id }
    }

    suspend fun setAiMode(mode: AiEngineMode) {
        context.dataStore.edit { it[KEY_AI_MODE] = mode.id }
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[KEY_API_KEY] = key }
    }

    suspend fun setAutoSpamBlock(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_SPAM_BLOCK] = enabled }
    }

    suspend fun setGoogleAccount(name: String, email: String) {
        context.dataStore.edit {
            it[KEY_GOOGLE_NAME] = name
            it[KEY_GOOGLE_EMAIL] = email
        }
    }

    suspend fun setLiquidGlass(intensity: Float) {
        context.dataStore.edit { it[KEY_LIQUID_GLASS] = intensity.coerceIn(0f, 1f) }
    }

    suspend fun setJellyBounce(intensity: Float) {
        context.dataStore.edit { it[KEY_JELLY_BOUNCE] = intensity.coerceIn(0f, 1f) }
    }

    suspend fun setHighRefresh(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HIGH_REFRESH] = enabled }
    }
}
