package com.example.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.AppCurrency
import com.example.data.model.CashBox
import com.example.data.model.ExpenseCategory
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.ReportTemplate
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.data.repository.AccountingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalDatabaseBackupManager {

    private const val PREFS_NAME = "backup_preferences"
    private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
    private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
    private const val KEY_LAST_BACKUP_TX_COUNT = "last_backup_tx_count"
    private const val MAX_AUTO_BACKUPS_RETENTION = 14

    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH)

    private fun getBackupsDirectory(context: Context): File {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun isAutoBackupEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, true)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
    }

    fun getLastBackupTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
    }

    private fun setLastBackupMetadata(context: Context, time: Long, txCount: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_BACKUP_TIME, time)
            .putInt(KEY_LAST_BACKUP_TX_COUNT, txCount)
            .apply()
    }

    /**
     * Checks if an automatic backup is needed (e.g. interval passed or transaction count changed).
     */
    suspend fun checkAndTriggerAutoBackup(
        context: Context,
        repository: AccountingRepository
    ): BackupFileInfo? = withContext(Dispatchers.IO) {
        if (!isAutoBackupEnabled(context)) return@withContext null

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastTime = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        val lastTxCount = prefs.getInt(KEY_LAST_BACKUP_TX_COUNT, 0)
        val currentTime = System.currentTimeMillis()

        val currentTxList = repository.getAllTransactionsList()
        val currentTxCount = currentTxList.size

        // Criteria: 1) First time with data, 2) >= 12 hours since last backup, 3) >= 3 new transactions
        val twelveHoursMs = 12 * 60 * 60 * 1000L
        val needsBackup = lastTime == 0L && currentTxCount > 0 ||
                (currentTime - lastTime > twelveHoursMs) ||
                (Math.abs(currentTxCount - lastTxCount) >= 3)

        if (needsBackup) {
            return@withContext performBackup(context, repository, isAuto = true)
        }
        null
    }

    /**
     * Creates a full, structured backup of all database tables locally.
     */
    suspend fun performBackup(
        context: Context,
        repository: AccountingRepository,
        isAuto: Boolean = false
    ): BackupFileInfo = withContext(Dispatchers.IO) {
        val boxes = repository.getAllCashBoxesList()
        val parties = repository.database.partyDao().getAllParties()
        val transactions = repository.getAllTransactionsList()
        val templates = repository.database.reportTemplateDao().getAllTemplates()
        val currencies = repository.getAllCurrenciesList()
        val categories = repository.getAllExpenseCategoriesList()
        val memories = repository.getAllAiMemoriesList()
        val chatMessages = repository.getAllAiChatMessagesList()

        val jsonRoot = JSONObject().apply {
            put("version", 4)
            put("appName", "SmartAccountant")
            put("timestamp", System.currentTimeMillis())
            put("backupType", if (isAuto) "AUTO" else "MANUAL")

            // 1. Cash Boxes
            val boxesArray = JSONArray()
            boxes.forEach { b ->
                boxesArray.put(JSONObject().apply {
                    put("id", b.id)
                    put("name", b.name)
                    put("balance", b.balance)
                    put("isDefault", b.isDefault)
                    put("note", b.note)
                    put("createdAt", b.createdAt)
                })
            }
            put("cashBoxes", boxesArray)

            // 2. Parties
            val partiesArray = JSONArray()
            parties.forEach { p ->
                partiesArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("type", p.type.name)
                    put("phone", p.phone)
                    put("balance", p.balance)
                    put("notes", p.notes)
                    put("createdAt", p.createdAt)
                })
            }
            put("parties", partiesArray)

            // 3. Transactions
            val txArray = JSONArray()
            transactions.forEach { tx ->
                txArray.put(JSONObject().apply {
                    put("id", tx.id)
                    put("type", tx.type.name)
                    put("amount", tx.amount)
                    put("cashBoxId", tx.cashBoxId ?: JSONObject.NULL)
                    put("targetCashBoxId", tx.targetCashBoxId ?: JSONObject.NULL)
                    put("partyId", tx.partyId ?: JSONObject.NULL)
                    put("category", tx.category)
                    put("currency", tx.currency)
                    put("description", tx.description)
                    put("timestamp", tx.timestamp)
                    put("isAiGenerated", tx.isAiGenerated)
                })
            }
            put("transactions", txArray)

            // 4. Report Templates
            val tplArray = JSONArray()
            templates.forEach { t ->
                tplArray.put(JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                    put("datePreset", t.datePreset)
                    put("accountScope", t.accountScope)
                    put("transactionTypes", t.transactionTypes)
                    put("createdAt", t.createdAt)
                })
            }
            put("reportTemplates", tplArray)

            // 5. Currencies
            val currArray = JSONArray()
            currencies.forEach { c ->
                currArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("code", c.code)
                    put("name", c.name)
                    put("symbol", c.symbol)
                    put("isDefault", c.isDefault)
                    put("exchangeRate", c.exchangeRate)
                    put("createdAt", c.createdAt)
                })
            }
            put("currencies", currArray)

            // 6. Expense Categories
            val catArray = JSONArray()
            categories.forEach { cat ->
                catArray.put(JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("iconTag", cat.iconTag)
                    put("note", cat.note)
                    put("isDefault", cat.isDefault)
                    put("createdAt", cat.createdAt)
                })
            }
            put("expenseCategories", catArray)

            // 7. AI Memories
            val memArray = JSONArray()
            memories.forEach { m ->
                memArray.put(JSONObject().apply {
                    put("id", m.id)
                    put("category", m.category.name)
                    put("key", m.key)
                    put("fact", m.fact)
                    put("timestamp", m.timestamp)
                    put("isAutoLearned", m.isAutoLearned)
                    put("isActive", m.isActive)
                })
            }
            put("aiMemories", memArray)

            // 8. AI Chat Messages
            val chatArray = JSONArray()
            chatMessages.forEach { msg ->
                chatArray.put(JSONObject().apply {
                    put("id", msg.id)
                    put("text", msg.text)
                    put("isUser", msg.isUser)
                    put("timestamp", msg.timestamp)
                    put("isSuccess", msg.isSuccess)
                    put("actionType", msg.actionType ?: JSONObject.NULL)
                    put("relatedTransactionId", msg.relatedTransactionId ?: JSONObject.NULL)
                    put("learnedMemoryText", msg.learnedMemoryText ?: JSONObject.NULL)
                })
            }
            put("aiChatMessages", chatArray)
        }

        val dir = getBackupsDirectory(context)
        val prefix = if (isAuto) "auto_backup" else "manual_backup"
        val timestampStr = fileDateFormat.format(Date())
        val filename = "${prefix}_${timestampStr}.json"
        val file = File(dir, filename)

        FileOutputStream(file).use { out ->
            out.write(jsonRoot.toString(2).toByteArray(Charsets.UTF_8))
        }

        // Clean up old auto-backups to maintain max retention
        if (isAuto) {
            cleanOldAutoBackups(dir)
        }

        val now = System.currentTimeMillis()
        setLastBackupMetadata(context, now, transactions.size)

        BackupFileInfo(
            file = file,
            filename = filename,
            timestamp = now,
            sizeBytes = file.length(),
            isAuto = isAuto,
            transactionCount = transactions.size,
            cashBoxCount = boxes.size,
            partyCount = parties.size
        )
    }

    private fun cleanOldAutoBackups(dir: File) {
        try {
            val autoFiles = dir.listFiles { _, name -> name.startsWith("auto_backup_") && name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() } ?: return

            if (autoFiles.size > MAX_AUTO_BACKUPS_RETENTION) {
                autoFiles.drop(MAX_AUTO_BACKUPS_RETENTION).forEach { it.delete() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Lists all available local backups ordered from newest to oldest.
     */
    suspend fun listAvailableBackups(context: Context): List<BackupFileInfo> = withContext(Dispatchers.IO) {
        val dir = getBackupsDirectory(context)
        val files = dir.listFiles { _, name -> name.endsWith(".json") } ?: return@withContext emptyList()

        files.mapNotNull { file ->
            try {
                val jsonStr = file.readText(Charsets.UTF_8)
                val json = JSONObject(jsonStr)
                val isAuto = json.optString("backupType", "") == "AUTO" || file.name.startsWith("auto_backup_")
                val timestamp = json.optLong("timestamp", file.lastModified())
                val txCount = json.optJSONArray("transactions")?.length() ?: 0
                val boxCount = json.optJSONArray("cashBoxes")?.length() ?: 0
                val partyCount = json.optJSONArray("parties")?.length() ?: 0

                BackupFileInfo(
                    file = file,
                    filename = file.name,
                    timestamp = timestamp,
                    sizeBytes = file.length(),
                    isAuto = isAuto,
                    transactionCount = txCount,
                    cashBoxCount = boxCount,
                    partyCount = partyCount
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.timestamp }
    }

    /**
     * Restores database from a local backup file atomically.
     */
    suspend fun restoreBackup(
        context: Context,
        repository: AccountingRepository,
        backupFile: File
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonStr = backupFile.readText(Charsets.UTF_8)
            val json = JSONObject(jsonStr)
            restoreFromJson(context, repository, json)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("فشل استعادة النسخة الاحتياطية: ${e.localizedMessage}"))
        }
    }

    /**
     * Restores database from an external file URI (e.g. from File Picker).
     */
    suspend fun restoreBackupFromUri(
        context: Context,
        repository: AccountingRepository,
        uri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("تعذر قراءة الملف المختار"))
            val jsonStr = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonStr)
            restoreFromJson(context, repository, json)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("فشل استعادة النسخة من الملف: ${e.localizedMessage}"))
        }
    }

    private suspend fun restoreFromJson(
        context: Context,
        repository: AccountingRepository,
        json: JSONObject
    ): Result<String> {
        val db = repository.database

        // Parse Cash Boxes
        val boxesJson = json.optJSONArray("cashBoxes") ?: JSONArray()
        val boxes = mutableListOf<CashBox>()
        for (i in 0 until boxesJson.length()) {
            val b = boxesJson.getJSONObject(i)
            boxes.add(
                CashBox(
                    id = b.optLong("id", 0L),
                    name = b.optString("name", "صندوق"),
                    balance = b.optDouble("balance", 0.0),
                    isDefault = b.optBoolean("isDefault", false),
                    note = b.optString("note", ""),
                    createdAt = b.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Parse Parties
        val partiesJson = json.optJSONArray("parties") ?: JSONArray()
        val parties = mutableListOf<Party>()
        for (i in 0 until partiesJson.length()) {
            val p = partiesJson.getJSONObject(i)
            val typeStr = p.optString("type", "CUSTOMER")
            val type = try { PartyType.valueOf(typeStr) } catch (e: Exception) { PartyType.CUSTOMER }
            parties.add(
                Party(
                    id = p.optLong("id", 0L),
                    name = p.optString("name", "جهة"),
                    type = type,
                    phone = p.optString("phone", ""),
                    balance = p.optDouble("balance", 0.0),
                    notes = p.optString("notes", ""),
                    createdAt = p.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Parse Transactions
        val txJson = json.optJSONArray("transactions") ?: JSONArray()
        val transactions = mutableListOf<TransactionRecord>()
        for (i in 0 until txJson.length()) {
            val t = txJson.getJSONObject(i)
            val typeStr = t.optString("type", "CUSTOMER_RECEIPT")
            val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.CUSTOMER_RECEIPT }
            transactions.add(
                TransactionRecord(
                    id = t.optLong("id", 0L),
                    type = type,
                    amount = t.optDouble("amount", 0.0),
                    cashBoxId = if (t.isNull("cashBoxId")) null else t.optLong("cashBoxId"),
                    targetCashBoxId = if (t.isNull("targetCashBoxId")) null else t.optLong("targetCashBoxId"),
                    partyId = if (t.isNull("partyId")) null else t.optLong("partyId"),
                    category = t.optString("category", ""),
                    currency = t.optString("currency", "ر.س"),
                    description = t.optString("description", ""),
                    timestamp = t.optLong("timestamp", System.currentTimeMillis()),
                    isAiGenerated = t.optBoolean("isAiGenerated", false)
                )
            )
        }

        // Parse Templates
        val tplJson = json.optJSONArray("reportTemplates") ?: JSONArray()
        val templates = mutableListOf<ReportTemplate>()
        for (i in 0 until tplJson.length()) {
            val t = tplJson.getJSONObject(i)
            templates.add(
                ReportTemplate(
                    id = t.optLong("id", 0L),
                    name = t.optString("name", "تقرير"),
                    datePreset = t.optString("datePreset", "ALL"),
                    accountScope = t.optString("accountScope", "ALL"),
                    transactionTypes = t.optString("transactionTypes", ""),
                    createdAt = t.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Parse Currencies
        val currJson = json.optJSONArray("currencies") ?: JSONArray()
        val currencies = mutableListOf<AppCurrency>()
        for (i in 0 until currJson.length()) {
            val c = currJson.getJSONObject(i)
            currencies.add(
                AppCurrency(
                    id = c.optLong("id", 0L),
                    code = c.optString("code", "SAR"),
                    name = c.optString("name", "ريال سعودي"),
                    symbol = c.optString("symbol", "ر.س"),
                    isDefault = c.optBoolean("isDefault", false),
                    exchangeRate = c.optDouble("exchangeRate", 1.0),
                    createdAt = c.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Parse Expense Categories
        val catJson = json.optJSONArray("expenseCategories") ?: JSONArray()
        val categories = mutableListOf<ExpenseCategory>()
        for (i in 0 until catJson.length()) {
            val c = catJson.getJSONObject(i)
            categories.add(
                ExpenseCategory(
                    id = c.optLong("id", 0L),
                    name = c.optString("name", "عام"),
                    iconTag = c.optString("iconTag", "receipt"),
                    note = c.optString("note", ""),
                    isDefault = c.optBoolean("isDefault", false),
                    createdAt = c.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Parse AI Memories
        val memJson = json.optJSONArray("aiMemories") ?: JSONArray()
        val memories = mutableListOf<com.example.data.model.AiMemoryFact>()
        for (i in 0 until memJson.length()) {
            val m = memJson.getJSONObject(i)
            val catStr = m.optString("category", "GENERAL")
            val cat = try { com.example.data.model.MemoryCategory.valueOf(catStr) } catch (e: Exception) { com.example.data.model.MemoryCategory.GENERAL }
            memories.add(
                com.example.data.model.AiMemoryFact(
                    id = m.optLong("id", 0L),
                    category = cat,
                    key = m.optString("key", "معلومة"),
                    fact = m.optString("fact", ""),
                    timestamp = m.optLong("timestamp", System.currentTimeMillis()),
                    isAutoLearned = m.optBoolean("isAutoLearned", true),
                    isActive = m.optBoolean("isActive", true)
                )
            )
        }

        // Parse AI Chat Messages
        val chatJson = json.optJSONArray("aiChatMessages") ?: JSONArray()
        val chatMessages = mutableListOf<com.example.data.model.AiChatMessageEntity>()
        for (i in 0 until chatJson.length()) {
            val cm = chatJson.getJSONObject(i)
            chatMessages.add(
                com.example.data.model.AiChatMessageEntity(
                    id = cm.optString("id", java.util.UUID.randomUUID().toString()),
                    text = cm.optString("text", ""),
                    isUser = cm.optBoolean("isUser", false),
                    timestamp = cm.optLong("timestamp", System.currentTimeMillis()),
                    isSuccess = cm.optBoolean("isSuccess", true),
                    actionType = if (cm.isNull("actionType")) null else cm.optString("actionType"),
                    relatedTransactionId = if (cm.isNull("relatedTransactionId")) null else cm.optLong("relatedTransactionId"),
                    learnedMemoryText = if (cm.isNull("learnedMemoryText")) null else cm.optString("learnedMemoryText")
                )
            )
        }

        // Atomic replace in Room DB
        db.withTransaction {
            db.transactionDao().deleteAll()
            db.partyDao().deleteAll()
            db.cashBoxDao().deleteAll()
            db.reportTemplateDao().deleteAll()
            db.currencyDao().deleteAll()
            db.expenseCategoryDao().deleteAll()
            db.aiMemoryDao().clearAll()
            db.aiChatMessageDao().clearAll()

            if (boxes.isNotEmpty()) db.cashBoxDao().insertAll(boxes)
            if (parties.isNotEmpty()) db.partyDao().insertAll(parties)
            if (transactions.isNotEmpty()) db.transactionDao().insertAll(transactions)
            if (templates.isNotEmpty()) db.reportTemplateDao().insertAll(templates)
            if (currencies.isNotEmpty()) db.currencyDao().insertAll(currencies)
            if (categories.isNotEmpty()) db.expenseCategoryDao().insertAll(categories)
            if (memories.isNotEmpty()) db.aiMemoryDao().insertAll(memories)
            if (chatMessages.isNotEmpty()) db.aiChatMessageDao().insertAll(chatMessages)
        }

        // Ensure defaults if any were missing
        repository.ensureDefaultCashBox()
        repository.ensureDefaultCurrencies()
        repository.ensureDefaultExpenseCategories()

        return Result.success("تم استعادة البيانات بنجاح (${transactions.size} عملية، ${parties.size} طرف، ${boxes.size} صندوق)")
    }

    /**
     * Deletes a local backup file.
     */
    fun deleteBackup(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Shares a backup file via Android Sharesheet.
     */
    fun shareBackupFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية محاسبية - ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة النسخة الاحتياطية"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
