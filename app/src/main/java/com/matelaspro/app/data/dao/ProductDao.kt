package com.matelaspro.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.matelaspro.app.data.entity.Product

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun getAllProducts(): LiveData<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE category = :category ORDER BY createdAt DESC")
    fun getProductsByCategory(category: String): LiveData<List<Product>>

    @Query("SELECT DISTINCT category FROM products WHERE category != '' ORDER BY category ASC")
    fun getAllCategories(): LiveData<List<String>>

    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    suspend fun getAllProductsSuspend(): List<Product>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchProducts(query: String): LiveData<List<Product>>

    @Query("SELECT * FROM products WHERE createdAt >= :startOfDay AND createdAt < :endOfDay ORDER BY createdAt ASC")
    suspend fun getProductsByDateRange(startOfDay: Long, endOfDay: Long): List<Product>

    @Query("SELECT * FROM products WHERE (createdAt >= :startOfDay AND createdAt < :endOfDay) AND fournisseur = :fournisseur ORDER BY createdAt ASC")
    suspend fun getProductsByDateRangeAndFournisseur(startOfDay: Long, endOfDay: Long, fournisseur: String): List<Product>

    @Query("SELECT * FROM products WHERE name = :name AND fournisseur = :fournisseur AND category = :category LIMIT 1")
    suspend fun getProductByNameAndFournisseur(name: String, fournisseur: String, category: String): Product?

    @Query("SELECT COALESCE(SUM(quantity * sellingPrice), 0) FROM products")
    fun getTotalStockValue(): LiveData<Double>

    @Query("SELECT * FROM products WHERE quantity <= :threshold ORDER BY quantity ASC")
    fun getLowStockProducts(threshold: Int = 5): LiveData<List<Product>>

    @Query("SELECT * FROM products WHERE fournisseur = :fournisseur AND category = :category ORDER BY name ASC")
    suspend fun getProductsByFournisseurAndCategory(fournisseur: String, category: String): List<Product>
}
