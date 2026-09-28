package com.example.search

import com.example.data.model.CashBox
import com.example.data.model.Party
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class SearchCriteria(
    val rawQuery: String = "",
    val keywords: List<String> = emptyList(),
    val targetTypes: Set<TransactionType>? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val exactAmount: Double? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val matchedPartyIds: Set<Long> = emptySet(),
    val matchedCashBoxIds: Set<Long> = emptySet(),
    val description: String = ""
)

data class SearchSummary(
    val transactions: List<TransactionRecord>,
    val count: Int,
    val totalAmount: Double,
    val totalInflow: Double,
    val totalOutflow: Double,
    val netBalance: Double,
    val matchedCriteriaDescription: String
)

object IntelligentSearchEngine {

    private val dateFormatYmd = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    private val dateFormatDmy = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)

    private val arabicMonths = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    fun parseSearchQuery(
        query: String,
        parties: List<Party>,
        cashBoxes: List<CashBox>
    ): SearchCriteria {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return SearchCriteria()

        val normalized = normalizeArabicNumbers(trimmed)
        val criteriaKeywords = mutableListOf<String>()
        var targetTypes: MutableSet<TransactionType>? = null
        var minAmount: Double? = null
        var maxAmount: Double? = null
        var exactAmount: Double? = null
        var startDate: Long? = null
        var endDate: Long? = null
        val matchedPartyIds = mutableSetOf<Long>()
        val matchedCashBoxIds = mutableSetOf<Long>()
        val explanations = mutableListOf<String>()

        // 1. Amount comparisons: e.g. "> 500", "أكبر من 500", "< 200", "أقل من 200", "بين 100 و 500"
        val betweenPattern = Pattern.compile("(?:بين|من)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:و|إلى|-)\\s*(\\d+(?:\\.\\d+)?)")
        val betweenMatcher = betweenPattern.matcher(normalized)
        if (betweenMatcher.find()) {
            val a = betweenMatcher.group(1)?.toDoubleOrNull() ?: 0.0
            val b = betweenMatcher.group(2)?.toDoubleOrNull() ?: 0.0
            minAmount = minOf(a, b)
            maxAmount = maxOf(a, b)
            explanations.add("المبلغ بين $minAmount و $maxAmount ر.س")
        } else {
            val greaterPattern = Pattern.compile("(?:>|أكبر من|أكثر من|\\+)\\s*(\\d+(?:\\.\\d+)?)")
            val greaterMatcher = greaterPattern.matcher(normalized)
            if (greaterMatcher.find()) {
                minAmount = greaterMatcher.group(1)?.toDoubleOrNull()
                explanations.add("المبلغ أكبر من $minAmount ر.س")
            }

            val lesserPattern = Pattern.compile("(?:<|أقل من|أصغر من)\\s*(\\d+(?:\\.\\d+)?)")
            val lesserMatcher = lesserPattern.matcher(normalized)
            if (lesserMatcher.find()) {
                maxAmount = lesserMatcher.group(1)?.toDoubleOrNull()
                explanations.add("المبلغ أقل من $maxAmount ر.س")
            }
        }

        // 2. Date presets in query
        val nowCal = Calendar.getInstance()
        if (normalized.contains("اليوم")) {
            val todayStart = getStartOfDay(nowCal.timeInMillis)
            startDate = todayStart
            endDate = nowCal.timeInMillis
            explanations.add("حركات اليوم")
        } else if (normalized.contains("أمس") || normalized.contains("الامس")) {
            val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            startDate = getStartOfDay(yesterdayCal.timeInMillis)
            endDate = getEndOfDay(yesterdayCal.timeInMillis)
            explanations.add("حركات أمس")
        } else if (normalized.contains("هذا الأسبوع") || normalized.contains("هذا الاسبوع")) {
            val weekCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            }
            startDate = getStartOfDay(weekCal.timeInMillis)
            endDate = nowCal.timeInMillis
            explanations.add("حركات هذا الأسبوع")
        } else if (normalized.contains("هذا الشهر")) {
            val monthCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }
            startDate = getStartOfDay(monthCal.timeInMillis)
            endDate = nowCal.timeInMillis
            explanations.add("حركات هذا الشهر")
        } else if (normalized.contains("الشهر الماضي")) {
            val lastMonthStart = Calendar.getInstance().apply {
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val lastMonthEnd = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.DAY_OF_MONTH, -1)
            }
            startDate = getStartOfDay(lastMonthStart.timeInMillis)
            endDate = getEndOfDay(lastMonthEnd.timeInMillis)
            explanations.add("حركات الشهر الماضي")
        } else {
            // Check for Arabic Month names: e.g. "سبتمبر", "أغسطس"
            arabicMonths.forEachIndexed { index, monthName ->
                if (normalized.contains(monthName)) {
                    val mCal = Calendar.getInstance().apply {
                        set(Calendar.MONTH, index)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val endMCal = Calendar.getInstance().apply {
                        set(Calendar.MONTH, index)
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    }
                    startDate = getStartOfDay(mCal.timeInMillis)
                    endDate = getEndOfDay(endMCal.timeInMillis)
                    explanations.add("شهر $monthName")
                }
            }
        }

        // Check for specific date format (e.g. 2026-09-27 or 27/09/2026)
        val datePattern = Pattern.compile("(\\d{4}-\\d{1,2}-\\d{1,2}|\\d{1,2}/\\d{1,2}/\\d{4})")
        val dateMatcher = datePattern.matcher(normalized)
        if (dateMatcher.find()) {
            val dateStr = dateMatcher.group(1)
            if (!dateStr.isNullOrBlank()) {
                val parsedDate = try {
                    if (dateStr.contains("-")) dateFormatYmd.parse(dateStr)
                    else dateFormatDmy.parse(dateStr)
                } catch (e: Exception) {
                    null
                }
                if (parsedDate != null) {
                    startDate = getStartOfDay(parsedDate.time)
                    endDate = getEndOfDay(parsedDate.time)
                    explanations.add("بتاريخ $dateStr")
                }
            }
        }

        // 3. Category / Type keywords
        val lower = normalized.lowercase()
        if (lower.contains("قبض") || lower.contains("مقبوضات") || lower.contains("استلام") || lower.contains("تحصيل")) {
            targetTypes = (targetTypes ?: mutableSetOf()).apply {
                add(TransactionType.CUSTOMER_RECEIPT)
                add(TransactionType.INCOME)
            }
            explanations.add("مقبوضات")
        }
        if (lower.contains("صرف") || lower.contains("مدفوعات") || lower.contains("سداد")) {
            targetTypes = (targetTypes ?: mutableSetOf()).apply {
                add(TransactionType.SUPPLIER_PAYMENT)
                add(TransactionType.EXPENSE)
            }
            explanations.add("مدفوعات")
        }
        if (lower.contains("مصروف") || lower.contains("مصاريف")) {
            targetTypes = (targetTypes ?: mutableSetOf()).apply {
                add(TransactionType.EXPENSE)
            }
            explanations.add("مصروفات")
        }
        if (lower.contains("إيراد") || lower.contains("دخل")) {
            targetTypes = (targetTypes ?: mutableSetOf()).apply {
                add(TransactionType.INCOME)
            }
            explanations.add("إيرادات")
        }
        if (lower.contains("دين") || lower.contains("آجل") || lower.contains("سلف")) {
            targetTypes = (targetTypes ?: mutableSetOf()).apply {
                add(TransactionType.CUSTOMER_NEW_DEBIT)
                add(TransactionType.SUPPLIER_NEW_CREDIT)
            }
            explanations.add("ديون")
        }
        if (lower.contains("تحويل")) {
            targetTypes = (targetTypes ?: mutableSetOf()).apply {
                add(TransactionType.TRANSFER)
            }
            explanations.add("تحويلات")
        }

        // 4. Exact amount if simply a number with no comparator
        if (minAmount == null && maxAmount == null) {
            val singleNumberPattern = Pattern.compile("(?:^|\\s)(\\d+(?:\\.\\d+)?)(?:\\s|$)")
            val numMatcher = singleNumberPattern.matcher(normalized)
            if (numMatcher.find()) {
                val num = numMatcher.group(1)?.toDoubleOrNull()
                // Only consider exact amount if it doesn't look like a year
                if (num != null && (num < 1900 || num > 2100)) {
                    exactAmount = num
                    explanations.add("المبلغ: $exactAmount ر.س")
                }
            }
        }

        // 5. Match with Parties (Customers / Suppliers)
        parties.forEach { party ->
            if (normalized.contains(party.name, ignoreCase = true) ||
                (party.phone.isNotBlank() && normalized.contains(party.phone))
            ) {
                matchedPartyIds.add(party.id)
                explanations.add(party.name)
            }
        }

        // 6. Match with Cash Boxes
        cashBoxes.forEach { box ->
            if (normalized.contains(box.name, ignoreCase = true)) {
                matchedCashBoxIds.add(box.id)
                explanations.add(box.name)
            }
        }

        // 7. General remaining keywords (words not absorbed by special tokens)
        val stopWords = setOf(
            "في", "من", "إلى", "على", "عن", "مع", "أو", "و", "اليوم", "أمس", "الامس",
            "هذا", "الأسبوع", "الاسبوع", "الشهر", "الماضي", "ريال", "ر.س", "دين",
            "قبض", "صرف", "مصروف", "إيراد", "تحويل", "أكبر", "أقل", "أصغر", "أكثر",
            "بين", "سداد", "دفعة"
        )
        val tokens = normalized.split(Regex("[\\s,،]+")).filter { token ->
            token.length > 1 && !stopWords.contains(token) && !token.matches(Regex("\\d+(\\.\\d+)?"))
        }
        criteriaKeywords.addAll(tokens)

        return SearchCriteria(
            rawQuery = query,
            keywords = criteriaKeywords,
            targetTypes = targetTypes,
            minAmount = minAmount,
            maxAmount = maxAmount,
            exactAmount = exactAmount,
            startDate = startDate,
            endDate = endDate,
            matchedPartyIds = matchedPartyIds,
            matchedCashBoxIds = matchedCashBoxIds,
            description = explanations.joinToString(" • ")
        )
    }

    fun executeSearch(
        query: String,
        transactions: List<TransactionRecord>,
        parties: List<Party>,
        cashBoxes: List<CashBox>
    ): SearchSummary {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return calculateSummary(transactions, "جميع العمليات المسجلة")
        }

        val criteria = parseSearchQuery(trimmed, parties, cashBoxes)
        val partyMap = parties.associateBy { it.id }
        val boxMap = cashBoxes.associateBy { it.id }

        val filtered = transactions.filter { tx ->
            // Check Date Range
            if (criteria.startDate != null && tx.timestamp < criteria.startDate) return@filter false
            if (criteria.endDate != null && tx.timestamp > criteria.endDate) return@filter false

            // Check Transaction Type
            if (criteria.targetTypes != null && !criteria.targetTypes.contains(tx.type)) {
                return@filter false
            }

            // Check Amount comparisons
            if (criteria.minAmount != null && tx.amount < criteria.minAmount) return@filter false
            if (criteria.maxAmount != null && tx.amount > criteria.maxAmount) return@filter false
            if (criteria.exactAmount != null) {
                // allow small difference or exact match
                val isExact = Math.abs(tx.amount - criteria.exactAmount) < 0.01
                val matchesPartially = tx.amount.toString().contains(criteria.exactAmount.toString())
                if (!isExact && !matchesPartially) return@filter false
            }

            // Check Party match if party was specifically identified
            if (criteria.matchedPartyIds.isNotEmpty()) {
                val matchesParty = tx.partyId != null && criteria.matchedPartyIds.contains(tx.partyId)
                if (!matchesParty) return@filter false
            }

            // Check Cash Box match if box was specifically identified
            if (criteria.matchedCashBoxIds.isNotEmpty()) {
                val matchesBox = (tx.cashBoxId != null && criteria.matchedCashBoxIds.contains(tx.cashBoxId)) ||
                        (tx.targetCashBoxId != null && criteria.matchedCashBoxIds.contains(tx.targetCashBoxId))
                if (!matchesBox) return@filter false
            }

            // Check Keywords against description, party name, cashbox name
            if (criteria.keywords.isNotEmpty()) {
                val partyName = tx.partyId?.let { partyMap[it]?.name } ?: ""
                val boxName = tx.cashBoxId?.let { boxMap[it]?.name } ?: ""
                val textToSearch = "${tx.description} $partyName $boxName ${tx.type.titleAr}".lowercase()

                val matchesAllKeywords = criteria.keywords.all { kw ->
                    textToSearch.contains(kw.lowercase())
                }
                if (!matchesAllKeywords) return@filter false
            }

            true
        }

        val desc = if (criteria.description.isNotBlank()) criteria.description else "نتائج البحث عن: \"$query\""
        return calculateSummary(filtered, desc)
    }

    private fun calculateSummary(list: List<TransactionRecord>, desc: String): SearchSummary {
        var total = 0.0
        var inflow = 0.0
        var outflow = 0.0

        list.forEach { tx ->
            total += tx.amount
            when (tx.type) {
                TransactionType.CUSTOMER_RECEIPT, TransactionType.INCOME -> inflow += tx.amount
                TransactionType.SUPPLIER_PAYMENT, TransactionType.EXPENSE -> outflow += tx.amount
                else -> {}
            }
        }

        return SearchSummary(
            transactions = list,
            count = list.size,
            totalAmount = total,
            totalInflow = inflow,
            totalOutflow = outflow,
            netBalance = inflow - outflow,
            matchedCriteriaDescription = desc
        )
    }

    private fun normalizeArabicNumbers(text: String): String {
        return text
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
    }

    private fun getStartOfDay(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getEndOfDay(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}
