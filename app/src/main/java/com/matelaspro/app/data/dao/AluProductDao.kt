package com.matelaspro.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.matelaspro.app.data.entity.AluProduct

@Dao
interface AluProductDao {
    @Query("SELECT * FROM alu_products ORDER BY name ASC")
    fun getAllProducts(): LiveData<List<AluProduct>>

    @Query("SELECT * FROM alu_products WHERE id = :id")
    suspend fun getById(id: Long): AluProduct?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: AluProduct): Long

    @Update
    suspend fun update(product: AluProduct)

    @Delete
    suspend fun delete(product: AluProduct)

    @Query("DELETE FROM alu_products WHERE id = :id")
    suspend fun deleteById(id: Long)
}
