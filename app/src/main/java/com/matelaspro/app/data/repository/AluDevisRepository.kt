package com.matelaspro.app.data.repository

import androidx.lifecycle.LiveData
import com.matelaspro.app.data.dao.AluDevisDao
import com.matelaspro.app.data.entity.AluDevis

class AluDevisRepository(private val dao: AluDevisDao) {
    val allDevis: LiveData<List<AluDevis>> = dao.getAll()

    suspend fun getById(id: Long): AluDevis? = dao.getById(id)

    suspend fun insert(devis: AluDevis): Long = dao.insert(devis)

    suspend fun update(devis: AluDevis) = dao.update(devis)

    suspend fun delete(devis: AluDevis) = dao.delete(devis)
}
