package com.matelaspro.app.ui.payment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputLayout
import com.matelaspro.app.MainViewModel
import com.matelaspro.app.R
import com.matelaspro.app.data.firestore.PaymentFS
import com.matelaspro.app.databinding.ActivityPaymentBinding
import com.matelaspro.app.databinding.DialogPaymentBinding
import kotlinx.coroutines.launch

class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: PaymentAdapter
    private var paymentList: List<PaymentFS> = emptyList()

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
        adapter = PaymentAdapter()
        binding.recyclerViewPayments.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewPayments.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.fabAddPayment.setOnClickListener { showPaymentDialog() }
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.allPayments.collect { payments ->
                paymentList = payments
                adapter.submitList(payments)
            }
        }
    }

    private fun showPaymentDialog() {
        val dialogBinding = DialogPaymentBinding.inflate(layoutInflater)
        val layoutAmount = dialogBinding.editAmount.parent as? TextInputLayout
        val layoutDescription = dialogBinding.editDescription.parent as? TextInputLayout

        dialogBinding.editAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                layoutAmount?.error = null
                val text = s.toString()
                if (text.isNotEmpty()) {
                    val clean = text.replace("[^\\d]".toRegex(), "")
                    if (clean != text) {
                        dialogBinding.editAmount.removeTextChangedListener(this)
                        dialogBinding.editAmount.setText(clean)
                        dialogBinding.editAmount.setSelection(clean.length)
                        dialogBinding.editAmount.addTextChangedListener(this)
                    }
                }
            }
        })

        AlertDialog.Builder(this)
            .setTitle("Nouveau paiement")
            .setView(dialogBinding.root)
            .setPositiveButton("Enregistrer", null)
            .setNegativeButton("Annuler", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        layoutAmount?.error = null
                        layoutDescription?.error = null
                        val montantText = dialogBinding.editAmount.text.toString().trim()
                        if (montantText.isEmpty()) {
                            layoutAmount?.error = "Le montant est requis"
                            dialogBinding.editAmount.requestFocus()
                            return@setOnClickListener
                        }
                        val montant = montantText.toDoubleOrNull() ?: 0.0
                        if (montant <= 0) {
                            layoutAmount?.error = "Le montant doit être supérieur à 0"
                            dialogBinding.editAmount.requestFocus()
                            return@setOnClickListener
                        }
                        val type = if (dialogBinding.radioVersement.isChecked) "VERSEMENT" else "DEPENSE"
                        val description = dialogBinding.editDescription.text.toString().trim()
                        viewModel.insertPayment(montant, type, description)
                        Toast.makeText(this@PaymentActivity, "Paiement enregistré", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                }
                show()
            }
    }
}
