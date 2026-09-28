package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ReportTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportTemplateDao {
    @Query("SELECT * FROM report_templates ORDER BY createdAt DESC")
    fun getAllTemplatesFlow(): Flow<List<ReportTemplate>>

    @Query("SELECT * FROM report_templates ORDER BY createdAt DESC")
    suspend fun getAllTemplates(): List<ReportTemplate>

    @Query("SELECT * FROM report_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: Long): ReportTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: ReportTemplate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<ReportTemplate>)

    @Delete
    suspend fun delete(template: ReportTemplate)

    @Query("DELETE FROM report_templates")
    suspend fun deleteAll()
}
