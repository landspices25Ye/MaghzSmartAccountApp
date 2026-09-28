package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_categories")
data class ExpenseCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,         // e.g. "إيجار", "فواتير ومرافق", "رواتب وأجور", "نقل ومواصلات وبنزين"
    val iconTag: String = "receipt", // tag for icon representation
    val note: String = "",
    val isDefault: Boolean = false,
    val monthlyBudget: Double = 0.0, // 0.0 = unlimited
    val alertThresholdPercent: Int = 80, // % threshold for warning (e.g. 80%)
    val createdAt: Long = System.currentTimeMillis()
)
