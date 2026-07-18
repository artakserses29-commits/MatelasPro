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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.data.firestore.AluNoteFS
import com.matelaspro.app.databinding.ActivityAluNoteListBinding
import com.matelaspro.app.databinding.ItemAluNoteBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency

class AluNoteListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAluNoteListBinding
    private lateinit var adapter: NoteAdapter
    private val app get() = application as MatelasProApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAluNoteListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.fabAdd.setOnClickListener { showNoteForm(null) }

        adapter = NoteAdapter(
            onClick = { note -> startActivity(Intent(this, AluNoteDetailActivity::class.java).putExtra("note_id", note.id)) },
            onDelete = { note -> deleteNote(note) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        showSkeleton()

        lifecycleScope.launch {
            app.firestoreService.allAluNotesFlow().collect { notes ->
                adapter.submitList(notes)
                val empty = notes.isEmpty()
                binding.textEmptyState.visibility = if (empty) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (empty) View.GONE else View.VISIBLE
                hideSkeleton()
            }
        }
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

    private fun showNoteForm(editNote: AluNoteFS?) {
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
            .setPositiveButton("Enregistrer", null)
            .setNegativeButton("Annuler", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        editName.error = null; editDesc.error = null; editTotal.error = null
                        val name = editName.text.toString().trim()
                        val desc = editDesc.text.toString().trim()
                        val total = editTotal.text.toString().trim().toDoubleOrNull()
                        if (name.isEmpty()) {
                            editName.error = "Le nom du client est requis"
                            editName.requestFocus()
                            return@setOnClickListener
                        }
                        if (desc.isEmpty()) {
                            editDesc.error = "La description est requise"
                            editDesc.requestFocus()
                            return@setOnClickListener
                        }
                        if (total == null || total <= 0) {
                            editTotal.error = "Le montant doit être supérieur à 0"
                            editTotal.requestFocus()
                            return@setOnClickListener
                        }
                        lifecycleScope.launch {
                            try {
                                if (editNote != null) {
                                    app.firestoreService.setAluNote(editNote.id, editNote.copy(clientName = name, description = desc, montantTotal = total, resteAPaye = total - editNote.montantPaye))
                                } else {
                                    app.firestoreService.setAluNote(null, AluNoteFS(clientName = name, description = desc, montantTotal = total))
                                }
                                dismiss()
                            } catch (e: Exception) {
                                editName.error = "Erreur lors de l'enregistrement"
                            }
                        }
                    }
                }
                show()
            }
    }

    private fun deleteNote(note: AluNoteFS) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer")
            .setMessage("Supprimer la note de ${note.clientName} ?")
            .setPositiveButton("Supprimer") { _, _ ->
                lifecycleScope.launch { app.firestoreService.deleteAluNote(note.id) }
                Toast.makeText(this, "Note supprimée", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}

private class NoteAdapter(
    private val onClick: (AluNoteFS) -> Unit,
    private val onDelete: (AluNoteFS) -> Unit
) : RecyclerView.Adapter<NoteAdapter.ViewHolder>() {
    private var items = listOf<AluNoteFS>()

    fun submitList(list: List<AluNoteFS>) { items = list; notifyDataSetChanged() }

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
