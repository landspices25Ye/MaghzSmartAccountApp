package com.example.domain

import com.example.ai.AccountingAssistant
import com.example.ai.LocalAccountingNLP
import com.example.ai.ParsedAccountingIntent
import com.example.data.model.AppCurrency
import com.example.data.model.CashBox
import com.example.data.model.ExpenseCategory
import com.example.data.model.Party
import com.example.data.model.TransactionType
import com.example.data.repository.AccountingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

object BatchCommandsEngine {

    private val decimalFormat = DecimalFormat("#,##0.##")

    val PRESET_TEMPLATES = listOf(
        BatchPresetTemplate(
            id = "daily_sales",
            title = "تحصيلات مبيعات اليوم",
            description = "تسجيل مقبوضات نقدية من عدة عملاء دفعة واحدة",
            commands = listOf(
                "قبضت 450 ريال من العميل محمد علي نقد مبيعات",
                "استلمت 700 ريال من العميل شركة النور",
                "قبض 250 ريال من أحمد سالم سداد حساب"
            )
        ),
        BatchPresetTemplate(
            id = "daily_expenses",
            title = "مصروفات تشغيلية يومية",
            description = "تسجيل بنزين، ضيافة، وفواتير متنوعة دفعة واحدة",
            commands = listOf(
                "صرفت 65 ريال بنزين للسيارة من الصندوق الرئيسي",
                "صرف 30 ريال ضيافة وبوفيه شاي وقهوة",
                "سجل مصروف 120 ريال صيانة كهرباء ومعدات"
            )
        ),
        BatchPresetTemplate(
            id = "supplier_and_transfer",
            title = "سداد مورد وتحويل عهدة",
            description = "دفع مستحقات للموردين مع تحويل نقدية لصندوق العهدة",
            commands = listOf(
                "سددت للمورد مؤسسة الأمل 1200 ريال دفعة حساب",
                "حول 500 ريال من الصندوق الرئيسي إلى صندوق العهدة",
                "سداد 400 ريال للمورد شركة التوزيع"
            )
        ),
        BatchPresetTemplate(
            id = "audit_and_reconcile",
            title = "تدقيق وجرد مالي",
            description = "إجراء جرد للصندوق مع تدقيق مالي شامل وميزان المراجعة",
            commands = listOf(
                "تسوية صندوق الصندوق الرئيسي بمبلغ 5000 ريال",
                "دقق الحسابات وافحص التوازن المالي",
                "استعرض ميزان المراجعة والقوائم المالية"
            )
        )
    )

    /**
     * Splits raw input into individual commands using newlines, bullets, numbers,
     * or compound conjunctions.
     */
    fun splitBatchText(input: String): List<String> {
        if (input.isBlank()) return emptyList()

        // 1. First split by lines
        val rawLines = input.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("//") }

        val candidateCommands = mutableListOf<String>()

        for (line in rawLines) {
            // Strip bullet markers or numbers like "1. ", "1- ", "• ", "- ", "* "
            val cleanLine = line
                .replace(Regex("""^[\s•\-\*]+"""), "")
                .replace(Regex("""^\d+[\.\-\)]\s*"""), "")
                .trim()

            if (cleanLine.isBlank()) continue

            // Check if line contains semicolons or Arabic semicolons
            if (cleanLine.contains(";") || cleanLine.contains("؛")) {
                val subParts = cleanLine.split(Regex("""[;؛]"""))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                candidateCommands.addAll(subParts)
            } else if (rawLines.size == 1 && containsMultipleSentenceCommands(cleanLine)) {
                // If the user typed a single paragraph containing multiple commands linked with "و"
                val splitParts = splitCompoundArabicSentence(cleanLine)
                if (splitParts.size > 1) {
                    candidateCommands.addAll(splitParts)
                } else {
                    candidateCommands.add(cleanLine)
                }
            } else {
                candidateCommands.add(cleanLine)
            }
        }

