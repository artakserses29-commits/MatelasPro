package com.matelaspro.app.data.repository

import androidx.lifecycle.LiveData
import com.matelaspro.app.data.dao.ProductDao
import com.matelaspro.app.data.entity.Product

class ProductRepository(private val productDao: ProductDao) {
    val allProducts: LiveData<List<Product>> = productDao.getAllProducts()
    val allCategories: LiveData<List<String>> = productDao.getAllCategories()

    fun getProductsByCategory(category: String): LiveData<List<Product>> = productDao.getProductsByCategory(category)

    fun searchProducts(query: String): LiveData<List<Product>> = productDao.searchProducts(query)

    suspend fun getAllProductsSuspend(): List<Product> = productDao.getAllProductsSuspend()

    suspend fun getProductById(id: Long): Product? = productDao.getProductById(id)

    suspend fun insert(product: Product): Long = productDao.insert(product)

    suspend fun update(product: Product) = productDao.update(product)

    suspend fun delete(product: Product) = productDao.delete(product)

    suspend fun deleteById(id: Long) = productDao.deleteById(id)

    suspend fun getProductsByDateRange(startOfDay: Long, endOfDay: Long): List<Product> =
        productDao.getProductsByDateRange(startOfDay, endOfDay)

    suspend fun getProductsByDateRangeAndFournisseur(startOfDay: Long, endOfDay: Long, fournisseur: String): List<Product> =
        productDao.getProductsByDateRangeAndFournisseur(startOfDay, endOfDay, fournisseur)

    suspend fun getProductByNameAndFournisseur(name: String, fournisseur: String, category: String): Product? =
        productDao.getProductByNameAndFournisseur(name, fournisseur, category)

    val totalStockValue: LiveData<Double> = productDao.getTotalStockValue()
    fun getLowStockProducts(threshold: Int = 5): LiveData<List<Product>> = productDao.getLowStockProducts(threshold)

    suspend fun getProductsByFournisseurAndCategory(fournisseur: String, category: String): List<Product> =
        productDao.getProductsByFournisseurAndCategory(fournisseur, category)
}
