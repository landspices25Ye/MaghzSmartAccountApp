package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppCurrency
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM currencies ORDER BY isDefault DESC, id ASC")
    fun getAllCurrenciesFlow(): Flow<List<AppCurrency>>

    @Query("SELECT * FROM currencies ORDER BY isDefault DESC, id ASC")
    suspend fun getAllCurrencies(): List<AppCurrency>

    @Query("SELECT * FROM currencies WHERE id = :id LIMIT 1")
    suspend fun getCurrencyById(id: Long): AppCurrency?

    @Query("SELECT * FROM currencies WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultCurrency(): AppCurrency?

    @Query("SELECT * FROM currencies WHERE LOWER(code) = LOWER(:code) OR LOWER(symbol) = LOWER(:code) LIMIT 1")
    suspend fun getCurrencyByCode(code: String): AppCurrency?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(currency: AppCurrency): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<AppCurrency>)

    @Update
    suspend fun update(currency: AppCurrency)

    @Delete
    suspend fun delete(currency: AppCurrency)

    @Query("DELETE FROM currencies")
    suspend fun deleteAll()

    @Query("UPDATE currencies SET isDefault = 0")
    suspend fun clearDefaultFlags()

    @Query("UPDATE currencies SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: Long)
}
