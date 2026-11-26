package com.jellyglass.dialer.ui.theme

import androidx.compose.ui.graphics.Color

enum class GlassThemeStyle(
    val id: String,
    val title: String,
    val subtitle: String,
    val bgTop: Color,
    val bgMid: Color,
    val bgBottom: Color,
    val glassSurface: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val accent: Color,
    val accentSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean
) {
    LIQUID_AURORA(
        id = "liquid_aurora",
        title = "iOS 26 Liquid Glass",
        subtitle = "Deep translucent obsidian & aurora glow",
        bgTop = Color(0xFF0A1024),
        bgMid = Color(0xFF132247),
        bgBottom = Color(0xFF091526),
        glassSurface = Color(0x28FFFFFF),
        glassBorderTop = Color(0x88FFFFFF),
        glassBorderBottom = Color(0x22FFFFFF),
        accent = Color(0xFF38BDF8),
        accentSecondary = Color(0xFF34D399),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFFB8C7E0),
        isDark = true
    ),
    COLOROS_FLUX(
        id = "coloros_flux",
        title = "ColorOS 17 Flux Glass",
        subtitle = "Luminous silk frost & emerald mist",
        bgTop = Color(0xFFEBF4FF),
        bgMid = Color(0xFFF5F0FF),
        bgBottom = Color(0xFFE6FAF5),
        glassSurface = Color(0x99FFFFFF),
        glassBorderTop = Color(0xFFFFFFFF),
        glassBorderBottom = Color(0x55CBD5E1),
        accent = Color(0xFF0284C7),
        accentSecondary = Color(0xFF059669),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        isDark = false
    ),
    MIDNIGHT_SAPPHIRE(
        id = "midnight_sapphire",
        title = "Sapphire Crystal",
        subtitle = "Rich royal cobalt with neon cyan rim",
        bgTop = Color(0xFF060B19),
        bgMid = Color(0xFF171C42),
        bgBottom = Color(0xFF1E1035),
        glassSurface = Color(0x24CBD5E1),
        glassBorderTop = Color(0x777DD3FC),
        glassBorderBottom = Color(0x1A7DD3FC),
        accent = Color(0xFF818CF8),
        accentSecondary = Color(0xFFF472B6),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFCBD5E1),
        isDark = true
    ),
    ROSE_QUARTZ(
        id = "rose_quartz",
        title = "Rose Quartz Frost",
        subtitle = "Warm champagne peach & frosted pearl",
        bgTop = Color(0xFFFFF1F2),
        bgMid = Color(0xFFFFF7ED),
        bgBottom = Color(0xFFFDF2F8),
        glassSurface = Color(0xA6FFFFFF),
        glassBorderTop = Color(0xFFFFFFFF),
        glassBorderBottom = Color(0x66FECDD3),
        accent = Color(0xFFE11D48),
        accentSecondary = Color(0xFFD97706),
        textPrimary = Color(0xFF1E1B18),
        textSecondary = Color(0xFF57534E),
        isDark = false
    )
}

enum class IconPackStyle(val id: String, val label: String, val desc: String) {
    IOS_LIQUID("ios_liquid", "iOS 26 Rounded Glass", "Crisp vector rounded icons"),
    COLOROS_SILK("coloros_silk", "ColorOS 17 Dual-Tone", "Modern filled & outlined vectors"),
    MINIMAL_LUXE("minimal_luxe", "Ultra Minimal Line", "Razor-thin luxury strokes"),
    NEO_CAPSULE("neo_capsule", "Neo-Glass Pill", "Bold geometric glass icons")
}

enum class CallScreenStyle(val id: String, val label: String, val desc: String) {
    IOS_26_LIQUID("ios_26", "iOS 26 Fullscreen Glass", "Dynamic mesh glow + frosted glass pills"),
    COLOROS_17_FLUX("coloros_17", "ColorOS 17 Flux Card", "Floating glass header + slide/tap deck"),
    DYNAMIC_ISLAND_PRO("dynamic_island", "Dynamic Island Minimal", "Compact top capsule + clean action grid"),
    CYBER_AURORA("cyber_aurora", "Aurora Wave Pro", "Animated pulsing rings + dual glass dock")
}
