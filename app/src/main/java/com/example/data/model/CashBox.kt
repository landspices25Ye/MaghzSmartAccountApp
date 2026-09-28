package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cash_boxes")
data class CashBox(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val balance: Double = 0.0,
    val isDefault: Boolean = false,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
