package com.example.ai

import com.example.data.model.PartyType
import com.example.data.model.TransactionType
import java.util.regex.Pattern

data class ParsedAccountingIntent(
    val action: String, // "TRANSACTION", "QUERY", "CHAT", "QUESTION", "REMEMBER", "ADVICE", "REPORT", "MANAGE_ENTITY"
    val transactionType: TransactionType? = null,
    val partyName: String? = null,
    val partyType: PartyType? = null,
    val partyPhone: String? = null,
    val cashBoxName: String? = null,
    val targetCashBoxName: String? = null,
    val category: String = "",
    val currency: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val replyMessage: String = "",
    val learnedMemoryCategory: String? = null,
    val learnedMemoryKey: String? = null,
    val learnedMemoryFact: String? = null,
    val reportType: String? = null, // "DAILY", "PARTY", "CASH", "CUSTOM"
    val entityOperation: String? = null, // "ADD_PARTY", "UPDATE_PARTY", "DELETE_PARTY", "ADD_CASH_BOX", "UPDATE_CASH_BOX", "DELETE_CASH_BOX", "TRANSFER_CASH", "ADD_EXPENSE_CATEGORY", "DELETE_EXPENSE_CATEGORY", "SET_DEFAULT_CURRENCY"
    val entityType: String? = null, // "CUSTOMER", "SUPPLIER", "CASH_BOX", "EXPENSE_CATEGORY", "CURRENCY"
    val newName: String? = null,
    val initialBalance: Double = 0.0
)

object LocalAccountingNLP {

