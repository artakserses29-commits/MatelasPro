package com.matelaspro.app.data.repository

import androidx.lifecycle.LiveData
import com.matelaspro.app.data.dao.ExpenseDao
import com.matelaspro.app.data.entity.Expense

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    val allExpenses: LiveData<List<Expense>> = expenseDao.getAllExpenses()
    suspend fun getAllExpensesList(): List<Expense> = expenseDao.getAllExpensesList()

    fun getExpensesByUser(userId: Long): LiveData<List<Expense>> = expenseDao.getExpensesByUser(userId)
    suspend fun getExpensesByUserList(userId: Long): List<Expense> = expenseDao.getExpensesByUserList(userId)

    suspend fun insert(expense: Expense): Long = expenseDao.insert(expense)

    suspend fun deleteById(id: Long) = expenseDao.deleteById(id)

    suspend fun getUserExpensesInRange(userId: Long, start: Long, end: Long): Double =
        expenseDao.getUserExpensesInRange(userId, start, end)

    suspend fun getAllExpensesInRange(start: Long, end: Long): Double =
        expenseDao.getAllExpensesInRange(start, end)

    suspend fun getUserExpensesInMonth(userId: Long, startOfMonth: Long, startOfNextMonth: Long): Double =
        expenseDao.getUserExpensesInMonth(userId, startOfMonth, startOfNextMonth)

    suspend fun getAllExpensesInMonth(startOfMonth: Long, startOfNextMonth: Long): Double =
        expenseDao.getAllExpensesInMonth(startOfMonth, startOfNextMonth)
}
