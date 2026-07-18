package com.matelaspro.app.ui.alu

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.matelaspro.app.R
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.data.firestore.AluNoteExpenseFS
import com.matelaspro.app.databinding.ActivityAluNoteExpenseBinding
import com.matelaspro.app.databinding.ItemAluNoteExpenseBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

class AluNoteExpenseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAluNoteExpenseBinding
    private var noteId: String = ""
    private var noteMontantPaye: Double = 0.0
    private var noteResteAPaye: Double = 0.0
    private lateinit var adapter: ExpenseAdapter
    private val app get() = application as MatelasProApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAluNoteExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        noteId = intent.getStringExtra("note_id") ?: ""
        if (noteId.isEmpty()) { finish(); return }

        binding.btnBack.setOnClickListener { finish() }
        binding.fabAdd.setOnClickListener { showExpenseForm(null) }

        adapter = ExpenseAdapter(
            onEdit = { expense -> showExpenseForm(expense) },
            onDelete = { expense -> deleteExpense(expense) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        lifecycleScope.launch {
            val note = app.firestoreService.getAluNoteById(noteId)
            if (note != null) {
                noteMontantPaye = note.montantPaye
                noteResteAPaye = note.resteAPaye
                val fmt = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
                binding.textClientName.text = note.clientName
                binding.textMontantPaye.text = fmt.format(note.montantPaye)
                binding.textReste.text = fmt.format(note.resteAPaye)
            }
        }

        lifecycleScope.launch {
            app.firestoreService.getAluNoteExpensesFlow(noteId).collect { expenses ->
                adapter.submitList(expenses)
                val totalDep = expenses.sumOf { it.montant }
                val fmt = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
                binding.textTotalDepenses.text = fmt.format(totalDep)
                val balance = noteMontantPaye - totalDep
                binding.textBalance.text = fmt.format(balance)
                binding.textBalance.setTextColor(ContextCompat.getColor(this@AluNoteExpenseActivity, if (balance >= 0) R.color.green else R.color.red))
                binding.textEmptyState.visibility = if (expenses.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (expenses.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun showExpenseForm(editExpense: AluNoteExpenseFS?) {
        val editDesc = EditText(this).apply { hint = "Description"; setText(editExpense?.description ?: "") }
        val editMontant = EditText(this).apply {
            hint = "Montant dépense"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(editExpense?.montant?.toBigDecimal()?.toPlainString() ?: "")
        }
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 16)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, 12)
            addView(editDesc, lp)
            addView(editMontant, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        AlertDialog.Builder(this)
            .setTitle(if (editExpense == null) "Ajouter Dépense" else "Modifier Dépense")
            .setView(ll)
            .setPositiveButton("Enregistrer", null)
            .setNegativeButton("Annuler", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        editDesc.error = null; editMontant.error = null
                        val desc = editDesc.text.toString().trim()
                        val montant = editMontant.text.toString().trim().toDoubleOrNull()
                        if (desc.isEmpty()) {
                            editDesc.error = "La description est requise"
                            editDesc.requestFocus()
                            return@setOnClickListener
                        }
                        if (montant == null || montant <= 0) {
                            editMontant.error = "Le montant doit être supérieur à 0"
                            editMontant.requestFocus()
                            return@setOnClickListener
                        }
                        lifecycleScope.launch {
                            try {
                                if (editExpense != null) {
                                    app.firestoreService.setAluNoteExpense(editExpense.id, editExpense.copy(description = desc, montant = montant))
                                } else {
                                    app.firestoreService.setAluNoteExpense(null, AluNoteExpenseFS(noteId = noteId, description = desc, montant = montant))
                                }
                                dismiss()
                            } catch (e: Exception) {
                                editDesc.error = "Erreur lors de l'enregistrement"
                            }
                        }
                    }
                }
                show()
            }
    }

    private fun deleteExpense(expense: AluNoteExpenseFS) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer")
            .setMessage("Supprimer cette dépense ?")
            .setPositiveButton("Supprimer") { _, _ ->
                lifecycleScope.launch { app.firestoreService.deleteAluNoteExpense(expense.id) }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}

private class ExpenseAdapter(
    private val onEdit: (AluNoteExpenseFS) -> Unit,
    private val onDelete: (AluNoteExpenseFS) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {
    private var items = listOf<AluNoteExpenseFS>()

    fun submitList(list: List<AluNoteExpenseFS>) { items = list; notifyDataSetChanged() }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAluNoteExpenseBinding.inflate(android.view.LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val expense = items[position]
        val fmt = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("fr", "FR"))
        holder.binding.textDescription.text = expense.description
        holder.binding.textDate.text = df.format(Date(expense.createdAt?.toDate()?.time ?: 0L))
        holder.binding.textMontant.text = fmt.format(expense.montant)
        holder.binding.btnEdit.setOnClickListener { onEdit(expense) }
        holder.binding.btnDelete.setOnClickListener { onDelete(expense) }
    }

    class ViewHolder(val binding: ItemAluNoteExpenseBinding) : RecyclerView.ViewHolder(binding.root)
}
