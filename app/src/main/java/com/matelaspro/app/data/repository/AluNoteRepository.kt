package com.matelaspro.app.data.repository

import androidx.lifecycle.LiveData
import com.matelaspro.app.data.dao.AluNoteDao
import com.matelaspro.app.data.dao.AluNoteExpenseDao
import com.matelaspro.app.data.dao.AluNotePaymentDao
import com.matelaspro.app.data.entity.AluNote
import com.matelaspro.app.data.entity.AluNoteExpense
import com.matelaspro.app.data.entity.AluNotePayment

class AluNoteRepository(
    private val noteDao: AluNoteDao,
    private val paymentDao: AluNotePaymentDao,
    private val expenseDao: AluNoteExpenseDao
) {
    val allNotes: LiveData<List<AluNote>> = noteDao.getAll()

    suspend fun getNoteById(id: Long): AluNote? = noteDao.getById(id)
    suspend fun insertNote(note: AluNote): Long = noteDao.insert(note)
    suspend fun updateNote(note: AluNote) = noteDao.update(note)
    suspend fun deleteNote(note: AluNote) = noteDao.delete(note)

    fun getPaymentsByNoteId(noteId: Long): LiveData<List<AluNotePayment>> = paymentDao.getByNoteId(noteId)
    suspend fun insertPayment(payment: AluNotePayment): Long = paymentDao.insert(payment)

    fun getExpensesByNoteId(noteId: Long): LiveData<List<AluNoteExpense>> = expenseDao.getByNoteId(noteId)
    suspend fun getExpenseById(id: Long): AluNoteExpense? = expenseDao.getById(id)
    suspend fun insertExpense(expense: AluNoteExpense): Long = expenseDao.insert(expense)
    suspend fun updateExpense(expense: AluNoteExpense) = expenseDao.update(expense)
    suspend fun deleteExpense(expense: AluNoteExpense) = expenseDao.delete(expense)
}
