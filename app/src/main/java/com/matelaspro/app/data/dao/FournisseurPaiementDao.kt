package com.matelaspro.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.matelaspro.app.data.entity.FournisseurPaiement

data class FournisseurPaymentTotal(
    val fournisseur: String,
    val totalPaye: Double
)

@Dao
interface FournisseurPaiementDao {
    @Query("SELECT * FROM fournisseur_paiements ORDER BY createdAt DESC")
    fun getAll(): LiveData<List<FournisseurPaiement>>

    @Query("SELECT * FROM fournisseur_paiements ORDER BY createdAt DESC")
    suspend fun getAllList(): List<FournisseurPaiement>

    @Query("SELECT * FROM fournisseur_paiements WHERE fournisseurId = :fournisseurId ORDER BY createdAt DESC")
    fun getByFournisseurId(fournisseurId: Long): LiveData<List<FournisseurPaiement>>

    @Query("SELECT COALESCE(SUM(montant), 0) FROM fournisseur_paiements WHERE fournisseurId = :fournisseurId")
    suspend fun getTotalPayeByFournisseurId(fournisseurId: Long): Double

    @Insert
    suspend fun insert(paiement: FournisseurPaiement): Long

    @Query("SELECT f.name AS fournisseur, COALESCE(SUM(fp.montant), 0) AS totalPaye FROM fournisseur_paiements fp LEFT JOIN fournisseurs f ON fp.fournisseurId = f.id GROUP BY fp.fournisseurId")
    suspend fun getPaymentTotalsByFournisseur(): List<FournisseurPaymentTotal>
}
