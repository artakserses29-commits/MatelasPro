package com.matelaspro.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.matelaspro.app.data.entity.AluNote

@Dao
interface AluNoteDao {
    @Query("SELECT * FROM alu_notes ORDER BY createdAt DESC")
    fun getAll(): LiveData<List<AluNote>>

    @Query("SELECT * FROM alu_notes WHERE id = :id")
    suspend fun getById(id: Long): AluNote?

    @Insert
    suspend fun insert(note: AluNote): Long

    @Update
    suspend fun update(note: AluNote)

    @Delete
    suspend fun delete(note: AluNote)
}
