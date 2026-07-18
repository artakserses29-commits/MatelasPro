package com.matelaspro.app.ui.profit

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
import com.matelaspro.app.util.FormatUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProfitMonthBreakdownActivity : AppCompatActivity() {

    private lateinit var binding: com.matelaspro.app.databinding.ActivityProfitMonthBreakdownBinding
    private lateinit var app: MatelasProApp
    private var currentUserId: Long = 0
    private var isAdmin: Boolean = false
    private var monthStart: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = com.matelaspro.app.databinding.ActivityProfitMonthBreakdownBinding.inflate(layoutInflater)
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

        loadMonthProfit()
    }

    private fun loadMonthProfit() {
        lifecycleScope.launch {
            val allSales = withContext(Dispatchers.IO) { app.saleRepository.getAllSalesList() }
            val filtered = if (currentUserId == 0L) allSales else allSales.filter { it.userId == currentUserId }

            val cal = Calendar.getInstance()
            cal.timeInMillis = monthStart
            cal.add(Calendar.MONTH, 1)
            val monthEnd = cal.timeInMillis

            val monthSales = filtered.filter { it.saleDate >= monthStart && it.saleDate < monthEnd }

            val totalProfit = monthSales.sumOf { it.profit }
            binding.textMonthTotal.text = FormatUtil.montant(totalProfit)

            val sdfDay = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
            val byDay = monthSales.groupBy { sdfDay.format(Date(it.saleDate)) }
                .mapValues { (_, list) -> list.sumOf { it.profit } }
                .entries.sortedByDescending { it.key }

            binding.layoutMonthDays.removeAllViews()
            if (byDay.isEmpty()) {
                val tv = TextView(this@ProfitMonthBreakdownActivity).apply {
                    text = "Aucun profit ce mois"
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.grey_medium, theme))
                }
                binding.layoutMonthDays.addView(tv)
            } else {
                for ((day, dayProfit) in byDay) {
                    addClickableRow(binding.layoutMonthDays, day, FormatUtil.montant(dayProfit)) {
                        val parts = day.split("/")
                        val cal2 = Calendar.getInstance()
                        cal2.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt(), 0, 0, 0)
                        cal2.set(Calendar.MILLISECOND, 0)
                        val dayStart = cal2.timeInMillis
                        val intent = Intent(this@ProfitMonthBreakdownActivity, ProfitDayDetailActivity::class.java)
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
            setBackgroundColor(ContextCompat.getColor(this@ProfitMonthBreakdownActivity, R.color.grey_medium))
        }
        layout.addView(divider)
    }
}
