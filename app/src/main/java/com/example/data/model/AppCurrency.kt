package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currencies")
data class AppCurrency(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,         // e.g. "SAR", "USD", "YER", "EGP"
    val name: String,         // e.g. "ريال سعودي", "دولار أمريكي", "ريال يمني"
    val symbol: String,       // e.g. "ر.س", "$", "ر.ي", "ج.م"
    val isDefault: Boolean = false,
    val exchangeRate: Double = 1.0,
    val createdAt: Long = System.currentTimeMillis()
)
