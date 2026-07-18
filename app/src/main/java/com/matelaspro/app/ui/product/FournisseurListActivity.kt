package com.matelaspro.app.ui.product

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.data.entity.Fournisseur
import com.matelaspro.app.databinding.ActivityFournisseurListBinding
import com.matelaspro.app.databinding.ItemFournisseurBinding
import com.matelaspro.app.util.FormatUtil
import kotlinx.coroutines.launch

data class FournisseurWithTotals(
    val fournisseur: Fournisseur,
    val totalADevoir: Double,
    val montantVerse: Double,
    val reste: Double
)

class FournisseurListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFournisseurListBinding
    private lateinit var adapter: FournisseurAdapter
    private lateinit var app: MatelasProApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFournisseurListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        app = application as MatelasProApp

        binding.btnBack.setOnClickListener { finish() }
        binding.fabAdd.setOnClickListener { showAddDialog() }

        adapter = FournisseurAdapter(
            onClick = { fournisseur ->
                startActivity(Intent(this, FournisseurDetailActivity::class.java)
                    .putExtra("fournisseur_id", fournisseur.id))
            },
            onEdit = { fournisseur -> showEditDialog(fournisseur) },
            onDelete = { fournisseur -> deleteFournisseur(fournisseur) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        app.fournisseurRepository.allFournisseurs.observe(this) { fournisseurs ->
            computeAndDisplay(fournisseurs)
        }
    }

    private fun computeAndDisplay(fournisseurs: List<Fournisseur>) {
        lifecycleScope.launch {
            val allCredits = app.creditEntryRepository.getAll()
            val activeCredits = allCredits.filter { it.quantity > 0 && !it.isOverridden }
            val list = fournisseurs.map { f ->
                val totalADevoir = activeCredits
                    .filter { it.fournisseur == f.name }
                    .sumOf { it.sellingPrice * it.quantity }
                val montantVerse = app.fournisseurRepository.getTotalPayeByFournisseurId(f.id)
                FournisseurWithTotals(f, totalADevoir, montantVerse, totalADevoir - montantVerse)
            }
            adapter.submitList(list)
            binding.textEmptyState.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            binding.recyclerView.visibility = if (list.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        }
    }

    private fun showAddDialog() {
        val editName = EditText(this).apply { hint = "Nom du fournisseur" }
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 16)
            addView(editName, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        AlertDialog.Builder(this)
            .setTitle("Nouveau fournisseur")
            .setView(ll)
            .setPositiveButton("Ajouter") { _, _ ->
                val name = editName.text.toString().trim()
                if (name.isEmpty()) { Toast.makeText(this, "Nom requis", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                lifecycleScope.launch { app.fournisseurRepository.insert(name) }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showEditDialog(fournisseur: Fournisseur) {
        val editName = EditText(this).apply {
            setText(fournisseur.name)
            hint = "Nom du fournisseur"
        }
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 16)
            addView(editName, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        AlertDialog.Builder(this)
            .setTitle("Modifier le fournisseur")
            .setView(ll)
            .setPositiveButton("Enregistrer") { _, _ ->
                val name = editName.text.toString().trim()
                if (name.isEmpty()) { Toast.makeText(this, "Nom requis", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                lifecycleScope.launch {
                    app.fournisseurRepository.update(fournisseur.copy(name = name))
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun deleteFournisseur(fournisseur: Fournisseur) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer le fournisseur")
            .setMessage("Voulez-vous vraiment supprimer ${fournisseur.name} ?")
            .setPositiveButton("Supprimer") { _, _ ->
                lifecycleScope.launch {
                    app.fournisseurRepository.delete(fournisseur)
                    Toast.makeText(this@FournisseurListActivity, "Fournisseur supprimé", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}

class FournisseurAdapter(
    private val onClick: (Fournisseur) -> Unit,
    private val onEdit: (Fournisseur) -> Unit,
    private val onDelete: (Fournisseur) -> Unit
) : RecyclerView.Adapter<FournisseurAdapter.ViewHolder>() {
    private var items = listOf<FournisseurWithTotals>()

    fun submitList(list: List<FournisseurWithTotals>) { items = list; notifyDataSetChanged() }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFournisseurBinding.inflate(android.view.LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.textFournisseurName.text = item.fournisseur.name
        holder.binding.textReste.text = FormatUtil.montant(item.reste)
        holder.binding.root.setOnClickListener { onClick(item.fournisseur) }
        holder.binding.btnEdit.setOnClickListener { onEdit(item.fournisseur) }
        holder.binding.btnDelete.setOnClickListener { onDelete(item.fournisseur) }
    }

    class ViewHolder(val binding: ItemFournisseurBinding) : RecyclerView.ViewHolder(binding.root)
}
