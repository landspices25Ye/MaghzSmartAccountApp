package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.JournalEntry
import com.example.data.model.JournalLineItem
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC, id DESC")
    fun getAllJournalEntriesFlow(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_line_items WHERE journalEntryId = :entryId ORDER BY id ASC")
    fun getLinesForEntryFlow(entryId: Long): Flow<List<JournalLineItem>>

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC, id DESC")
    suspend fun getAllJournalEntriesList(): List<JournalEntry>

    @Query("SELECT * FROM journal_line_items WHERE journalEntryId = :entryId ORDER BY id ASC")
    suspend fun getLinesForEntry(entryId: Long): List<JournalLineItem>

    @Query("SELECT * FROM journal_line_items ORDER BY id ASC")
    suspend fun getAllLineItemsList(): List<JournalLineItem>

    @Query("SELECT * FROM journal_entries WHERE sourceTransactionId = :transactionId LIMIT 1")
    suspend fun getEntryBySourceTransaction(transactionId: Long): JournalEntry?

    @Query("SELECT * FROM journal_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): JournalEntry?

    @Query("SELECT COUNT(*) FROM journal_entries")
    suspend fun getEntryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(items: List<JournalLineItem>)

    @Delete
    suspend fun deleteEntry(entry: JournalEntry)

    @Query("DELETE FROM journal_line_items WHERE journalEntryId = :entryId")
    suspend fun deleteLinesForEntry(entryId: Long)

    @Transaction
    suspend fun deleteEntryWithLines(entryId: Long) {
        deleteLinesForEntry(entryId)
        deleteEntryById(entryId)
    }

    @Query("DELETE FROM journal_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: Long)

    @Query("DELETE FROM journal_entries WHERE sourceTransactionId = :txId")
    suspend fun deleteEntryBySourceTransactionId(txId: Long)

    @Query("DELETE FROM journal_line_items")
    suspend fun deleteAllLines()

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAllEntries()
}
