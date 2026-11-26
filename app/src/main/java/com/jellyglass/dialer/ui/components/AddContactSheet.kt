package com.jellyglass.dialer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jellyglass.dialer.ui.theme.GlassThemeStyle

@Composable
fun AddContactDialog(
    initialPhone: String = "",
    hasWhatsAppMatch: Boolean = false,
    googleEmail: String = "",
    theme: GlassThemeStyle,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, syncToGoogle: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(initialPhone) }
    var syncToGoogle by remember { mutableStateOf(googleEmail.isNotBlank()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (theme.isDark) Color(0xFF131D36) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = theme.accent)
                Text(
                    text = "New Contact",
                    color = theme.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (hasWhatsAppMatch) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF25D366).copy(alpha = 0.14f))
                            .padding(10.dp)
                    ) {
                        WhatsAppBadge()
                        Text(
                            text = "WhatsApp account active on this number",
                            color = theme.textPrimary,
                            fontSize = 12.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (googleEmail.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Save to Google Cloud", color = theme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(googleEmail, color = theme.textSecondary, fontSize = 11.sp)
                        }
                        Switch(checked = syncToGoogle, onCheckedChange = { syncToGoogle = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phone.isNotBlank()) {
                        onSave(name.ifBlank { phone }, phone, syncToGoogle)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Contact", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = theme.textSecondary)
            }
        }
    )
}
