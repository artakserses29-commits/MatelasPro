package com.matelaspro.app.ui.sale

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.matelaspro.app.data.entity.Product
import com.matelaspro.app.data.entity.Sale
import com.matelaspro.app.databinding.ItemSaleBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

class SaleAdapter(
    private val onCancel: (Sale) -> Unit,
    productMap: Map<Long, Product> = emptyMap()
) : ListAdapter<Sale, SaleAdapter.SaleViewHolder>(SaleDiffCallback()) {
    var productMap: Map<Long, Product> = productMap
        set(value) { field = value; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaleViewHolder {
        val binding = ItemSaleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SaleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SaleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SaleViewHolder(private val binding: ItemSaleBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(sale: Sale) {
            val now = System.currentTimeMillis()
            val thirtyMinutes = 30 * 60 * 1000L
            val cancelAllowed = now - sale.saleDate <= thirtyMinutes

            val currencyFormat = NumberFormat.getCurrencyInstance().apply {
                currency = Currency.getInstance("MGA")
            }
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("fr", "FR"))
            val product = productMap[sale.productId]
            val extra = when {
                product == null -> ""
                product.category == "Matelas" && product.epaisseur > 0 -> " (${product.epaisseur}cm)"
                product.category == "Pieces de bois" && product.longueur > 0 -> " (${product.longueur}m)"
                product.category == "Meuble" && product.description.isNotEmpty() -> " (${product.description})"
                else -> ""
            }
            binding.textProductName.text = sale.productName + extra
            binding.textQuantity.text = "Qté: ${sale.quantity}"
            binding.textPrixUnitaireRef.text = "PU: ${currencyFormat.format(sale.purchasePrice)}"
            binding.textPrixVente.text = "PV: ${currencyFormat.format(sale.unitPrice)}"
            binding.textTotal.text = "Total: ${currencyFormat.format(sale.totalAmount)}"
            binding.textProfit.text = "Bénéfice: ${currencyFormat.format(sale.profit)}"
            binding.textDate.text = dateFormat.format(Date(sale.saleDate))
            binding.btnCancel.visibility = if (cancelAllowed) View.VISIBLE else View.GONE
            binding.btnCancel.setOnClickListener { onCancel(sale) }
        }
    }

    class SaleDiffCallback : DiffUtil.ItemCallback<Sale>() {
        override fun areItemsTheSame(oldItem: Sale, newItem: Sale): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Sale, newItem: Sale): Boolean =
            oldItem == newItem
    }
}