        return candidateCommands
    }

    private fun containsMultipleSentenceCommands(text: String): Boolean {
        val count = Regex("""(قبض|استلمت|صرفت|صرف|سددت|سداد|دين على|التزام|تحويل|حول|قيد)\s+\d+""").findAll(text).count()
        return count >= 2
    }

    private fun splitCompoundArabicSentence(text: String): List<String> {
        val regex = Regex("""(?<=\b)\s+(?:و\s+|ثم\s+)(?=(?:قبض|استلمت|صرفت|صرف|سددت|سداد|دين على|التزام|تحويل|حول|قيد)\b)""")
        val parts = text.split(regex).map { it.trim() }.filter { it.isNotBlank() }
        return if (parts.isNotEmpty()) parts else listOf(text)
    }

    /**
     * Parses a list of raw command strings into BatchCommandItems with parsed intents.
     */
    suspend fun parseBatchItems(
        commands: List<String>,
        defaultCurrencySymbol: String,
        defaultCashBoxName: String,
        availableCategories: List<String>,
        assistant: AccountingAssistant? = null,
        repository: AccountingRepository? = null,
        parties: List<Party> = emptyList(),
        cashBoxes: List<CashBox> = emptyList(),
        currencies: List<AppCurrency> = emptyList(),
        expenseCategories: List<ExpenseCategory> = emptyList()
    ): List<BatchCommandItem> = withContext(Dispatchers.Default) {
        val result = mutableListOf<BatchCommandItem>()

        for (cmd in commands) {
            val intent = LocalAccountingNLP.parse(
                input = cmd,
                defaultCurrencySymbol = defaultCurrencySymbol,
                defaultCashBoxName = defaultCashBoxName,
                availableCategories = availableCategories
            )

            result.add(
                BatchCommandItem(
                    rawText = cmd,
                    parsedIntent = intent,
                    isSelected = true,
                    status = BatchCommandStatus.PENDING
                )
            )
        }

        result
    }

    /**
     * Executes the selected batch commands sequentially and atomically records each transaction
     * and its corresponding balanced double-entry journal entry.
     */
    suspend fun executeBatch(
        items: List<BatchCommandItem>,
        assistant: AccountingAssistant,
        repository: AccountingRepository,
        parties: List<Party>,
        cashBoxes: List<CashBox>,
        expenseCategories: List<ExpenseCategory>,
        currencies: List<AppCurrency>,
        currencySymbol: String = "ر.س",
        onProgress: (currentIndex: Int, totalCount: Int, currentItem: BatchCommandItem) -> Unit
    ): BatchExecutionSummary = withContext(Dispatchers.IO) {
        val selectedItems = items.filter { it.isSelected }
        val executedResults = mutableListOf<BatchCommandItem>()

        var successCount = 0
        var failedCount = 0
        var totalInflow = 0.0
        var totalOutflow = 0.0
        val reportBuilder = StringBuilder()

        val totalSelected = selectedItems.size

        for ((index, item) in selectedItems.withIndex()) {
            onProgress(index + 1, totalSelected, item.copy(status = BatchCommandStatus.PROCESSING))

            try {
                val currentParties = repository.getAllPartiesList()
                val currentBoxes = repository.getAllCashBoxesList()
                val currentCategories = repository.getAllExpenseCategoriesList()
                val currentCurrencies = repository.getAllCurrenciesList()
                val activeMemories = repository.getActiveAiMemoriesList()

                val assistantResult = assistant.processUserSpeechOrText(
                    input = item.rawText,
                    repository = repository,
                    existingParties = currentParties,
                    existingBoxes = currentBoxes,
                    existingCategories = currentCategories,
                    existingCurrencies = currentCurrencies,
                    activeMemories = activeMemories
                )

                if (assistantResult.isSuccess) {
                    successCount++
                    val tx = assistantResult.executedTransaction

                    if (tx != null) {
                        when (tx.type) {
                            TransactionType.CUSTOMER_RECEIPT, TransactionType.INCOME -> {
                                totalInflow += tx.amount
                            }
                            TransactionType.SUPPLIER_PAYMENT, TransactionType.EXPENSE -> {
                                totalOutflow += tx.amount
                            }
                            else -> {}
                        }
                    }

                    val updatedItem = item.copy(
                        status = BatchCommandStatus.SUCCESS,
                        executionMessage = assistantResult.reply,
                        createdTransactionId = tx?.id
                    )
                    executedResults.add(updatedItem)
                    reportBuilder.append("✔ ${index + 1}. [${item.rawText}]: ${assistantResult.reply}\n\n")
                } else {
                    failedCount++
                    val updatedItem = item.copy(
                        status = BatchCommandStatus.FAILED,
                        executionMessage = assistantResult.reply
                    )
                    executedResults.add(updatedItem)
                    reportBuilder.append("✖ ${index + 1}. [${item.rawText}]: ${assistantResult.reply}\n\n")
                }
            } catch (e: Exception) {
                failedCount++
                val errorMsg = e.localizedMessage ?: "خطأ أثناء تنفيذ العملية"
                val updatedItem = item.copy(
                    status = BatchCommandStatus.FAILED,
                    executionMessage = errorMsg
                )
                executedResults.add(updatedItem)
                reportBuilder.append("✖ ${index + 1}. [${item.rawText}]: $errorMsg\n\n")
            }
        }

        // Add unselected items as SKIPPED
        items.filter { !it.isSelected }.forEach { unselected ->
            executedResults.add(unselected.copy(status = BatchCommandStatus.SKIPPED, executionMessage = "تم استبعادها من الحزمة"))
        }

        val summaryReport = """
            ⚡ تقرير تنفيذ حزمة العمليات:
            • إجمالي العمليات المعالجة: $totalSelected
            • العمليات الناجحة: $successCount
            • العمليات المتعثرة: $failedCount
            • إجمالي المقبوضات: ${decimalFormat.format(totalInflow)} $currencySymbol
            • إجمالي المدفوعات: ${decimalFormat.format(totalOutflow)} $currencySymbol
            
            التفاصيل:
            $reportBuilder
        """.trimIndent()

        BatchExecutionSummary(
            totalCommands = totalSelected,
            successfulCount = successCount,
            failedCount = failedCount,
            totalInflow = totalInflow,
            totalOutflow = totalOutflow,
            executionReport = summaryReport,
            executedItemResults = executedResults
        )
    }
}
