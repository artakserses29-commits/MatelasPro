package com.matelaspro.app.ui.payment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.matelaspro.app.data.entity.Payment
import com.matelaspro.app.databinding.ItemPaymentBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

class PaymentAdapter(
    private val onDelete: (Payment) -> Unit
) : ListAdapter<Payment, PaymentAdapter.PaymentViewHolder>(PaymentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding = ItemPaymentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PaymentViewHolder(private val binding: ItemPaymentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(payment: Payment) {
            val currencyFormat = NumberFormat.getCurrencyInstance().apply {
                currency = Currency.getInstance("MGA")
            }
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("fr", "FR"))

            binding.textPaymentType.text = payment.type
            binding.textAmount.text = currencyFormat.format(payment.amount)
            binding.textDescription.text = payment.description.ifEmpty { "Aucune description" }
            binding.textDate.text = dateFormat.format(Date(payment.date))
            binding.btnDelete.setOnClickListener { onDelete(payment) }
        }
    }

    class PaymentDiffCallback : DiffUtil.ItemCallback<Payment>() {
        override fun areItemsTheSame(oldItem: Payment, newItem: Payment): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Payment, newItem: Payment): Boolean =
            oldItem == newItem
    }
}
