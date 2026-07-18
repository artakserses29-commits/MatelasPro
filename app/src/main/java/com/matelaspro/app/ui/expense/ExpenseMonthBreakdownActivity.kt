package com.matelaspro.app.ui.expense

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.R
import com.matelaspro.app.data.entity.Expense
import com.matelaspro.app.util.FormatUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseMonthBreakdownActivity : AppCompatActivity() {

    private lateinit var binding: com.matelaspro.app.databinding.ActivityExpenseMonthBreakdownBinding
    private lateinit var app: MatelasProApp
    private var currentUserId: Long = 0
    private var isAdmin: Boolean = false
    private var monthStart: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = com.matelaspro.app.databinding.ActivityExpenseMonthBreakdownBinding.inflate(layoutInflater)
        setContentView(binding.root)

        monthStart = intent.getLongExtra("monthStart", 0)
        if (monthStart == 0L) { finish(); return }

        val loginPrefs = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        currentUserId = intent.getLongExtra("selectedUserId", loginPrefs.getLong("currentUserId", 0))
        isAdmin = loginPrefs.getBoolean("isAdmin", false)

        app = application as MatelasProApp

        val sdf = SimpleDateFormat("MMMM yyyy", Locale.FRANCE)
        binding.toolbar.title = sdf.format(Date(monthStart)).replaceFirstChar { it.uppercase() }
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadMonthExpenses()
    }

    private fun loadMonthExpenses() {
        lifecycleScope.launch {
            val allExpenses = if (currentUserId == 0L) app.expenseRepository.getAllExpensesList()
            else app.expenseRepository.getExpensesByUserList(currentUserId)

            val cal = Calendar.getInstance()
            cal.timeInMillis = monthStart
            cal.add(Calendar.MONTH, 1)
            val monthEnd = cal.timeInMillis

            val monthExpenses = allExpenses.filter { it.createdAt >= monthStart && it.createdAt < monthEnd }

            val total = monthExpenses.sumOf { it.amount }
            binding.textMonthTotal.text = FormatUtil.montant(total)

            val sdfDay = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
            val byDay = monthExpenses.groupBy { sdfDay.format(Date(it.createdAt)) }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .entries.sortedByDescending { it.key }

            binding.layoutMonthDays.removeAllViews()
            if (byDay.isEmpty()) {
                val tv = TextView(this@ExpenseMonthBreakdownActivity).apply {
                    text = "Aucune dépense ce mois"
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.grey_medium, theme))
                }
                binding.layoutMonthDays.addView(tv)
            } else {
                for ((day, dayTotal) in byDay) {
                    addClickableRow(binding.layoutMonthDays, day, FormatUtil.montant(dayTotal)) {
                        val cal2 = Calendar.getInstance()
                        val parts = day.split("/")
                        cal2.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt(), 0, 0, 0)
                        cal2.set(Calendar.MILLISECOND, 0)
                        val dayStart = cal2.timeInMillis
                        val intent = Intent(this@ExpenseMonthBreakdownActivity, ExpenseDayDetailActivity::class.java)
                        intent.putExtra("dayStart", dayStart)
                        intent.putExtra("selectedUserId", currentUserId)
                        startActivity(intent)
                    }
                }
            }
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
            setBackgroundColor(ContextCompat.getColor(this@ExpenseMonthBreakdownActivity, R.color.grey_medium))
        }
        layout.addView(divider)
    }
}
