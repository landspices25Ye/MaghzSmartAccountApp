package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.AppCurrency
import com.example.data.model.CashBox
import com.example.data.model.ExpenseCategory
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

class AccountingRepository(val database: AppDatabase) {
    private val cashBoxDao = database.cashBoxDao()
    private val partyDao = database.partyDao()
    private val transactionDao = database.transactionDao()
    private val reportTemplateDao = database.reportTemplateDao()
    private val currencyDao = database.currencyDao()
    private val expenseCategoryDao = database.expenseCategoryDao()
    private val aiMemoryDao = database.aiMemoryDao()
    private val aiChatMessageDao = database.aiChatMessageDao()

    // Flows
    val allCashBoxes: Flow<List<CashBox>> = cashBoxDao.getAllCashBoxesFlow()
    val allParties: Flow<List<Party>> = partyDao.getAllPartiesFlow()
    val allTransactions: Flow<List<TransactionRecord>> = transactionDao.getAllTransactionsFlow()
    val recentTransactions: Flow<List<TransactionRecord>> = transactionDao.getRecentTransactionsFlow(20)
    val allReportTemplates: Flow<List<com.example.data.model.ReportTemplate>> = reportTemplateDao.getAllTemplatesFlow()
    val allCurrencies: Flow<List<AppCurrency>> = currencyDao.getAllCurrenciesFlow()
    val allExpenseCategories: Flow<List<ExpenseCategory>> = expenseCategoryDao.getAllCategoriesFlow()
    val allAiMemories: Flow<List<com.example.data.model.AiMemoryFact>> = aiMemoryDao.getAllMemoriesFlow()
    val allAiChatMessages: Flow<List<com.example.data.model.AiChatMessageEntity>> = aiChatMessageDao.getAllMessagesFlow()
    val activeAiMemoriesCount: Flow<Int> = aiMemoryDao.getActiveCountFlow()

    // AI Memories
    suspend fun getAllAiMemoriesList(): List<com.example.data.model.AiMemoryFact> = aiMemoryDao.getAllMemories()
    suspend fun getActiveAiMemoriesList(): List<com.example.data.model.AiMemoryFact> = aiMemoryDao.getActiveMemories()
    suspend fun addAiMemory(memory: com.example.data.model.AiMemoryFact): Long = aiMemoryDao.insert(memory)
    suspend fun updateAiMemory(memory: com.example.data.model.AiMemoryFact) = aiMemoryDao.update(memory)
    suspend fun deleteAiMemory(memory: com.example.data.model.AiMemoryFact) = aiMemoryDao.delete(memory)
    suspend fun deleteAiMemoryById(id: Long) = aiMemoryDao.deleteById(id)
    suspend fun toggleAiMemoryActive(id: Long, isActive: Boolean) = aiMemoryDao.setActiveStatus(id, isActive)
    suspend fun clearAllAiMemories() = aiMemoryDao.clearAll()

    // AI Chat Messages
    suspend fun getAllAiChatMessagesList(): List<com.example.data.model.AiChatMessageEntity> = aiChatMessageDao.getAllMessages()
    suspend fun getRecentAiChatMessages(limit: Int = 20): List<com.example.data.model.AiChatMessageEntity> = aiChatMessageDao.getRecentMessages(limit)
    suspend fun saveAiChatMessage(message: com.example.data.model.AiChatMessageEntity) = aiChatMessageDao.insert(message)
    suspend fun clearAllAiChatMessages() = aiChatMessageDao.clearAll()
    suspend fun deleteAiChatMessage(id: String) = aiChatMessageDao.deleteById(id)

    // Currencies
    suspend fun getAllCurrenciesList(): List<AppCurrency> = currencyDao.getAllCurrencies()

    suspend fun ensureDefaultCurrencies(): AppCurrency {
        val existing = currencyDao.getDefaultCurrency() ?: currencyDao.getAllCurrencies().firstOrNull()
        if (existing != null) return existing

        val defaultCurrencies = listOf(
            AppCurrency(code = "SAR", name = "ريال سعودي", symbol = "ر.س", isDefault = true),
            AppCurrency(code = "USD", name = "دولار أمريكي", symbol = "$", isDefault = false),
            AppCurrency(code = "YER", name = "ريال يمني", symbol = "ر.ي", isDefault = false),
            AppCurrency(code = "EGP", name = "جنيه مصري", symbol = "ج.م", isDefault = false),
            AppCurrency(code = "AED", name = "درهم إماراتي", symbol = "د.إ", isDefault = false),
            AppCurrency(code = "KWD", name = "دينار كويتي", symbol = "د.ك", isDefault = false),
            AppCurrency(code = "QAR", name = "ريال قطري", symbol = "ر.ق", isDefault = false),
            AppCurrency(code = "OMR", name = "ريال عماني", symbol = "ر.ع", isDefault = false),
            AppCurrency(code = "EUR", name = "يورو", symbol = "€", isDefault = false)
        )
        defaultCurrencies.forEach { currencyDao.insert(it) }
        return defaultCurrencies.first()
    }

