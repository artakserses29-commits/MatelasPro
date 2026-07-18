package com.matelaspro.app.ui.alu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.data.entity.AluDevis
import com.matelaspro.app.data.entity.AluNote
import com.matelaspro.app.data.entity.AluNoteExpense
import com.matelaspro.app.data.entity.AluNotePayment
import com.matelaspro.app.data.entity.AluProduct
import kotlinx.coroutines.launch

class AluViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MatelasProApp
    private val productRepo = app.aluProductRepository
    private val devisRepo = app.aluDevisRepository
    private val noteRepo = app.aluNoteRepository

    val allProducts: LiveData<List<AluProduct>> = productRepo.allProducts
    val allDevis: LiveData<List<AluDevis>> = devisRepo.allDevis
    val allNotes: LiveData<List<AluNote>> = noteRepo.allNotes

    var editingDevisId: Long? = null
    var editingClientName: String = ""
    var editingClientAddress: String = ""
    var editingClientPhone: String = ""

    fun insertProduct(name: String, surface: Double, prixUnitaire: Double) {
        viewModelScope.launch {
            productRepo.insert(AluProduct(name = name, surface = surface, prixUnitaire = prixUnitaire))
        }
    }

    fun updateProduct(product: AluProduct) {
        viewModelScope.launch { productRepo.update(product) }
    }

    fun deleteProduct(product: AluProduct) {
        viewModelScope.launch { productRepo.delete(product) }
    }

    fun getProductById(id: Long, callback: (AluProduct?) -> Unit) {
        viewModelScope.launch {
            callback(productRepo.getById(id))
        }
    }

    fun getDevisById(id: Long, callback: (AluDevis?) -> Unit) {
        viewModelScope.launch {
            callback(devisRepo.getById(id))
        }
    }

    fun saveDevis(clientName: String, clientAddress: String, clientPhone: String, items: List<DevisItem>, totalAmount: Double) {
        viewModelScope.launch {
            val itemsStr = items.joinToString("\n") { "${it.product.id}|${it.product.name}|${it.quantity}|${it.surface}|${it.product.prixUnitaire}|${it.prixTotal}" }
            devisRepo.insert(AluDevis(
                clientName = clientName,
                clientAddress = clientAddress,
                clientPhone = clientPhone,
                items = itemsStr,
                totalAmount = totalAmount
            ))
        }
    }

    fun updateDevis(devisId: Long, items: List<DevisItem>, totalAmount: Double,
                    clientName: String? = null, clientAddress: String? = null, clientPhone: String? = null) {
        viewModelScope.launch {
            val existing = devisRepo.getById(devisId) ?: return@launch
            val itemsStr = items.joinToString("\n") { "${it.product.id}|${it.product.name}|${it.quantity}|${it.surface}|${it.product.prixUnitaire}|${it.prixTotal}" }
            devisRepo.update(existing.copy(
                items = itemsStr, totalAmount = totalAmount,
                clientName = clientName ?: existing.clientName,
                clientAddress = clientAddress ?: existing.clientAddress,
                clientPhone = clientPhone ?: existing.clientPhone
            ))
        }
    }

    fun deleteDevis(devis: AluDevis) {
        viewModelScope.launch { devisRepo.delete(devis) }
    }

    fun getNoteById(id: Long, callback: (AluNote?) -> Unit) {
        viewModelScope.launch { callback(noteRepo.getNoteById(id)) }
    }

    fun insertNote(clientName: String, description: String, montantTotal: Double) {
        viewModelScope.launch {
            noteRepo.insertNote(AluNote(clientName = clientName, description = description, montantTotal = montantTotal))
        }
    }

    fun updateNote(note: AluNote) {
        viewModelScope.launch { noteRepo.updateNote(note) }
    }

    fun deleteNote(note: AluNote) {
        viewModelScope.launch { noteRepo.deleteNote(note) }
    }

    fun getPaymentsByNoteId(noteId: Long): LiveData<List<AluNotePayment>> = noteRepo.getPaymentsByNoteId(noteId)

    fun addPayment(noteId: Long, montant: Double, callback: (AluNote?) -> Unit = {}) {
        viewModelScope.launch {
            noteRepo.insertPayment(AluNotePayment(noteId = noteId, montant = montant))
            val note = noteRepo.getNoteById(noteId) ?: return@launch
            val newPaye = note.montantPaye + montant
            val newReste = note.montantTotal - newPaye
            noteRepo.updateNote(note.copy(montantPaye = newPaye, resteAPaye = newReste))
            callback(noteRepo.getNoteById(noteId))
        }
    }

    fun getExpensesByNoteId(noteId: Long): LiveData<List<AluNoteExpense>> = noteRepo.getExpensesByNoteId(noteId)

    fun insertExpense(noteId: Long, description: String, montant: Double) {
        viewModelScope.launch {
            noteRepo.insertExpense(AluNoteExpense(noteId = noteId, description = description, montant = montant))
        }
    }

    fun updateExpense(expense: AluNoteExpense) {
        viewModelScope.launch { noteRepo.updateExpense(expense) }
    }

    fun deleteExpense(expense: AluNoteExpense) {
        viewModelScope.launch { noteRepo.deleteExpense(expense) }
    }
}
