package com.example.ai

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
import java.text.DecimalFormat

data class AssistantResult(
    val reply: String,
    val executedTransaction: TransactionRecord? = null,
    val partyName: String? = null,
    val isSuccess: Boolean = true,
    val learnedMemory: AiMemoryFact? = null,
    val actionType: String = "CHAT"
)

class AccountingAssistant(
    private val geminiService: GeminiAccountingService = GeminiAccountingService()
) {
    private val currencyFormat = DecimalFormat("#,##0.##")

    suspend fun processUserSpeechOrText(
        input: String,
        repository: AccountingRepository,
        existingParties: List<Party>,
        existingBoxes: List<CashBox>,
        existingCategories: List<ExpenseCategory> = emptyList(),
        existingCurrencies: List<AppCurrency> = emptyList(),
        activeMemories: List<AiMemoryFact> = emptyList(),
        conversationHistory: List<Pair<String, Boolean>> = emptyList()
    ): AssistantResult {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return AssistantResult(
                reply = "لم أسمع أو أقرأ أي عملية. يمكنك التحدث أو كتابة المعاملة مثل: 'استلمت 500 من محمد' أو 'صرفت 50 بنزين'.",
                isSuccess = false,
                actionType = "ERROR"
            )
        }

        val defaultBox = existingBoxes.firstOrNull { it.isDefault } ?: repository.ensureDefaultCashBox()
        val defaultCurrency = existingCurrencies.firstOrNull { it.isDefault } ?: repository.ensureDefaultCurrencies()
        val defaultCurrencySymbol = defaultCurrency.symbol.ifBlank { "ر.س" }

        val partyNames = existingParties.map { it.name }
        val boxNames = existingBoxes.map { it.name }
        val categoryNames = existingCategories.map { it.name }
        val currencyStrings = existingCurrencies.map { "${it.name} (${it.symbol})" }

        val partiesWithBalances = existingParties.map { p ->
            val role = if (p.type == PartyType.CUSTOMER) "عميل" else "مورد"
            val balanceDesc = if (p.balance != 0.0) "رصيده: ${currencyFormat.format(p.balance)} $defaultCurrencySymbol" else "حسابه خالص"
            "${p.name} ($role, $balanceDesc)"
        }

        val boxesWithBalances = existingBoxes.map { b ->
            "${b.name} (رصيده: ${currencyFormat.format(b.balance)} $defaultCurrencySymbol)"
        }

        // Try Gemini AI first
        var intent = geminiService.analyzeAccountingMessage(
            userInput = trimmed,
            existingParties = partyNames,
            existingCashBoxes = boxNames,
            defaultCashBoxName = defaultBox.name,
            availableExpenseCategories = categoryNames,
            availableCurrencies = currencyStrings,
            defaultCurrencySymbol = defaultCurrencySymbol,
            activeMemories = activeMemories,
            conversationHistory = conversationHistory,
            partiesWithBalances = partiesWithBalances,
            cashBoxesWithBalances = boxesWithBalances
        )

        // If Gemini is not configured or fails, use local NLP
        if (intent == null) {
            intent = LocalAccountingNLP.parse(
                input = trimmed,
                defaultCurrencySymbol = defaultCurrencySymbol,
                defaultCashBoxName = defaultBox.name,
                availableCategories = categoryNames,
                activeMemories = activeMemories
            )
        }

        // Handle auto-learning / memory recording if present
        var savedMemoryFact: AiMemoryFact? = null
        if (!intent.learnedMemoryFact.isNullOrBlank()) {
            val cat = try {
                if (!intent.learnedMemoryCategory.isNullOrBlank()) {
                    MemoryCategory.valueOf(intent.learnedMemoryCategory)
                } else {
                    MemoryCategory.GENERAL
                }
            } catch (e: Exception) {
                MemoryCategory.GENERAL
            }
            val key = intent.learnedMemoryKey?.ifBlank { null } ?: "معلومة محاسبية"
            val fact = intent.learnedMemoryFact

            // Check if already exists to avoid exact duplicates
            val isDuplicate = activeMemories.any { it.fact.equals(fact, ignoreCase = true) }
            if (!isDuplicate) {
                val newMemory = AiMemoryFact(
                    category = cat,
                    key = key,
                    fact = fact,
                    isAutoLearned = true,
                    isActive = true
                )
                val newId = repository.addAiMemory(newMemory)
                savedMemoryFact = newMemory.copy(id = newId)
            }
        }

        // 1. Explicit Memory Action
        if (intent.action == "REMEMBER") {
            return AssistantResult(
                reply = intent.replyMessage.ifBlank { "🧠 تم حفظ المعلومة بنجاح في ذاكرتي المحاسبية!" },
                isSuccess = true,
                learnedMemory = savedMemoryFact,
                actionType = "REMEMBER"
            )
        }

        // 2. Clarification Question from AI
        if (intent.action == "QUESTION") {
            return AssistantResult(
                reply = "❓ ${intent.replyMessage}",
                isSuccess = true,
                learnedMemory = savedMemoryFact,
                actionType = "QUESTION"
            )
        }

        // 3. Financial Query or Advice
        if (intent.action == "QUERY" || intent.action == "ADVICE") {
            val replyText = if (intent.replyMessage.isNotBlank() && !intent.replyMessage.contains("استعلام عن الأرصدة والموقف")) {
                intent.replyMessage
            } else {
                val totalCash = existingBoxes.sumOf { it.balance }
                val totalReceivables = existingParties.filter { it.type == PartyType.CUSTOMER && it.balance > 0 }.sumOf { it.balance }
                val totalPayables = existingParties.filter { it.type == PartyType.SUPPLIER && it.balance > 0 }.sumOf { it.balance }
                val netWorth = totalCash + totalReceivables - totalPayables

                """
                📊 ملخص موقفك المالي الحالي:
                💰 النقدية في الصناديق: ${currencyFormat.format(totalCash)} $defaultCurrencySymbol
                🟢 ما لك عند العملاء: ${currencyFormat.format(totalReceivables)} $defaultCurrencySymbol
                🔴 ما عليك للموردين: ${currencyFormat.format(totalPayables)} $defaultCurrencySymbol
                ⚖️ صافي القيمة المالية: ${currencyFormat.format(netWorth)} $defaultCurrencySymbol
                """.trimIndent()
            }
            return AssistantResult(
                reply = replyText,
                isSuccess = true,
                learnedMemory = savedMemoryFact,
                actionType = intent.action
            )
        }

        // 4. Executable Transaction
        if (intent.transactionType != null && intent.amount > 0) {
            val chosenBox = if (!intent.cashBoxName.isNullOrBlank()) {
                existingBoxes.firstOrNull { it.name.contains(intent.cashBoxName, ignoreCase = true) }
                    ?: repository.findOrCreateCashBox(intent.cashBoxName)
            } else {
                defaultBox
            }

            // Find or create party if applicable
            var matchedParty: Party? = null
            if (!intent.partyName.isNullOrBlank()) {
                val partyType = intent.partyType ?: when (intent.transactionType) {
                    TransactionType.CUSTOMER_RECEIPT, TransactionType.CUSTOMER_NEW_DEBIT -> PartyType.CUSTOMER
                    TransactionType.SUPPLIER_PAYMENT, TransactionType.SUPPLIER_NEW_CREDIT -> PartyType.SUPPLIER
                    else -> PartyType.CUSTOMER
                }
                matchedParty = existingParties.firstOrNull {
                    it.name.contains(intent.partyName, ignoreCase = true)
                } ?: repository.findOrCreateParty(intent.partyName, partyType)
            }

            // Target cash box for transfers
            var targetBox: CashBox? = null
            if (intent.transactionType == TransactionType.TRANSFER && !intent.targetCashBoxName.isNullOrBlank()) {
                targetBox = existingBoxes.firstOrNull {
                    it.name.contains(intent.targetCashBoxName, ignoreCase = true)
                } ?: repository.findOrCreateCashBox(intent.targetCashBoxName)
            }

            val finalCategory = if (intent.transactionType == TransactionType.EXPENSE) {
                if (intent.category.isNotBlank()) intent.category
                else LocalAccountingNLP.predictCategory(intent.description.ifBlank { trimmed }, categoryNames)
            } else {
                intent.category
            }

            val finalCurrency = intent.currency.ifBlank { defaultCurrencySymbol }

            val tx = TransactionRecord(
                type = intent.transactionType,
                amount = intent.amount,
                cashBoxId = chosenBox.id,
                targetCashBoxId = targetBox?.id,
                partyId = matchedParty?.id,
                category = finalCategory,
                currency = finalCurrency,
                description = intent.description.ifBlank { intent.transactionType.titleAr },
                timestamp = System.currentTimeMillis(),
                isAiGenerated = true
            )

            repository.recordTransaction(tx)

            val baseReply = if (intent.replyMessage.isNotBlank()) {
                intent.replyMessage
            } else {
                val catInfo = if (finalCategory.isNotBlank()) " في بند [$finalCategory]" else ""
                "تم تسجيل عملية ${intent.transactionType.titleAr} بمبلغ ${currencyFormat.format(intent.amount)} $finalCurrency$catInfo في (${chosenBox.name}) بنجاح."
            }

            val memoryAppend = if (savedMemoryFact != null) "\n💡 (تم حفظ معلومة جديدة في الذاكرة: ${savedMemoryFact.fact})" else ""

            return AssistantResult(
                reply = "✅ $baseReply$memoryAppend",
                executedTransaction = tx,
                partyName = matchedParty?.name,
                isSuccess = true,
                learnedMemory = savedMemoryFact,
                actionType = "TRANSACTION"
            )
        }

        // 5. General Chat / Help
        val defaultHelp = "أهلاً بك! أنا محاسبك الذكي المتطور.\nأفهم حديثك المالي بالنص أو الصوت، وأتذكر تفضيلاتك وقواعدك المالية بدقة. جرب مثلاً: 'استلمت 500 من محمد' أو 'تذكر أن خالد شريكي' أو 'كم رصيدي اليوم؟'."
        val reply = intent.replyMessage.ifBlank { defaultHelp }

        return AssistantResult(
            reply = reply,
            isSuccess = true,
            learnedMemory = savedMemoryFact,
            actionType = intent.action.ifBlank { "CHAT" }
        )
    }
}
