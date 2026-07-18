package com.matelaspro.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.matelaspro.app.data.firestore.*
import com.matelaspro.app.service.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MatelasProApp
    private val fs = app.firestoreService

    val allProducts: Flow<List<ProductFS>> = fs.productsFlow()
    val allCategories: Flow<List<String>> = fs.categoriesFlow()
    val allSales: Flow<List<SaleFS>> = fs.allSalesFlow()
    val allPayments: Flow<List<PaymentFS>> = fs.allPaymentsFlow()

    private val _totalProfit = MutableStateFlow(0.0)
    val totalProfit: StateFlow<Double> = _totalProfit.asStateFlow()

    private val _totalSalesAmount = MutableStateFlow(0.0)
    val totalSalesAmount: StateFlow<Double> = _totalSalesAmount.asStateFlow()

    private val _totalVersements = MutableStateFlow(0.0)
    val totalVersements: StateFlow<Double> = _totalVersements.asStateFlow()

    private val _totalDepenses = MutableStateFlow(0.0)
    val totalDepenses: StateFlow<Double> = _totalDepenses.asStateFlow()

    private val _totalStockValue = MutableStateFlow(0.0)
    val totalStockValue: StateFlow<Double> = _totalStockValue.asStateFlow()

    private val _lowStockProducts = MutableStateFlow<List<ProductFS>>(emptyList())
    val lowStockProducts: StateFlow<List<ProductFS>> = _lowStockProducts.asStateFlow()

    private val _oneTenthProfit = MutableStateFlow(0.0)
    val oneTenthProfit: StateFlow<Double> = _oneTenthProfit.asStateFlow()

    init {
        viewModelScope.launch {
            _totalProfit.value = fs.getTotalProfit()
            _totalSalesAmount.value = fs.getTotalSalesAmount()
            _totalVersements.value = fs.getTotalVersements()
            _totalDepenses.value = fs.getTotalDepenses()
            _totalStockValue.value = fs.getAllProducts().sumOf { it.quantity * it.sellingPrice }
            _lowStockProducts.value = fs.getLowStockProducts(5)
            _oneTenthProfit.value = _totalProfit.value / 10.0
        }
    }

    fun insertProduct(name: String, category: String, fournisseur: String = "", description: String, quantity: Int, purchasePrice: Double, sellingPrice: Double, epaisseur: Double = 0.0, prixUnitaireCm: Double = 0.0, longueur: Double = 0.0, createdAt: Long? = null) {
        viewModelScope.launch {
            val productId = fs.setProduct(null, ProductFS(
                name = name, category = category, fournisseur = fournisseur,
                description = description, quantity = quantity,
                purchasePrice = purchasePrice, sellingPrice = sellingPrice,
                epaisseur = epaisseur, prixUnitaireCm = prixUnitaireCm,
                longueur = longueur
            ))
            fs.setCreditEntry(null, CreditEntryFS(
                fournisseur = fournisseur, productName = name,
                category = category, epaisseur = epaisseur,
                longueur = longueur, description = description,
                quantity = quantity, sellingPrice = sellingPrice,
                productId = productId
            ))
        }
    }

    fun updateProduct(product: ProductFS) {
        viewModelScope.launch { fs.setProduct(product.id, product) }
    }

    fun deleteProduct(product: ProductFS) {
        viewModelScope.launch { fs.deleteProduct(product.id) }
    }

    fun deleteProductById(id: String) {
        viewModelScope.launch { fs.deleteProduct(id) }
    }

    fun recordSale(productId: String, productName: String, quantity: Int, unitPrice: Double, purchasePrice: Double, userId: String = "") {
        val totalAmount = quantity * unitPrice
        val profit = (unitPrice - purchasePrice) * quantity
        viewModelScope.launch {
            fs.setSale(null, SaleFS(
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

    fun deleteSale(sale: SaleFS) {
        viewModelScope.launch { fs.deleteSale(sale.id) }
    }

    fun insertPayment(amount: Double, type: String, description: String) {
        viewModelScope.launch {
            fs.setPayment(null, PaymentFS(
                amount = amount,
                type = type,
                description = description
            ))
        }
    }

    fun deletePayment(payment: PaymentFS) {
        viewModelScope.launch { fs.deletePayment(payment.id) }
    }
}
