package com.matelaspro.app.ui.product

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.data.entity.Fournisseur
import com.matelaspro.app.data.entity.FournisseurPaiement
import com.matelaspro.app.databinding.ActivityPaiementFournisseurListBinding
import com.matelaspro.app.databinding.ItemPaiementFournisseurBinding
import com.matelaspro.app.util.FormatUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaiementFournisseurListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaiementFournisseurListBinding
    private lateinit var app: MatelasProApp
    private lateinit var adapter: PaiementGlobalAdapter
    private var allPaiements = listOf<FournisseurPaiement>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaiementFournisseurListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        app = application as MatelasProApp

        binding.btnBack.setOnClickListener { finish() }

        adapter = PaiementGlobalAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        app.fournisseurRepository.allFournisseurs.observe(this) { fournisseurs ->
            adapter.setFournisseurMap(fournisseurs.associateBy { it.id })
        }

        app.fournisseurRepository.allPaiements.observe(this) { paiements ->
            allPaiements = paiements
            adapter.submitList(paiements)
            updateEmptyState(paiements)
        }

        binding.editSearchDate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterPaiements(s?.toString() ?: "")
            }
        })
    }

    private fun filterPaiements(query: String) {
        if (query.isBlank()) {
            adapter.submitList(allPaiements)
            updateEmptyState(allPaiements)
            return
        }
        val filtered = allPaiements.filter { p ->
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dateStr = sdf.format(Date(p.createdAt))
            dateStr.contains(query)
        }
        adapter.submitList(filtered)
        updateEmptyState(filtered)
    }

    private fun updateEmptyState(list: List<FournisseurPaiement>) {
        binding.textEmptyState.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        binding.recyclerView.visibility = if (list.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
    }
}

class PaiementGlobalAdapter : RecyclerView.Adapter<PaiementGlobalAdapter.ViewHolder>() {
    private var items = listOf<FournisseurPaiement>()
    private var fournisseurMap = mapOf<Long, Fournisseur>()

    fun setFournisseurMap(map: Map<Long, Fournisseur>) { fournisseurMap = map; notifyDataSetChanged() }

    fun submitList(list: List<FournisseurPaiement>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPaiementFournisseurBinding.inflate(android.view.LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = items[position]
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val f = fournisseurMap[p.fournisseurId]
        holder.binding.textFournisseurName.text = f?.name ?: "?"
        holder.binding.textMontant.text = FormatUtil.montant(p.montant)
        holder.binding.textDate.text = sdf.format(Date(p.createdAt))
    }

    class ViewHolder(val binding: ItemPaiementFournisseurBinding) : RecyclerView.ViewHolder(binding.root)
}
