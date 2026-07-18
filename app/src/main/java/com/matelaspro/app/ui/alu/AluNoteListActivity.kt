package com.matelaspro.app.ui.alu

import android.app.AlertDialog
import android.content.Intent
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
import com.matelaspro.app.data.entity.AluNote
import com.matelaspro.app.databinding.ActivityAluNoteListBinding
import com.matelaspro.app.databinding.ItemAluNoteBinding
import java.text.NumberFormat
import java.util.Currency

class AluNoteListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAluNoteListBinding
    private lateinit var viewModel: AluViewModel
    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAluNoteListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[AluViewModel::class.java]

        binding.btnBack.setOnClickListener { finish() }
        binding.fabAdd.setOnClickListener { showNoteForm(null) }

        adapter = NoteAdapter(
            onClick = { note -> startActivity(Intent(this, AluNoteDetailActivity::class.java).putExtra("note_id", note.id)) },
            onDelete = { note -> deleteNote(note) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel.allNotes.observe(this) { notes ->
            adapter.submitList(notes)
            val empty = notes.isEmpty()
            binding.textEmptyState.visibility = if (empty) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (empty) View.GONE else View.VISIBLE
        }
    }

    private fun showNoteForm(editNote: AluNote?) {
        val editName = EditText(this).apply { hint = "Nom Client"; setText(editNote?.clientName ?: "") }
        val editDesc = EditText(this).apply { hint = "Description"; setText(editNote?.description ?: "") }
        val editTotal = EditText(this).apply {
            hint = "Montant Total"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(editNote?.montantTotal?.toBigDecimal()?.toPlainString() ?: "")
        }
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 16)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, 12)
            addView(editName, lp)
            addView(editDesc, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 12) })
            addView(editTotal, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        AlertDialog.Builder(this)
            .setTitle(if (editNote == null) "Nouvelle Note" else "Modifier Note")
            .setView(ll)
            .setPositiveButton("Enregistrer") { _, _ ->
                val name = editName.text.toString().trim()
                val desc = editDesc.text.toString().trim()
                val total = editTotal.text.toString().trim().toDoubleOrNull()
                if (name.isEmpty() || desc.isEmpty() || total == null || total <= 0) {
                    Toast.makeText(this, "Vérifiez les champs", Toast.LENGTH_SHORT).show(); return@setPositiveButton
                }
                if (editNote != null) {
                    viewModel.updateNote(editNote.copy(clientName = name, description = desc, montantTotal = total, resteAPaye = total - editNote.montantPaye))
                } else {
                    viewModel.insertNote(name, desc, total)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun deleteNote(note: AluNote) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer")
            .setMessage("Supprimer la note de ${note.clientName} ?")
            .setPositiveButton("Supprimer") { _, _ -> viewModel.deleteNote(note); Toast.makeText(this, "Note supprimée", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("Annuler", null)
            .show()
    }
}

private class NoteAdapter(
    private val onClick: (AluNote) -> Unit,
    private val onDelete: (AluNote) -> Unit
) : RecyclerView.Adapter<NoteAdapter.ViewHolder>() {
    private var items = listOf<AluNote>()

    fun submitList(list: List<AluNote>) { items = list; notifyDataSetChanged() }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAluNoteBinding.inflate(android.view.LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note = items[position]
        val fmt = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
        holder.binding.textClientName.text = note.clientName
        holder.binding.textDescription.text = note.description
        holder.binding.textMontantTotal.text = fmt.format(note.montantTotal)
        holder.binding.textMontantPaye.text = fmt.format(note.montantPaye)
        holder.binding.textReste.text = fmt.format(note.resteAPaye)
        holder.binding.root.setOnClickListener { onClick(note) }
        holder.binding.root.setOnLongClickListener { onDelete(note); true }
    }

    class ViewHolder(val binding: ItemAluNoteBinding) : RecyclerView.ViewHolder(binding.root)
}
