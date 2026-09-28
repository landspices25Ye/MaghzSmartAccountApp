package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseCategoryDao {
    @Query("SELECT * FROM expense_categories ORDER BY isDefault DESC, id ASC")
    fun getAllCategoriesFlow(): Flow<List<ExpenseCategory>>

    @Query("SELECT * FROM expense_categories ORDER BY isDefault DESC, id ASC")
    suspend fun getAllCategories(): List<ExpenseCategory>

    @Query("SELECT * FROM expense_categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): ExpenseCategory?

    @Query("SELECT * FROM expense_categories WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getCategoryByName(name: String): ExpenseCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: ExpenseCategory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<ExpenseCategory>)

    @Update
    suspend fun update(category: ExpenseCategory)

    @Delete
    suspend fun delete(category: ExpenseCategory)

    @Query("DELETE FROM expense_categories")
    suspend fun deleteAll()
}
