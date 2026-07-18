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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.matelaspro.app.data.entity.AluNoteExpense
import com.matelaspro.app.databinding.ActivityAluNoteExpenseBinding
import com.matelaspro.app.databinding.ItemAluNoteExpenseBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

class AluNoteExpenseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAluNoteExpenseBinding
    private lateinit var viewModel: AluViewModel
    private var noteId: Long = -1L
    private lateinit var adapter: ExpenseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAluNoteExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        noteId = intent.getLongExtra("note_id", -1L)
        if (noteId == -1L) { finish(); return }

        viewModel = ViewModelProvider(this)[AluViewModel::class.java]

        binding.btnBack.setOnClickListener { finish() }
        binding.fabAdd.setOnClickListener { showExpenseForm(null) }

        adapter = ExpenseAdapter(
            onEdit = { expense -> showExpenseForm(expense) },
            onDelete = { expense -> deleteExpense(expense) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel.getNoteById(noteId) { note ->
            if (note != null) {
                val fmt = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
                binding.textClientName.text = note.clientName
                binding.textMontantPaye.text = fmt.format(note.montantPaye)
                binding.textReste.text = fmt.format(note.resteAPaye)
            }
        }

        viewModel.getExpensesByNoteId(noteId).observe(this) { expenses ->
            adapter.submitList(expenses)
            val fmt = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
            binding.textTotalDepenses.text = fmt.format(expenses.sumOf { it.montant })
            binding.textEmptyState.visibility = if (expenses.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (expenses.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun showExpenseForm(editExpense: AluNoteExpense?) {
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
            .setPositiveButton("Enregistrer") { _, _ ->
                val desc = editDesc.text.toString().trim()
                val montant = editMontant.text.toString().trim().toDoubleOrNull()
                if (desc.isEmpty() || montant == null || montant <= 0) {
                    Toast.makeText(this, "Vérifiez les champs", Toast.LENGTH_SHORT).show(); return@setPositiveButton
                }
                if (editExpense != null) {
                    viewModel.updateExpense(editExpense.copy(description = desc, montant = montant))
                } else {
                    viewModel.insertExpense(noteId, desc, montant)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun deleteExpense(expense: AluNoteExpense) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer")
            .setMessage("Supprimer cette dépense ?")
            .setPositiveButton("Supprimer") { _, _ -> viewModel.deleteExpense(expense) }
            .setNegativeButton("Annuler", null)
            .show()
    }
}

private class ExpenseAdapter(
    private val onEdit: (AluNoteExpense) -> Unit,
    private val onDelete: (AluNoteExpense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {
    private var items = listOf<AluNoteExpense>()

    fun submitList(list: List<AluNoteExpense>) { items = list; notifyDataSetChanged() }

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
        holder.binding.textDate.text = df.format(Date(expense.createdAt))
        holder.binding.textMontant.text = fmt.format(expense.montant)
        holder.binding.btnEdit.setOnClickListener { onEdit(expense) }
        holder.binding.btnDelete.setOnClickListener { onDelete(expense) }
    }

    class ViewHolder(val binding: ItemAluNoteExpenseBinding) : RecyclerView.ViewHolder(binding.root)
}
