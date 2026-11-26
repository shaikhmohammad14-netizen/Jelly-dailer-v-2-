package com.jellyglass.dialer.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.jellyglass.dialer.ui.theme.IconPackStyle

object PremiumIcons {
    fun tabFavorites(pack: IconPackStyle, selected: Boolean): ImageVector = when (pack) {
        IconPackStyle.IOS_LIQUID -> if (selected) Icons.Rounded.Star else Icons.Rounded.StarOutline
        IconPackStyle.COLOROS_SILK -> if (selected) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder
        IconPackStyle.MINIMAL_LUXE -> Icons.Outlined.StarBorder
        IconPackStyle.NEO_CAPSULE -> Icons.Rounded.AutoAwesome
    }

    fun tabRecents(pack: IconPackStyle, selected: Boolean): ImageVector = when (pack) {
        IconPackStyle.IOS_LIQUID -> if (selected) Icons.Rounded.History else Icons.Outlined.History
        IconPackStyle.COLOROS_SILK -> if (selected) Icons.Rounded.Schedule else Icons.Outlined.Schedule
        IconPackStyle.MINIMAL_LUXE -> Icons.Outlined.AccessTime
        IconPackStyle.NEO_CAPSULE -> Icons.Rounded.HistoryToggleOff
    }

    fun tabContacts(pack: IconPackStyle, selected: Boolean): ImageVector = when (pack) {
        IconPackStyle.IOS_LIQUID -> if (selected) Icons.Rounded.Contacts else Icons.Outlined.Contacts
        IconPackStyle.COLOROS_SILK -> if (selected) Icons.Rounded.People else Icons.Outlined.People
        IconPackStyle.MINIMAL_LUXE -> Icons.Outlined.PersonSearch
        IconPackStyle.NEO_CAPSULE -> Icons.Rounded.PermContactCalendar
    }

    fun tabKeypad(pack: IconPackStyle, selected: Boolean): ImageVector = when (pack) {
        IconPackStyle.IOS_LIQUID -> if (selected) Icons.Rounded.Dialpad else Icons.Outlined.Dialpad
        IconPackStyle.COLOROS_SILK -> Icons.Rounded.Apps
        IconPackStyle.MINIMAL_LUXE -> Icons.Outlined.Dialpad
        IconPackStyle.NEO_CAPSULE -> Icons.Rounded.GridView
    }

    fun tabAi(pack: IconPackStyle, selected: Boolean): ImageVector = when (pack) {
        IconPackStyle.IOS_LIQUID -> if (selected) Icons.Rounded.AutoAwesome else Icons.Outlined.AutoAwesome
        IconPackStyle.COLOROS_SILK -> Icons.Rounded.Psychology
        IconPackStyle.MINIMAL_LUXE -> Icons.Outlined.Bolt
        IconPackStyle.NEO_CAPSULE -> Icons.Rounded.Shield
    }

    fun tabSettings(pack: IconPackStyle, selected: Boolean): ImageVector = when (pack) {
        IconPackStyle.IOS_LIQUID -> if (selected) Icons.Rounded.Tune else Icons.Outlined.Tune
        IconPackStyle.COLOROS_SILK -> if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings
        IconPackStyle.MINIMAL_LUXE -> Icons.Outlined.Settings
        IconPackStyle.NEO_CAPSULE -> Icons.Rounded.DashboardCustomize
    }

    fun callAnswer(pack: IconPackStyle): ImageVector = when (pack) {
        IconPackStyle.IOS_LIQUID -> Icons.Rounded.Call
        IconPackStyle.COLOROS_SILK -> Icons.Rounded.PhoneInTalk
        IconPackStyle.MINIMAL_LUXE -> Icons.Outlined.Call
        IconPackStyle.NEO_CAPSULE -> Icons.Rounded.WifiCalling3
    }

    fun callEnd(pack: IconPackStyle): ImageVector = when (pack) {
        IconPackStyle.IOS_LIQUID -> Icons.Rounded.CallEnd
        IconPackStyle.COLOROS_SILK -> Icons.Rounded.PhoneDisabled
        IconPackStyle.MINIMAL_LUXE -> Icons.Outlined.CallEnd
        IconPackStyle.NEO_CAPSULE -> Icons.Rounded.Close
    }
}
