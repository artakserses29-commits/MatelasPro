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
import com.matelaspro.app.data.entity.Sale
import com.matelaspro.app.databinding.ActivityProfitDashboardBinding
import com.matelaspro.app.util.FormatUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProfitDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfitDashboardBinding
    private lateinit var app: MatelasProApp
    private var currentUserId: Long = 0
    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfitDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val loginPrefs = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        currentUserId = intent.getLongExtra("selectedUserId", loginPrefs.getLong("currentUserId", 0))
        isAdmin = loginPrefs.getBoolean("isAdmin", false)

        app = application as MatelasProApp

        binding.btnBack.setOnClickListener { finish() }

        loadProfit()
    }

    override fun onResume() {
        super.onResume()
        loadProfit()
    }

    private fun loadProfit() {
        lifecycleScope.launch {
            val sales = getSales()
            updateToday(sales)
            updateMonth(sales)
            renderDailyBreakdown(sales)
            renderMonthlyBreakdown(sales)
        }
    }

    private suspend fun getSales(): List<Sale> {
        val all = withContext(Dispatchers.IO) { app.saleRepository.getAllSalesList() }
        return if (currentUserId == 0L) all else all.filter { it.userId == currentUserId }
    }

    private fun updateToday(sales: List<Sale>) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis
        val todayProfit = sales.filter { it.saleDate in startOfDay..endOfDay }.sumOf { it.profit }
        binding.textTodayProfit.text = FormatUtil.montant(todayProfit)
        binding.cardToday.setOnClickListener {
            val intent = Intent(this, ProfitDayDetailActivity::class.java)
            intent.putExtra("dayStart", startOfDay)
            intent.putExtra("selectedUserId", currentUserId)
            startActivity(intent)
        }
    }

    private fun updateMonth(sales: List<Sale>) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startOfMonth = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val startOfNextMonth = cal.timeInMillis
        val monthProfit = sales.filter { it.saleDate in startOfMonth until startOfNextMonth }.sumOf { it.profit }
        binding.textMonthProfit.text = FormatUtil.montant(monthProfit)
        binding.cardMonth.setOnClickListener {
            val intent = Intent(this, ProfitMonthBreakdownActivity::class.java)
            intent.putExtra("monthStart", startOfMonth)
            intent.putExtra("selectedUserId", currentUserId)
            startActivity(intent)
        }
    }

    private fun renderDailyBreakdown(sales: List<Sale>) {
        binding.layoutDailyProfit.removeAllViews()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        var hasData = false
        for (i in 0 until 7) {
            val dayStart = todayStart - i * 86400000L
            val dayStartCal = Calendar.getInstance().apply {
                timeInMillis = dayStart
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val dayStartMs = dayStartCal.timeInMillis
            val dayEndMs = dayStartMs + 86400000L
            val dayProfit = sales.filter { it.saleDate >= dayStartMs && it.saleDate < dayEndMs }.sumOf { it.profit }
            if (dayProfit > 0) {
                hasData = true
                val dayLabel = sdf.format(Date(dayStartMs))
                addClickableRow(binding.layoutDailyProfit, dayLabel, FormatUtil.montant(dayProfit)) {
                    val intent = Intent(this, ProfitDayDetailActivity::class.java)
                    intent.putExtra("dayStart", dayStartMs)
                    intent.putExtra("selectedUserId", currentUserId)
                    startActivity(intent)
                }
            }
        }
        if (!hasData) {
            addText(binding.layoutDailyProfit, "Aucun profit ces 7 derniers jours")
        }
    }

    private fun renderMonthlyBreakdown(sales: List<Sale>) {
        binding.layoutMonthlyProfit.removeAllViews()
        val sdf = SimpleDateFormat("MM/yyyy", Locale.FRANCE)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        var hasData = false
        for (month in 0 until 12) {
            val monthCal = Calendar.getInstance()
            monthCal.set(currentYear, month, 1, 0, 0, 0)
            monthCal.set(Calendar.MILLISECOND, 0)
            val startOfMonth = monthCal.timeInMillis
            monthCal.add(Calendar.MONTH, 1)
            val startOfNextMonth = monthCal.timeInMillis
            val monthProfit = sales.filter { it.saleDate >= startOfMonth && it.saleDate < startOfNextMonth }.sumOf { it.profit }
            if (monthProfit > 0) {
                hasData = true
                val monthLabel = sdf.format(Date(startOfMonth))
                addClickableRow(binding.layoutMonthlyProfit, monthLabel, FormatUtil.montant(monthProfit)) {
                    val intent = Intent(this, ProfitMonthBreakdownActivity::class.java)
                    intent.putExtra("monthStart", startOfMonth)
                    intent.putExtra("selectedUserId", currentUserId)
                    startActivity(intent)
                }
            }
        }
        if (!hasData) {
            addText(binding.layoutMonthlyProfit, "Aucun profit cette année")
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
            setBackgroundColor(ContextCompat.getColor(this@ProfitDashboardActivity, R.color.grey_medium))
        }
        layout.addView(divider)
    }

    private fun addText(layout: LinearLayout, text: String) {
        val tv = TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@ProfitDashboardActivity, R.color.grey_medium))
        }
        layout.addView(tv)
    }
}
