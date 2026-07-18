package com.matelaspro.app.ui.product

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.matelaspro.app.data.firestore.ProductFS
import com.matelaspro.app.databinding.ItemProductBinding
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class ProductAdapter(
    private val onEdit: (ProductFS) -> Unit,
    private val onDelete: (ProductFS) -> Unit
) : ListAdapter<ProductFS, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(private val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: ProductFS) {
            val currencyFormat = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
            val nameExtra = when {
                product.category == "Matelas" && product.epaisseur > 0 -> " (${product.epaisseur}cm)"
                product.category == "Pieces de bois" && product.longueur > 0 -> " (${product.longueur}m)"
                product.category == "Meuble" && product.description.isNotEmpty() -> " (${product.description})"
                else -> ""
            }
            val attrText = when {
                product.category == "Matelas" && product.epaisseur > 0 -> "${product.epaisseur}cm"
                product.category == "Pieces de bois" && product.longueur > 0 -> "${product.longueur}m"
                product.description.isNotEmpty() -> product.description
                else -> ""
            }

            binding.textProductName.text = product.name + nameExtra
            binding.textProductCategory.text = product.category.ifEmpty { "Général" }
            binding.textProductQuantity.text = "Stock: ${product.quantity}"
            binding.textPrixUnitaire.text = "PA: ${currencyFormat.format(product.purchasePrice)} | PV: ${currencyFormat.format(product.sellingPrice)}"
            binding.textPrixTotal.text = "Val: ${currencyFormat.format(product.quantity * product.sellingPrice)}"
            binding.textEpaisseur.text = attrText
            binding.textProductDate.text = product.fournisseur.ifEmpty { "Fournisseur inconnu" }

            binding.btnEdit.setOnClickListener { onEdit(product) }
            binding.btnDelete.setOnClickListener { onDelete(product) }
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<ProductFS>() {
        override fun areItemsTheSame(oldItem: ProductFS, newItem: ProductFS): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ProductFS, newItem: ProductFS): Boolean = oldItem == newItem
    }
}
