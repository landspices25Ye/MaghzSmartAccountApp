package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TransactionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactionsFlow(limit: Int): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions WHERE partyId = :partyId ORDER BY timestamp DESC")
    fun getTransactionsByPartyFlow(partyId: Long): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions WHERE cashBoxId = :cashBoxId OR targetCashBoxId = :cashBoxId ORDER BY timestamp DESC")
    fun getTransactionsByCashBoxFlow(cashBoxId: Long): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getTransactionsBetween(startTime: Long, endTime: Long): List<TransactionRecord>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<TransactionRecord>

    @Query("SELECT * FROM transactions WHERE partyId = :partyId ORDER BY timestamp ASC")
    suspend fun getTransactionsForParty(partyId: Long): List<TransactionRecord>

    @Query("SELECT * FROM transactions WHERE cashBoxId = :cashBoxId OR targetCashBoxId = :cashBoxId ORDER BY timestamp ASC")
    suspend fun getTransactionsForCashBox(cashBoxId: Long): List<TransactionRecord>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionRecord?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<TransactionRecord>)

    @Delete
    suspend fun delete(transaction: TransactionRecord)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
