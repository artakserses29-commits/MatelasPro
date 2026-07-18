package com.matelaspro.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.matelaspro.app.data.entity.AluNoteExpense

@Dao
interface AluNoteExpenseDao {
    @Query("SELECT * FROM alu_note_expenses WHERE noteId = :noteId ORDER BY createdAt DESC")
    fun getByNoteId(noteId: Long): LiveData<List<AluNoteExpense>>

    @Query("SELECT * FROM alu_note_expenses WHERE id = :id")
    suspend fun getById(id: Long): AluNoteExpense?

    @Insert
    suspend fun insert(expense: AluNoteExpense): Long

    @Update
    suspend fun update(expense: AluNoteExpense)

    @Delete
    suspend fun delete(expense: AluNoteExpense)
}
