package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AccountingAssistant
import com.example.data.local.AppDatabase
import com.example.data.model.AiChatMessageEntity
import com.example.data.model.AiMemoryFact
import com.example.data.model.AppCurrency
import com.example.data.model.CashBox
import com.example.data.model.ExpenseCategory
import com.example.data.model.MemoryCategory
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.data.repository.AccountingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true,
    val actionType: String? = null,
    val learnedMemoryText: String? = null
)

fun AiChatMessageEntity.toChatMessage(): ChatMessage = ChatMessage(
    id = id,
    text = text,
    isUser = isUser,
    timestamp = timestamp,
    isSuccess = isSuccess,
    actionType = actionType,
    learnedMemoryText = learnedMemoryText
)

data class FinancialSummary(
    val totalCash: Double = 0.0,
    val owedToMe: Double = 0.0,      // ما لي عند العملاء
    val owedByMe: Double = 0.0,      // ما علي للموردين
    val netWorth: Double = 0.0,      // صافي الثروة
    val currencySymbol: String = "ر.س"
)

class AccountingViewModel(application: Application) : AndroidViewModel(application) {

    val repository: AccountingRepository
    val aiPreferences: com.example.ai.AiPreferencesManager
    val themePreferences: com.example.ui.theme.ThemePreferences
    val themeMode: StateFlow<com.example.ui.theme.AppThemeMode>
    private val geminiService: com.example.ai.GeminiAccountingService
    private val assistant: AccountingAssistant

    val cashBoxes: StateFlow<List<CashBox>>
    val parties: StateFlow<List<Party>>
    val transactions: StateFlow<List<TransactionRecord>>
    val recentTransactions: StateFlow<List<TransactionRecord>>
    val reportTemplates: StateFlow<List<com.example.data.model.ReportTemplate>>
    val currencies: StateFlow<List<AppCurrency>>
    val expenseCategories: StateFlow<List<ExpenseCategory>>
    val aiMemories: StateFlow<List<AiMemoryFact>>
    val activeMemoriesCount: StateFlow<Int>

    val defaultCurrency: StateFlow<AppCurrency?>
    val defaultCashBox: StateFlow<CashBox?>

    val chatMessages: StateFlow<List<ChatMessage>>

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _operationFeedback = MutableStateFlow<String?>(null)
    val operationFeedback: StateFlow<String?> = _operationFeedback.asStateFlow()

