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
import java.text.DecimalFormat

data class ChatReportAction(
    val reportType: String, // "DAILY", "PARTY", "CASH", "CUSTOM"
    val reportTitle: String,
    val partyId: Long? = null,
    val partyName: String? = null,
    val cashBoxId: Long? = null,
    val cashBoxName: String? = null,
    val totalIn: Double = 0.0,
    val totalOut: Double = 0.0,
    val netBalance: Double = 0.0,
    val count: Int = 0
) {
    fun toActionTypeString(): String {
        return "REPORT|$reportType|$reportTitle|${partyId ?: 0}|${partyName ?: ""}|${cashBoxId ?: 0}|${cashBoxName ?: ""}|$totalIn|$totalOut|$netBalance|$count"
    }

    companion object {
        fun fromActionTypeString(actionType: String?): ChatReportAction? {
            if (actionType == null || !actionType.startsWith("REPORT|")) return null
            return try {
                val parts = actionType.split("|")
                if (parts.size >= 11) {
                    ChatReportAction(
                        reportType = parts[1],
                        reportTitle = parts[2],
                        partyId = parts[3].toLongOrNull()?.takeIf { it > 0 },
                        partyName = parts[4].ifBlank { null },
                        cashBoxId = parts[5].toLongOrNull()?.takeIf { it > 0 },
                        cashBoxName = parts[6].ifBlank { null },
                        totalIn = parts[7].toDoubleOrNull() ?: 0.0,
                        totalOut = parts[8].toDoubleOrNull() ?: 0.0,
                        netBalance = parts[9].toDoubleOrNull() ?: 0.0,
                        count = parts[10].toIntOrNull() ?: 0
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true,
    val actionType: String? = null,
    val learnedMemoryText: String? = null,
    val reportAction: ChatReportAction? = null
)

fun AiChatMessageEntity.toChatMessage(): ChatMessage = ChatMessage(
    id = id,
    text = text,
    isUser = isUser,
    timestamp = timestamp,
    isSuccess = isSuccess,
    actionType = actionType,
    learnedMemoryText = learnedMemoryText,
    reportAction = ChatReportAction.fromActionTypeString(actionType)
)

data class FinancialSummary(
    val totalCash: Double = 0.0,
    val owedToMe: Double = 0.0,      // ما لي عند العملاء
    val owedByMe: Double = 0.0,      // ما علي للموردين
    val netWorth: Double = 0.0,      // صافي الثروة
    val currencySymbol: String = "ر.س"
)

data class FinancialHealth(
    val score: Int = 100, // 0 - 100
    val status: String = "ممتاز",
    val message: String = "الوضع المالي متزن ومطمئن",
    val assetsRatio: Float = 1.0f
)

data class CashRunway(
    val dailyExpense: Double = 0.0,
    val runwayDays: Int = 90,
    val totalMonthlyExpense: Double = 0.0,
    val statusText: String = "سيولة كافية ومستقرة"
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
    val journalEntries: StateFlow<List<com.example.data.model.JournalEntry>>

    val defaultCurrency: StateFlow<AppCurrency?>
    val defaultCashBox: StateFlow<CashBox?>

    val chatMessages: StateFlow<List<ChatMessage>>

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _operationFeedback = MutableStateFlow<String?>(null)
    val operationFeedback: StateFlow<String?> = _operationFeedback.asStateFlow()

    private val _trialBalance = MutableStateFlow<List<com.example.data.model.TrialBalanceRow>>(emptyList())
    val trialBalance: StateFlow<List<com.example.data.model.TrialBalanceRow>> = _trialBalance.asStateFlow()

    private val _financialStatementSummary = MutableStateFlow<com.example.data.model.FinancialStatementSummary?>(null)
    val financialStatementSummary: StateFlow<com.example.data.model.FinancialStatementSummary?> = _financialStatementSummary.asStateFlow()

    private val _journalEntriesWithLines = MutableStateFlow<List<com.example.data.model.JournalEntryWithLines>>(emptyList())
    val journalEntriesWithLines: StateFlow<List<com.example.data.model.JournalEntryWithLines>> = _journalEntriesWithLines.asStateFlow()

    private val _auditReport = MutableStateFlow<com.example.domain.AuditReportResult?>(null)
    val auditReport: StateFlow<com.example.domain.AuditReportResult?> = _auditReport.asStateFlow()

    private val _isAuditing = MutableStateFlow(false)
    val isAuditing: StateFlow<Boolean> = _isAuditing.asStateFlow()

    private val _batchItems = MutableStateFlow<List<com.example.domain.BatchCommandItem>>(emptyList())
    val batchItems: StateFlow<List<com.example.domain.BatchCommandItem>> = _batchItems.asStateFlow()

    private val _isBatchExecuting = MutableStateFlow(false)
    val isBatchExecuting: StateFlow<Boolean> = _isBatchExecuting.asStateFlow()

    private val _batchProgress = MutableStateFlow(Pair(0, 0))
    val batchProgress: StateFlow<Pair<Int, Int>> = _batchProgress.asStateFlow()

    private val _batchSummary = MutableStateFlow<com.example.domain.BatchExecutionSummary?>(null)
    val batchSummary: StateFlow<com.example.domain.BatchExecutionSummary?> = _batchSummary.asStateFlow()

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

        journalEntries = repository.allJournalEntries.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
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

            refreshFinancialStatements()
            com.example.data.backup.LocalDatabaseBackupManager.checkAndTriggerAutoBackup(application, repository)
        }
    }

    fun refreshFinancialStatements() {
        viewModelScope.launch {
            _trialBalance.value = repository.getTrialBalance()
            _financialStatementSummary.value = repository.getFinancialSummary()
            _journalEntriesWithLines.value = repository.getJournalEntriesWithLines()
        }
    }

    fun runComprehensiveAudit() {
        viewModelScope.launch {
            _isAuditing.value = true
            try {
                _auditReport.value = repository.runAudit()
            } finally {
                _isAuditing.value = false
            }
        }
    }

    fun clearAuditReport() {
        _auditReport.value = null
    }

    fun reconcileCashBox(cashBoxId: Long, physicalBalance: Double, note: String = "تسوية جرد فعلي") {
        viewModelScope.launch {
            val tx = repository.reconcileCashBox(cashBoxId, physicalBalance, note)
            if (tx != null) {
                _operationFeedback.value = "تمت تسوية رصيد الصندوق بنجاح وتوليد قيد التسوية"
                refreshFinancialStatements()
            } else {
                _operationFeedback.value = "رصيد الصندوق مطابق للجرد الفعلي ولا يحتاج لتسوية"
            }
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

    val financialHealth: StateFlow<FinancialHealth> = combine(summary, transactions) { summ, txList ->
        val assets = summ.totalCash + summ.owedToMe
        val liabilities = summ.owedByMe
        val total = assets + liabilities
        val ratio = if (total > 0) (assets / total).toFloat().coerceIn(0f, 1f) else 1f

        val score = when {
            liabilities <= 0 && summ.totalCash >= 0 -> 100
            summ.totalCash >= liabilities * 2 -> 95
            summ.totalCash >= liabilities -> 85
            summ.netWorth > 0 -> (60 + (ratio * 30)).toInt().coerceIn(60, 84)
            else -> (ratio * 50).toInt().coerceIn(10, 59)
        }

        val status = when {
            score >= 90 -> "ممتاز جداً"
            score >= 75 -> "جيد ومستقر"
            score >= 60 -> "متوسط ومقبول"
            else -> "يحتاج عناية ومتابعة"
        }

        val message = when {
            score >= 90 -> "سيولة قوية وديون الموردين مغطاة بالكامل وفائض مالي صحي."
            score >= 75 -> "المركز المالي آمن، يوصى بالاستمرار في تحصيل مستحقات العملاء بانتظام."
            score >= 60 -> "الالتزامات قريبة من السيولة المتاحة، تجنب المصروفات غير الضرورية."
            else -> "تنبيه: ديون الموردين تفوق السيولة المتوفرة. يجب تسريع التحصيل وإدارة النقد."
        }

        FinancialHealth(score = score, status = status, message = message, assetsRatio = ratio)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialHealth())

    val cashRunway: StateFlow<CashRunway> = combine(summary, transactions) { summ, txList ->
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val recentExpenses = txList.filter { it.type == TransactionType.EXPENSE && it.timestamp >= thirtyDaysAgo }
        val totalExpenseLast30Days = recentExpenses.sumOf { it.amount }
        val dailyExpense = if (totalExpenseLast30Days > 0) totalExpenseLast30Days / 30.0 else 0.0

        val days = if (dailyExpense > 0) {
            (summ.totalCash / dailyExpense).toInt().coerceAtLeast(0)
        } else {
            if (summ.totalCash > 0) 999 else 0
        }

        val statusText = when {
            days >= 90 -> "أمان مالي مرتفع (أكثر من 3 أشهر)"
            days in 30..89 -> "تغطية مقبولة (1 - 3 أشهر)"
            days in 1..29 -> "تغطية حرجة (أقل من شهر)"
            else -> "لا توجد مصروفات مسجلة مؤخراً"
        }

        CashRunway(
            dailyExpense = dailyExpense,
            runwayDays = days,
            totalMonthlyExpense = totalExpenseLast30Days,
            statusText = statusText
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CashRunway())

    val smartFinancialTip: StateFlow<String> = combine(summary, financialHealth, cashRunway) { summ, health, runway ->
        when {
            summ.owedByMe > summ.totalCash && summ.owedByMe > 0 ->
                "⚠️ تنبيه تدفق نقدي: إجمالي ما عليك للموردين يفوق النقدية الحالية. بادر بتحصيل ديون العملاء البالغة (${DecimalFormat("#,##0").format(summ.owedToMe)} ${summ.currencySymbol}) لسداد الموردين بأمان."
            summ.owedToMe > summ.totalCash * 1.5 && summ.owedToMe > 0 ->
                "💡 نصيحة نمو: لديك مبالغ مستحقة كبيرة عند العملاء. جدول تذكيرات لطيفة لتحصيلها وتعزيز السيولة في الصناديق."
            runway.runwayDays in 1..25 ->
                "⏳ تنبيه أمان: السيولة المتوفرة تغطي النفقات لحوالي ${runway.runwayDays} يوماً فقط بناءً على معدل صرفك اليومي."
            health.score >= 90 ->
                "🌟 أداء ممتاز: نقدك يغطي التزاماتك بالكامل. يمكنك تخصيص جزء من الفائض للاستثمار أو التوسع التجاري."
            else ->
                "✅ نصيحة مالية: حافظ على تسجيل كل عملية فور حدوثها بالصوت أو النص لضمان دقة كشوفات الحساب وصافي الثروة."
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "سجل عملياتك بانتظام للحصول على تحليلات دقيقة")

    fun getWhatsAppReminderMessage(party: Party, currencySymbol: String): String {
        val amountStr = java.text.DecimalFormat("#,##0.##").format(party.balance)
        return if (party.type == PartyType.CUSTOMER) {
            "السلام عليكم ورحمة الله أخي الكريم ${party.name}،\nنود تذكيركم برصيد الحساب المستحق وقدره $amountStr $currencySymbol.\nشاكرين ومقدرين حسن تعاونكم الدائم معنا."
        } else {
            "السلام عليكم ورحمة الله أخي الكريم ${party.name}،\nبخصوص حسابنا لديكم، مسجل لدينا رصيد مستحق لكم قدره $amountStr $currencySymbol.\nنرجو التكرم بمطابقة الرصيد وترتيب دفعة السداد القادمة. مع خالص التقدير."
        }
    }

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

    fun processVoiceCommand(spokenText: String) {
        if (spokenText.isBlank()) return
        val trimmed = spokenText.trim()
        viewModelScope.launch {
            _isAiThinking.value = true
            try {
                val userEntity = AiChatMessageEntity(
                    text = "🎙️ $trimmed",
                    isUser = true
                )
                repository.saveAiChatMessage(userEntity)

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
                    _operationFeedback.value = "تم تسجيل العملية صوتياً وتحديث الرصيد"
                    com.example.data.backup.LocalDatabaseBackupManager.checkAndTriggerAutoBackup(getApplication(), repository)
                } else if (result.actionType == "MANAGE_ENTITY") {
                    _operationFeedback.value = "تم تنفيذ الأمر الصوتي بنجاح"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                repository.saveAiChatMessage(
                    AiChatMessageEntity(
                        text = "عذراً، حدث خطأ أثناء تنفيذ الأمر الصوتي: ${e.localizedMessage}",
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
            refreshFinancialStatements()
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
            refreshFinancialStatements()
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

    // ==================== BATCH COMMANDS PIPELINE ====================
    fun parseBatchCommands(rawText: String) {
        viewModelScope.launch {
            val lines = com.example.domain.BatchCommandsEngine.splitBatchText(rawText)
            val defaultCurr = defaultCurrency.value?.symbol?.ifBlank { "ر.س" } ?: "ر.س"
            val defaultBox = defaultCashBox.value?.name ?: "الصندوق الرئيسي"
            val categories = expenseCategories.value.map { it.name }
            val parsed = com.example.domain.BatchCommandsEngine.parseBatchItems(
                commands = lines,
                defaultCurrencySymbol = defaultCurr,
                defaultCashBoxName = defaultBox,
                availableCategories = categories
            )
            _batchItems.value = parsed
            _batchSummary.value = null
        }
    }

    fun toggleBatchCommandItem(id: String) {
        _batchItems.value = _batchItems.value.map {
            if (it.id == id) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun removeBatchCommandItem(id: String) {
        _batchItems.value = _batchItems.value.filter { it.id != id }
    }

    fun executeBatchCommands(onComplete: (com.example.domain.BatchExecutionSummary) -> Unit = {}) {
        val items = _batchItems.value
        if (items.none { it.isSelected }) return

        viewModelScope.launch {
            _isBatchExecuting.value = true
            _batchProgress.value = Pair(0, items.count { it.isSelected })
            try {
                val currSymbol = defaultCurrency.value?.symbol?.ifBlank { "ر.س" } ?: "ر.س"
                val summary = com.example.domain.BatchCommandsEngine.executeBatch(
                    items = items,
                    assistant = assistant,
                    repository = repository,
                    parties = parties.value,
                    cashBoxes = cashBoxes.value,
                    expenseCategories = expenseCategories.value,
                    currencies = currencies.value,
                    currencySymbol = currSymbol,
                    onProgress = { current, total, updatedItem ->
                        _batchProgress.value = Pair(current, total)
                        _batchItems.value = _batchItems.value.map {
                            if (it.id == updatedItem.id) updatedItem else it
                        }
                    }
                )
                _batchSummary.value = summary
                _operationFeedback.value = "⚡ تم تنفيذ حزمة العمليات (${summary.successfulCount}/${summary.totalCommands}) بنجاح"
                com.example.data.backup.LocalDatabaseBackupManager.checkAndTriggerAutoBackup(getApplication(), repository)
                refreshFinancialStatements()
                onComplete(summary)
            } catch (e: Exception) {
                _operationFeedback.value = "حدث خطأ أثناء تنفيذ الحزمة: ${e.localizedMessage}"
            } finally {
                _isBatchExecuting.value = false
            }
        }
    }

    fun clearBatchState() {
        _batchItems.value = emptyList()
        _batchSummary.value = null
        _batchProgress.value = Pair(0, 0)
        _isBatchExecuting.value = false
    }
}
