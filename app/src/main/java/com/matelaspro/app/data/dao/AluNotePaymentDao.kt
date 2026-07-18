package com.matelaspro.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.matelaspro.app.data.entity.AluNotePayment

@Dao
interface AluNotePaymentDao {
    @Query("SELECT * FROM alu_note_payments WHERE noteId = :noteId ORDER BY createdAt DESC")
    fun getByNoteId(noteId: Long): LiveData<List<AluNotePayment>>

    @Insert
    suspend fun insert(payment: AluNotePayment): Long
}
