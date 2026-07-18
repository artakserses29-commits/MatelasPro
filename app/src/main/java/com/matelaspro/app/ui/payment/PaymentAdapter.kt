package com.matelaspro.app.ui.payment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.matelaspro.app.data.firestore.PaymentFS
import com.matelaspro.app.databinding.ItemPaymentBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

class PaymentAdapter : ListAdapter<PaymentFS, PaymentAdapter.PaymentViewHolder>(PaymentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding = ItemPaymentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PaymentViewHolder(private val binding: ItemPaymentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(payment: PaymentFS) {
            val currencyFormat = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("fr", "FR"))
            binding.textAmount.text = currencyFormat.format(payment.amount)
            binding.textPaymentType.text = payment.type
            binding.textDescription.text = payment.description.ifEmpty { "Sans description" }
            binding.textDate.text = dateFormat.format(Date(payment.date?.toDate()?.time ?: 0L))
        }
    }

    class PaymentDiffCallback : DiffUtil.ItemCallback<PaymentFS>() {
        override fun areItemsTheSame(oldItem: PaymentFS, newItem: PaymentFS): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PaymentFS, newItem: PaymentFS): Boolean = oldItem == newItem
    }
}
