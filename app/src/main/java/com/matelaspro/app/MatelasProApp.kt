package com.matelaspro.app

import android.app.Application
import com.matelaspro.app.data.AppDatabase
import com.matelaspro.app.data.repository.AluDevisRepository
import com.matelaspro.app.data.repository.AluNoteRepository
import com.matelaspro.app.data.repository.AluProductRepository
import com.matelaspro.app.data.repository.AuditLogRepository
import com.matelaspro.app.data.repository.CreditEntryRepository
import com.matelaspro.app.data.repository.ExpenseRepository
import com.matelaspro.app.data.repository.FournisseurRepository
import com.matelaspro.app.data.repository.PaymentRepository
import com.matelaspro.app.data.repository.ProductRepository
import com.matelaspro.app.data.repository.SaleRepository
import com.matelaspro.app.data.repository.UserRepository
import com.matelaspro.app.data.repository.UserStockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MatelasProApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val productRepository: ProductRepository by lazy { ProductRepository(database.productDao()) }
    val saleRepository: SaleRepository by lazy { SaleRepository(database.saleDao()) }
    val paymentRepository: PaymentRepository by lazy { PaymentRepository(database.paymentDao()) }
    val aluProductRepository: AluProductRepository by lazy { AluProductRepository(database.aluProductDao()) }
    val aluDevisRepository: AluDevisRepository by lazy { AluDevisRepository(database.aluDevisDao()) }
    val aluNoteRepository: AluNoteRepository by lazy { AluNoteRepository(database.aluNoteDao(), database.aluNotePaymentDao(), database.aluNoteExpenseDao()) }
    val fournisseurRepository: FournisseurRepository by lazy { FournisseurRepository(database.fournisseurDao(), database.fournisseurPaiementDao()) }
    val creditEntryRepository: CreditEntryRepository by lazy { CreditEntryRepository(database.creditEntryDao()) }
    val auditLogRepository: AuditLogRepository by lazy { AuditLogRepository(database.auditLogDao()) }
    val userRepository: UserRepository by lazy { UserRepository(database.userDao()) }
    val expenseRepository: ExpenseRepository by lazy { ExpenseRepository(database.expenseDao()) }
    val userStockRepository: UserStockRepository by lazy { UserStockRepository(database.userStockDao()) }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            seedAdmin()
        }
    }

    private suspend fun seedAdmin() {
        if (userRepository.adminCount() == 0) {
            userRepository.insert(
                com.matelaspro.app.data.entity.User(
                    name = "admin",
                    password = "admin",
                    isAdmin = true
                )
            )
        }
    }
}
