package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CashBox
import kotlinx.coroutines.flow.Flow

@Dao
interface CashBoxDao {
    @Query("SELECT * FROM cash_boxes ORDER BY isDefault DESC, id ASC")
    fun getAllCashBoxesFlow(): Flow<List<CashBox>>

    @Query("SELECT * FROM cash_boxes ORDER BY isDefault DESC, id ASC")
    suspend fun getAllCashBoxes(): List<CashBox>

    @Query("SELECT * FROM cash_boxes WHERE id = :id LIMIT 1")
    suspend fun getCashBoxById(id: Long): CashBox?

    @Query("SELECT * FROM cash_boxes WHERE name = :name LIMIT 1")
    suspend fun getCashBoxByName(name: String): CashBox?

    @Query("SELECT * FROM cash_boxes WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultCashBox(): CashBox?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cashBox: CashBox): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<CashBox>)

    @Update
    suspend fun update(cashBox: CashBox)

    @Delete
    suspend fun delete(cashBox: CashBox)

    @Query("DELETE FROM cash_boxes")
    suspend fun deleteAll()

    @Query("UPDATE cash_boxes SET isDefault = 0")
    suspend fun clearDefaultFlags()

    @Query("UPDATE cash_boxes SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: Long)

    @Query("UPDATE cash_boxes SET balance = balance + :delta WHERE id = :id")
    suspend fun updateBalance(id: Long, delta: Double)

    @Query("UPDATE cash_boxes SET balance = :newBalance WHERE id = :id")
    suspend fun setBalance(id: Long, newBalance: Double)
}
