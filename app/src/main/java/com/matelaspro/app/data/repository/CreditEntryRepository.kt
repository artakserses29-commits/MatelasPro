package com.matelaspro.app.data.repository

import com.matelaspro.app.data.dao.CreditEntryDao
import com.matelaspro.app.data.entity.CreditEntry

class CreditEntryRepository(private val dao: CreditEntryDao) {
    suspend fun getAll(): List<CreditEntry> = dao.getAll()
    suspend fun getByFournisseur(fournisseur: String): List<CreditEntry> = dao.getByFournisseur(fournisseur)
    suspend fun getActiveByFournisseur(fournisseur: String): List<CreditEntry> = dao.getActiveByFournisseur(fournisseur)
    suspend fun getByDateRangeAndFournisseur(startOfDay: Long, endOfDay: Long, fournisseur: String): List<CreditEntry> =
        dao.getByDateRangeAndFournisseur(startOfDay, endOfDay, fournisseur)
    suspend fun getActiveByDateRangeAndFournisseur(startOfDay: Long, endOfDay: Long, fournisseur: String): List<CreditEntry> =
        dao.getActiveByDateRangeAndFournisseur(startOfDay, endOfDay, fournisseur)
    suspend fun insert(entry: CreditEntry): Long = dao.insert(entry)
    suspend fun update(entry: CreditEntry) = dao.update(entry)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