    init {
        val database = AppDatabase.getInstance(application)
        repository = AccountingRepository(database)
        aiPreferences = com.example.ai.AiPreferencesManager.getInstance(application)
        themePreferences = com.example.ui.theme.ThemePreferences(application)
        themeMode = themePreferences.themeMode
        geminiService = com.example.ai.GeminiAccountingService(aiPreferences)
        assistant = AccountingAssistant(geminiService)

        cashBoxes = repository.allCashBoxes.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        parties = repository.allParties.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        transactions = repository.allTransactions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        recentTransactions = repository.recentTransactions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        reportTemplates = repository.allReportTemplates.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        currencies = repository.allCurrencies.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        expenseCategories = repository.allExpenseCategories.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        aiMemories = repository.allAiMemories.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        activeMemoriesCount = repository.activeAiMemoriesCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

        chatMessages = repository.allAiChatMessages.map { list ->
            if (list.isEmpty()) {
                listOf(
                    ChatMessage(
                        text = "مرحباً بك! أنا محاسبك المالي ومستشارك الذكي 🤖\nأفهم حديثك المالي بالنص أو الصوت، وأتذكر تفاصيل نشاطك وقواعدك بدقة:\n• 'استلمت 500 من العميل محمد'\n• 'صرفت 60 بنزين' (سأحدد البند والصندوق تلقائياً)\n• 'تذكر أن خالد شريكي في الأرباح'\n• 'ماذا تتذكر عني؟' أو 'كم رصيدي وصافي مالي؟'\nوسأتولى قيدها وحفظها في الذاكرة!",
                        isUser = false
                    )
                )
            } else {
                list.map { it.toChatMessage() }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        defaultCurrency = currencies.map { list ->
            list.firstOrNull { it.isDefault } ?: list.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        defaultCashBox = cashBoxes.map { list ->
            list.firstOrNull { it.isDefault } ?: list.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        viewModelScope.launch {
            repository.ensureDefaultCashBox()
            repository.ensureDefaultCurrencies()
            repository.ensureDefaultExpenseCategories()

            // Check if initial greeting should be saved
            val existingMsgs = repository.getAllAiChatMessagesList()
            if (existingMsgs.isEmpty()) {
                repository.saveAiChatMessage(
                    AiChatMessageEntity(
                        text = "مرحباً بك! أنا محاسبك المالي ومستشارك الذكي 🤖\nأفهم حديثك المالي بالنص أو الصوت، وأتذكر تفاصيل نشاطك وقواعدك بدقة:\n• 'استلمت 500 من العميل محمد'\n• 'صرفت 60 بنزين' (سأحدد البند والصندوق تلقائياً)\n• 'تذكر أن خالد شريكي في الأرباح'\n• 'ماذا تتذكر عني؟' أو 'كم رصيدي وصافي مالي؟'\nوسأتولى قيدها وحفظها في الذاكرة!",
                        isUser = false
                    )
                )
            }

            com.example.data.backup.LocalDatabaseBackupManager.checkAndTriggerAutoBackup(application, repository)
        }
    }

    val summary: StateFlow<FinancialSummary> = combine(cashBoxes, parties, defaultCurrency) { boxes, partyList, defCurr ->
        val totalCash = boxes.sumOf { it.balance }
        val owedToMe = partyList.filter { it.type == PartyType.CUSTOMER && it.balance > 0 }.sumOf { it.balance }
        val owedByMe = partyList.filter { it.type == PartyType.SUPPLIER && it.balance > 0 }.sumOf { it.balance }
        val net = totalCash + owedToMe - owedByMe
        val symbol = defCurr?.symbol?.ifBlank { "ر.س" } ?: "ر.س"
        FinancialSummary(
            totalCash = totalCash,
            owedToMe = owedToMe,
            owedByMe = owedByMe,
            netWorth = net,
            currencySymbol = symbol
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummary())

    fun clearFeedback() {
        _operationFeedback.value = null
    }

    // AI Chat & Voice Processor with Full Multi-Turn Memory & Active Fact Injection
    suspend fun transcribeAudioFile(file: java.io.File, mimeType: String = "audio/mp4"): String? {
        return try {
            if (!file.exists() || file.length() <= 0) return null
            val bytes = file.readBytes()
            geminiService.transcribeAudioBytes(bytes, mimeType)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun assistantProcessSpeech(spokenText: String): com.example.ai.AssistantResult {
        val userEntity = AiChatMessageEntity(
            text = "🎙️ $spokenText",
            isUser = true
        )
        repository.saveAiChatMessage(userEntity)

        val activeMems = repository.getActiveAiMemoriesList()
        val historyTurns = repository.getRecentAiChatMessages(12).reversed().map { Pair(it.text, it.isUser) }

        val result = assistant.processUserSpeechOrText(
            input = spokenText,
            repository = repository,
            existingParties = parties.value,
            existingBoxes = cashBoxes.value,
            existingCategories = expenseCategories.value,
            existingCurrencies = currencies.value,
            activeMemories = activeMems,
            conversationHistory = historyTurns
        )

        val aiEntity = AiChatMessageEntity(
            text = result.reply,
            isUser = false,
            isSuccess = result.isSuccess,
            actionType = result.actionType,
            relatedTransactionId = result.executedTransaction?.id,
            learnedMemoryText = result.learnedMemory?.fact
        )
        repository.saveAiChatMessage(aiEntity)

        if (result.executedTransaction != null) {
            _operationFeedback.value = "تم تسجيل العملية صوتياً وتحديث الرصيد"
            com.example.data.backup.LocalDatabaseBackupManager.checkAndTriggerAutoBackup(getApplication(), repository)
        }
        return result
    }

    suspend fun testGeminiConnection(apiKey: String, model: String): Result<String> {
        return geminiService.testConnection(apiKey, model)
    }

    fun sendAiMessage(userText: String) {
        if (userText.isBlank()) return
        val trimmed = userText.trim()

        viewModelScope.launch {
            val userEntity = AiChatMessageEntity(
                text = trimmed,
                isUser = true
            )
            repository.saveAiChatMessage(userEntity)
            _isAiThinking.value = true

            try {
                val activeMems = repository.getActiveAiMemoriesList()
                val historyTurns = repository.getRecentAiChatMessages(12).reversed().map { Pair(it.text, it.isUser) }

                val result = assistant.processUserSpeechOrText(
                    input = trimmed,
                    repository = repository,
                    existingParties = parties.value,
                    existingBoxes = cashBoxes.value,
                    existingCategories = expenseCategories.value,
                    existingCurrencies = currencies.value,
                    activeMemories = activeMems,
                    conversationHistory = historyTurns
                )

                val aiEntity = AiChatMessageEntity(
                    text = result.reply,
                    isUser = false,
                    isSuccess = result.isSuccess,
                    actionType = result.actionType,
                    relatedTransactionId = result.executedTransaction?.id,
                    learnedMemoryText = result.learnedMemory?.fact
                )
                repository.saveAiChatMessage(aiEntity)

                if (result.executedTransaction != null) {
                    _operationFeedback.value = "تم تسجيل العملية وتحديث الرصيد بنجاح"
                    com.example.data.backup.LocalDatabaseBackupManager.checkAndTriggerAutoBackup(getApplication(), repository)
                }
            } catch (e: Exception) {
                repository.saveAiChatMessage(
                    AiChatMessageEntity(
                        text = "عذراً، حدث خطأ أثناء المعالجة: ${e.localizedMessage}",
                        isUser = false,
                        isSuccess = false,
                        actionType = "ERROR"
                    )
                )
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    // AI Memory Management
    fun addAiMemory(key: String, fact: String, category: MemoryCategory = MemoryCategory.GENERAL) {
        viewModelScope.launch {
            val memory = AiMemoryFact(
                key = key.trim(),
                fact = fact.trim(),
                category = category,
                isAutoLearned = false,
                isActive = true
            )
            repository.addAiMemory(memory)
            _operationFeedback.value = "🧠 تمت إضافة التوجيه إلى ذاكرة المحاسب بنجاح"
        }
    }

    fun updateAiMemory(
        memory: AiMemoryFact,
        newKey: String,
        newFact: String,
        newCategory: MemoryCategory,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            val updated = memory.copy(
                key = newKey.trim(),
                fact = newFact.trim(),
                category = newCategory,
                isActive = isActive
            )
            repository.updateAiMemory(updated)
            _operationFeedback.value = "🧠 تم تحديث المعلومة في الذاكرة"
        }
    }

    fun toggleAiMemoryActive(memoryId: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleAiMemoryActive(memoryId, isActive)
        }
    }

    fun deleteAiMemory(memory: AiMemoryFact) {
        viewModelScope.launch {
            repository.deleteAiMemory(memory)
            _operationFeedback.value = "تم حذف المعلومة من ذاكرة المحاسب"
        }
    }

    fun clearAllAiMemories() {
        viewModelScope.launch {
            repository.clearAllAiMemories()
            _operationFeedback.value = "تمت إعادة ضبط ذاكرة المحاسب بالكامل"
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearAllAiChatMessages()
            // Re-insert welcoming initial message
            repository.saveAiChatMessage(
                AiChatMessageEntity(
                    text = "تم بدء جلسة محادثة جديدة! يمكنك سؤالي أو إعطائي أي عملية أو قاعدة محاسبية وسأتذكرها فوراً.",
                    isUser = false
                )
            )
            _operationFeedback.value = "تم مسح سجل المحادثة"
        }
    }

    // Direct Form Actions
    fun recordDirectTransaction(
        type: TransactionType,
        amount: Double,
        cashBoxId: Long?,
        partyId: Long?,
        description: String,
        targetCashBoxId: Long? = null,
        category: String = "",
        currency: String = ""
    ) {
        viewModelScope.launch {
            val defBox = defaultCashBox.value
            val effectiveBoxId = cashBoxId ?: defBox?.id
            val defCurr = defaultCurrency.value?.symbol ?: "ر.س"
            val effectiveCurrency = currency.ifBlank { defCurr }

            val tx = TransactionRecord(
                type = type,
                amount = amount,
                cashBoxId = effectiveBoxId,
                targetCashBoxId = targetCashBoxId,
                partyId = partyId,
                category = category,
                currency = effectiveCurrency,
                description = description.ifBlank { type.titleAr },
                timestamp = System.currentTimeMillis()
            )
            repository.recordTransaction(tx)
            _operationFeedback.value = "تم تسجيل عملية ${type.titleAr} بنجاح"
            com.example.data.backup.LocalDatabaseBackupManager.checkAndTriggerAutoBackup(getApplication(), repository)
        }
    }

    // Cash Boxes Operations
    fun addCashBox(name: String, initialBalance: Double, note: String, isDefault: Boolean = false) {
        viewModelScope.launch {
            val box = CashBox(
                name = name.trim(),
                balance = initialBalance,
                isDefault = isDefault,
                note = note.trim()
            )
            val boxId = repository.addCashBox(box)
            if (initialBalance != 0.0) {
                val tx = TransactionRecord(
                    type = TransactionType.INCOME,
                    amount = initialBalance,
                    cashBoxId = boxId,
                    description = "رصيد افتتاحي للصندوق",
                    timestamp = System.currentTimeMillis()
                )
                repository.recordTransaction(tx)
            }
            _operationFeedback.value = "تمت إضافة الخزينة/الصندوق بنجاح"
        }
    }

    fun updateCashBox(
        box: CashBox,
        newName: String,
        newNote: String,
        isDefault: Boolean,
        adjustedBalance: Double? = null
    ) {
        viewModelScope.launch {
            val updated = box.copy(
                name = newName.trim(),
                note = newNote.trim(),
                isDefault = isDefault
            )
            repository.updateCashBox(updated)

            if (adjustedBalance != null && adjustedBalance != box.balance) {
                repository.setCashBoxBalance(box.id, adjustedBalance)
            }
            _operationFeedback.value = "تم تعديل بيانات الصندوق بنجاح"
        }
    }

    fun setDefaultCashBox(boxId: Long) {
        viewModelScope.launch {
            repository.setDefaultCashBox(boxId)
            _operationFeedback.value = "تم تعيين الصندوق كصندوق افتراضي"
        }
    }

    fun transferBetweenBoxes(fromBoxId: Long, toBoxId: Long, amount: Double, note: String) {
        viewModelScope.launch {
            val tx = TransactionRecord(
                type = TransactionType.TRANSFER,
                amount = amount,
                cashBoxId = fromBoxId,
                targetCashBoxId = toBoxId,
                description = if (note.isNotBlank()) note else "تحويل بين الصناديق",
                timestamp = System.currentTimeMillis()
            )
            repository.recordTransaction(tx)
            _operationFeedback.value = "تم تحويل المبلغ بنجاح"
        }
    }

    fun deleteCashBox(box: CashBox) {
        viewModelScope.launch {
            if (box.isDefault) {
                _operationFeedback.value = "لا يمكن حذف الصندوق الرئيسي الافتراضي"
                return@launch
            }
            repository.deleteCashBox(box)
            _operationFeedback.value = "تم حذف الصندوق"
        }
    }

    // Parties Operations
    fun addParty(name: String, type: PartyType, phone: String, initialBalance: Double, notes: String) {
        viewModelScope.launch {
            val party = Party(
                name = name.trim(),
                type = type,
                phone = phone.trim(),
                balance = 0.0,
                notes = notes.trim()
            )
            val partyId = repository.addParty(party)
            if (initialBalance > 0) {
                val txType = if (type == PartyType.CUSTOMER) TransactionType.CUSTOMER_NEW_DEBIT else TransactionType.SUPPLIER_NEW_CREDIT
                val tx = TransactionRecord(
                    type = txType,
                    amount = initialBalance,
                    partyId = partyId,
                    description = "رصيد افتتاحي سابق",
                    timestamp = System.currentTimeMillis()
                )
                repository.recordTransaction(tx)
            }
            _operationFeedback.value = "تمت إضافة ${if (type == PartyType.CUSTOMER) "العميل" else "المورد"} بنجاح"
        }
    }

    fun updateParty(
        party: Party,
        newName: String,
        newPhone: String,
        newNotes: String,
        adjustedBalance: Double? = null
    ) {
        viewModelScope.launch {
            val updated = party.copy(
                name = newName.trim(),
                phone = newPhone.trim(),
                notes = newNotes.trim()
            )
            repository.updateParty(updated)

            if (adjustedBalance != null && adjustedBalance != party.balance) {
                repository.setPartyBalance(party.id, adjustedBalance)
            }
            _operationFeedback.value = "تم تعديل بيانات ${if (party.type == PartyType.CUSTOMER) "العميل" else "المورد"} بنجاح"
        }
    }

    fun deleteParty(party: Party) {
        viewModelScope.launch {
            repository.deleteParty(party)
            _operationFeedback.value = "تم حذف الطرف بنجاح"
        }
    }

    // Currencies Operations
    fun addCurrency(code: String, name: String, symbol: String, isDefault: Boolean) {
        viewModelScope.launch {
            val currency = AppCurrency(
                code = code.trim().uppercase(),
                name = name.trim(),
                symbol = symbol.trim(),
                isDefault = isDefault
            )
            repository.addCurrency(currency)
            _operationFeedback.value = "تمت إضافة العملة بنجاح"
        }
    }

    fun updateCurrency(currency: AppCurrency, code: String, name: String, symbol: String, isDefault: Boolean) {
        viewModelScope.launch {
            val updated = currency.copy(
                code = code.trim().uppercase(),
                name = name.trim(),
                symbol = symbol.trim(),
                isDefault = isDefault
            )
            repository.updateCurrency(updated)
            _operationFeedback.value = "تم تعديل بيانات العملة"
        }
    }

    fun setDefaultCurrency(currencyId: Long) {
        viewModelScope.launch {
            repository.setDefaultCurrency(currencyId)
            _operationFeedback.value = "تم تعيين العملة الافتراضية بنجاح"
        }
    }

    fun deleteCurrency(currency: AppCurrency) {
        viewModelScope.launch {
            repository.deleteCurrency(currency)
            _operationFeedback.value = "تم حذف العملة"
        }
    }

    // Expense Categories Operations
    fun addExpenseCategory(
        name: String,
        iconTag: String = "receipt",
        note: String = "",
        monthlyBudget: Double = 0.0,
        alertThresholdPercent: Int = 80
    ) {
        viewModelScope.launch {
            val cat = ExpenseCategory(
                name = name.trim(),
                iconTag = iconTag,
                note = note.trim(),
                monthlyBudget = monthlyBudget.coerceAtLeast(0.0),
                alertThresholdPercent = alertThresholdPercent.coerceIn(50, 100)
            )
            repository.addExpenseCategory(cat)
            _operationFeedback.value = "تمت إضافة بند المصروف بنجاح"
        }
    }

    fun updateExpenseCategory(category: ExpenseCategory, newName: String, newNote: String, iconTag: String) {
        viewModelScope.launch {
            val updated = category.copy(
                name = newName.trim(),
                note = newNote.trim(),
                iconTag = iconTag
            )
            repository.updateExpenseCategory(updated)
            _operationFeedback.value = "تم تعديل بند المصروف بنجاح"
        }
    }

    fun updateCategoryBudget(category: ExpenseCategory, monthlyBudget: Double, alertThresholdPercent: Int = 80) {
        viewModelScope.launch {
            val updated = category.copy(
                monthlyBudget = monthlyBudget.coerceAtLeast(0.0),
                alertThresholdPercent = alertThresholdPercent.coerceIn(50, 100)
            )
            repository.updateExpenseCategory(updated)
            _operationFeedback.value = "تم تحديث الميزانية التقديرية لبند (${category.name})"
        }
    }

    fun deleteExpenseCategory(category: ExpenseCategory) {
        viewModelScope.launch {
            repository.deleteExpenseCategory(category)
            _operationFeedback.value = "تم حذف بند المصروف"
        }
    }

    // Transaction & Report Templates
    fun deleteTransaction(tx: TransactionRecord) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            _operationFeedback.value = "تم حذف العملية وإرجاع الأرصدة لسابقها"
            com.example.data.backup.LocalDatabaseBackupManager.checkAndTriggerAutoBackup(getApplication(), repository)
        }
    }

    fun saveReportTemplate(template: com.example.data.model.ReportTemplate) {
        viewModelScope.launch {
            repository.saveReportTemplate(template)
            _operationFeedback.value = "تم حفظ نموذج التقرير بنجاح"
        }
    }

    fun deleteReportTemplate(template: com.example.data.model.ReportTemplate) {
        viewModelScope.launch {
            repository.deleteReportTemplate(template)
            _operationFeedback.value = "تم حذف نموذج التقرير"
        }
    }

    fun setThemeMode(mode: com.example.ui.theme.AppThemeMode) {
        themePreferences.setThemeMode(mode)
        _operationFeedback.value = "تم تغيير مظهر التطبيق إلى: ${mode.title}"
    }
}
