package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MemoryCategory(val titleAr: String, val iconTag: String) {
    PARTY_NOTE("أطراف وعملاء", "person"),
    BUSINESS_INFO("طبيعة النشاط والمحل", "store"),
    ACCOUNTING_RULE("قواعد وتوجيهات محاسبية", "rule"),
    USER_PREFERENCE("تفضيلات وأولويات", "tune"),
    GENERAL("معلومات عامة", "lightbulb")
}

@Entity(tableName = "ai_memory_facts")
data class AiMemoryFact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: MemoryCategory = MemoryCategory.GENERAL,
    val key: String, // عنوان أو مفتاح المعلومة (مثال: "طبيعة تعامل أحمد", "اسم النشاط")
    val fact: String, // محتوى المعلومة المحفوظة في الذاكرة
    val timestamp: Long = System.currentTimeMillis(),
    val isAutoLearned: Boolean = true, // هل تعلمها الذكاء الاصطناعي تلقائياً أم أضافها المستخدم يدوياً
    val isActive: Boolean = true // مفعلة لتضمينها في سياق المحاسب
)
