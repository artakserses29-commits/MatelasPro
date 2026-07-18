package com.matelaspro.app.ui.expense

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.R
import com.matelaspro.app.data.firestore.ExpenseFS
import com.matelaspro.app.databinding.ActivityExpenseDashboardBinding
import com.matelaspro.app.service.SessionManager
import com.google.android.material.textfield.TextInputLayout
import com.matelaspro.app.util.FormatUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseDashboardBinding
    private lateinit var app: MatelasProApp
    private var currentUserId: String = ""
    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = intent.getStringExtra("selectedUserId") ?: SessionManager.currentUserId
        isAdmin = SessionManager.isAdmin

        app = application as MatelasProApp

        binding.btnBack.setOnClickListener { finish() }
        showSkeleton()
        binding.btnAddExpense.setOnClickListener {
            if (currentUserId.isEmpty()) {
                Toast.makeText(this, "Sélectionnez un utilisateur spécifique pour ajouter une dépense", Toast.LENGTH_SHORT).show()
            } else {
                showExpenseDialog()
            }
        }

        loadExpenses()
    }

    override fun onResume() {
        super.onResume()
        showSkeleton()
        loadExpenses()
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            try {
                val expenses = getExpenses()
                updateToday(expenses)
                updateMonth(expenses)
                renderDailyBreakdown(expenses)
                renderMonthlyBreakdown(expenses)
                hideSkeleton()
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                Toast.makeText(this@ExpenseDashboardActivity, "Erreur: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun getExpenses(): List<ExpenseFS> {
        return if (currentUserId.isEmpty()) {
            app.firestoreService.getAllExpenses()
        } else {
            app.firestoreService.getExpensesByUserList(currentUserId)
        }
    }

    private fun updateToday(expenses: List<ExpenseFS>) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis
        val todayTotal = expenses.filter { (it.createdAt?.toDate()?.time ?: 0L) in startOfDay..endOfDay }.sumOf { it.amount }
        binding.textTodayExpense.text = FormatUtil.montant(todayTotal)
        binding.cardToday.setOnClickListener {
            val intent = Intent(this, ExpenseDayDetailActivity::class.java)
            intent.putExtra("dayStart", startOfDay)
            intent.putExtra("selectedUserId", currentUserId)
            startActivity(intent)
        }
    }

    private fun updateMonth(expenses: List<ExpenseFS>) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfMonth = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val startOfNextMonth = cal.timeInMillis
        val monthTotal = expenses.filter { (it.createdAt?.toDate()?.time ?: 0L) in startOfMonth until startOfNextMonth }.sumOf { it.amount }
        binding.textMonthExpense.text = FormatUtil.montant(monthTotal)
        binding.cardMonth.setOnClickListener {
            val intent = Intent(this, ExpenseMonthBreakdownActivity::class.java)
            intent.putExtra("monthStart", startOfMonth)
            intent.putExtra("selectedUserId", currentUserId)
            startActivity(intent)
        }
    }

    private fun renderDailyBreakdown(expenses: List<ExpenseFS>) {
        binding.layoutDailyExpenses.removeAllViews()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayStartMillis = mutableListOf<Long>()
        for (i in 0 until 7) {
            val dayCal = Calendar.getInstance()
            dayCal.timeInMillis = todayStart - i * 86400000L
            val dayStart = dayCal.apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = dayStart + 86400000L
            val dayTotal = expenses.filter { val d = it.createdAt?.toDate()?.time ?: 0L; d >= dayStart && d < dayEnd }.sumOf { it.amount }
            if (dayTotal > 0) {
                dayStartMillis.add(dayStart)
                val dayLabel = sdf.format(Date(dayStart))
                addClickableRow(binding.layoutDailyExpenses, dayLabel, FormatUtil.montant(dayTotal)) {
                    val intent = Intent(this, ExpenseDayDetailActivity::class.java)
                    intent.putExtra("dayStart", dayStart)
                    intent.putExtra("selectedUserId", currentUserId)
                    startActivity(intent)
                }
            }
        }
        if (dayStartMillis.isEmpty()) {
            addText(binding.layoutDailyExpenses, "Aucune dépense ces 7 derniers jours")
        }
    }

    private fun renderMonthlyBreakdown(expenses: List<ExpenseFS>) {
        binding.layoutMonthlyExpenses.removeAllViews()
        val sdf = SimpleDateFormat("MM/yyyy", Locale.FRANCE)
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)

        var hasData = false
        for (month in 0 until 12) {
            val monthCal = Calendar.getInstance()
            monthCal.set(currentYear, month, 1, 0, 0, 0)
            monthCal.set(Calendar.MILLISECOND, 0)
            val startOfMonth = monthCal.timeInMillis
            monthCal.add(Calendar.MONTH, 1)
            val startOfNextMonth = monthCal.timeInMillis
            val monthTotal = expenses.filter { val d = it.createdAt?.toDate()?.time ?: 0L; d >= startOfMonth && d < startOfNextMonth }.sumOf { it.amount }
            if (monthTotal > 0) {
                hasData = true
                val monthLabel = sdf.format(Date(startOfMonth))
                addClickableRow(binding.layoutMonthlyExpenses, monthLabel, FormatUtil.montant(monthTotal)) {
                    val intent = Intent(this, ExpenseMonthBreakdownActivity::class.java)
                    intent.putExtra("monthStart", startOfMonth)
                    intent.putExtra("selectedUserId", currentUserId)
                    startActivity(intent)
                }
            }
        }
        if (!hasData) {
            addText(binding.layoutMonthlyExpenses, "Aucune dépense cette année")
        }
    }

    private fun showExpenseDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_expense, null)
        val etAmount = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etAmount)
        val etDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDescription)
        val etDate = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDate)
        val layoutAmount = etAmount.parent as? TextInputLayout
        val layoutDescription = etDescription.parent as? TextInputLayout
        val layoutDate = etDate.parent as? TextInputLayout

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
        etDate.setText(sdf.format(Date()))
        etDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this,
                { _, year, month, dayOfMonth ->
                    val picked = Calendar.getInstance()
                    picked.set(year, month, dayOfMonth)
                    TimePickerDialog(this,
                        { _, hour, minute ->
                            picked.set(Calendar.HOUR_OF_DAY, hour)
                            picked.set(Calendar.MINUTE, minute)
                            etDate.setText(sdf.format(picked.time))
                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
                    .show()
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            .show()
        }

        FormatUtil.applyMoneyFormat(etAmount)

        AlertDialog.Builder(this)
            .setTitle("Nouvelle dépense")
            .setView(dialogView)
            .setPositiveButton("Enregistrer", null)
            .setNegativeButton("Annuler", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        layoutAmount?.error = null
                        layoutDescription?.error = null
                        layoutDate?.error = null
                        val amountText = etAmount.text.toString().trim()
                        val description = etDescription.text.toString().trim()
                        val dateStr = etDate.text.toString().trim()

                        if (amountText.isEmpty()) {
                            layoutAmount?.error = "Le montant est requis"
                            etAmount.requestFocus()
                            return@setOnClickListener
                        }
                        val amount = FormatUtil.parseMontant(amountText)
                        if (amount == null || amount <= 0) {
                            layoutAmount?.error = "Le montant doit être supérieur à 0"
                            etAmount.requestFocus()
                            return@setOnClickListener
                        }
                        if (description.isEmpty()) {
                            layoutDescription?.error = "La description est requise"
                            etDescription.requestFocus()
                            return@setOnClickListener
                        }
                        if (dateStr.isEmpty()) {
                            layoutDate?.error = "La date est requise"
                            etDate.requestFocus()
                            return@setOnClickListener
                        }

                        var timestamp = System.currentTimeMillis()
                        try {
                            timestamp = sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
                        } catch (_: Exception) {}

                        lifecycleScope.launch {
                            try {
                                val currentBalance = getCurrentBalance()
                                if (currentBalance - amount < 0) {
                                    layoutAmount?.error = "Balance insuffisante (${FormatUtil.montant(currentBalance)})"
                                    return@launch
                                }
                                app.firestoreService.setExpense(null,
                                    ExpenseFS(
                                        userId = currentUserId,
                                        amount = amount,
                                        description = description,
                                        createdAt = com.google.firebase.Timestamp(timestamp / 1000, 0)
                                    )
                                )
                                loadExpenses()
                                Toast.makeText(this@ExpenseDashboardActivity, "Dépense enregistrée", Toast.LENGTH_SHORT).show()
                                dismiss()
                            } catch (_: CancellationException) {
                            } catch (e: Exception) {
                                layoutAmount?.error = "Erreur lors de l'enregistrement"
                            }
                        }
                    }
                }
                show()
            }
    }

    private suspend fun getCurrentBalance(): Double {
        return try {
            val soldeInitial = withContext(Dispatchers.IO) {
                val prefs = getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE)
                prefs.getFloat("solde_initial", 0f).toDouble()
            }
            val sales = app.firestoreService.getAllSales()
            val expenses = app.firestoreService.getAllExpenses()
            val paiements = app.firestoreService.getAllPaiements()
            soldeInitial + sales.sumOf { it.totalAmount } - expenses.sumOf { it.amount } - paiements.sumOf { it.montant }
        } catch (_: CancellationException) {
            throw CancellationException()
        } catch (_: Exception) {
            0.0
        }
    }

    private fun showSkeleton() {
        binding.skeleton.root.apply {
            visibility = View.VISIBLE
            startShimmer()
        }
    }

    private fun hideSkeleton() {
        binding.skeleton.root.apply {
            stopShimmer()
            visibility = View.GONE
        }
    }

    private fun addClickableRow(layout: LinearLayout, title: String, subtitle: String, onClick: () -> Unit) {
        val inflater = layoutInflater
        val row = inflater.inflate(R.layout.item_dashboard_row, null) as LinearLayout
        row.findViewById<TextView>(R.id.text_row_title).text = title
        row.findViewById<TextView>(R.id.text_row_subtitle).text = subtitle
        row.isClickable = true
        row.isFocusable = true
        val outValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        row.foreground = ContextCompat.getDrawable(this, outValue.resourceId)
        row.setOnClickListener { onClick() }
        layout.addView(row)
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { setMargins(0, 4, 0, 4) }
            setBackgroundColor(ContextCompat.getColor(this@ExpenseDashboardActivity, R.color.grey_medium))
        }
        layout.addView(divider)
    }

    private fun addText(layout: LinearLayout, text: String) {
        val tv = TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.grey_medium))
        }
        layout.addView(tv)
    }
}
