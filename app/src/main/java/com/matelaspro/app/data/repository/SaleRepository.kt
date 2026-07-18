package com.matelaspro.app.data.repository

import androidx.lifecycle.LiveData
import com.matelaspro.app.data.dao.SaleDao
import com.matelaspro.app.data.entity.Sale

class SaleRepository(private val saleDao: SaleDao) {
    val allSales: LiveData<List<Sale>> = saleDao.getAllSales()

    suspend fun getSaleById(id: Long): Sale? = saleDao.getSaleById(id)

    fun getSalesByProductId(productId: Long): LiveData<List<Sale>> = saleDao.getSalesByProductId(productId)

    suspend fun insert(sale: Sale): Long = saleDao.insert(sale)

    suspend fun update(sale: Sale) = saleDao.update(sale)

    suspend fun delete(sale: Sale) = saleDao.delete(sale)

    val totalProfit: LiveData<Double> = saleDao.getTotalProfit()
    val totalSalesAmount: LiveData<Double> = saleDao.getTotalSalesAmount()
    val totalQuantitySold: LiveData<Int> = saleDao.getTotalQuantitySold()

    suspend fun getAllSalesList(): List<Sale> = saleDao.getAllSalesList()

    suspend fun getTotalQuantitySoldByProductId(productId: Long): Int =
        saleDao.getTotalQuantitySoldByProductId(productId)

    suspend fun getAllSoldQuantities(): List<com.matelaspro.app.data.dao.ProductSold> =
        saleDao.getAllSoldQuantities()

    val salesByCategory: LiveData<List<com.matelaspro.app.data.dao.CategorySales>> = saleDao.getSalesByCategory()
    val topProducts: LiveData<List<com.matelaspro.app.data.dao.TopProduct>> = saleDao.getTopProducts()
    val monthlySales: LiveData<List<com.matelaspro.app.data.dao.MonthlySales>> = saleDao.getMonthlySales()
    val productMargins: LiveData<List<com.matelaspro.app.data.dao.ProductMargin>> = saleDao.getProductMargins()

    suspend fun getUserSalesTotalInRange(userId: Long, start: Long, end: Long): Double =
        saleDao.getUserSalesTotalInRange(userId, start, end)

    suspend fun getUserProfitInRange(userId: Long, start: Long, end: Long): Double =
        saleDao.getUserProfitInRange(userId, start, end)

    suspend fun getUserSalesTotalInMonth(userId: Long, startOfMonth: Long, startOfNextMonth: Long): Double =
        saleDao.getUserSalesTotalInMonth(userId, startOfMonth, startOfNextMonth)

    suspend fun getUserProfitInMonth(userId: Long, startOfMonth: Long, startOfNextMonth: Long): Double =
        saleDao.getUserProfitInMonth(userId, startOfMonth, startOfNextMonth)
}
