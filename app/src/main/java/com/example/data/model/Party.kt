package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PartyType {
    CUSTOMER, // عميل / مدين (ما لي عنده)
    SUPPLIER  // مورد / دائن (ما علي له)
}

@Entity(tableName = "parties")
data class Party(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: PartyType,
    val phone: String = "",
    // Balance:
    // For CUSTOMER: balance > 0 means the customer owes me (ما لي عنده).
    // For SUPPLIER: balance > 0 means I owe the supplier (ما علي له).
    val balance: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
