package com.matelaspro.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.matelaspro.app.data.entity.Payment
import com.matelaspro.app.data.entity.Product
import com.matelaspro.app.data.entity.Sale
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MatelasProApp
    private val productRepo = app.productRepository
    private val saleRepo = app.saleRepository
    private val paymentRepo = app.paymentRepository

    val allProducts: LiveData<List<Product>> = productRepo.allProducts
    val allCategories: LiveData<List<String>> = productRepo.allCategories
    val allSales: LiveData<List<Sale>> = saleRepo.allSales
    val allPayments: LiveData<List<Payment>> = paymentRepo.allPayments
    val totalProfit: LiveData<Double> = saleRepo.totalProfit
    val totalSalesAmount: LiveData<Double> = saleRepo.totalSalesAmount
    val totalVersements: LiveData<Double> = paymentRepo.totalVersements
    val totalDepenses: LiveData<Double> = paymentRepo.totalDepenses

    val salesByCategory: LiveData<List<com.matelaspro.app.data.dao.CategorySales>> = saleRepo.salesByCategory
    val topProducts: LiveData<List<com.matelaspro.app.data.dao.TopProduct>> = saleRepo.topProducts
    val monthlySales: LiveData<List<com.matelaspro.app.data.dao.MonthlySales>> = saleRepo.monthlySales
    val productMargins: LiveData<List<com.matelaspro.app.data.dao.ProductMargin>> = saleRepo.productMargins
    val totalStockValue: LiveData<Double> = productRepo.totalStockValue
    val lowStockProducts: LiveData<List<Product>> = productRepo.getLowStockProducts(5)

    val oneTenthProfit: LiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(totalProfit) { profit ->
            value = profit / 10.0
        }
    }

    fun insertProduct(name: String, category: String, fournisseur: String = "", description: String, quantity: Int, purchasePrice: Double, sellingPrice: Double, epaisseur: Double = 0.0, prixUnitaireCm: Double = 0.0, longueur: Double = 0.0, createdAt: Long? = null) {
        val now = createdAt ?: System.currentTimeMillis()
        viewModelScope.launch {
            val productId = productRepo.insert(Product(
                name = name, category = category, fournisseur = fournisseur,
                description = description, quantity = quantity,
                purchasePrice = purchasePrice, sellingPrice = sellingPrice,
                epaisseur = epaisseur, prixUnitaireCm = prixUnitaireCm,
                longueur = longueur, createdAt = now
            ))
            app.creditEntryRepository.insert(com.matelaspro.app.data.entity.CreditEntry(
                fournisseur = fournisseur, productName = name,
                category = category, epaisseur = epaisseur,
                longueur = longueur, description = description,
                quantity = quantity, sellingPrice = sellingPrice,
                createdAt = now, productId = productId
            ))
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch { productRepo.update(product) }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch { productRepo.delete(product) }
    }

    fun deleteProductById(id: Long) {
        viewModelScope.launch { productRepo.deleteById(id) }
    }

    fun recordSale(productId: Long, productName: String, quantity: Int, unitPrice: Double, purchasePrice: Double, userId: Long = 0) {
        val totalAmount = quantity * unitPrice
        val profit = (unitPrice - purchasePrice) * quantity
        viewModelScope.launch {
            saleRepo.insert(Sale(
                productId = productId,
                productName = productName,
                quantity = quantity,
                unitPrice = unitPrice,
                purchasePrice = purchasePrice,
                totalAmount = totalAmount,
                profit = profit,
                userId = userId
            ))
        }
    }

    fun deleteSale(sale: Sale) {
        viewModelScope.launch { saleRepo.delete(sale) }
    }

    fun insertPayment(amount: Double, type: String, description: String) {
        viewModelScope.launch {
            paymentRepo.insert(Payment(
                amount = amount,
                type = type,
                description = description
            ))
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch { paymentRepo.delete(payment) }
    }
}
