package com.matelaspro.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.matelaspro.app.data.entity.Sale

data class ProductSold(
    val productId: Long,
    val totalSold: Int
)

data class CategorySales(
    val category: String,
    val totalQuantity: Int,
    val totalAmount: Double
)

data class TopProduct(
    val productName: String,
    val totalQuantity: Int,
    val totalAmount: Double,
    val category: String = "",
    val epaisseur: Double = 0.0,
    val longueur: Double = 0.0,
    val description: String = ""
)

data class MonthlySales(
    val yearMonth: String,
    val totalAmount: Double,
    val totalProfit: Double
)

data class ProductMargin(
    val productName: String,
    val avgMargin: Double,
    val category: String = "",
    val epaisseur: Double = 0.0,
    val longueur: Double = 0.0,
    val description: String = ""
)

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY saleDate DESC")
    fun getAllSales(): LiveData<List<Sale>>

    @Query("SELECT * FROM sales")
    suspend fun getAllSalesList(): List<Sale>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Long): Sale?

    @Query("SELECT * FROM sales WHERE productId = :productId ORDER BY saleDate DESC")
    fun getSalesByProductId(productId: Long): LiveData<List<Sale>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sale: Sale): Long

    @Update
    suspend fun update(sale: Sale)

    @Delete
    suspend fun delete(sale: Sale)

    @Query("SELECT COALESCE(SUM(profit), 0) FROM sales")
    fun getTotalProfit(): LiveData<Double>

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM sales")
    fun getTotalSalesAmount(): LiveData<Double>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM sales")
    fun getTotalQuantitySold(): LiveData<Int>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM sales WHERE productId = :productId")
    suspend fun getTotalQuantitySoldByProductId(productId: Long): Int

    @Query("SELECT productId, COALESCE(SUM(quantity), 0) AS totalSold FROM sales GROUP BY productId")
    suspend fun getAllSoldQuantities(): List<com.matelaspro.app.data.dao.ProductSold>

    @Query("SELECT p.category, COALESCE(SUM(s.quantity), 0) AS totalQuantity, COALESCE(SUM(s.totalAmount), 0) AS totalAmount FROM sales s LEFT JOIN products p ON s.productId = p.id GROUP BY p.category ORDER BY totalAmount DESC")
    fun getSalesByCategory(): LiveData<List<CategorySales>>

    @Query("SELECT s.productName, COALESCE(SUM(s.quantity), 0) AS totalQuantity, COALESCE(SUM(s.totalAmount), 0) AS totalAmount, COALESCE(p.category, '') AS category, COALESCE(p.epaisseur, 0) AS epaisseur, COALESCE(p.longueur, 0) AS longueur, COALESCE(p.description, '') AS description FROM sales s LEFT JOIN products p ON s.productId = p.id GROUP BY s.productName ORDER BY totalQuantity DESC LIMIT 10")
    fun getTopProducts(): LiveData<List<TopProduct>>

    @Query("SELECT strftime('%Y-%m', s.saleDate / 1000, 'unixepoch') AS yearMonth, COALESCE(SUM(s.totalAmount), 0) AS totalAmount, COALESCE(SUM(s.profit), 0) AS totalProfit FROM sales s GROUP BY yearMonth ORDER BY yearMonth DESC LIMIT 12")
    fun getMonthlySales(): LiveData<List<MonthlySales>>

    @Query("SELECT s.productName, AVG(CASE WHEN s.quantity > 0 THEN s.profit / CAST(s.quantity AS REAL) ELSE 0 END) AS avgMargin, COALESCE(p.category, '') AS category, COALESCE(p.epaisseur, 0) AS epaisseur, COALESCE(p.longueur, 0) AS longueur, COALESCE(p.description, '') AS description FROM sales s LEFT JOIN products p ON s.productId = p.id GROUP BY s.productName ORDER BY avgMargin DESC")
    fun getProductMargins(): LiveData<List<ProductMargin>>

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM sales WHERE userId = :userId AND saleDate >= :start AND saleDate <= :end")
    suspend fun getUserSalesTotalInRange(userId: Long, start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(profit), 0) FROM sales WHERE userId = :userId AND saleDate >= :start AND saleDate <= :end")
    suspend fun getUserProfitInRange(userId: Long, start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM sales WHERE userId = :userId AND saleDate >= :startOfMonth AND saleDate < :startOfNextMonth")
    suspend fun getUserSalesTotalInMonth(userId: Long, startOfMonth: Long, startOfNextMonth: Long): Double

    @Query("SELECT COALESCE(SUM(profit), 0) FROM sales WHERE userId = :userId AND saleDate >= :startOfMonth AND saleDate < :startOfNextMonth")
    suspend fun getUserProfitInMonth(userId: Long, startOfMonth: Long, startOfNextMonth: Long): Double
}
