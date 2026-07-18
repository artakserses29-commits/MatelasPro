package com.matelaspro.app.data.dao

import androidx.room.*
import com.matelaspro.app.data.entity.CreditEntry

data class FournisseurCreditTotal(
    val fournisseur: String,
    val totalCredit: Double
)

@Dao
interface CreditEntryDao {
    @Query("SELECT * FROM credit_entries ORDER BY createdAt DESC")
    suspend fun getAll(): List<CreditEntry>

    @Query("SELECT * FROM credit_entries WHERE fournisseur = :fournisseur ORDER BY createdAt DESC")
    suspend fun getByFournisseur(fournisseur: String): List<CreditEntry>

    @Query("SELECT * FROM credit_entries WHERE quantity > 0 AND isOverridden = 0 AND fournisseur = :fournisseur ORDER BY createdAt DESC")
    suspend fun getActiveByFournisseur(fournisseur: String): List<CreditEntry>

    @Query("SELECT * FROM credit_entries WHERE createdAt >= :startOfDay AND createdAt < :endOfDay AND fournisseur = :fournisseur ORDER BY createdAt ASC")
    suspend fun getByDateRangeAndFournisseur(startOfDay: Long, endOfDay: Long, fournisseur: String): List<CreditEntry>

    @Query("SELECT * FROM credit_entries WHERE quantity > 0 AND isOverridden = 0 AND createdAt >= :startOfDay AND createdAt < :endOfDay AND fournisseur = :fournisseur ORDER BY createdAt ASC")
    suspend fun getActiveByDateRangeAndFournisseur(startOfDay: Long, endOfDay: Long, fournisseur: String): List<CreditEntry>

    @Insert
    suspend fun insert(entry: CreditEntry): Long

    @Update
    suspend fun update(entry: CreditEntry)

    @Query("DELETE FROM credit_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT fournisseur, COALESCE(SUM(quantity * sellingPrice), 0) AS totalCredit FROM credit_entries WHERE quantity > 0 AND isOverridden = 0 GROUP BY fournisseur")
    suspend fun getCreditTotalsByFournisseur(): List<FournisseurCreditTotal>
}