    suspend fun addCurrency(currency: AppCurrency): Long {
        if (currency.isDefault) {
            currencyDao.clearDefaultFlags()
        }
        return currencyDao.insert(currency)
    }

    suspend fun updateCurrency(currency: AppCurrency) {
        if (currency.isDefault) {
            currencyDao.clearDefaultFlags()
        }
        currencyDao.update(currency)
    }

    suspend fun setDefaultCurrency(currencyId: Long) {
        database.withTransaction {
            currencyDao.clearDefaultFlags()
            currencyDao.setDefault(currencyId)
        }
    }

    suspend fun deleteCurrency(currency: AppCurrency) {
        currencyDao.delete(currency)
        // Ensure at least one default
        val remaining = currencyDao.getAllCurrencies()
        if (remaining.isNotEmpty() && remaining.none { it.isDefault }) {
            currencyDao.setDefault(remaining.first().id)
        }
    }

    // Expense Categories
    suspend fun getAllExpenseCategoriesList(): List<ExpenseCategory> = expenseCategoryDao.getAllCategories()

    suspend fun ensureDefaultExpenseCategories(): List<ExpenseCategory> {
        val existing = expenseCategoryDao.getAllCategories()
        if (existing.isNotEmpty()) return existing

        val defaultCategories = listOf(
            ExpenseCategory(name = "إيجار", iconTag = "home", note = "إيجار محلات وسكن ومستودعات"),
            ExpenseCategory(name = "فواتير ومرافق", iconTag = "bolt", note = "كهرباء ومياه وإنترنت وهاتف"),
            ExpenseCategory(name = "رواتب وأجور", iconTag = "badge", note = "رواتب العمال والموظفين والحوافز"),
            ExpenseCategory(name = "نقل ومواصلات وبنزين", iconTag = "car", note = "وقود ومواصلات وشحن بضائع وتاكسي"),
            ExpenseCategory(name = "صيانة وتشغيل", iconTag = "build", note = "صيانة أجهزة ومعدات ومرافق"),
            ExpenseCategory(name = "ضيافة وبوفيه ومأكولات", iconTag = "coffee", note = "شاي وقهوة وضيافة عملاء ووجبات"),
            ExpenseCategory(name = "بضاعة ومشتريات", iconTag = "shopping", note = "مشتريات بضاعة ومواد خام ومستلزمات"),
            ExpenseCategory(name = "تسويق وإعلانات", iconTag = "campaign", note = "دعاية وإعلان وحملات تسويقية"),
            ExpenseCategory(name = "نثرية ومصروفات عامة", iconTag = "receipt", note = "مصروفات عامة ومتنوعة", isDefault = true)
        )
        defaultCategories.forEach { expenseCategoryDao.insert(it) }
        return expenseCategoryDao.getAllCategories()
    }

    suspend fun addExpenseCategory(category: ExpenseCategory): Long = expenseCategoryDao.insert(category)
    suspend fun updateExpenseCategory(category: ExpenseCategory) = expenseCategoryDao.update(category)
    suspend fun deleteExpenseCategory(category: ExpenseCategory) = expenseCategoryDao.delete(category)

    // Report Templates
    suspend fun saveReportTemplate(template: com.example.data.model.ReportTemplate): Long =
        reportTemplateDao.insert(template)

    suspend fun deleteReportTemplate(template: com.example.data.model.ReportTemplate) =
        reportTemplateDao.delete(template)

    fun getPartiesByType(type: PartyType): Flow<List<Party>> = partyDao.getPartiesByTypeFlow(type)
    fun getTransactionsByParty(partyId: Long): Flow<List<TransactionRecord>> = transactionDao.getTransactionsByPartyFlow(partyId)
    fun getTransactionsByCashBox(cashBoxId: Long): Flow<List<TransactionRecord>> = transactionDao.getTransactionsByCashBoxFlow(cashBoxId)

    suspend fun getCashBoxById(id: Long): CashBox? = cashBoxDao.getCashBoxById(id)
    suspend fun getPartyById(id: Long): Party? = partyDao.getPartyById(id)

    suspend fun getAllCashBoxesList(): List<CashBox> = cashBoxDao.getAllCashBoxes()
    suspend fun getAllTransactionsList(): List<TransactionRecord> = transactionDao.getAllTransactionsList()

    suspend fun ensureDefaultCashBox(): CashBox {
        val existing = cashBoxDao.getDefaultCashBox() ?: cashBoxDao.getAllCashBoxes().firstOrNull()
        if (existing != null) return existing
        val newBox = CashBox(
            name = "الصندوق الرئيسي",
            balance = 0.0,
            isDefault = true,
            note = "الصندوق النقدي الأساسي"
        )
        val id = cashBoxDao.insert(newBox)
        return newBox.copy(id = id)
    }

    suspend fun addCashBox(cashBox: CashBox): Long {
        if (cashBox.isDefault) {
            cashBoxDao.clearDefaultFlags()
        }
        return cashBoxDao.insert(cashBox)
    }