    fun parse(
        input: String,
        defaultCurrencySymbol: String = "ر.س",
        defaultCashBoxName: String = "الصندوق الرئيسي",
        availableCategories: List<String> = emptyList(),
        activeMemories: List<com.example.data.model.AiMemoryFact> = emptyList()
    ): ParsedAccountingIntent {
        val text = input.trim()
        val lowerText = text.lowercase()

        // 0. Check for explicit memory instruction e.g. "تذكر أن...", "احفظ عندك أن...", "remember that..."
        if (text.startsWith("تذكر ان", ignoreCase = true) ||
            text.startsWith("تذكر أن", ignoreCase = true) ||
            text.startsWith("احفظ عندك", ignoreCase = true) ||
            text.startsWith("احفظ ان", ignoreCase = true) ||
            text.startsWith("احفظ أن", ignoreCase = true) ||
            lowerText.startsWith("remember that") ||
            lowerText.startsWith("remember:")
        ) {
            val fact = text.replace(Regex("^(تذكر أن|تذكر ان|احفظ عندك أن|احفظ عندك ان|احفظ عندك|احفظ أن|احفظ ان|remember that|remember:)\\s*", RegexOption.IGNORE_CASE), "").trim()
            if (fact.isNotBlank()) {
                val key = if (fact.length > 25) fact.take(25) + "..." else fact
                return ParsedAccountingIntent(
                    action = "REMEMBER",
                    replyMessage = "🧠 تم حفظ هذه المعلومة في ذاكرة المحاسب بنجاح: \"$fact\". سأعتمدها في المعاملات والاستشارات القادمة!",
                    learnedMemoryCategory = "USER_PREFERENCE",
                    learnedMemoryKey = key,
                    learnedMemoryFact = fact
                )
            }
        }

        // Check for memory query: "ماذا تتذكر؟" or "ما هي ذاكرتك؟" or "what do you remember?"
        if (lowerText.contains("ماذا تتذكر") || lowerText.contains("ايش تتذكر") || lowerText.contains("شو بتتذكر") || lowerText.contains("ذاكرتك") || lowerText.contains("what do you remember")) {
            val memoriesSummary = if (activeMemories.isNotEmpty()) {
                "🧠 إليك أهم ما أتذكره في ذاكرتي المحاسبية:\n" + activeMemories.take(8).joinToString("\n") { "• [${it.category.titleAr}] ${it.fact}" }
            } else {
                "🧠 ذاكرتي حالياً نظيفة وجاهزة! يمكنك إخباري بأي تفاصيل مثل 'تذكر أن المحل اسمه متجر الأمانة' أو 'تذكر أن محمد مورد البضاعة' وسأحفظها فوراً."
            }
            return ParsedAccountingIntent(
                action = "CHAT",
                replyMessage = memoriesSummary
            )
        }

        // Extract amount: e.g. 500, 500.5, 500 ريال, $500, 500 usd
        val amount = extractAmount(text)
        val extractedCurrency = extractCurrency(text).ifBlank { defaultCurrencySymbol }

        // Check for specific report generation queries (Daily, Party Statement, Cash in Hand, Custom)
        if (matchesAny(lowerText, listOf("تقرير", "كشف حساب", "كشف", "ملخص اليوم", "حركة اليوم", "تقرير اليوم", "statement", "report", "daily summary", "cash in hand", "تصدير"))) {
            when {
                matchesAny(lowerText, listOf("اليوم", "اليومية", "حركة اليوم", "ملخص اليوم", "daily")) -> {
                    return ParsedAccountingIntent(
                        action = "REPORT",
                        reportType = "DAILY",
                        replyMessage = "تقرير حركة وملخص اليوم المالي"
                    )
                }
                matchesAny(lowerText, listOf("صندوق", "صناديق", "خزينة", "النقدية", "cash")) -> {
                    return ParsedAccountingIntent(
                        action = "REPORT",
                        reportType = "CASH",
                        replyMessage = "كشف النقدية والصناديق الحالية"
                    )
                }
                matchesAny(lowerText, listOf("عميل", "عملاء", "ما لي", "ديون العملاء", "customer")) -> {
                    val pName = extractPartyNameFromReportQuery(text)
                    return ParsedAccountingIntent(
                        action = "REPORT",
                        reportType = if (pName.isNotBlank()) "PARTY" else "CUSTOM",
                        partyName = pName.ifBlank { null },
                        replyMessage = if (pName.isNotBlank()) "كشف حساب العميل $pName" else "تقرير ديون ومستحقات العملاء"
                    )
                }
                matchesAny(lowerText, listOf("مورد", "موردين", "ما علي", "التزامات", "supplier")) -> {
                    val pName = extractPartyNameFromReportQuery(text)
                    return ParsedAccountingIntent(
                        action = "REPORT",
                        reportType = if (pName.isNotBlank()) "PARTY" else "CUSTOM",
                        partyName = pName.ifBlank { null },
                        replyMessage = if (pName.isNotBlank()) "كشف حساب المورد $pName" else "تقرير التزامات الموردين"
                    )
                }
                lowerText.contains("كشف حساب") || lowerText.contains("كشف") || lowerText.contains("statement") -> {
                    val pName = extractPartyNameFromReportQuery(text)
                    if (pName.isNotBlank()) {
                        return ParsedAccountingIntent(
                            action = "REPORT",
                            reportType = "PARTY",
                            partyName = pName,
                            replyMessage = "كشف حساب $pName"
                        )
                    } else {
                        return ParsedAccountingIntent(
                            action = "REPORT",
                            reportType = "DAILY",
                            replyMessage = "كشف التقرير المالي العام"
                        )
                    }
                }
                else -> {
                    return ParsedAccountingIntent(
                        action = "REPORT",
                        reportType = "CUSTOM",
                        replyMessage = "تقرير مالي مخصص"
                    )
                }
            }
        }

        // Check for balance queries in Arabic or English
        if (isBalanceQuery(lowerText)) {
            return ParsedAccountingIntent(
                action = "QUERY",
                replyMessage = "استعلام عن الأرصدة والموقف المالي الحالي"
            )
        }

        // ==========================================
        // Entity Management (Add / Update / Delete Parties, Cash Boxes, Expense Categories, Transfers, Transactions)
        // ==========================================
        // 0. Delete / Revert Last Transaction: "احذف آخر عملية", "امسح آخر حركة", "الغاء آخر عملية", "تراجع عن آخر حركة", "delete last transaction"
        if (matchesAny(lowerText, listOf(
                "احذف آخر عملية", "احذف اخر عملية", "احذف آخر عمليه", "احذف اخر عمليه",
                "امسح آخر عملية", "امسح اخر عملية", "الغاء آخر عملية", "إلغاء آخر عملية",
                "احذف آخر قيد", "احذف اخر قيد", "امسح آخر حركة", "امسح اخر حركة",
                "تراجع عن آخر", "تراجع عن اخر", "delete last transaction", "revert last transaction",
                "احذف العملية الأخيرة", "احذف العملية الاخيرة"
            ))
        ) {
            return ParsedAccountingIntent(
                action = "MANAGE_ENTITY",
                entityOperation = "DELETE_TRANSACTION",
                replyMessage = "حذف آخر عملية مالية مسجلة وإعادة ضبط الأرصدة"
            )
        }

        // 1. Cash Transfer between Cash Boxes: "حول 500 من الصندوق الرئيسي إلى بنك الراجحي", "transfer 500 from box A to box B"
        if ((lowerText.contains("حول") || lowerText.contains("تحويل") || lowerText.contains("حولت") || lowerText.contains("transfer")) &&
            (lowerText.contains("من") || lowerText.contains("from")) &&
            (lowerText.contains("إلى") || lowerText.contains("الى") || lowerText.contains("to"))
        ) {
            val fromBox = extractBetween(text, listOf("من", "from"), listOf("إلى", "الى", "to"))
            val toBox = extractAfter(text, listOf("إلى", "الى", "to"))
            if (amount > 0 && fromBox.isNotBlank() && toBox.isNotBlank()) {
                return ParsedAccountingIntent(
                    action = "MANAGE_ENTITY",
                    entityOperation = "TRANSFER_CASH",
                    amount = amount,
                    cashBoxName = fromBox,
                    targetCashBoxName = toBox,
                    description = "تحويل نقدي من $fromBox إلى $toBox",
                    replyMessage = "تحويل $amount $extractedCurrency من $fromBox إلى $toBox"
                )
            }
        }

        // 2. Add Customer or Supplier
        val isAddCustomer = matchesAny(lowerText, listOf(
            "أضف عميل", "اضف عميل", "ضيف عميل", "عميل جديد", "تسجيل عميل", "سجل عميل",
            "سجل لي عميل", "انشئ عميل", "إنشاء عميل", "افتح حساب عميل", "إضافة عميل",
            "اضافة عميل", "اريد اضافة عميل", "ابغى اضيف عميل", "add customer", "new customer", "create customer"
        ))
        val isAddSupplier = matchesAny(lowerText, listOf(
            "أضف مورد", "اضف مورد", "ضيف مورد", "مورد جديد", "تسجيل مورد", "سجل مورد",
            "سجل لي مورد", "انشئ مورد", "إنشاء مورد", "افتح حساب مورد", "إضافة مورد",
            "اضافة مورد", "اريد اضافة مورد", "ابغى اضيف مورد", "add supplier", "new supplier", "create supplier"
        ))

        if (isAddCustomer || isAddSupplier) {
            val isCustomer = !isAddSupplier
            val phone = extractPhoneNumber(text)
            val pName = extractEntityNameAfterKeywords(
                text = text,
                keywords = listOf(
                    "اسمه", "اسم", "باسم", "بإسم", "العميل", "عميل", "المورد", "مورد",
                    "عميل جديد", "مورد جديد", "customer", "supplier", "customer named"
                ),
                stopWords = listOf("ورقمه", "رقم", "هاتف", "هاتفه", "برصيد", "رصيد", "phone", "with balance")
            )

            return ParsedAccountingIntent(
                action = "MANAGE_ENTITY",
                entityOperation = "ADD_PARTY",
                partyType = if (isCustomer) PartyType.CUSTOMER else PartyType.SUPPLIER,
                partyName = pName.ifBlank { if (isCustomer) "عميل جديد" else "مورد جديد" },
                partyPhone = phone.ifBlank { null },
                initialBalance = amount,
                replyMessage = "إضافة ${if (isCustomer) "العميل" else "المورد"} $pName"
            )
        }

        // 3. Update Customer / Supplier: "عدل اسم العميل ماجد إلى ماجد الأحمد", "عدل رقم العميل ماجد إلى 0555", "عدل رصيد العميل ماجد إلى 500"
        val isUpdateParty = matchesAny(lowerText, listOf(
            "عدل العميل", "تعديل العميل", "عدل بيانات العميل", "عدل اسم العميل", "تغيير اسم العميل",
            "عدل رقم العميل", "تعديل رقم العميل", "عدل هاتف العميل", "عدل رصيد العميل", "تعديل رصيد العميل",
            "عدل المورد", "تعديل المورد", "عدل بيانات المورد", "عدل اسم المورد", "تغيير اسم المورد",
            "عدل رقم المورد", "تعديل رقم المورد", "عدل رصيد المورد", "تعديل رصيد المورد",
            "update customer", "edit customer", "update supplier", "edit supplier"
        ))

        if (isUpdateParty) {
            val isCustomer = !matchesAny(lowerText, listOf("مورد", "supplier"))
            val phone = extractPhoneNumber(text)
            val pName = extractEntityNameAfterKeywords(
                text = text,
                keywords = listOf("اسم العميل", "اسم المورد", "العميل", "عميل", "المورد", "مورد", "customer", "supplier"),
                stopWords = listOf("إلى", "الى", "ورقمه", "رقم", "رصيد", "to", "phone")
            )
            val newName = if (lowerText.contains("اسم") && (lowerText.contains("إلى") || lowerText.contains("الى") || lowerText.contains("to"))) {
                extractAfter(text, listOf("إلى", "الى", "to"))
            } else null

            return ParsedAccountingIntent(
                action = "MANAGE_ENTITY",
                entityOperation = "UPDATE_PARTY",
                partyType = if (isCustomer) PartyType.CUSTOMER else PartyType.SUPPLIER,
                partyName = pName,
                newName = newName,
                partyPhone = phone.ifBlank { null },
                amount = amount,
                replyMessage = "تعديل بيانات ${if (isCustomer) "العميل" else "المورد"} $pName"
            )
        }

        // 4. Delete Party: "احذف العميل ماجد", "امسح المورد شركة النور", "delete customer John"
        if (matchesAny(lowerText, listOf(
                "احذف العميل", "امسح العميل", "حذف العميل", "إلغاء العميل", "ازالة العميل",
                "احذف المورد", "امسح المورد", "حذف المورد", "إلغاء المورد", "ازالة المورد",
                "احذف عميل", "امسح عميل", "احذف مورد", "امسح مورد",
                "delete customer", "delete supplier"
            ))
        ) {
            val isCustomer = !matchesAny(lowerText, listOf("مورد", "supplier"))
            val pName = extractEntityNameAfterKeywords(text, listOf("العميل", "عميل", "المورد", "مورد", "customer", "supplier"))
            return ParsedAccountingIntent(
                action = "MANAGE_ENTITY",
                entityOperation = "DELETE_PARTY",
                partyType = if (isCustomer) PartyType.CUSTOMER else PartyType.SUPPLIER,
                partyName = pName,
                replyMessage = "حذف ${if (isCustomer) "العميل" else "المورد"} $pName"
            )
        }

        // 5. Add Cash Box / Bank: "أضف صندوق جديد باسم بنك الراجحي برصيد 5000", "أضف بنك الراجحي", "add cash box Bank ABC"
        if (matchesAny(lowerText, listOf(
                "أضف صندوق", "اضف صندوق", "ضيف صندوق", "إضافة صندوق", "اضافة صندوق",
                "أنشئ صندوق", "انشئ صندوق", "صندوق جديد", "خزينة جديدة", "حساب بنكي جديد",
                "أضف بنك", "اضف بنك", "ضيف بنك", "إضافة بنك", "اضافة بنك", "افتح حساب بنكي",
                "add cash box", "add bank", "new cash box", "new bank"
            ))
        ) {
            val boxName = extractEntityNameAfterKeywords(
                text = text,
                keywords = listOf(
                    "باسم", "بإسم", "اسم", "صندوق جديد", "خزينة جديدة", "حساب بنكي",
                    "أضف بنك", "اضف بنك", "ضيف بنك", "بنك", "الصندوق", "صندوق", "cash box", "bank"
                ),
                stopWords = listOf("برصيد", "رصيد", "with balance", "balance")
            )
            return ParsedAccountingIntent(
                action = "MANAGE_ENTITY",
                entityOperation = "ADD_CASH_BOX",
                cashBoxName = boxName.ifBlank { "صندوق جديد" },
                amount = amount,
                replyMessage = "إضافة صندوق/حساب بنكي باسم $boxName"
            )
        }

        // 6. Update Cash Box: "عدل رصيد الصندوق الرئيسي إلى 5000", "عدل اسم الصندوق..."
        if (matchesAny(lowerText, listOf(
                "عدل الصندوق", "تعديل الصندوق", "عدل رصيد الصندوق", "تعديل رصيد الصندوق",
                "عدل اسم الصندوق", "تغيير اسم الصندوق", "عدل الخزينة", "عدل البنك", "update cash box"
            ))
        ) {
            val boxName = extractEntityNameAfterKeywords(text, listOf("الصندوق", "صندوق", "الخزينة", "البنك", "cash box"), stopWords = listOf("إلى", "الى", "رصيد", "to"))
            val newName = if (lowerText.contains("اسم") && (lowerText.contains("إلى") || lowerText.contains("الى") || lowerText.contains("to"))) {
                extractAfter(text, listOf("إلى", "الى", "to"))
            } else null
            return ParsedAccountingIntent(
                action = "MANAGE_ENTITY",
                entityOperation = "UPDATE_CASH_BOX",
                cashBoxName = boxName,
                newName = newName,
                amount = amount,
                replyMessage = "تعديل بيانات الصندوق $boxName"
            )
        }

        // 7. Delete Cash Box: "احذف الصندوق بنك الراجحي", "delete cash box..."
        if (matchesAny(lowerText, listOf("احذف الصندوق", "امسح الصندوق", "حذف الصندوق", "احذف الخزينة", "احذف البنك", "حذف البنك", "delete cash box"))) {
            val boxName = extractEntityNameAfterKeywords(text, listOf("الصندوق", "صندوق", "الخزينة", "البنك", "cash box"))
            return ParsedAccountingIntent(
                action = "MANAGE_ENTITY",
                entityOperation = "DELETE_CASH_BOX",
                cashBoxName = boxName,
                replyMessage = "حذف الصندوق $boxName"
            )
        }

        // 8. Add Expense Category: "أضف بند مصروف جديد باسم صيانة السيارات", "add expense category Maintenance"
        if (matchesAny(lowerText, listOf(
                "أضف بند مصروف", "اضف بند مصروف", "ضيف بند مصروف",
                "أضف تصنيف مصروف", "اضف تصنيف مصروف", "ضيف تصنيف مصروف",
                "إضافة بند مصروف", "اضافة بند مصروف", "بند مصروف جديد", "تصنيف مصروف جديد",
                "أضف مصروف جديد", "اضف مصروف جديد",
                "add expense category", "new expense category"
            ))
        ) {
            val catName = extractEntityNameAfterKeywords(text, listOf("باسم", "بإسم", "اسم", "مصروف", "تصنيف", "category"))
            return ParsedAccountingIntent(
                action = "MANAGE_ENTITY",
                entityOperation = "ADD_EXPENSE_CATEGORY",
                category = catName.ifBlank { "بند مصروف جديد" },
                replyMessage = "إضافة بند مصروف جديد باسم $catName"
            )
        }

        // 9. Update Expense Category: "عدل بند المصروف صيانة إلى صيانة عامة"
        if (matchesAny(lowerText, listOf("عدل بند المصروف", "تعديل بند المصروف", "تغيير اسم بند المصروف", "عدل تصنيف المصروف", "تعديل تصنيف المصروف"))) {
            val catName = extractEntityNameAfterKeywords(text, listOf("المصروف", "بند", "تصنيف", "category"), stopWords = listOf("إلى", "الى", "to"))
            val newName = if (lowerText.contains("إلى") || lowerText.contains("الى") || lowerText.contains("to")) {
                extractAfter(text, listOf("إلى", "الى", "to"))
            } else null
            return ParsedAccountingIntent(
                action = "MANAGE_ENTITY",
                entityOperation = "UPDATE_EXPENSE_CATEGORY",
                category = catName,
                newName = newName,
                replyMessage = "تعديل بند المصروف $catName"
            )
        }

        // 10. Delete Expense Category: "احذف بند المصروف صيانة السيارات"
        if (matchesAny(lowerText, listOf("احذف بند المصروف", "احذف تصنيف المصروف", "امسح بند المصروف", "حذف بند المصروف", "delete expense category"))) {
            val catName = extractEntityNameAfterKeywords(text, listOf("المصروف", "بند", "تصنيف", "category"))
            return ParsedAccountingIntent(
                action = "MANAGE_ENTITY",
                entityOperation = "DELETE_EXPENSE_CATEGORY",
                category = catName,
                replyMessage = "حذف بند المصروف $catName"
            )
        }

        // 11. Add Currency: "أضف عملة جديدة باسم درهم", "أضف عملة اليورو"
        if (matchesAny(lowerText, listOf("أضف عملة", "اضف عملة", "ضيف عملة", "عملة جديدة", "add currency"))) {
            val currName = extractEntityNameAfterKeywords(text, listOf("باسم", "بإسم", "اسم", "عملة", "currency"))
            return ParsedAccountingIntent(
                action = "MANAGE_ENTITY",
                entityOperation = "ADD_CURRENCY",
                currency = currName,
                replyMessage = "إضافة عملة جديدة $currName"
            )
        }

        // 12. Change Default Currency: "غير العملة الافتراضية إلى الدولار", "اجعل العملة الافتراضية دولار"
        if (matchesAny(lowerText, listOf("غير العملة", "تغيير العملة", "اجعل العملة", "العملة الافتراضية", "change currency", "default currency"))) {
            val currName = extractEntityNameAfterKeywords(text, listOf("إلى", "الى", "الافتراضية", "currency", "to"))
            return ParsedAccountingIntent(
                action = "MANAGE_ENTITY",
                entityOperation = "SET_DEFAULT_CURRENCY",
                currency = currName,
                replyMessage = "تغيير العملة الافتراضية إلى $currName"
            )
        }

        // ==========================================
        // 1. English Expense Pattern: "I paid 500 for rent", "spent 60 on gas", "bought supplies for 100"
        // ==========================================
        if (matchesAny(lowerText, listOf("paid", "spent", "bought", "cost")) &&
            matchesAny(lowerText, listOf("for", "on", "rent", "gas", "food", "dinner", "lunch", "coffee", "groceries", "electricity", "water", "bill", "fuel", "taxi", "hotel", "supplies"))
        ) {
            val desc = extractEnglishExpenseDesc(lowerText)
            val predictedCat = predictCategory(desc, availableCategories)
            return ParsedAccountingIntent(
                action = "TRANSACTION",
                transactionType = TransactionType.EXPENSE,
                amount = amount,
                category = predictedCat,
                currency = extractedCurrency,
                cashBoxName = defaultCashBoxName,
                description = if (desc.isNotBlank()) desc else "Expense / مصروف",
                replyMessage = "تم تسجيل مصروف بقيمة $amount $extractedCurrency ($desc) في بند [$predictedCat] من $defaultCashBoxName"
            )
        }

        // ==========================================
        // 2. English Payment to Supplier: "I paid 300 to John", "paid 200 to supplier Mike"
        // ==========================================
        if (lowerText.contains("paid") && (lowerText.contains("to") || lowerText.contains("supplier"))) {
            val party = extractEnglishPartyName(text, listOf("to supplier", "to", "supplier"))
            return ParsedAccountingIntent(
                action = "TRANSACTION",
                transactionType = TransactionType.SUPPLIER_PAYMENT,
                partyName = party.ifBlank { "Supplier" },
                partyType = PartyType.SUPPLIER,
                amount = amount,
                currency = extractedCurrency,
                cashBoxName = defaultCashBoxName,
                description = "Payment to supplier $party",
                replyMessage = "Recorded payment of $amount $extractedCurrency to supplier $party / تم تسجيل سند صرف لمورد $party"
            )
        }

        // ==========================================
        // 3. English Receipt from Customer: "received 500 from Mike", "collected 300 from customer Sarah", "got 200 from Alex"
        // ==========================================
        if (matchesAny(lowerText, listOf("received", "collected", "got from", "payment from", "receipt from")) ||
            (lowerText.contains("got") && lowerText.contains("from"))
        ) {
            val party = extractEnglishPartyName(text, listOf("from customer", "from client", "from"))
            return ParsedAccountingIntent(
                action = "TRANSACTION",
                transactionType = TransactionType.CUSTOMER_RECEIPT,
                partyName = party.ifBlank { "Customer" },
                partyType = PartyType.CUSTOMER,
                amount = amount,
                currency = extractedCurrency,
                cashBoxName = defaultCashBoxName,
                description = "Receipt from customer $party",
                replyMessage = "Recorded receipt of $amount $extractedCurrency from $party / تم تسجيل سند قبض من العميل $party"
            )
        }

        // ==========================================
        // 4. English New Debt on Customer: "loaned 200 to David", "lent 100 to Kevin", "debt on John 300"
        // ==========================================
        if (matchesAny(lowerText, listOf("loaned", "lent", "debt on", "credit to"))) {
            val party = extractEnglishPartyName(text, listOf("to", "on"))
            return ParsedAccountingIntent(
                action = "TRANSACTION",
                transactionType = TransactionType.CUSTOMER_NEW_DEBIT,
                partyName = party.ifBlank { "Customer" },
                partyType = PartyType.CUSTOMER,
                amount = amount,
                currency = extractedCurrency,
                cashBoxName = defaultCashBoxName,
                description = "Loan/Debit to $party",
                replyMessage = "Recorded new debt of $amount $extractedCurrency on $party (receivable) / تم قيد دين على $party"
            )
        }

        // ==========================================
        // 5. English Debt from Supplier: "borrowed 300 from Alex", "debt from Sarah 400"
        // ==========================================
        if (matchesAny(lowerText, listOf("borrowed", "debt from", "payable to"))) {
            val party = extractEnglishPartyName(text, listOf("from", "to"))
            return ParsedAccountingIntent(
                action = "TRANSACTION",
                transactionType = TransactionType.SUPPLIER_NEW_CREDIT,
                partyName = party.ifBlank { "Supplier" },
                partyType = PartyType.SUPPLIER,
                amount = amount,
                currency = extractedCurrency,
                cashBoxName = defaultCashBoxName,
                description = "Debt from supplier $party",
                replyMessage = "Recorded new payable of $amount $extractedCurrency from $party / تم تسجيل التزام مالي لـ $party"
            )
        }

        // ==========================================
        // 6. English Income: "salary 5000", "income 2000", "earned 500 profit"
        // ==========================================
        if (matchesAny(lowerText, listOf("salary", "income", "earned", "revenue", "profit", "bonus"))) {
            return ParsedAccountingIntent(
                action = "TRANSACTION",
                transactionType = TransactionType.INCOME,
                amount = amount,
                currency = extractedCurrency,
                cashBoxName = defaultCashBoxName,
                description = "Income / Salary / إيراد",
                replyMessage = "Recorded income of $amount $extractedCurrency in $defaultCashBoxName / تم تسجيل إيراد $amount في $defaultCashBoxName"
            )
        }

        // ==========================================
        // 7. English Transfer: "transfer 500 to bank", "transferred 200 to cash"
        // ==========================================
        if (matchesAny(lowerText, listOf("transfer", "transferred"))) {
            return ParsedAccountingIntent(
                action = "TRANSACTION",
                transactionType = TransactionType.TRANSFER,
                amount = amount,
                currency = extractedCurrency,
                cashBoxName = defaultCashBoxName,
                description = "Transfer between cash boxes",
                replyMessage = "Transferred $amount $extractedCurrency between cash boxes / تم تحويل المبلغ بين الصناديق"
            )
        }

        // ==========================================
        // Arabic Receipt from Customer (قبض من عميل)
        // ==========================================
        if (matchesAny(lowerText, listOf("استلمت", "قبضت", "قبض", "سدد لي", "وصلني", "أخذت من", "تحصيل من", "دفعة من"))) {
            val party = extractPartyName(text, listOf("من", "عن", "العميل", "سدد لي"))
            val note = extractNote(text, listOf("من", "في", "على", "بخصوص", "عن"))
            return ParsedAccountingIntent(
                action = "TRANSACTION",
                transactionType = TransactionType.CUSTOMER_RECEIPT,
                partyName = party.ifBlank { "عميل عام" },
                partyType = PartyType.CUSTOMER,
                amount = amount,
                currency = extractedCurrency,
                cashBoxName = defaultCashBoxName,
                description = note.ifBlank { "قبض نقدي من $party" },
                replyMessage = "تم تسجيل سند قبض بقيمة $amount $extractedCurrency من $party في $defaultCashBoxName بنجاح"
            )
        }

        // ==========================================
        // Arabic Payment to Supplier (دفع لمورد)
        // ==========================================
        if (matchesAny(lowerText, listOf("دفعت ل", "سددت ل", "أعطيت ل", "صرفت ل", "دفع للمورد", "سداد للمورد", "دفعت للمورد", "سددت للمورد"))) {
            val party = extractPartyName(text, listOf("لـ", "ل", "إلى", "للمورد", "المورد"))
            val note = extractNote(text, listOf("ل", "في", "من", "عن"))
            return ParsedAccountingIntent(
                action = "TRANSACTION",
                transactionType = TransactionType.SUPPLIER_PAYMENT,
                partyName = party.ifBlank { "مورد عام" },
                partyType = PartyType.SUPPLIER,
                amount = amount,
                currency = extractedCurrency,
                cashBoxName = defaultCashBoxName,
                description = note.ifBlank { "سداد نقدي للمورد $party" },
                replyMessage = "تم تسجيل سداد نقدي بقيمة $amount $extractedCurrency للمورد $party من $defaultCashBoxName بنجاح"
            )
        }

        // ==========================================
        // Arabic New Debt on Customer (دين على عميل)
        // ==========================================
        if (matchesAny(lowerText, listOf("دين على", "سلفت", "أقرضت", "سلف ل", "بعت آجل", "دين لعميل", "سجل على"))) {
            val party = extractPartyName(text, listOf("على", "لـ", "ل", "العميل"))
            val note = extractNote(text, listOf("على", "ل", "مقابل", "عن"))
            return ParsedAccountingIntent(
                action = "TRANSACTION",
                transactionType = TransactionType.CUSTOMER_NEW_DEBIT,
                partyName = party.ifBlank { "عميل" },
                partyType = PartyType.CUSTOMER,
                amount = amount,
                currency = extractedCurrency,
                cashBoxName = defaultCashBoxName,
                description = note.ifBlank { "دين/مبيعات آجل على $party" },
                replyMessage = "تم قيد دين جديد على العميل $party بمبلغ $amount $extractedCurrency (ما لك عنده)"
            )
        }

        // ==========================================
        // Arabic New Debt from Supplier (دين من مورد)
        // ==========================================
        if (matchesAny(lowerText, listOf("دين من", "اشتريت آجل", "تداينت من", "استلفت من", "شريت آجل", "دين علي ل"))) {
            val party = extractPartyName(text, listOf("من", "المورد", "لـ", "ل"))
            val note = extractNote(text, listOf("من", "ل", "بضاعة", "عن"))
            return ParsedAccountingIntent(
                action = "TRANSACTION",
                transactionType = TransactionType.SUPPLIER_NEW_CREDIT,
                partyName = party.ifBlank { "مورد" },
                partyType = PartyType.SUPPLIER,
                amount = amount,
                currency = extractedCurrency,
                cashBoxName = defaultCashBoxName,
                description = note.ifBlank { "دين جديد/مشتريات آجل من $party" },
                replyMessage = "تم تسجيل التزام مالي جديد للمورد $party بمبلغ $amount $extractedCurrency (ما عليك له)"
            )
        }

        // ==========================================
        // Arabic Transfer between cash boxes
        // ==========================================
        if (matchesAny(lowerText, listOf("تحويل", "حولت", "نقلت من صندوق"))) {
            return ParsedAccountingIntent(
                action = "TRANSACTION",
                transactionType = TransactionType.TRANSFER,
                amount = amount,
                currency = extractedCurrency,
                cashBoxName = defaultCashBoxName,
                description = "تحويل نقدي بين الصناديق",
                replyMessage = "تم تحويل مبلغ $amount $extractedCurrency بين الصناديق بنجاح"
            )
        }

        // ==========================================
        // Arabic General Expense with Smart Category Prediction
        // ==========================================
        if (matchesAny(lowerText, listOf("صرفت", "مصروف", "فاتورة", "بنزين", "غداء", "عشاء", "إيجار", "شراء", "اشتريت", "دفعت", "صيانة", "كهرباء", "مياه", "نت", "رواتب", "راتب", "بوفيه", "قهوة", "شاي", "إعلان"))) {
            val desc = text.replace(Regex("[0-9٠-٩]+(\\.[0-9٠-٩]+)?"), "").replace("ريال", "").replace("صرفت", "").replace("دفعت", "").trim()
            val predictedCat = predictCategory(text, availableCategories)
            return ParsedAccountingIntent(
                action = "TRANSACTION",
                transactionType = TransactionType.EXPENSE,
                amount = amount,
                category = predictedCat,
                currency = extractedCurrency,
                cashBoxName = defaultCashBoxName,
                description = if (desc.isNotBlank()) desc else "مصروف ($predictedCat)",
                replyMessage = "تم تسجيل مصروف بقيمة $amount $extractedCurrency في بند [$predictedCat] من $defaultCashBoxName"
            )
        }

        // ==========================================
        // Arabic General Income
        // ==========================================
        if (matchesAny(lowerText, listOf("إيراد", "دخل", "أرباح", "مكافأة", "حافز"))) {
            val desc = text.replace(Regex("[0-9٠-٩]+(\\.[0-9٠-٩]+)?"), "").replace("ريال", "").trim()
            return ParsedAccountingIntent(
                action = "TRANSACTION",
                transactionType = TransactionType.INCOME,
                amount = amount,
                currency = extractedCurrency,
                cashBoxName = defaultCashBoxName,
                description = if (desc.isNotBlank()) desc else "إيراد إضافي",
                replyMessage = "تم تسجيل إيراد نقدي بقيمة $amount $extractedCurrency في $defaultCashBoxName"
            )
        }

        // Ambiguous Name + Number without clear intent (e.g. "أحمد 500" or "خالد 200") -> Smart Clarification Question
        if (amount > 0 && text.split(Regex("\\s+")).size <= 3) {
            val nameCandidate = text.replace(Regex("[0-9٠-٩]+(\\.[0-9٠-٩]+)?"), "").trim()
            if (nameCandidate.isNotBlank()) {
                return ParsedAccountingIntent(
                    action = "QUESTION",
                    amount = amount,
                    partyName = nameCandidate,
                    currency = extractedCurrency,
                    replyMessage = "هل تقصد قبض $amount $extractedCurrency من $nameCandidate، أم دفع له، أم تسجيل دين عليه؟ يرجى التحديد لأقوم بالقيد فوراً."
                )
            }
        }

        // Fallback Chat/Explanation
        return ParsedAccountingIntent(
            action = "CHAT",
            replyMessage = "أهلاً بك! أنا محاسبك الشخصي الذكي. يمكنك إخباري بالصوت أو النص بما حدث، مثلاً:\n• 'استلمت 500 من العميل محمد'\n• 'دفعت 300 للمورد أحمد'\n• 'صرفت 60 بنزين' (سيتم تصنيفه في نقل وبنزين تلقائياً)\n• 'دفعت 150 فاتورة كهرباء' (سيتم تصنيفه في فواتير ومرافق)\nوسأقوم بتسجيلها في الصندوق والعملة الافتراضية وتحديث أرصدتك بدقة!"
        )
    }

