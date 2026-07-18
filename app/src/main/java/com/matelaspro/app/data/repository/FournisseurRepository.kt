package com.matelaspro.app.data.repository

import com.matelaspro.app.data.dao.FournisseurDao
import com.matelaspro.app.data.dao.FournisseurPaiementDao
import com.matelaspro.app.data.entity.Fournisseur
import com.matelaspro.app.data.entity.FournisseurPaiement

class FournisseurRepository(
    private val dao: FournisseurDao,
    private val paiementDao: FournisseurPaiementDao
) {
    val allFournisseurs: androidx.lifecycle.LiveData<List<Fournisseur>> = dao.getAll()

    suspend fun getById(id: Long): Fournisseur? = dao.getById(id)
    suspend fun getByName(name: String): Fournisseur? = dao.getByName(name)
    suspend fun insert(name: String): Long = dao.insert(Fournisseur(name = name))
    suspend fun update(f: Fournisseur) = dao.update(f)
    suspend fun delete(f: Fournisseur) = dao.delete(f)

    val allPaiements: androidx.lifecycle.LiveData<List<FournisseurPaiement>> = paiementDao.getAll()

    fun getPaiementsByFournisseurId(id: Long): androidx.lifecycle.LiveData<List<FournisseurPaiement>> =
        paiementDao.getByFournisseurId(id)

    suspend fun getTotalPayeByFournisseurId(id: Long): Double =
        paiementDao.getTotalPayeByFournisseurId(id)

    suspend fun insertPaiement(p: FournisseurPaiement): Long = paiementDao.insert(p)

    suspend fun getAllPaiementsList(): List<FournisseurPaiement> = paiementDao.getAllList()
}