    suspend fun updateCashBox(cashBox: CashBox) {
        if (cashBox.isDefault) {
            cashBoxDao.clearDefaultFlags()
        }
        cashBoxDao.update(cashBox)
    }

    suspend fun setDefaultCashBox(boxId: Long) {
        database.withTransaction {
            cashBoxDao.clearDefaultFlags()
            cashBoxDao.setDefault(boxId)
        }
    }

    suspend fun setCashBoxBalance(boxId: Long, newBalance: Double) {
        cashBoxDao.setBalance(boxId, newBalance)
    }

    suspend fun deleteCashBox(cashBox: CashBox) = cashBoxDao.delete(cashBox)

    suspend fun addParty(party: Party): Long = partyDao.insert(party)
    suspend fun updateParty(party: Party) = partyDao.update(party)
    suspend fun setPartyBalance(partyId: Long, newBalance: Double) {
        partyDao.setBalance(partyId, newBalance)
    }
    suspend fun deleteParty(party: Party) = partyDao.delete(party)

    suspend fun findOrCreateParty(name: String, type: PartyType, phone: String = ""): Party {
        val cleanName = name.trim()
        val existing = partyDao.getPartyByName(cleanName)
        if (existing != null) return existing
        val newParty = Party(
            name = cleanName,
            type = type,
            phone = phone,
            balance = 0.0
        )
        val id = partyDao.insert(newParty)
        return newParty.copy(id = id)
    }

    suspend fun findOrCreateCashBox(name: String): CashBox {
        val cleanName = name.trim()
        val existing = cashBoxDao.getCashBoxByName(cleanName)
        if (existing != null) return existing
        val newBox = CashBox(
            name = cleanName,
            balance = 0.0,
            isDefault = false
        )
        val id = cashBoxDao.insert(newBox)
        return newBox.copy(id = id)
    }

    // Direct Transaction Queries for Reports & AI Agent
    suspend fun getTransactionsBetween(startTime: Long, endTime: Long): List<TransactionRecord> = transactionDao.getTransactionsBetween(startTime, endTime)
    suspend fun getTransactionsForParty(partyId: Long): List<TransactionRecord> = transactionDao.getTransactionsForParty(partyId)
    suspend fun getTransactionsForCashBox(cashBoxId: Long): List<TransactionRecord> = transactionDao.getTransactionsForCashBox(cashBoxId)

    /**
     * Records a financial transaction and applies balance adjustments atomically.
     */
    suspend fun recordTransaction(transaction: TransactionRecord): Long {
        return database.withTransaction {
            val txId = transactionDao.insert(transaction)
            applyBalanceChanges(transaction, isRevert = false)
            txId
        }
    }

    suspend fun deleteTransaction(transaction: TransactionRecord) {
        database.withTransaction {
            transactionDao.delete(transaction)
            applyBalanceChanges(transaction, isRevert = true)
        }
    }

    private suspend fun applyBalanceChanges(tx: TransactionRecord, isRevert: Boolean) {
        val multiplier = if (isRevert) -1.0 else 1.0
        val amount = tx.amount * multiplier

        when (tx.type) {
            TransactionType.CUSTOMER_RECEIPT -> {
                // Receipt from customer: cash increases, customer debt decreases
                tx.cashBoxId?.let { cashBoxDao.updateBalance(it, amount) }
                tx.partyId?.let { partyDao.updateBalance(it, -amount) }
            }
            TransactionType.SUPPLIER_PAYMENT -> {
                // Payment to supplier: cash decreases, supplier debt decreases
                tx.cashBoxId?.let { cashBoxDao.updateBalance(it, -amount) }
                tx.partyId?.let { partyDao.updateBalance(it, -amount) }
            }
            TransactionType.CUSTOMER_NEW_DEBIT -> {
                // Customer owes more
                tx.partyId?.let { partyDao.updateBalance(it, amount) }
                // If money was paid out of cash box (e.g. loan/cash debt), decrease cash box
                tx.cashBoxId?.let { cashBoxDao.updateBalance(it, -amount) }
            }
            TransactionType.SUPPLIER_NEW_CREDIT -> {
                // We owe supplier more
                tx.partyId?.let { partyDao.updateBalance(it, amount) }
            }
            TransactionType.EXPENSE -> {
                // Expense: cash decreases
                tx.cashBoxId?.let { cashBoxDao.updateBalance(it, -amount) }
            }
            TransactionType.INCOME -> {
                // Income: cash increases
                tx.cashBoxId?.let { cashBoxDao.updateBalance(it, amount) }
            }
            TransactionType.TRANSFER -> {
                // Transfer between boxes: source decreases, target increases
                tx.cashBoxId?.let { cashBoxDao.updateBalance(it, -amount) }
                tx.targetCashBoxId?.let { cashBoxDao.updateBalance(it, amount) }
            }
            TransactionType.SETTLEMENT -> {
                // Settlement/Discount directly affecting party balance
                tx.partyId?.let { partyDao.updateBalance(it, -amount) }
            }
        }
    }
}
