package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Party
import com.example.data.model.PartyType
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyDao {
    @Query("SELECT * FROM parties ORDER BY name ASC")
    fun getAllPartiesFlow(): Flow<List<Party>>

    @Query("SELECT * FROM parties ORDER BY name ASC")
    suspend fun getAllParties(): List<Party>

    @Query("SELECT * FROM parties WHERE type = :type ORDER BY name ASC")
    fun getPartiesByTypeFlow(type: PartyType): Flow<List<Party>>

    @Query("SELECT * FROM parties WHERE id = :id LIMIT 1")
    suspend fun getPartyById(id: Long): Party?

    @Query("SELECT * FROM parties WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getPartyByName(name: String): Party?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(party: Party): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<Party>)

    @Update
    suspend fun update(party: Party)

    @Delete
    suspend fun delete(party: Party)

    @Query("DELETE FROM parties")
    suspend fun deleteAll()

    @Query("UPDATE parties SET balance = balance + :delta WHERE id = :id")
    suspend fun updateBalance(id: Long, delta: Double)

    @Query("UPDATE parties SET balance = :newBalance WHERE id = :id")
    suspend fun setBalance(id: Long, newBalance: Double)
}
