package com.matelaspro.app.data.repository

import androidx.lifecycle.LiveData
import com.matelaspro.app.data.dao.AluProductDao
import com.matelaspro.app.data.entity.AluProduct

class AluProductRepository(private val dao: AluProductDao) {
    val allProducts: LiveData<List<AluProduct>> = dao.getAllProducts()

    suspend fun getById(id: Long): AluProduct? = dao.getById(id)

    suspend fun insert(product: AluProduct): Long = dao.insert(product)

    suspend fun update(product: AluProduct) = dao.update(product)

    suspend fun delete(product: AluProduct) = dao.delete(product)

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
