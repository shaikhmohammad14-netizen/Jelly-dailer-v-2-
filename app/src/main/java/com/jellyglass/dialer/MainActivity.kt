package com.jellyglass.dialer

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.jellyglass.dialer.ai.SpamAndAiEngine
import com.jellyglass.dialer.ai.SpamVerdict
import com.jellyglass.dialer.data.*
import com.jellyglass.dialer.telecom.CallStateManager
import com.jellyglass.dialer.ui.components.*
import com.jellyglass.dialer.ui.theme.CallScreenStyle
import com.jellyglass.dialer.ui.theme.GlassThemeStyle
import com.jellyglass.dialer.ui.theme.IconPackStyle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        CallStateManager.clearCallState()
        val initialNumber = intent?.data?.schemeSpecificPart.orEmpty()

        setContent {
            JellyGlassApp(initialDialNumber = initialNumber)
        }
    }
}

enum class MainTab(val label: String) {
    FAVORITES("Favorites"),
    RECENTS("Recents"),
    CONTACTS("Contacts"),
    KEYPAD("Keypad"),
    AI("AI Shield"),
    SETTINGS("Settings")
}

@Composable
fun JellyGlassApp(initialDialNumber: String = "") {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }
    val repo = remember { ContactsRepository(context) }

    val theme by store.themeFlow.collectAsState(initial = GlassThemeStyle.LIQUID_AURORA)
    val iconPack by store.iconPackFlow.collectAsState(initial = IconPackStyle.IOS_LIQUID)
    val callStyle by store.callStyleFlow.collectAsState(initial = CallScreenStyle.IOS_26_LIQUID)
    val aiMode by store.aiModeFlow.collectAsState(initial = AiEngineMode.HYBRID)
    val apiKey by store.apiKeyFlow.collectAsState(initial = "")
    val autoSpamBlock by store.autoSpamBlockFlow.collectAsState(initial = false)
    val googleEmail by store.googleEmailFlow.collectAsState(initial = "")
    val googleName by store.googleNameFlow.collectAsState(initial = "")
    val liquidGlass by store.liquidGlassFlow.collectAsState(initial = 0.70f)
    val jellyBounce by store.jellyBounceFlow.collectAsState(initial = 0.75f)
    val highRefresh by store.highRefreshFlow.collectAsState(initial = true)

    LaunchedEffect(highRefresh) {
        (context as? Activity)?.let { applyPreferredRefreshRate(it, highRefresh) }
    }

    var currentTab by remember { mutableStateOf(MainTab.KEYPAD) }
    var dialInput by remember { mutableStateOf(initialDialNumber) }
    var contacts by remember { mutableStateOf<List<ContactItem>>(emptyList()) }
    var recents by remember { mutableStateOf<List<RecentCallItem>>(emptyList()) }
    var waNumbers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showAddContactForNumber by remember { mutableStateOf<String?>(null) }

    fun refreshData() {
        scope.launch {
            waNumbers = repo.fetchWhatsAppNumbers()
            contacts = repo.loadContacts()
            recents = repo.loadRecents()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshData()
    }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ANSWER_PHONE_CALLS
            )
        )
        requestDefaultDialerRole(context) { intent ->
            roleLauncher.launch(intent)
        }
        refreshData()
    }

    val bgBrush = Brush.verticalGradient(
        colors = listOf(theme.bgTop, theme.bgMid, theme.bgBottom)
    )

    CompositionLocalProvider(
        LocalLiquidGlassIntensity provides liquidGlass,
        LocalJellyBounceIntensity provides jellyBounce
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    MainTab.FAVORITES -> FavoritesTabScreen(
                        contacts = contacts.filter { it.isFavorite },
                        allContacts = contacts,
                        theme = theme,
                        onCall = { CallStateManager.placeCall(context, it) },
                        onWhatsApp = { repo.openWhatsAppChat(it) },
                        onAddContact = { showAddContactForNumber = "" }
                    )

                    MainTab.RECENTS -> RecentsTabScreen(
                        recents = recents,
                        theme = theme,
                        onCall = { CallStateManager.placeCall(context, it) },
                        onWhatsApp = { repo.openWhatsAppChat(it) },
                        onAddContact = { showAddContactForNumber = it },
                        onRefresh = { refreshData() }
                    )

                    MainTab.CONTACTS -> ContactsSearchTabScreen(
                        contacts = contacts,
                        googleEmail = googleEmail,
                        theme = theme,
                        onCall = { CallStateManager.placeCall(context, it) },
                        onWhatsApp = { repo.openWhatsAppChat(it) },
                        onAddNewContact = { showAddContactForNumber = "" }
                    )

                    MainTab.KEYPAD -> KeypadTabScreen(
                        dialInput = dialInput,
                        onDialInputChange = { dialInput = it },
                        contacts = contacts,
                        waNumbers = waNumbers,
                        theme = theme,
                        iconPack = iconPack,
                        onPlaceCall = { number ->
                            CallStateManager.placeCall(context, number)
                        },
                        onAddNewContact = { number ->
                            showAddContactForNumber = number
                        },
                        onOpenWhatsApp = { number ->
                            repo.openWhatsAppChat(number)
                        }
                    )

                    MainTab.AI -> AiShieldTabScreen(
                        aiMode = aiMode,
                        apiKey = apiKey,
                        recents = recents,
                        theme = theme,
                        onModeChange = { scope.launch { store.setAiMode(it) } }
                    )

                    MainTab.SETTINGS -> SettingsTabScreen(
                        theme = theme,
                        iconPack = iconPack,
                        callStyle = callStyle,
                        aiMode = aiMode,
                        apiKey = apiKey,
                        autoSpamBlock = autoSpamBlock,
                        liquidGlass = liquidGlass,
                        jellyBounce = jellyBounce,
                        highRefresh = highRefresh,
                        googleName = googleName,
                        googleEmail = googleEmail,
                        contacts = contacts,
                        onSelectTheme = { scope.launch { store.setTheme(it) } },
                        onSelectIconPack = { scope.launch { store.setIconPack(it) } },
                        onSelectCallStyle = { scope.launch { store.setCallStyle(it) } },
                        onLiquidGlassChange = { scope.launch { store.setLiquidGlass(it) } },
                        onJellyBounceChange = { scope.launch { store.setJellyBounce(it) } },
                        onHighRefreshToggle = { scope.launch { store.setHighRefresh(it) } },
                        onApiKeyChange = { scope.launch { store.setApiKey(it) } },
                        onAutoSpamToggle = { scope.launch { store.setAutoSpamBlock(it) } },
                        onGoogleAccountUpdated = { name, email ->
                            scope.launch {
                                store.setGoogleAccount(name, email)
                                refreshData()
                            }
                        },
                        onContactsImported = { refreshData() },
                        onTestCallUi = { isIncoming ->
                            CallStateManager.launchPreviewCall(
                                context = context,
                                name = if (isIncoming) "Aarav Sharma" else "iGlass HD Test",
                                number = "+91 98201 44510",
                                isIncoming = isIncoming
                            )
                        }
                    )
                }
            }

            FrostedGlassBottomNav(
                currentTab = currentTab,
                theme = theme,
                iconPack = iconPack,
                onSelectTab = { currentTab = it }
            )
        }

        showAddContactForNumber?.let { presetPhone ->
            val norm = repo.normalizePhone(presetPhone)
            val isWa = norm.length >= 7 && waNumbers.contains(norm)
            AddContactDialog(
                initialPhone = presetPhone,
                hasWhatsAppMatch = isWa,
                googleEmail = googleEmail,
                theme = theme,
                onDismiss = { showAddContactForNumber = null },
                onSave = { name, phone, syncGoogle ->
                    scope.launch {
                        val saved = repo.addNewContact(
                            name = name,
                            phone = phone,
                            accountEmail = if (syncGoogle) googleEmail else null
                        )
                        Toast.makeText(
                            context,
                            if (saved) "Saved $name!" else "Grant Contacts permission first",
                            Toast.LENGTH_SHORT
                        ).show()
                        showAddContactForNumber = null
                        refreshData()
                    }
                }
            )
        }
    }
    }
}

