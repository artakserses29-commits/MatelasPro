package com.matelaspro.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.matelaspro.app.data.entity.Expense

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun getAllExpenses(): LiveData<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    suspend fun getAllExpensesList(): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY createdAt DESC")
    fun getExpensesByUser(userId: Long): LiveData<List<Expense>>

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getExpensesByUserList(userId: Long): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE userId = :userId AND createdAt >= :start AND createdAt <= :end")
    suspend fun getUserExpensesInRange(userId: Long, start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE createdAt >= :start AND createdAt <= :end")
    suspend fun getAllExpensesInRange(start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE userId = :userId AND createdAt >= :startOfMonth AND createdAt < :startOfNextMonth")
    suspend fun getUserExpensesInMonth(userId: Long, startOfMonth: Long, startOfNextMonth: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE createdAt >= :startOfMonth AND createdAt < :startOfNextMonth")
    suspend fun getAllExpensesInMonth(startOfMonth: Long, startOfNextMonth: Long): Double
}
