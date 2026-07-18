package com.matelaspro.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.matelaspro.app.data.entity.Fournisseur

@Dao
interface FournisseurDao {
    @Query("SELECT * FROM fournisseurs ORDER BY name ASC")
    fun getAll(): LiveData<List<Fournisseur>>

    @Query("SELECT * FROM fournisseurs WHERE id = :id")
    suspend fun getById(id: Long): Fournisseur?

    @Query("SELECT * FROM fournisseurs WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Fournisseur?

    @Insert
    suspend fun insert(fournisseur: Fournisseur): Long

    @Update
    suspend fun update(fournisseur: Fournisseur)

    @Delete
    suspend fun delete(fournisseur: Fournisseur)
}
