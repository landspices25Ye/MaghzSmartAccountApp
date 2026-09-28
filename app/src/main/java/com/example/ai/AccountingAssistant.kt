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

        // 3.4 Entity Management (Add / Update / Delete Parties, Cash Boxes, Expense Categories, Transfers, Currencies, Transactions)
        val isEntityAction = intent.action == "MANAGE_ENTITY" ||
                !intent.entityOperation.isNullOrBlank() ||
                intent.action in listOf("ADD_PARTY", "UPDATE_PARTY", "DELETE_PARTY", "ADD_CASH_BOX", "UPDATE_CASH_BOX", "DELETE_CASH_BOX", "TRANSFER_CASH", "ADD_EXPENSE_CATEGORY", "UPDATE_EXPENSE_CATEGORY", "DELETE_EXPENSE_CATEGORY", "SET_DEFAULT_CURRENCY", "ADD_CURRENCY", "DELETE_TRANSACTION")

        if (isEntityAction) {
            val op = (intent.entityOperation ?: intent.action).uppercase()
            when {
                op in listOf("DELETE_TRANSACTION", "REVERT_TRANSACTION", "DELETE_LAST_TRANSACTION", "CANCEL_TRANSACTION") -> {
                    val allTx = repository.getAllTransactionsList()
                    val lastTx = allTx.maxByOrNull { it.timestamp }
                    if (lastTx != null) {
                        repository.deleteTransaction(lastTx)
                        return AssistantResult(
                            reply = "🗑️ تم إلغاء وحذف آخر عملية مالية بنجاح:\n• البيان: ${lastTx.description}\n• المبلغ: ${currencyFormat.format(lastTx.amount)} ${lastTx.currency}\n• تم إعادة ضبط أرصدة الصناديق والأطراف المرتبطة بها تلقائياً.",
                            isSuccess = true,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    } else {
                        return AssistantResult(
                            reply = "⚠️ لا توجد أي عمليات مالية مسجلة في السجل لحذفها.",
                            isSuccess = false,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    }
                }

                op in listOf("ADD_PARTY", "ADD_CUSTOMER", "ADD_SUPPLIER", "NEW_PARTY", "CREATE_PARTY", "NEW_CUSTOMER", "NEW_SUPPLIER") -> {
                    val pName = intent.partyName?.trim()?.ifBlank { null } ?: "طرف جديد"
                    val pType = intent.partyType ?: if (op.contains("SUPPLIER")) PartyType.SUPPLIER else PartyType.CUSTOMER
                    val phone = intent.partyPhone?.trim() ?: ""
                    val initBal = intent.initialBalance
                    val existing = existingParties.firstOrNull { it.name.equals(pName, ignoreCase = true) }
                    if (existing != null) {
                        return AssistantResult(
                            reply = "⚠️ الطرف '${existing.name}' موجود بالفعل في سجلاتك كرصيد: ${currencyFormat.format(existing.balance)} $defaultCurrencySymbol.",
                            isSuccess = true,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    }
                    val newParty = Party(
                        name = pName,
                        type = pType,
                        phone = phone,
                        balance = initBal
                    )
                    repository.addParty(newParty)
                    val role = if (pType == PartyType.CUSTOMER) "العميل" else "المورد"
                    val phoneText = if (phone.isNotBlank()) " ورقم الهاتف ($phone)" else ""
                    val balText = if (initBal != 0.0) " وبرصيد افتتاحي ${currencyFormat.format(initBal)} $defaultCurrencySymbol" else ""
                    return AssistantResult(
                        reply = "✅ تم إضافة $role الجديد **$pName** بنجاح$phoneText$balText.",
                        isSuccess = true,
                        partyName = pName,
                        learnedMemory = savedMemoryFact,
                        actionType = "MANAGE_ENTITY"
                    )
                }

                op in listOf("UPDATE_PARTY", "EDIT_PARTY", "UPDATE_CUSTOMER", "UPDATE_SUPPLIER", "EDIT_CUSTOMER", "EDIT_SUPPLIER") -> {
                    val pName = intent.partyName?.trim() ?: ""
                    val matchedParty = existingParties.firstOrNull { it.name.contains(pName, ignoreCase = true) }
                    if (matchedParty != null) {
                        val newPhone = intent.partyPhone?.takeIf { it.isNotBlank() } ?: matchedParty.phone
                        val newName = intent.newName?.takeIf { it.isNotBlank() } ?: matchedParty.name
                        val updatedParty = matchedParty.copy(
                            name = newName,
                            phone = newPhone
                        )
                        repository.updateParty(updatedParty)
                        if (intent.amount > 0 || intent.initialBalance > 0) {
                            val newBal = if (intent.amount > 0) intent.amount else intent.initialBalance
                            repository.setPartyBalance(matchedParty.id, newBal)
                        }
                        val role = if (matchedParty.type == PartyType.CUSTOMER) "العميل" else "المورد"
                        return AssistantResult(
                            reply = "✅ تم تحديث بيانات $role **${matchedParty.name}** بنجاح.",
                            isSuccess = true,
                            partyName = newName,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    } else {
                        return AssistantResult(
                            reply = "⚠️ لم أجد عميلاً أو مورداً مطابقاً للاسم '$pName' لتعديله.",
                            isSuccess = false,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    }
                }

                op in listOf("DELETE_PARTY", "REMOVE_PARTY", "DELETE_CUSTOMER", "DELETE_SUPPLIER") -> {
                    val pName = intent.partyName?.trim() ?: ""
                    val matchedParty = existingParties.firstOrNull { it.name.contains(pName, ignoreCase = true) }
                    if (matchedParty != null) {
                        repository.deleteParty(matchedParty)
                        val role = if (matchedParty.type == PartyType.CUSTOMER) "العميل" else "المورد"
                        return AssistantResult(
                            reply = "🗑️ تم حذف $role **${matchedParty.name}** من النظام وسجل الحسابات بنجاح.",
                            isSuccess = true,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    } else {
                        return AssistantResult(
                            reply = "⚠️ لم يتم العثور على طرف بهذا الاسم لحذفه.",
                            isSuccess = false,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    }
                }

                op in listOf("ADD_CASH_BOX", "ADD_BOX", "ADD_BANK", "NEW_CASH_BOX", "NEW_BANK") -> {
                    val boxName = intent.cashBoxName?.trim()?.ifBlank { null } ?: "صندوق جديد"
                    val initBal = intent.amount
                    val existing = existingBoxes.firstOrNull { it.name.equals(boxName, ignoreCase = true) }
                    if (existing != null) {
                        return AssistantResult(
                            reply = "⚠️ الصندوق أو الحساب البنكي '${existing.name}' موجود مسبقاً برصيد ${currencyFormat.format(existing.balance)} $defaultCurrencySymbol.",
                            isSuccess = true,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    }
                    val newBox = CashBox(
                        name = boxName,
                        balance = initBal,
                        isDefault = false
                    )
                    repository.addCashBox(newBox)
                    return AssistantResult(
                        reply = "✅ تم إنشاء الصندوق/الحساب البنكي الجديد **$boxName** برصيد ${currencyFormat.format(initBal)} $defaultCurrencySymbol بنجاح.",
                        isSuccess = true,
                        learnedMemory = savedMemoryFact,
                        actionType = "MANAGE_ENTITY"
                    )
                }

                op in listOf("UPDATE_CASH_BOX", "EDIT_CASH_BOX", "EDIT_BOX", "UPDATE_BOX") -> {
                    val boxName = intent.cashBoxName?.trim() ?: ""
                    val matchedBox = existingBoxes.firstOrNull { it.name.contains(boxName, ignoreCase = true) }
                    if (matchedBox != null) {
                        if (intent.amount > 0) {
                            repository.setCashBoxBalance(matchedBox.id, intent.amount)
                        }
                        if (!intent.newName.isNullOrBlank()) {
                            repository.updateCashBox(matchedBox.copy(name = intent.newName))
                        }
                        return AssistantResult(
                            reply = "✅ تم تحديث بيانات الصندوق **${matchedBox.name}** بنجاح.",
                            isSuccess = true,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    } else {
                        return AssistantResult(
                            reply = "⚠️ لم أجد صندوقاً بهذا الاسم لتعديله.",
                            isSuccess = false,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    }
                }

                op in listOf("DELETE_CASH_BOX", "REMOVE_CASH_BOX", "DELETE_BOX", "DELETE_BANK") -> {
                    val boxName = intent.cashBoxName?.trim() ?: ""
                    val matchedBox = existingBoxes.firstOrNull { it.name.contains(boxName, ignoreCase = true) }
                    if (matchedBox != null) {
                        if (matchedBox.isDefault) {
                            return AssistantResult(
                                reply = "⚠️ لا يمكن حذف الصندوق الافتراضي الرئيسي للنظام.",
                                isSuccess = false,
                                learnedMemory = savedMemoryFact,
                                actionType = "MANAGE_ENTITY"
                            )
                        }
                        repository.deleteCashBox(matchedBox)
                        return AssistantResult(
                            reply = "🗑️ تم حذف الصندوق **${matchedBox.name}** بنجاح.",
                            isSuccess = true,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    } else {
                        return AssistantResult(
                            reply = "⚠️ لم أجد الصندوق المطلوب لحذفه.",
                            isSuccess = false,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    }
                }

                op in listOf("TRANSFER_CASH", "TRANSFER") -> {
                    val srcName = intent.cashBoxName?.trim() ?: ""
                    val dstName = intent.targetCashBoxName?.trim() ?: ""
                    val srcBox = existingBoxes.firstOrNull { it.name.contains(srcName, ignoreCase = true) } ?: defaultBox
                    val dstBox = existingBoxes.firstOrNull { it.name.contains(dstName, ignoreCase = true) }
                    if (dstBox == null) {
                        return AssistantResult(
                            reply = "⚠️ يرجى تحديد الصندوق أو الحساب البنكي المحول إليه بشكل واضح.",
                            isSuccess = false,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    }
                    if (srcBox.id == dstBox.id) {
                        return AssistantResult(
                            reply = "⚠️ لا يمكن التحويل لنفس الصندوق.",
                            isSuccess = false,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    }
                    val tx = TransactionRecord(
                        type = TransactionType.TRANSFER,
                        amount = intent.amount,
                        cashBoxId = srcBox.id,
                        targetCashBoxId = dstBox.id,
                        description = intent.description.ifBlank { "تحويل نقدي من ${srcBox.name} إلى ${dstBox.name}" },
                        currency = defaultCurrencySymbol,
                        isAiGenerated = true
                    )
                    repository.recordTransaction(tx)
                    return AssistantResult(
                        reply = "🔄 تم تحويل ${currencyFormat.format(intent.amount)} $defaultCurrencySymbol بنجاح من **${srcBox.name}** إلى **${dstBox.name}**.",
                        executedTransaction = tx,
                        isSuccess = true,
                        learnedMemory = savedMemoryFact,
                        actionType = "MANAGE_ENTITY"
                    )
                }

                op in listOf("ADD_EXPENSE_CATEGORY", "ADD_CATEGORY", "CREATE_CATEGORY", "NEW_CATEGORY") -> {
                    val catName = intent.category.trim().ifBlank { intent.description.trim() }.ifBlank { "بند مصروف جديد" }
                    val existing = existingCategories.firstOrNull { it.name.equals(catName, ignoreCase = true) }
                    if (existing != null) {
                        return AssistantResult(
                            reply = "⚠️ بند المصروف '${existing.name}' مسجل بالفعل في التصنيفات.",
                            isSuccess = true,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    }
                    val newCategory = ExpenseCategory(
                        name = catName,
                        iconTag = "receipt",
                        note = "تم إنشاؤه عبر المحاسب الذكي"
                    )
                    repository.addExpenseCategory(newCategory)
                    return AssistantResult(
                        reply = "✅ تم إضافة تصنيف وبند المصروفات الجديد **$catName** بنجاح.",
                        isSuccess = true,
                        learnedMemory = savedMemoryFact,
                        actionType = "MANAGE_ENTITY"
                    )
                }

                op in listOf("UPDATE_EXPENSE_CATEGORY", "EDIT_EXPENSE_CATEGORY", "UPDATE_CATEGORY", "RENAME_CATEGORY") -> {
                    val catName = intent.category.trim()
                    val matched = existingCategories.firstOrNull { it.name.contains(catName, ignoreCase = true) }
                    if (matched != null) {
                        val newName = intent.newName?.trim()?.takeIf { it.isNotBlank() } ?: matched.name
                        repository.updateExpenseCategory(matched.copy(name = newName))
                        return AssistantResult(
                            reply = "✅ تم تحديث بند المصروف إلى **$newName** بنجاح.",
                            isSuccess = true,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    } else {
                        return AssistantResult(
                            reply = "⚠️ لم أجد بند مصروف بهذا الاسم لتعديله.",
                            isSuccess = false,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    }
                }

                op in listOf("DELETE_EXPENSE_CATEGORY", "DELETE_CATEGORY", "REMOVE_CATEGORY") -> {
                    val catName = intent.category.trim()
                    val matched = existingCategories.firstOrNull { it.name.contains(catName, ignoreCase = true) }
                    if (matched != null) {
                        repository.deleteExpenseCategory(matched)
                        return AssistantResult(
                            reply = "🗑️ تم حذف بند المصروف **${matched.name}** بنجاح.",
                            isSuccess = true,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    } else {
                        return AssistantResult(
                            reply = "⚠️ لم أجد بند مصروف بهذا الاسم لحذفه.",
                            isSuccess = false,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    }
                }

                op in listOf("ADD_CURRENCY", "NEW_CURRENCY") -> {
                    val currName = intent.currency.trim().ifBlank { "عملة جديدة" }
                    val newCurr = AppCurrency(
                        code = currName.take(3).uppercase(),
                        name = currName,
                        symbol = currName.take(3),
                        exchangeRate = 1.0,
                        isDefault = false
                    )
                    repository.addCurrency(newCurr)
                    return AssistantResult(
                        reply = "💱 تم إضافة العملة الجديدة **$currName** بنجاح.",
                        isSuccess = true,
                        learnedMemory = savedMemoryFact,
                        actionType = "MANAGE_ENTITY"
                    )
                }

                op in listOf("SET_DEFAULT_CURRENCY", "CHANGE_CURRENCY") -> {
                    val currName = intent.currency.trim()
                    val matched = existingCurrencies.firstOrNull {
                        it.name.contains(currName, ignoreCase = true) ||
                        it.code.equals(currName, ignoreCase = true) ||
                        it.symbol.equals(currName, ignoreCase = true)
                    }
                    if (matched != null) {
                        repository.setDefaultCurrency(matched.id)
                        return AssistantResult(
                            reply = "💱 تم تعيين **${matched.name} (${matched.symbol})** كعملة افتراضية أساسية للتطبيق بنجاح.",
                            isSuccess = true,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    } else {
                        return AssistantResult(
                            reply = "⚠️ لم أجد عملة مطابقة لـ '$currName' في قائمة العملات المتاحة.",
                            isSuccess = false,
                            learnedMemory = savedMemoryFact,
                            actionType = "MANAGE_ENTITY"
                        )
                    }
                }

                else -> {
                    return AssistantResult(
                        reply = "⚠️ لم أتمكن من تحديد تفاصيل الكيان المراد إدارته بدقة. يمكنك مثلاً إخباري: 'أضف عميل ماجد رقمه 0501234567' أو 'عدل رصيد العميل ماجد إلى 500' أو 'أضف بنك الراجحي برصيد 5000' أو 'احذف آخر عملية'.",
                        isSuccess = false,
                        learnedMemory = savedMemoryFact,
                        actionType = "MANAGE_ENTITY"
                    )
                }
            }
        }

        // 3.5 Financial Report Generation (Daily Summary, Party Statement, Cash in Hand, Custom)
        if (intent.action == "REPORT") {
            when (intent.reportType) {
                "DAILY" -> {
                    val cal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val startOfDay = cal.timeInMillis
                    val endOfDay = System.currentTimeMillis()
                    val dailyTx = repository.getTransactionsBetween(startOfDay, endOfDay)
                    val totalIn = dailyTx.filter { it.type == TransactionType.CUSTOMER_RECEIPT || it.type == TransactionType.INCOME }.sumOf { it.amount }
                    val totalOut = dailyTx.filter { it.type == TransactionType.SUPPLIER_PAYMENT || it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    val net = totalIn - totalOut

                    val reportAction = com.example.ui.viewmodel.ChatReportAction(
                        reportType = "DAILY",
                        reportTitle = "تقرير حركة اليوم",
                        totalIn = totalIn,
                        totalOut = totalOut,
                        netBalance = net,
                        count = dailyTx.size
                    )

                    val replyText = """
                        📊 **تقرير ملخص حركة اليوم:**
                        🟢 إجمالي المقبوضات (+): ${currencyFormat.format(totalIn)} $defaultCurrencySymbol
                        🔴 إجمالي المدفوعات والمصروفات (-): ${currencyFormat.format(totalOut)} $defaultCurrencySymbol
                        ⚖️ صافي حركة اليوم: ${currencyFormat.format(net)} $defaultCurrencySymbol
                        📝 عدد الحركات المنفذة اليوم: ${dailyTx.size} عملية

                        💡 يمكنك الآن استخراج التقرير وتصديره فوراً بصيغة PDF للطباعة أو Excel (CSV) من الأزرار التفاعلية أدناه:
                    """.trimIndent()

                    return AssistantResult(
                        reply = replyText,
                        isSuccess = true,
                        learnedMemory = savedMemoryFact,
                        actionType = reportAction.toActionTypeString()
                    )
                }

                "PARTY" -> {
                    val matchedParty = if (!intent.partyName.isNullOrBlank()) {
                        existingParties.firstOrNull { it.name.contains(intent.partyName, ignoreCase = true) }
                    } else existingParties.firstOrNull()

                    if (matchedParty != null) {
                        val partyTx = repository.getTransactionsForParty(matchedParty.id)
                        val totalIn = partyTx.filter { it.type == TransactionType.CUSTOMER_RECEIPT }.sumOf { it.amount }
                        val totalOut = partyTx.filter { it.type == TransactionType.SUPPLIER_PAYMENT }.sumOf { it.amount }
                        val role = if (matchedParty.type == PartyType.CUSTOMER) "العميل" else "المورد"
                        val balanceDesc = if (matchedParty.type == PartyType.CUSTOMER) {
                            if (matchedParty.balance >= 0) "ما لنا عنده: ${currencyFormat.format(matchedParty.balance)} $defaultCurrencySymbol"
                            else "له علينا: ${currencyFormat.format(-matchedParty.balance)} $defaultCurrencySymbol"
                        } else {
                            if (matchedParty.balance >= 0) "ما له علينا: ${currencyFormat.format(matchedParty.balance)} $defaultCurrencySymbol"
                            else "لنا عنده: ${currencyFormat.format(-matchedParty.balance)} $defaultCurrencySymbol"
                        }

                        val reportAction = com.example.ui.viewmodel.ChatReportAction(
                            reportType = "PARTY",
                            reportTitle = "كشف حساب $role: ${matchedParty.name}",
                            partyId = matchedParty.id,
                            partyName = matchedParty.name,
                            totalIn = totalIn,
                            totalOut = totalOut,
                            netBalance = matchedParty.balance,
                            count = partyTx.size
                        )

                        val replyText = """
                            👤 **كشف حساب $role: ${matchedParty.name}**
                            💰 الرصيد الحالي: $balanceDesc
                            📝 إجمالي الحركات المسجلة في حسابه: ${partyTx.size} حركة
                            ${if (matchedParty.phone.isNotBlank()) "📞 رقم التواصل: ${matchedParty.phone}" else ""}

                            💡 تم تجهيز كشف الحساب الرسمي ويمكنك تصديره كملف PDF قابل للطباعة أو جدول Excel أدناه مباشرة:
                        """.trimIndent()

                        return AssistantResult(
                            reply = replyText,
                            isSuccess = true,
                            learnedMemory = savedMemoryFact,
                            actionType = reportAction.toActionTypeString()
                        )
                    } else {
                        val replyText = "لم أجد طرفاً مطابقاً بالاسم المطلوب، يرجى كتابة اسم العميل أو المورد بدقة، أو اختر من القائمة لتصدير كشف حسابه."
                        return AssistantResult(
                            reply = replyText,
                            isSuccess = true,
                            learnedMemory = savedMemoryFact,
                            actionType = "CHAT"
                        )
                    }
                }

                "CASH" -> {
                    val matchedBox = existingBoxes.firstOrNull { !intent.cashBoxName.isNullOrBlank() && it.name.contains(intent.cashBoxName, ignoreCase = true) } ?: defaultBox
                    val boxTx = repository.getTransactionsForCashBox(matchedBox.id)
                    val totalIn = boxTx.filter { it.type == TransactionType.CUSTOMER_RECEIPT || it.type == TransactionType.INCOME }.sumOf { it.amount }
                    val totalOut = boxTx.filter { it.type == TransactionType.SUPPLIER_PAYMENT || it.type == TransactionType.EXPENSE }.sumOf { it.amount }

                    val reportAction = com.example.ui.viewmodel.ChatReportAction(
                        reportType = "CASH",
                        reportTitle = "كشف نقدية ${matchedBox.name}",
                        cashBoxId = matchedBox.id,
                        cashBoxName = matchedBox.name,
                        totalIn = totalIn,
                        totalOut = totalOut,
                        netBalance = matchedBox.balance,
                        count = boxTx.size
                    )

                    val replyText = """
                        💵 **كشف النقدية المتاحة في (${matchedBox.name}):**
                        💰 الرصيد المتوفر حالياً: ${currencyFormat.format(matchedBox.balance)} $defaultCurrencySymbol
                        🟢 إجمالي المقبوضات الواردة: ${currencyFormat.format(totalIn)} $defaultCurrencySymbol
                        🔴 إجمالي المدفوعات الصادرة: ${currencyFormat.format(totalOut)} $defaultCurrencySymbol
                        📝 عدد الحركات النقدية: ${boxTx.size} حركة

                        💡 تم إعداد كشف النقدية وجاهز للتصدير والطباعة كـ PDF أو Excel عبر الأزرار أدناه:
                    """.trimIndent()

                    return AssistantResult(
                        reply = replyText,
                        isSuccess = true,
                        learnedMemory = savedMemoryFact,
                        actionType = reportAction.toActionTypeString()
                    )
                }

                else -> {
                    val allTx = repository.getAllTransactionsList()
                    val totalIn = allTx.filter { it.type == TransactionType.CUSTOMER_RECEIPT || it.type == TransactionType.INCOME }.sumOf { it.amount }
                    val totalOut = allTx.filter { it.type == TransactionType.SUPPLIER_PAYMENT || it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    val net = totalIn - totalOut
                    val totalCash = existingBoxes.sumOf { it.balance }
                    val totalReceivables = existingParties.filter { it.type == PartyType.CUSTOMER && it.balance > 0 }.sumOf { it.balance }
                    val totalPayables = existingParties.filter { it.type == PartyType.SUPPLIER && it.balance > 0 }.sumOf { it.balance }

                    val reportAction = com.example.ui.viewmodel.ChatReportAction(
                        reportType = "CUSTOM",
                        reportTitle = "التقرير المالي العام الشامل",
                        totalIn = totalIn,
                        totalOut = totalOut,
                        netBalance = net,
                        count = allTx.size
                    )

                    val replyText = """
                        📑 **التقرير المالي العام الشامل:**
                        💰 النقدية الإجمالية في الصناديق: ${currencyFormat.format(totalCash)} $defaultCurrencySymbol
                        🟢 إجمالي ديون العملاء (ما لك): ${currencyFormat.format(totalReceivables)} $defaultCurrencySymbol
                        🔴 إجمالي التزامات الموردين (ما عليك): ${currencyFormat.format(totalPayables)} $defaultCurrencySymbol
                        ⚖️ صافي الموقف المالي: ${currencyFormat.format(totalCash + totalReceivables - totalPayables)} $defaultCurrencySymbol
                        📝 إجمالي العمليات المسجلة: ${allTx.size} حركة

                        💡 يمكنك تصدير هذا التقرير الشامل ومشاركته بملف PDF رسمي أو جدول Excel أدناه:
                    """.trimIndent()

                    return AssistantResult(
                        reply = replyText,
                        isSuccess = true,
                        learnedMemory = savedMemoryFact,
                        actionType = reportAction.toActionTypeString()
                    )
                }
            }
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
