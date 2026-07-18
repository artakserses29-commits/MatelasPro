package com.matelaspro.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.matelaspro.app.data.entity.AluDevis

@Dao
interface AluDevisDao {
    @Query("SELECT * FROM alu_devis ORDER BY createdAt DESC")
    fun getAll(): LiveData<List<AluDevis>>

    @Query("SELECT * FROM alu_devis WHERE id = :id")
    suspend fun getById(id: Long): AluDevis?

    @Insert
    suspend fun insert(devis: AluDevis): Long

    @Update
    suspend fun update(devis: AluDevis)

    @Delete
    suspend fun delete(devis: AluDevis)
}