    fun predictCategory(text: String, customCategories: List<String>): String {
        val lower = text.lowercase()

        // 1. Check custom categories first if any match
        for (cat in customCategories) {
            val catLower = cat.lowercase()
            if (lower.contains(catLower) || catLower.contains(lower)) {
                return cat
            }
        }

        // 2. Keyword smart heuristics
        return when {
            matchesAny(lower, listOf("إيجار", "ايجار", "سكن", "محل", "مستودع", "شقة", "rent")) ->
                customCategories.firstOrNull { it.contains("إيجار") || it.contains("ايجار") } ?: "إيجار"

            matchesAny(lower, listOf("كهرباء", "ماء", "مياه", "فاتورة", "فواتير", "نت", "انترنت", "هاتف", "اتصالات", "شحن رصيد", "electricity", "water", "bill", "wifi", "internet")) ->
                customCategories.firstOrNull { it.contains("فواتير") || it.contains("مرافق") } ?: "فواتير ومرافق"

            matchesAny(lower, listOf("بنزين", "وقود", "سيارة", "ديزل", "تاكسي", "مواصلات", "شحن", "توصيل", "نقل", "gas", "fuel", "taxi", "transport")) ->
                customCategories.firstOrNull { it.contains("نقل") || it.contains("بنزين") || it.contains("مواصلات") } ?: "نقل ومواصلات وبنزين"

            matchesAny(lower, listOf("راتب", "رواتب", "أجور", "اجور", "عامل", "موظف", "حافز", "مكافأة", "salary", "wage", "employee")) ->
                customCategories.firstOrNull { it.contains("رواتب") || it.contains("أجور") } ?: "رواتب وأجور"

            matchesAny(lower, listOf("صيانة", "تصليح", "قطع غيار", "سباكة", "ورشة", "ترميم", "repair", "maintenance")) ->
                customCategories.firstOrNull { it.contains("صيانة") } ?: "صيانة وتشغيل"

            matchesAny(lower, listOf("قهوة", "شاي", "غداء", "عشاء", "فطور", "مطعم", "بوفيه", "ضيافة", "وجبة", "ماء شرب", "coffee", "tea", "lunch", "dinner", "food", "restaurant")) ->
                customCategories.firstOrNull { it.contains("ضيافة") || it.contains("بوفيه") || it.contains("مأكولات") } ?: "ضيافة وبوفيه ومأكولات"

            matchesAny(lower, listOf("بضاعة", "شراء مواد", "مشتريات", "كراتين", "تغليف", "أكياس", "مواد خام", "supplies", "inventory", "stock")) ->
                customCategories.firstOrNull { it.contains("بضاعة") || it.contains("مشتريات") } ?: "بضاعة ومشتريات"

            matchesAny(lower, listOf("إعلان", "اعلان", "تسويق", "دعاية", "سناب", "انستقرام", "تيك توك", "marketing", "ads")) ->
                customCategories.firstOrNull { it.contains("تسويق") || it.contains("إعلانات") } ?: "تسويق وإعلانات"

            else ->
                customCategories.firstOrNull { it.contains("نثرية") || it.contains("عامة") }
                    ?: customCategories.firstOrNull()
                    ?: "نثرية ومصروفات عامة"
        }
    }

