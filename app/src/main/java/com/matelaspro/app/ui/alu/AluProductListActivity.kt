package com.matelaspro.app.ui.alu

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.matelaspro.app.R
import com.matelaspro.app.data.entity.AluProduct
import com.matelaspro.app.databinding.ActivityAluProductListBinding
import com.matelaspro.app.databinding.DialogAluProductBinding
import com.matelaspro.app.databinding.ItemAluProductBinding
import java.text.NumberFormat
import java.util.Currency

class AluProductListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAluProductListBinding
    private lateinit var viewModel: AluViewModel
    private lateinit var adapter: AluProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAluProductListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[AluViewModel::class.java]
        setupRecyclerView()
        setupListeners()
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
        viewModel.allProducts.observe(this) { products ->
            adapter.submitList(products)
            val empty = products.isEmpty()
            binding.textEmptyState.visibility = if (empty) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (empty) View.GONE else View.VISIBLE
        }
    }

    private fun showProductDialog(product: AluProduct?) {
        val dialogBinding = DialogAluProductBinding.inflate(layoutInflater)
        val existingProduct = product

        if (existingProduct != null) {
            dialogBinding.editName.setText(existingProduct.name)
            dialogBinding.editSurface.setText(if (existingProduct.surface > 0) existingProduct.surface.toString() else "")
            dialogBinding.editPrixUnitaire.setText(if (existingProduct.prixUnitaire > 0) existingProduct.prixUnitaire.toString() else "")
            dialogBinding.textDialogTitle.text = getString(R.string.edit_product)
        }

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = dialogBinding.editName.text.toString().trim()
                val surface = dialogBinding.editSurface.text.toString().toDoubleOrNull() ?: 0.0
                val prixUnitaire = dialogBinding.editPrixUnitaire.text.toString().toDoubleOrNull() ?: 0.0
                if (name.isEmpty()) {
                    Toast.makeText(this, getString(R.string.name_required), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (existingProduct != null) {
                    viewModel.updateProduct(existingProduct.copy(name = name, surface = surface, prixUnitaire = prixUnitaire))
                } else {
                    viewModel.insertProduct(name, surface, prixUnitaire)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteProduct(product: AluProduct) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_product))
            .setMessage(getString(R.string.delete_product_confirm, product.name))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteProduct(product)
                Toast.makeText(this, getString(R.string.product_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}

class AluProductAdapter(
    private val onEdit: (AluProduct) -> Unit,
    private val onDelete: (AluProduct) -> Unit
) : ListAdapter<AluProduct, AluProductAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAluProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemAluProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: AluProduct) {
            val format = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
            binding.textName.text = product.name
            binding.textSurface.text = "Surface: ${product.surface}"
            binding.textPrixUnitaire.text = format.format(product.prixUnitaire)
            binding.btnEdit.setOnClickListener { onEdit(product) }
            binding.btnDelete.setOnClickListener { onDelete(product) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AluProduct>() {
        override fun areItemsTheSame(a: AluProduct, b: AluProduct) = a.id == b.id
        override fun areContentsTheSame(a: AluProduct, b: AluProduct) = a == b
    }
}
