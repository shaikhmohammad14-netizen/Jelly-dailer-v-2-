package com.jellyglass.dialer.data

import android.Manifest
import android.content.ContentProviderOperation
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.jellyglass.dialer.ai.SpamAndAiEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ContactsRepository(private val context: Context) {

    fun normalizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    suspend fun fetchWhatsAppNumbers(): Set<String> = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) return@withContext emptySet()

        val waNumbers = mutableSetOf<String>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.RawContacts.ACCOUNT_TYPE
        )
        val selection = "${ContactsContract.RawContacts.ACCOUNT_TYPE} IN (?, ?)"
        val selectionArgs = arrayOf("com.whatsapp", "com.whatsapp.w4b")

        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val raw = cursor.getString(numIdx) ?: continue
                    val norm = normalizePhone(raw)
                    if (norm.length >= 7) waNumbers.add(norm)
                }
            }
        }
        waNumbers
    }

    suspend fun loadContacts(cloudFilterEmail: String? = null): List<ContactItem> = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) return@withContext emptyList()

        val waSet = fetchWhatsAppNumbers()
        val contactMap = LinkedHashMap<String, ContactItem>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.STARRED,
            ContactsContract.RawContacts.ACCOUNT_NAME
        )

        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val starIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
                val accIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIdx)?.trim().orEmpty()
                    val number = cursor.getString(numIdx)?.trim().orEmpty()
                    if (number.isBlank()) continue
                    val norm = normalizePhone(number)
                    val accName = if (accIdx >= 0) cursor.getString(accIdx) ?: "Device" else "Device"

                    if (!cloudFilterEmail.isNullOrBlank() && !accName.equals(cloudFilterEmail, ignoreCase = true)) {
                        continue
                    }

                    if (!contactMap.containsKey(norm)) {
                        contactMap[norm] = ContactItem(
                            id = cursor.getString(idIdx) ?: norm,
                            name = name.ifBlank { number },
                            phone = number,
                            hasWhatsApp = waSet.contains(norm),
                            isFavorite = if (starIdx >= 0) cursor.getInt(starIdx) == 1 else false,
                            accountSource = accName
                        )
                    }
                }
            }
        }
        contactMap.values.toList()
    }

    suspend fun loadRecents(): List<RecentCallItem> = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) return@withContext emptyList()

        val waSet = fetchWhatsAppNumbers()
        val list = mutableListOf<RecentCallItem>()

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        runCatching {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT 120"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)

                while (cursor.moveToNext()) {
                    val number = cursor.getString(numIdx).orEmpty()
                    val name = cursor.getString(nameIdx).orEmpty()
                    val norm = normalizePhone(number)
                    val spamAnalysis = SpamAndAiEngine.analyzeOfflineSpam(number, name)

                    list.add(
                        RecentCallItem(
                            id = cursor.getString(idIdx) ?: "${System.nanoTime()}",
                            name = name.ifBlank { number.ifBlank { "Unknown" } },
                            number = number,
                            type = cursor.getInt(typeIdx),
                            timestamp = cursor.getLong(dateIdx),
                            durationSec = cursor.getLong(durIdx),
                            hasWhatsApp = waSet.contains(norm),
                            spamRiskScore = spamAnalysis.score,
                            spamLabel = if (spamAnalysis.isSpam) spamAnalysis.reason else null
                        )
                    )
                }
            }
        }
        list
    }

    suspend fun addNewContact(
        name: String,
        phone: String,
        accountEmail: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) return@withContext false

        val ops = ArrayList<ContentProviderOperation>()
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(
                    ContactsContract.RawContacts.ACCOUNT_TYPE,
                    if (!accountEmail.isNullOrBlank()) "com.google" else null
                )
                .withValue(
                    ContactsContract.RawContacts.ACCOUNT_NAME,
                    accountEmail?.takeIf { it.isNotBlank() }
                )
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name.trim())
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                )
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.trim())
                .withValue(
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                )
                .build()
        )

        return@withContext runCatching {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        }.getOrDefault(false)
    }

    suspend fun exportContactsToVcf(contacts: List<ContactItem>): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "contacts").apply { mkdirs() }
            val vcfFile = File(dir, "iGlass_Contacts_Backup.vcf")
            val sb = StringBuilder()
            contacts.forEach { c ->
                sb.appendLine("BEGIN:VCARD")
                sb.appendLine("VERSION:3.0")
                sb.appendLine("FN:${c.name}")
                sb.appendLine("TEL;TYPE=CELL:${c.phone}")
                sb.appendLine("END:VCARD")
            }
            vcfFile.writeText(sb.toString())
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                vcfFile
            )
        }.getOrNull()
    }

    suspend fun importContactsFromVcf(uri: Uri, accountEmail: String?): Int = withContext(Dispatchers.IO) {
        var importedCount = 0
        runCatching {
            val lines = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readLines() ?: return@runCatching
            var currentName = ""
            var currentPhone = ""
            for (line in lines) {
                when {
                    line.startsWith("BEGIN:VCARD", ignoreCase = true) -> {
                        currentName = ""
                        currentPhone = ""
                    }
                    line.startsWith("FN:", ignoreCase = true) -> {
                        currentName = line.substringAfter(":").trim()
                    }
                    line.startsWith("TEL", ignoreCase = true) -> {
                        currentPhone = line.substringAfter(":").trim()
                    }
                    line.startsWith("END:VCARD", ignoreCase = true) -> {
                        if (currentPhone.isNotBlank()) {
                            val saved = addNewContact(
                                name = currentName.ifBlank { currentPhone },
                                phone = currentPhone,
                                accountEmail = accountEmail
                            )
                            if (saved) importedCount++
                        }
                    }
                }
            }
        }
        importedCount
    }

    fun openWhatsAppChat(phone: String) {
        val clean = phone.filter { it.isDigit() }
        val formatted = if (clean.length == 10) "91$clean" else clean
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formatted")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
