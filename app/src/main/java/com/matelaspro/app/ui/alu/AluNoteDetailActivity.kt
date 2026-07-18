package com.matelaspro.app.ui.alu

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.matelaspro.app.data.entity.AluNotePayment
import com.matelaspro.app.databinding.ActivityAluNoteDetailBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

class AluNoteDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAluNoteDetailBinding
    private lateinit var viewModel: AluViewModel
    private var noteId: Long = -1L
    private lateinit var paymentAdapter: PaymentHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAluNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        noteId = intent.getLongExtra("note_id", -1L)
        if (noteId == -1L) { finish(); return }

        viewModel = ViewModelProvider(this)[AluViewModel::class.java]

        binding.btnBack.setOnClickListener { finish() }

        paymentAdapter = PaymentHistoryAdapter()
        binding.recyclerPayments.layoutManager = LinearLayoutManager(this)
        binding.recyclerPayments.adapter = paymentAdapter

        binding.btnPayer.setOnClickListener { showPaymentDialog() }
        binding.btnDepenses.setOnClickListener {
            startActivity(Intent(this, AluNoteExpenseActivity::class.java).putExtra("note_id", noteId))
        }

        viewModel.getNoteById(noteId) { note ->
            if (note != null) updateUI(note)
        }

        viewModel.getPaymentsByNoteId(noteId).observe(this) { payments ->
            paymentAdapter.submitList(payments)
            binding.textEmptyPayments.visibility = if (payments.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun updateUI(note: com.matelaspro.app.data.entity.AluNote) {
        val fmt = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
        binding.textClientName.text = note.clientName
        binding.textDescription.text = note.description
        binding.textMontantTotal.text = fmt.format(note.montantTotal)
        binding.textMontantPaye.text = fmt.format(note.montantPaye)
        binding.textReste.text = fmt.format(note.resteAPaye)
    }

    private fun showPaymentDialog() {
        val fmt = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
        viewModel.getNoteById(noteId) { note ->
            if (note == null || note.resteAPaye <= 0) {
                Toast.makeText(this, "Déjà entièrement payé", Toast.LENGTH_SHORT).show(); return@getNoteById
            }
            val editMontant = EditText(this).apply {
                hint = "Montant (max: ${fmt.format(note.resteAPaye)})"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
            val ll = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 16)
                addView(editMontant, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            }
            AlertDialog.Builder(this)
                .setTitle("Paiement")
                .setMessage("Reste: ${fmt.format(note.resteAPaye)}")
                .setView(ll)
                .setPositiveButton("Confirmer") { _, _ ->
                    val montant = editMontant.text.toString().trim().toDoubleOrNull()
                    if (montant == null || montant <= 0 || montant > note.resteAPaye) {
                        Toast.makeText(this, "Montant invalide", Toast.LENGTH_SHORT).show(); return@setPositiveButton
                    }
                    viewModel.addPayment(noteId, montant) { updated ->
                        if (updated != null) updateUI(updated)
                    }
                    Toast.makeText(this, "Paiement enregistré", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
    }

}

private class PaymentHistoryAdapter : RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder>() {
    private var items = listOf<AluNotePayment>()

    fun submitList(list: List<AluNotePayment>) { items = list; notifyDataSetChanged() }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val tv = TextView(parent.context).apply { setPadding(16, 8, 16, 8); textSize = 14f }
        return ViewHolder(tv)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = items[position]
        val fmt = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("fr", "FR"))
        holder.tv.text = "${fmt.format(p.montant)}  —  ${df.format(Date(p.createdAt))}"
    }

    class ViewHolder(val tv: TextView) : RecyclerView.ViewHolder(tv)
}
