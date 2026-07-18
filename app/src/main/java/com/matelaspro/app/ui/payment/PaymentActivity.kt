package com.matelaspro.app.ui.payment

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager

import com.matelaspro.app.MainViewModel
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.R
import com.matelaspro.app.data.entity.Payment
import com.matelaspro.app.databinding.ActivityPaymentBinding
import com.matelaspro.app.databinding.DialogPaymentBinding
import kotlinx.coroutines.launch

class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: PaymentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = PaymentAdapter(
            onDelete = { payment -> deletePayment(payment) }
        )
        binding.recyclerViewPayments.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewPayments.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.fabAddPayment.setOnClickListener { showPaymentDialog() }
    }

    private fun observeData() {
        viewModel.allPayments.observe(this) { payments ->
            adapter.submitList(payments)
            val empty = payments.isEmpty()
            binding.textEmptyState.visibility = if (empty) View.VISIBLE else View.GONE
            binding.recyclerViewPayments.visibility = if (empty) View.GONE else View.VISIBLE
        }
    }

    private fun showPaymentDialog() {
        val dialogBinding = DialogPaymentBinding.inflate(layoutInflater)

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val amount = dialogBinding.editAmount.text.toString().toDoubleOrNull() ?: 0.0
                val description = dialogBinding.editDescription.text.toString().trim()
                val selectedTypeId = dialogBinding.radioGroupType.checkedRadioButtonId
                val type = if (selectedTypeId == R.id.radioVersement) "VERSEMENT" else "DEPENSE"

                if (amount <= 0) {
                    Toast.makeText(this, getString(R.string.amount_invalid), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.insertPayment(amount, type, description)
                Toast.makeText(this, getString(R.string.payment_recorded), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deletePayment(payment: Payment) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer le paiement")
            .setMessage("Voulez-vous vraiment supprimer ce paiement ?")
            .setPositiveButton("Supprimer") { _, _ ->
                lifecycleScope.launch {
                    val app = application as MatelasProApp
                    app.auditLogRepository.insert("DELETE", "payments", payment.id, payment.description)
                    viewModel.deletePayment(payment)
                    Toast.makeText(this@PaymentActivity, "Supprimé", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}
