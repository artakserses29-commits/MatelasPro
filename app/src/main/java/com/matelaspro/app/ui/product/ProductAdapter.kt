package com.matelaspro.app.ui.product

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.matelaspro.app.data.entity.Product
import com.matelaspro.app.databinding.ItemProductBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

class ProductAdapter(
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            val format = NumberFormat.getCurrencyInstance().apply {
                currency = Currency.getInstance("MGA")
            }
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("fr", "FR"))
            binding.textProductName.text = product.name
            binding.textProductCategory.text = product.category
            val attrs = mutableListOf<String>()
            if (product.epaisseur > 0) attrs.add("${product.epaisseur} cm")
            if (product.longueur > 0) attrs.add("${product.longueur}m")
            if (product.description.isNotEmpty()) attrs.add(product.description)
            binding.textEpaisseur.text = if (attrs.isNotEmpty()) attrs.joinToString(" | ") else "-"
            binding.textProductQuantity.text = "Stock: ${product.quantity}"
            val unitPrice = if (product.category == "Pieces de bois") product.purchasePrice else product.sellingPrice
            binding.textPrixUnitaire.text = format.format(unitPrice)
            binding.textPrixTotal.text = format.format(unitPrice * product.quantity)
            binding.textProductDate.text = dateFormat.format(Date(product.createdAt))
            binding.btnEdit.setOnClickListener { onEdit(product) }
            binding.btnDelete.setOnClickListener { onDelete(product) }
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean =
            oldItem == newItem
    }
}
