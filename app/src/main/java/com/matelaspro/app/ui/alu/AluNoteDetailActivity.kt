package com.matelaspro.app.ui.alu

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.data.firestore.AluNoteFS
import com.matelaspro.app.data.firestore.AluNotePaymentFS
import com.matelaspro.app.databinding.ActivityAluNoteDetailBinding
import com.matelaspro.app.databinding.ItemAluNotePaymentBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

class AluNoteDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAluNoteDetailBinding
    private var noteId: String = ""
    private lateinit var paymentAdapter: PaymentHistoryAdapter
    private val app get() = application as MatelasProApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAluNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        noteId = intent.getStringExtra("note_id") ?: ""
        if (noteId.isEmpty()) { finish(); return }

        binding.btnBack.setOnClickListener { finish() }

        paymentAdapter = PaymentHistoryAdapter()
        binding.recyclerPayments.layoutManager = LinearLayoutManager(this)
        binding.recyclerPayments.adapter = paymentAdapter

        binding.btnPayer.setOnClickListener { showPaymentDialog() }
        binding.btnDepenses.setOnClickListener {
            startActivity(Intent(this, AluNoteExpenseActivity::class.java).putExtra("note_id", noteId))
        }
        showSkeleton()

        lifecycleScope.launch {
            val note = app.firestoreService.getAluNoteById(noteId)
            if (note != null) updateUI(note)
        }

        lifecycleScope.launch {
            app.firestoreService.getAluNotePaymentsFlow(noteId).collect { payments ->
                paymentAdapter.submitList(payments)
                binding.textEmptyPayments.visibility = if (payments.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun updateUI(note: AluNoteFS) {
        val fmt = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
        binding.textClientName.text = note.clientName
        binding.textDescription.text = note.description
        binding.textMontantTotal.text = fmt.format(note.montantTotal)
        binding.textMontantPaye.text = fmt.format(note.montantPaye)
        binding.textReste.text = fmt.format(note.resteAPaye)
        hideSkeleton()
    }

    private fun showSkeleton() {
        binding.skeleton.root.apply {
            visibility = android.view.View.VISIBLE
            startShimmer()
        }
    }

    private fun hideSkeleton() {
        binding.skeleton.root.apply {
            stopShimmer()
            visibility = android.view.View.GONE
        }
    }

    private fun showPaymentDialog() {
        val fmt = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
        lifecycleScope.launch {
            try {
                val note = app.firestoreService.getAluNoteById(noteId) ?: return@launch
                if (note.resteAPaye <= 0) {
                    Toast.makeText(this@AluNoteDetailActivity, "Déjà entièrement payé", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val editMontant = EditText(this@AluNoteDetailActivity).apply {
                    hint = "Montant (max: ${fmt.format(note.resteAPaye)})"
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                }
                val ll = LinearLayout(this@AluNoteDetailActivity).apply {
                    orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 16)
                    addView(editMontant, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                }
                AlertDialog.Builder(this@AluNoteDetailActivity)
                    .setTitle("Paiement")
                    .setMessage("Reste: ${fmt.format(note.resteAPaye)}")
                    .setView(ll)
                    .setPositiveButton("Confirmer", null)
                    .setNegativeButton("Annuler", null)
                    .create().apply {
                        setOnShowListener {
                            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                                editMontant.error = null
                                val montant = editMontant.text.toString().trim().toDoubleOrNull()
                                if (montant == null || montant <= 0) {
                                    editMontant.error = "Le montant doit être supérieur à 0"
                                    editMontant.requestFocus()
                                    return@setOnClickListener
                                }
                                if (montant > note.resteAPaye) {
                                    editMontant.error = "Maximum: ${fmt.format(note.resteAPaye)}"
                                    editMontant.requestFocus()
                                    return@setOnClickListener
                                }
                                lifecycleScope.launch {
                                    try {
                                        app.firestoreService.setAluNotePayment(null, AluNotePaymentFS(noteId = noteId, montant = montant))
                                        val updatedNote = app.firestoreService.getAluNoteById(noteId) ?: return@launch
                                        val newPaye = updatedNote.montantPaye + montant
                                        val newReste = updatedNote.montantTotal - newPaye
                                        app.firestoreService.setAluNote(noteId, updatedNote.copy(montantPaye = newPaye, resteAPaye = newReste))
                                        val finalNote = app.firestoreService.getAluNoteById(noteId)
                                        if (finalNote != null) updateUI(finalNote)
                                        dismiss()
                                        Toast.makeText(this@AluNoteDetailActivity, "Paiement enregistré", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        editMontant.error = "Erreur lors du paiement"
                                    }
                                }
                            }
                        }
                        show()
                    }
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                Toast.makeText(this@AluNoteDetailActivity, "Erreur: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

private class PaymentHistoryAdapter : RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder>() {
    private var items = listOf<AluNotePaymentFS>()

    fun submitList(list: List<AluNotePaymentFS>) { items = list; notifyDataSetChanged() }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAluNotePaymentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = items[position]
        val fmt = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("fr", "FR"))
        holder.binding.textMontant.text = fmt.format(p.montant)
        holder.binding.textDate.text = df.format(Date(p.createdAt?.toDate()?.time ?: 0L))
    }

    class ViewHolder(val binding: ItemAluNotePaymentBinding) : RecyclerView.ViewHolder(binding.root)
}
