package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AiChatMessageEntity
import com.example.data.model.AiMemoryFact
import com.example.data.model.AppCurrency
import com.example.data.model.CashBox
import com.example.data.model.ExpenseCategory
import com.example.data.model.Party
import com.example.data.model.ReportTemplate
import com.example.data.model.TransactionRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CashBox::class,
        Party::class,
        TransactionRecord::class,
        ReportTemplate::class,
        AppCurrency::class,
        ExpenseCategory::class,
        AiMemoryFact::class,
        AiChatMessageEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cashBoxDao(): CashBoxDao
    abstract fun partyDao(): PartyDao
    abstract fun transactionDao(): TransactionDao
    abstract fun reportTemplateDao(): ReportTemplateDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao
    abstract fun aiMemoryDao(): AiMemoryDao
    abstract fun aiChatMessageDao(): AiChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_accountant.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed default cash box, currencies, expense categories, and report templates
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                database.cashBoxDao().insert(
                                    CashBox(
                                        name = "الصندوق الرئيسي",
                                        balance = 0.0,
                                        isDefault = true,
                                        note = "الصندوق النقدي الأساسي"
                                    )
                                )

                                // Seed Currencies
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
                                defaultCurrencies.forEach { database.currencyDao().insert(it) }

                                // Seed Expense Categories
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
                                defaultCategories.forEach { database.expenseCategoryDao().insert(it) }

                                // Seed common report templates
                                database.reportTemplateDao().insert(
                                    ReportTemplate(
                                        name = "تقرير ديون العملاء (ما لي)",
                                        datePreset = "ALL",
                                        accountScope = "CUSTOMERS_ALL",
                                        transactionTypes = "CUSTOMER_RECEIPT,CUSTOMER_NEW_DEBIT"
                                    )
                                )
                                database.reportTemplateDao().insert(
                                    ReportTemplate(
                                        name = "تقرير التزامات الموردين (ما علي)",
                                        datePreset = "ALL",
                                        accountScope = "SUPPLIERS_ALL",
                                        transactionTypes = "SUPPLIER_PAYMENT,SUPPLIER_NEW_CREDIT"
                                    )
                                )
                                database.reportTemplateDao().insert(
                                    ReportTemplate(
                                        name = "المصروفات الشهرية",
                                        datePreset = "THIS_MONTH",
                                        accountScope = "ALL",
                                        transactionTypes = "EXPENSE"
                                    )
                                )
                                database.reportTemplateDao().insert(
                                    ReportTemplate(
                                        name = "حركة المقبوضات النقدية الأسبوعية",
                                        datePreset = "THIS_WEEK",
                                        accountScope = "CASH_ALL",
                                        transactionTypes = "CUSTOMER_RECEIPT,INCOME"
                                    )
                                )
                            }
                        }
                    }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
