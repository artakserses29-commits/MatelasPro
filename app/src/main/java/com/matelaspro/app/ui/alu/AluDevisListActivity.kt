package com.matelaspro.app.ui.alu

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.matelaspro.app.data.entity.AluDevis
import com.matelaspro.app.databinding.ActivityAluDevisListBinding
import com.matelaspro.app.databinding.ItemSavedDevisBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

class AluDevisListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAluDevisListBinding
    private lateinit var viewModel: AluViewModel
    private lateinit var adapter: DevisCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAluDevisListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[AluViewModel::class.java]

        binding.btnBack.setOnClickListener { finish() }

        adapter = DevisCardAdapter(
            onEdit = { devis -> editDevis(devis) },
            onDelete = { devis -> deleteDevis(devis) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel.allDevis.observe(this) { devis ->
            adapter.submitList(devis)
            val empty = devis.isEmpty()
            binding.textEmptyState.visibility = if (empty) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (empty) View.GONE else View.VISIBLE
        }
    }

    private fun editDevis(devis: AluDevis) {
        val intent = Intent(this, AluDevisActivity::class.java)
        intent.putExtra("devis_id", devis.id)
        startActivity(intent)
    }

    private fun deleteDevis(devis: AluDevis) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer")
            .setMessage("Supprimer le devis de ${devis.clientName} ?")
            .setPositiveButton("Supprimer") { _, _ ->
                viewModel.deleteDevis(devis)
                Toast.makeText(this, "Devis supprimé", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}

class DevisCardAdapter(
    private val onEdit: (AluDevis) -> Unit,
    private val onDelete: (AluDevis) -> Unit
) : RecyclerView.Adapter<DevisCardAdapter.ViewHolder>() {
    private var items = listOf<AluDevis>()

    fun submitList(list: List<AluDevis>) { items = list; notifyDataSetChanged() }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavedDevisBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val devis = items[position]
        val format = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("fr", "FR"))
        holder.binding.textClientName.text = devis.clientName
        holder.binding.textAddress.text = devis.clientAddress
        holder.binding.textAddress.visibility = if (devis.clientAddress.isNotEmpty()) View.VISIBLE else View.GONE
        holder.binding.textPhone.text = devis.clientPhone
        holder.binding.textPhone.visibility = if (devis.clientPhone.isNotEmpty()) View.VISIBLE else View.GONE
        holder.binding.textDate.text = dateFormat.format(Date(devis.createdAt))
        holder.binding.textTotal.text = format.format(devis.totalAmount)

        val lines = devis.items.split("\n")
        val preview = lines.take(3).joinToString("\n") { line ->
            val parts = line.split("|")
            if (parts.size >= 3) "${parts[1]} x${parts[2]}" else line
        }
        if (lines.size > 3) {
            holder.binding.textItems.text = "$preview\n... (+${lines.size - 3} autres)"
        } else {
            holder.binding.textItems.text = preview
        }

        holder.binding.btnModifier.setOnClickListener { onEdit(devis) }
        holder.binding.btnSupprimer.setOnClickListener { onDelete(devis) }
    }

    class ViewHolder(val binding: ItemSavedDevisBinding) : RecyclerView.ViewHolder(binding.root)
}