    private fun extractCurrency(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("دولار") || lower.contains("usd") || lower.contains("$") -> "$"
            lower.contains("يمني") || lower.contains("ريال يمني") || lower.contains("yer") -> "ر.ي"
            lower.contains("جنيه") || lower.contains("مصر") || lower.contains("egp") -> "ج.م"
            lower.contains("درهم") || lower.contains("aed") -> "د.إ"
            lower.contains("دينار كويتي") || lower.contains("kwd") -> "د.ك"
            lower.contains("قطري") || lower.contains("qar") -> "ر.ق"
            lower.contains("عماني") || lower.contains("omr") -> "ر.ع"
            lower.contains("يورو") || lower.contains("eur") || lower.contains("€") -> "€"
            lower.contains("سعودي") || lower.contains("ريال") || lower.contains("sar") || lower.contains("ر.س") -> "ر.س"
            else -> ""
        }
    }

    private fun isBalanceQuery(text: String): Boolean {
        return matchesAny(text, listOf("كم رصيد", "كم معي", "كم لي", "كم علي", "رصيد الصندوق", "الأرصدة", "كشف حساب", "التقرير", "تقرير اليوم", "balance", "how much", "net worth", "summary"))
    }

    private fun matchesAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    fun extractAmount(text: String): Double {
        // Convert Eastern Arabic numerals to Western
        val normalized = text
            .replace('٠', '0')
            .replace('١', '1')
            .replace('٢', '2')
            .replace('٣', '3')
            .replace('٤', '4')
            .replace('٥', '5')
            .replace('٦', '6')
            .replace('٧', '7')
            .replace('٨', '8')
            .replace('٩', '9')
            .replace("$", " ")
            .replace(",", "")

        val pattern = Pattern.compile("(\\d+(\\.\\d+)?)")
        val matcher = pattern.matcher(normalized)
        if (matcher.find()) {
            return matcher.group(1)?.toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }

    private fun extractEnglishExpenseDesc(lowerText: String): String {
        for (prep in listOf("for", "on", "in")) {
            val idx = lowerText.indexOf(" $prep ")
            if (idx != -1) {
                val after = lowerText.substring(idx + prep.length + 2).trim()
                val words = after.split(Regex("\\s+")).filter { it.isNotBlank() }
                if (words.isNotEmpty()) {
                    return words.take(3).joinToString(" ")
                }
            }
        }
        for (category in listOf("rent", "gas", "groceries", "dinner", "lunch", "coffee", "food", "electricity", "water", "bill", "fuel", "taxi", "hotel", "supplies")) {
            if (lowerText.contains(category)) return category
        }
        return "General Expense"
    }

    private fun extractEnglishPartyName(text: String, prefixes: List<String>): String {
        val lower = text.lowercase()
        for (prefix in prefixes) {
            val idx = lower.indexOf(prefix)
            if (idx != -1) {
                val after = text.substring(idx + prefix.length).trim()
                val words = after.split(Regex("\\s+")).filter { it.isNotBlank() }
                val cleanWords = mutableListOf<String>()
                for (w in words) {
                    if (w.matches(Regex(".*\\d.*")) || w.lowercase() in listOf("dollars", "usd", "cash", "box", "bank", "yesterday", "today")) {
                        break
                    }
                    cleanWords.add(w)
                    if (cleanWords.size >= 2) break
                }
                if (cleanWords.isNotEmpty()) {
                    return cleanWords.joinToString(" ")
                }
            }
        }
        return ""
    }

    private fun extractPartyName(text: String, prepositions: List<String>): String {
        for (prep in prepositions) {
            val idx = text.indexOf(prep)
            if (idx != -1) {
                val after = text.substring(idx + prep.length).trim()
                val words = after.split(Regex("\\s+")).filter { it.isNotBlank() }
                val cleanWords = mutableListOf<String>()
                for (w in words) {
                    if (w.matches(Regex(".*[0-9٠-٩].*")) || w in listOf("في", "على", "ريال", "بقيمة", "مبلغ", "دين", "الصندوق")) {
                        break
                    }
                    cleanWords.add(w)
                    if (cleanWords.size >= 3) break
                }
                if (cleanWords.isNotEmpty()) {
                    return cleanWords.joinToString(" ")
                }
            }
        }
        return ""
    }

    private fun extractNote(text: String, stopWords: List<String>): String {
        val clean = text.replace(Regex("[0-9٠-٩]+(\\.[0-9٠-٩]+)?"), "").replace("ريال", "").trim()
        return clean
    }

    private fun extractPartyNameFromReportQuery(text: String): String {
        val prefixes = listOf(
            "كشف حساب العميل",
            "كشف حساب المورد",
            "كشف حساب لـ",
            "كشف حساب ل",
            "كشف حساب",
            "كشف العميل",
            "كشف المورد",
            "كشف لـ",
            "كشف ل",
            "كشف",
            "تقرير العميل",
            "تقرير المورد",
            "تقرير",
            "statement for",
            "statement of",
            "report for"
        )
        for (prefix in prefixes) {
            val idx = text.indexOf(prefix, ignoreCase = true)
            if (idx != -1) {
                val after = text.substring(idx + prefix.length).trim()
                val words = after.split(Regex("\\s+")).filter { it.isNotBlank() }
                val cleanWords = mutableListOf<String>()
                for (w in words) {
                    if (w in listOf("مع", "تصدير", "pdf", "excel", "إكسل", "بي", "دي", "اف", "طباعة", "اليوم", "هذا", "الشهر", "الماضي", "كامل")) {
                        break
                    }
                    cleanWords.add(w)
                    if (cleanWords.size >= 3) break
                }
                if (cleanWords.isNotEmpty()) {
                    return cleanWords.joinToString(" ")
                }
            }
        }
        return ""
    }

    private fun extractPhoneNumber(text: String): String {
        // Extract 9-14 digit numbers or common phone formats e.g. 0501234567, +966..., 05...
        val match = Regex("(?:\\+?\\d{1,4}[-\\s]?)?\\d{7,14}").find(text)
        return match?.value?.replace(Regex("[\\s-]"), "") ?: ""
    }

    private fun extractEntityNameAfterKeywords(
        text: String,
        keywords: List<String>,
        stopWords: List<String> = emptyList()
    ): String {
        for (kw in keywords) {
            val idx = text.indexOf(kw, ignoreCase = true)
            if (idx != -1) {
                val after = text.substring(idx + kw.length).trim()
                val words = after.split(Regex("\\s+")).filter { it.isNotBlank() }
                val cleanWords = mutableListOf<String>()
                for (w in words) {
                    val isStop = stopWords.any { w.equals(it, ignoreCase = true) } ||
                            w.matches(Regex(".*[0-9٠-٩].*")) ||
                            w in listOf("برصيد", "ورقم", "ورقمه", "هاتف", "هاتفه", "بمبلغ", "مبلغ", "رصيد", "في", "إلى", "الى", "to", "phone")
                    if (isStop) break
                    cleanWords.add(w)
                    if (cleanWords.size >= 4) break
                }
                if (cleanWords.isNotEmpty()) {
                    return cleanWords.joinToString(" ")
                }
            }
        }
        return ""
    }

    private fun extractBetween(text: String, startKeywords: List<String>, endKeywords: List<String>): String {
        for (start in startKeywords) {
            val startIdx = text.indexOf(start, ignoreCase = true)
            if (startIdx != -1) {
                val sub = text.substring(startIdx + start.length).trim()
                for (end in endKeywords) {
                    val endIdx = sub.indexOf(end, ignoreCase = true)
                    if (endIdx != -1) {
                        val result = sub.substring(0, endIdx).trim()
                        val words = result.split(Regex("\\s+")).filter { !it.matches(Regex(".*[0-9٠-٩].*")) }
                        if (words.isNotEmpty()) return words.joinToString(" ")
                    }
                }
            }
        }
        return ""
    }

    private fun extractAfter(text: String, keywords: List<String>): String {
        for (kw in keywords) {
            val idx = text.indexOf(kw, ignoreCase = true)
            if (idx != -1) {
                val after = text.substring(idx + kw.length).trim()
                val words = after.split(Regex("\\s+")).filter { !it.matches(Regex(".*[0-9٠-٩].*")) && it !in listOf("مبلغ", "ريال", "دينار", "دولار", "بمبلغ") }
                if (words.isNotEmpty()) return words.take(3).joinToString(" ")
            }
        }
        return ""
    }
}