@Composable
fun FrostedGlassBottomNav(
    currentTab: MainTab,
    theme: GlassThemeStyle,
    iconPack: IconPackStyle,
    onSelectTab: (MainTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        FrostedGlassCard(
            theme = theme,
            shape = RoundedCornerShape(34.dp),
            elevation = 14.dp,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainTab.entries.forEach { tab ->
                    val selected = tab == currentTab
                    val icon = when (tab) {
                        MainTab.FAVORITES -> PremiumIcons.tabFavorites(iconPack, selected)
                        MainTab.RECENTS -> PremiumIcons.tabRecents(iconPack, selected)
                        MainTab.CONTACTS -> PremiumIcons.tabContacts(iconPack, selected)
                        MainTab.KEYPAD -> PremiumIcons.tabKeypad(iconPack, selected)
                        MainTab.AI -> PremiumIcons.tabAi(iconPack, selected)
                        MainTab.SETTINGS -> PremiumIcons.tabSettings(iconPack, selected)
                    }

                    val pillAlpha by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
                        label = "navPill"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(theme.accent.copy(alpha = 0.18f * pillAlpha))
                            .jellyBounce(pressedScale = 0.84f) { onSelectTab(tab) }
                            .padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.label,
                            tint = if (selected) theme.accent else theme.textSecondary,
                            modifier = Modifier.size(21.dp)
                        )
                        Text(
                            text = tab.label,
                            color = if (selected) theme.accent else theme.textSecondary,
                            fontSize = 9.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesTabScreen(
    contacts: List<ContactItem>,
    allContacts: List<ContactItem>,
    theme: GlassThemeStyle,
    onCall: (String) -> Unit,
    onWhatsApp: (String) -> Unit,
    onAddContact: () -> Unit
) {
    val listState = rememberLazyListState()
    val displayList = contacts.ifEmpty { allContacts.take(6) }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (contacts.isNotEmpty()) "Favorites" else "Speed Dial",
                        color = theme.textPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap any glass card for instant HD call",
                        color = theme.textSecondary,
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(theme.accent.copy(alpha = 0.2f))
                        .jellyBounce { onAddContact() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = "Add Contact", tint = theme.accent)
                }
            }
        }

        items(displayList, key = { it.id + it.phone }) { contact ->
            FrostedGlassCard(
                theme = theme,
                modifier = Modifier
                    .fillMaxWidth()
                    .jellyBounce(pressedScale = 0.96f) { onCall(contact.phone) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(theme.accent, theme.accentSecondary))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = contact.name,
                                color = theme.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (contact.hasWhatsApp) WhatsAppBadge()
                        }
                        Text(text = contact.phone, color = theme.textSecondary, fontSize = 13.sp)
                    }
                    if (contact.hasWhatsApp) {
                        IconButton(onClick = { onWhatsApp(contact.phone) }) {
                            Icon(Icons.Rounded.Chat, contentDescription = "WhatsApp", tint = Color(0xFF16A34A))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(theme.accentSecondary.copy(alpha = 0.2f))
                            .jellyBounce { onCall(contact.phone) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = "Call", tint = theme.accentSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RecentsTabScreen(
    recents: List<RecentCallItem>,
    theme: GlassThemeStyle,
    onCall: (String) -> Unit,
    onWhatsApp: (String) -> Unit,
    onAddContact: (String) -> Unit,
    onRefresh: () -> Unit
) {
    val listState = rememberLazyListState()
    val formatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recents",
                    color = theme.textPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = theme.accent)
                }
            }
        }

        if (recents.isEmpty()) {
            item {
                FrostedGlassCard(theme = theme, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No call history found yet. Make or receive a call to see live AI spam tags & WhatsApp indicators.",
                        color = theme.textSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        items(recents, key = { it.id }) { item ->
            val typeIcon = when (item.type) {
                CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Rounded.CallReceived
                CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Rounded.CallMade
                else -> Icons.AutoMirrored.Rounded.CallMissed
            }
            val typeColor = when {
                item.spamRiskScore >= 70 -> Color(0xFFF43F5E)
                item.type == CallLog.Calls.MISSED_TYPE -> Color(0xFFFB7185)
                else -> theme.accentSecondary
            }

            FrostedGlassCard(
                theme = theme,
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .jellyBounce(pressedScale = 0.97f) { onCall(item.number) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(typeColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(20.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = item.name,
                                color = if (item.spamRiskScore >= 70) Color(0xFFF43F5E) else theme.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.hasWhatsApp) WhatsAppBadge()
                        }
                        Text(
                            text = "${item.number} • ${formatter.format(Date(item.timestamp))}",
                            color = theme.textSecondary,
                            fontSize = 12.sp
                        )
                        item.spamLabel?.let { warn ->
                            Text(
                                text = "⚠ $warn",
                                color = Color(0xFFF43F5E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (item.name == item.number && item.number.isNotBlank()) {
                        IconButton(onClick = { onAddContact(item.number) }) {
                            Icon(Icons.Rounded.PersonAddAlt1, contentDescription = "Save Contact", tint = theme.accent)
                        }
                    }
                    if (item.hasWhatsApp) {
                        IconButton(onClick = { onWhatsApp(item.number) }) {
                            Icon(Icons.Rounded.Chat, contentDescription = "WhatsApp", tint = Color(0xFF16A34A))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsSearchTabScreen(
    contacts: List<ContactItem>,
    googleEmail: String,
    theme: GlassThemeStyle,
    onCall: (String) -> Unit,
    onWhatsApp: (String) -> Unit,
    onAddNewContact: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var onlyWhatsApp by remember { mutableStateOf(false) }
    var onlyGoogleCloud by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val filtered = remember(contacts, searchQuery, onlyWhatsApp, onlyGoogleCloud, googleEmail) {
        contacts.filter { c ->
            val matchesQuery = searchQuery.isBlank() ||
                c.name.contains(searchQuery, ignoreCase = true) ||
                c.phone.contains(searchQuery)
            val matchesWa = !onlyWhatsApp || c.hasWhatsApp
            val matchesCloud = !onlyGoogleCloud || (googleEmail.isNotBlank() && c.accountSource.equals(googleEmail, true))
            matchesQuery && matchesWa && matchesCloud
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Contacts", color = theme.textPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("${filtered.size} synced contacts", color = theme.textSecondary, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(theme.accent)
                    .jellyBounce { onAddNewContact() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text("Add New", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search name or phone number...", color = theme.textSecondary) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = theme.accent) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = theme.textSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = theme.glassSurface,
                unfocusedContainerColor = theme.glassSurface,
                focusedBorderColor = theme.accent,
                unfocusedBorderColor = theme.glassBorderTop,
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = !onlyWhatsApp && !onlyGoogleCloud,
                    onClick = {
                        onlyWhatsApp = false
                        onlyGoogleCloud = false
                    },
                    label = { Text("All (${contacts.size})") }
                )
            }
            item {
                FilterChip(
                    selected = onlyWhatsApp,
                    onClick = { onlyWhatsApp = !onlyWhatsApp },
                    label = { Text("WhatsApp Active (${contacts.count { it.hasWhatsApp }})") }
                )
            }
            if (googleEmail.isNotBlank()) {
                item {
                    FilterChip(
                        selected = onlyGoogleCloud,
                        onClick = { onlyGoogleCloud = !onlyGoogleCloud },
                        label = { Text("Google Cloud") }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it.id + it.phone }) { contact ->
                FrostedGlassCard(
                    theme = theme,
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .jellyBounce(pressedScale = 0.97f) { onCall(contact.phone) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(theme.accent, theme.accentSecondary))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contact.name.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = contact.name,
                                    color = theme.textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (contact.hasWhatsApp) WhatsAppBadge()
                            }
                            Text(
                                text = "${contact.phone} • ${contact.accountSource}",
                                color = theme.textSecondary,
                                fontSize = 12.sp
                            )
                        }

                        if (contact.hasWhatsApp) {
                            IconButton(onClick = { onWhatsApp(contact.phone) }) {
                                Icon(Icons.Rounded.Chat, contentDescription = "WhatsApp", tint = Color(0xFF16A34A))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(theme.accent.copy(alpha = 0.18f))
                                .jellyBounce { onCall(contact.phone) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Call, contentDescription = "Call", tint = theme.accent, modifier = Modifier.size(19.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadTabScreen(
    dialInput: String,
    onDialInputChange: (String) -> Unit,
    contacts: List<ContactItem>,
    waNumbers: Set<String>,
    theme: GlassThemeStyle,
    iconPack: IconPackStyle,
    onPlaceCall: (String) -> Unit,
    onAddNewContact: (String) -> Unit,
    onOpenWhatsApp: (String) -> Unit
) {
    val cleanDigits = dialInput.filter { it.isDigit() }
    val norm10 = if (cleanDigits.length >= 10) cleanDigits.takeLast(10) else cleanDigits
    val hasWhatsApp = norm10.length >= 7 && waNumbers.contains(norm10)

    val matchingContacts = remember(dialInput, contacts) {
        if (dialInput.isBlank()) emptyList()
        else contacts.filter {
            it.phone.filter { ch -> ch.isDigit() }.contains(cleanDigits) ||
                it.name.contains(dialInput, ignoreCase = true)
        }.take(2)
    }

    val liveSpamCheck = remember(dialInput) {
        if (cleanDigits.length >= 6) SpamAndAiEngine.analyzeOfflineSpam(dialInput) else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Keypad", color = theme.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                if (dialInput.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(theme.accent.copy(alpha = 0.2f))
                            .border(1.dp, theme.accent.copy(alpha = 0.5f), CircleShape)
                            .jellyBounce { onAddNewContact(dialInput) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
                            Text("Add to Contact", color = theme.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = dialInput.ifEmpty { "Enter number" },
                color = if (dialInput.isEmpty()) theme.textSecondary.copy(alpha = 0.5f) else theme.textPrimary,
                fontSize = if (dialInput.length > 13) 26.sp else 34.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            if (dialInput.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasWhatsApp) {
                        Box(modifier = Modifier.jellyBounce { onOpenWhatsApp(dialInput) }) {
                            WhatsAppBadge()
                        }
                    }
                    if (liveSpamCheck?.isSpam == true) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFF43F5E).copy(alpha = 0.18f))
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "⚠ ${liveSpamCheck.category}",
                                color = Color(0xFFF43F5E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            matchingContacts.firstOrNull()?.let { match ->
                Spacer(Modifier.height(6.dp))
                FrostedGlassCard(
                    theme = theme,
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.jellyBounce { onDialInputChange(match.phone) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(match.name, color = theme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(match.phone, color = theme.textSecondary, fontSize = 12.sp)
                        if (match.hasWhatsApp) WhatsAppBadge()
                    }
                }
            }
        }

        val latestDialInput by rememberUpdatedState(dialInput)
        val toneGenerator = remember {
            runCatching { ToneGenerator(AudioManager.STREAM_DTMF, 65) }.getOrNull()
        }
        DisposableEffect(Unit) {
            onDispose { runCatching { toneGenerator?.release() } }
        }

        val keys = listOf(
            listOf("1" to "∞", "2" to "ABC", "3" to "DEF"),
            listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
            listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
            listOf("*" to "", "0" to "+", "#" to "")
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { (digit, sub) ->
                        FrostedGlassCard(
                            theme = theme,
                            shape = RoundedCornerShape(26.dp),
                            elevation = 6.dp,
                            contentPadding = PaddingValues(vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .jellyBounce(pressedScale = 0.85f) {
                                    onDialInputChange(latestDialInput + digit)
                                    val tone = when (digit) {
                                        "0" -> ToneGenerator.TONE_DTMF_0
                                        "1" -> ToneGenerator.TONE_DTMF_1
                                        "2" -> ToneGenerator.TONE_DTMF_2
                                        "3" -> ToneGenerator.TONE_DTMF_3
                                        "4" -> ToneGenerator.TONE_DTMF_4
                                        "5" -> ToneGenerator.TONE_DTMF_5
                                        "6" -> ToneGenerator.TONE_DTMF_6
                                        "7" -> ToneGenerator.TONE_DTMF_7
                                        "8" -> ToneGenerator.TONE_DTMF_8
                                        "9" -> ToneGenerator.TONE_DTMF_9
                                        "*" -> ToneGenerator.TONE_DTMF_S
                                        "#" -> ToneGenerator.TONE_DTMF_P
                                        else -> ToneGenerator.TONE_DTMF_0
                                    }
                                    runCatching { toneGenerator?.startTone(tone, 80) }
                                }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = digit,
                                    color = theme.textPrimary,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = sub,
                                    color = theme.textSecondary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color(0xFF25D366).copy(alpha = if (dialInput.isNotBlank()) 0.22f else 0.08f))
                        .border(1.dp, Color(0xFF25D366).copy(alpha = 0.4f), RoundedCornerShape(30.dp))
                        .jellyBounce {
                            if (dialInput.isNotBlank()) onOpenWhatsApp(dialInput)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Chat, contentDescription = "WhatsApp", tint = Color(0xFF16A34A), modifier = Modifier.size(24.dp))
                }

                Box(
                    modifier = Modifier
                        .weight(1.6f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(32.dp))
                        .jellyBounce(pressedScale = 0.86f) {
                            if (dialInput.isNotBlank()) onPlaceCall(dialInput)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PremiumIcons.callAnswer(iconPack),
                        contentDescription = "Call",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(theme.glassSurface)
                        .border(1.dp, theme.glassBorderTop, RoundedCornerShape(30.dp))
                        .jellyBounce {
                            if (dialInput.isNotEmpty()) {
                                onDialInputChange(dialInput.dropLast(1))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Backspace,
                        contentDescription = "Delete",
                        tint = theme.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AiShieldTabScreen(
    aiMode: AiEngineMode,
    apiKey: String,
    recents: List<RecentCallItem>,
    theme: GlassThemeStyle,
    onModeChange: (AiEngineMode) -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var checkNumber by remember { mutableStateOf("") }
    var spamResult by remember { mutableStateOf<SpamVerdict?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    var selectedScenario by remember { mutableStateOf("Delivery / Courier") }
    var generatedScripts by remember { mutableStateOf<List<String>>(emptyList()) }
    var recentsSummary by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(aiMode, selectedScenario) {
        generatedScripts = SpamAndAiEngine.generateCallScripts(selectedScenario, "Caller", aiMode, apiKey)
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("AI Call Shield", color = theme.textPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Works 100% offline without API, or supercharged with Gemini API",
                color = theme.textSecondary,
                fontSize = 13.sp
            )
        }

        item {
            FrostedGlassCard(theme = theme, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("AI Engine Mode", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiEngineMode.entries.forEach { mode ->
                            val selected = mode == aiMode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) theme.accent else theme.glassSurface)
                                    .jellyBounce { onModeChange(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.label,
                                    color = if (selected) Color.White else theme.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(text = aiMode.desc, color = theme.textSecondary, fontSize = 12.sp)
                }
            }
        }

        item {
            FrostedGlassCard(theme = theme, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Instant Spam & Scam Checker", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = checkNumber,
                        onValueChange = { checkNumber = it },
                        placeholder = { Text("Enter number (e.g., 1408801234 or +9198...)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (checkNumber.isNotBlank()) {
                                isChecking = true
                                scope.launch {
                                    spamResult = SpamAndAiEngine.evaluateNumber(checkNumber, null, aiMode, apiKey)
                                    isChecking = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isChecking) "Analyzing..." else "Analyze Number Now", fontWeight = FontWeight.Bold)
                    }

                    spamResult?.let { verdict ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (verdict.isSpam) Color(0xFFF43F5E).copy(alpha = 0.16f)
                                    else Color(0xFF10B981).copy(alpha = 0.16f)
                                )
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${if (verdict.isSpam) "⚠ SPAM RISK" else "✅ SAFE"} • ${verdict.category} (${verdict.score}%)",
                                    color = if (verdict.isSpam) Color(0xFFF43F5E) else Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(verdict.reason, color = theme.textPrimary, fontSize = 13.sp)
                                Text("Engine: ${verdict.sourceEngine}", color = theme.textSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            FrostedGlassCard(theme = theme, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("AI Call Scripts & Screening", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    val scenarios = listOf("Delivery / Courier", "Unknown / Spam Screen", "Busy / In Meeting")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(scenarios) { sc ->
                            FilterChip(
                                selected = selectedScenario == sc,
                                onClick = { selectedScenario = sc },
                                label = { Text(sc) }
                            )
                        }
                    }
                    generatedScripts.forEach { script ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(theme.glassSurface)
                                .padding(10.dp)
                        ) {
                            Text("“$script”", color = theme.textPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        item {
            FrostedGlassCard(theme = theme, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Recents Call Insights", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            scope.launch {
                                recentsSummary = SpamAndAiEngine.summarizeRecents(recents, aiMode, apiKey)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accentSecondary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Summarize Recent Calls", fontWeight = FontWeight.Bold)
                    }
                    recentsSummary?.let {
                        Text(it, color = theme.textPrimary, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTabScreen(
    theme: GlassThemeStyle,
    iconPack: IconPackStyle,
    callStyle: CallScreenStyle,
    aiMode: AiEngineMode,
    apiKey: String,
    autoSpamBlock: Boolean,
    liquidGlass: Float,
    jellyBounce: Float,
    highRefresh: Boolean,
    googleName: String,
    googleEmail: String,
    contacts: List<ContactItem>,
    onSelectTheme: (GlassThemeStyle) -> Unit,
    onSelectIconPack: (IconPackStyle) -> Unit,
    onSelectCallStyle: (CallScreenStyle) -> Unit,
    onLiquidGlassChange: (Float) -> Unit,
    onJellyBounceChange: (Float) -> Unit,
    onHighRefreshToggle: (Boolean) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onAutoSpamToggle: (Boolean) -> Unit,
    onGoogleAccountUpdated: (String, String) -> Unit,
    onContactsImported: () -> Unit,
    onTestCallUi: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ContactsRepository(context) }
    val listState = rememberLazyListState()
    var keyDraft by remember(apiKey) { mutableStateOf(apiKey) }
    var manualGoogleEmail by remember { mutableStateOf("") }

    val importVcfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val count = repo.importContactsFromVcf(uri, googleEmail.ifBlank { null })
                Toast.makeText(context, "Imported $count contacts!", Toast.LENGTH_SHORT).show()
                onContactsImported()
            }
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Settings & Studio", color = theme.textPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Customize Liquid Glass, Bouncy Jelly physics, 120Hz display & cloud backup", color = theme.textSecondary, fontSize = 13.sp)
        }

        item {
            FrostedGlassCard(theme = theme, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, tint = theme.accent)
                        Text("Liquid Glass & Bouncy Jelly Studio", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Liquid Glass Frost & Specular Glow", color = theme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("${(liquidGlass * 100).toInt()}%", color = theme.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = liquidGlass,
                            onValueChange = onLiquidGlassChange,
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = theme.accent,
                                activeTrackColor = theme.accent
                            )
                        )
                    }

                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bouncy Jelly Touch Effect", color = theme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("${(jellyBounce * 100).toInt()}%", color = theme.accentSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = jellyBounce,
                            onValueChange = onJellyBounceChange,
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = theme.accentSecondary,
                                activeTrackColor = theme.accentSecondary
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("90Hz – 120Hz Smooth Refresh Lock", color = theme.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Runs keypad & jelly animations at native 120Hz", color = theme.textSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = highRefresh,
                            onCheckedChange = onHighRefreshToggle
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Auto-Block High-Risk Spam", color = theme.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Silences 140/160 telemarketers automatically", color = theme.textSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = autoSpamBlock,
                            onCheckedChange = onAutoSpamToggle
                        )
                    }
                }
            }
        }

        item {
            FrostedGlassCard(theme = theme, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.CloudSync, contentDescription = null, tint = theme.accent)
                        Text("Google Cloud Contacts", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    if (googleEmail.isNotBlank()) {
                        Text(
                            text = "Signed in: ${googleName.ifBlank { "Google User" }} ($googleEmail)",
                            color = theme.accentSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedButton(
                            onClick = { onGoogleAccountUpdated("", "") },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Sign Out")
                        }
                    } else {
                        Text(
                            text = "Sign in with Google to sync & filter your cloud contacts directly.",
                            color = theme.textSecondary,
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    signInWithGoogleCredentialManager(
                                        context = context,
                                        onSuccess = { name, email ->
                                            onGoogleAccountUpdated(name, email)
                                            Toast.makeText(context, "Connected $email", Toast.LENGTH_SHORT).show()
                                        },
                                        onFallbackNeeded = {
                                            Toast.makeText(
                                                context,
                                                "Enter your Google email below to link device Google Contacts",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.AccountCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Sign in with Google", fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = manualGoogleEmail,
                                onValueChange = { manualGoogleEmail = it },
                                placeholder = { Text("or enter your Gmail address") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            Button(
                                onClick = {
                                    if (manualGoogleEmail.contains("@")) {
                                        onGoogleAccountUpdated(manualGoogleEmail.substringBefore("@"), manualGoogleEmail.trim())
                                    }
                                },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Link")
                            }
                        }
                    }
                }
            }
        }

        item {
            FrostedGlassCard(theme = theme, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Import & Export Contacts", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { importVcfLauncher.launch(arrayOf("text/x-vcard", "text/vcard", "*/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Import VCF")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val uri = repo.exportContactsToVcf(contacts)
                                    if (uri != null) {
                                        val share = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/x-vcard"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(share, "Export Contacts Backup"))
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accentSecondary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export VCF")
                        }
                    }
                }
            }
        }

        item {
            FrostedGlassCard(theme = theme, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Frosted Glass Theme", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    GlassThemeStyle.entries.forEach { itemTheme ->
                        val selected = itemTheme == theme
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) theme.accent.copy(alpha = 0.2f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (selected) theme.accent else theme.glassBorderBottom,
                                    RoundedCornerShape(16.dp)
                                )
                                .jellyBounce { onSelectTheme(itemTheme) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(itemTheme.title, color = theme.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(itemTheme.subtitle, color = theme.textSecondary, fontSize = 12.sp)
                            }
                            if (selected) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = theme.accent)
                            }
                        }
                    }
                }
            }
        }

        item {
            FrostedGlassCard(theme = theme, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("App & Tab Icon Pack", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconPackStyle.entries.forEach { pack ->
                        val selected = pack == iconPack
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) theme.accent.copy(alpha = 0.2f) else Color.Transparent)
                                .jellyBounce { onSelectIconPack(pack) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(pack.label, color = theme.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(pack.desc, color = theme.textSecondary, fontSize = 12.sp)
                            }
                            if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = theme.accent)
                        }
                    }
                }
            }
        }

        item {
            FrostedGlassCard(theme = theme, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("In-Call Screen Style", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    CallScreenStyle.entries.forEach { style ->
                        val selected = style == callStyle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) theme.accent.copy(alpha = 0.2f) else Color.Transparent)
                                .jellyBounce { onSelectCallStyle(style) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(style.label, color = theme.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(style.desc, color = theme.textSecondary, fontSize = 12.sp)
                            }
                            if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = theme.accent)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onTestCallUi(true) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Preview Incoming UI", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onTestCallUi(false) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Preview Active UI", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            FrostedGlassCard(theme = theme, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Online AI Key (Optional)", color = theme.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Offline spam shield & call scripts work automatically with zero API key. Add a Gemini API key only if you want cloud AI.",
                        color = theme.textSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = keyDraft,
                        onValueChange = { keyDraft = it },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onApiKeyChange(keyDraft.trim())
                            Toast.makeText(context, "API key saved", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save Key")
                    }
                }
            }
        }
    }
}

private fun requestDefaultDialerRole(context: Context, onLaunch: (Intent) -> Unit) {
    val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return
    if (telecom.defaultDialerPackage == context.packageName) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
            !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        ) {
            onLaunch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
        }
    } else {
        val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
            putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
        }
        onLaunch(intent)
    }
}

private suspend fun signInWithGoogleCredentialManager(
    context: Context,
    onSuccess: (String, String) -> Unit,
    onFallbackNeeded: () -> Unit
) {
    runCatching {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("000000000000-placeholder.apps.googleusercontent.com")
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val result = credentialManager.getCredential(context, request)
        val cred = GoogleIdTokenCredential.createFrom(result.credential.data)
        onSuccess(cred.displayName ?: "Google User", cred.id)
    }.onFailure {
        onFallbackNeeded()
    }
}
