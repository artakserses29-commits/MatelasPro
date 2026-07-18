package com.matelaspro.app.ui.alu

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.R
import com.matelaspro.app.data.firestore.AluProductFS
import com.matelaspro.app.databinding.ActivityAluProductListBinding
import com.matelaspro.app.databinding.DialogAluProductBinding
import com.matelaspro.app.databinding.ItemAluProductBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency

class AluProductListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAluProductListBinding
    private lateinit var adapter: AluProductAdapter
    private val app get() = application as MatelasProApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAluProductListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        setupListeners()
        showSkeleton()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = AluProductAdapter(
            onEdit = { showProductDialog(it) },
            onDelete = { deleteProduct(it) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.fabAdd.setOnClickListener { showProductDialog(null) }
    }

    private fun observeData() {
        lifecycleScope.launch {
            app.firestoreService.aluProductsFlow().collect { products ->
                adapter.submitList(products)
                val empty = products.isEmpty()
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

    private fun showProductDialog(product: AluProductFS?) {
        val dialogBinding = DialogAluProductBinding.inflate(layoutInflater)
        val existingProduct = product
        val layoutName = dialogBinding.editName.parent as? com.google.android.material.textfield.TextInputLayout
        val layoutSurface = dialogBinding.editSurface.parent as? com.google.android.material.textfield.TextInputLayout
        val layoutPrix = dialogBinding.editPrixUnitaire.parent as? com.google.android.material.textfield.TextInputLayout

        if (existingProduct != null) {
            dialogBinding.editName.setText(existingProduct.name)
            dialogBinding.editSurface.setText(if (existingProduct.surface > 0) existingProduct.surface.toString() else "")
            dialogBinding.editPrixUnitaire.setText(if (existingProduct.prixUnitaire > 0) existingProduct.prixUnitaire.toString() else "")
            dialogBinding.textDialogTitle.text = getString(R.string.edit_product)
        }

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.save), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        layoutName?.error = null
                        layoutSurface?.error = null
                        layoutPrix?.error = null
                        val name = dialogBinding.editName.text.toString().trim()
                        val surface = dialogBinding.editSurface.text.toString().toDoubleOrNull() ?: 0.0
                        val prixUnitaire = dialogBinding.editPrixUnitaire.text.toString().toDoubleOrNull() ?: 0.0
                        if (name.isEmpty()) {
                            layoutName?.error = "Le nom du produit est requis"
                            dialogBinding.editName.requestFocus()
                            return@setOnClickListener
                        }
                        lifecycleScope.launch {
                            try {
                                if (existingProduct != null) {
                                    app.firestoreService.setAluProduct(existingProduct.id, existingProduct.copy(name = name, surface = surface, prixUnitaire = prixUnitaire))
                                } else {
                                    app.firestoreService.setAluProduct(null, AluProductFS(name = name, surface = surface, prixUnitaire = prixUnitaire))
                                }
                                dismiss()
                            } catch (e: Exception) {
                                layoutName?.error = "Erreur lors de l'enregistrement"
                            }
                        }
                    }
                }
                show()
            }
    }

    private fun deleteProduct(product: AluProductFS) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_product))
            .setMessage(getString(R.string.delete_product_confirm, product.name))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                lifecycleScope.launch { app.firestoreService.deleteAluProduct(product.id) }
                Toast.makeText(this, getString(R.string.product_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}

class AluProductAdapter(
    private val onEdit: (AluProductFS) -> Unit,
    private val onDelete: (AluProductFS) -> Unit
) : ListAdapter<AluProductFS, AluProductAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAluProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemAluProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: AluProductFS) {
            val format = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
            binding.textName.text = product.name
            binding.textSurface.text = "Surface: ${product.surface}"
            binding.textPrixUnitaire.text = format.format(product.prixUnitaire)
            binding.btnEdit.setOnClickListener { onEdit(product) }
            binding.btnDelete.setOnClickListener { onDelete(product) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AluProductFS>() {
        override fun areItemsTheSame(a: AluProductFS, b: AluProductFS) = a.id == b.id
        override fun areContentsTheSame(a: AluProductFS, b: AluProductFS) = a == b
    }
}
