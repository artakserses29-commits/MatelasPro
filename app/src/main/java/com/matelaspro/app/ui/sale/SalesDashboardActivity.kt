package com.matelaspro.app.ui.sale

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
import com.matelaspro.app.data.firestore.SaleFS
import com.matelaspro.app.databinding.ActivitySalesDashboardBinding
import com.matelaspro.app.service.SessionManager
import com.matelaspro.app.util.FormatUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SalesDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalesDashboardBinding
    private lateinit var app: MatelasProApp
    private var currentUserId: String = ""
    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = intent.getStringExtra("selectedUserId") ?: SessionManager.currentUserId
        isAdmin = SessionManager.isAdmin

        app = application as MatelasProApp

        binding.btnBack.setOnClickListener { finish() }
        showSkeleton()
        loadSales()
    }

    override fun onResume() {
        super.onResume()
        showSkeleton()
        loadSales()
    }

    private fun loadSales() {
        lifecycleScope.launch {
            val sales = getSales()
            updateToday(sales)
            updateMonth(sales)
            renderDailyBreakdown(sales)
            renderMonthlyBreakdown(sales)
            hideSkeleton()
        }
    }

    private suspend fun getSales(): List<SaleFS> {
        val all = app.firestoreService.getAllSales()
        return if (currentUserId.isEmpty()) all else all.filter { it.userId == currentUserId }
    }

    private fun updateToday(sales: List<SaleFS>) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis
        val todayTotal = sales.filter { (it.saleDate?.toDate()?.time ?: 0L) in startOfDay..endOfDay }.sumOf { it.totalAmount }
        binding.textTodaySales.text = FormatUtil.montant(todayTotal)
        binding.cardToday.setOnClickListener {
            val intent = Intent(this, SaleDayDetailActivity::class.java)
            intent.putExtra("dayStart", startOfDay)
            intent.putExtra("selectedUserId", currentUserId)
            startActivity(intent)
        }
    }

    private fun updateMonth(sales: List<SaleFS>) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startOfMonth = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val startOfNextMonth = cal.timeInMillis
        val monthTotal = sales.filter { (it.saleDate?.toDate()?.time ?: 0L) in startOfMonth until startOfNextMonth }.sumOf { it.totalAmount }
        binding.textMonthSales.text = FormatUtil.montant(monthTotal)
        binding.cardMonth.setOnClickListener {
            val intent = Intent(this, SaleMonthBreakdownActivity::class.java)
            intent.putExtra("monthStart", startOfMonth)
            intent.putExtra("selectedUserId", currentUserId)
            startActivity(intent)
        }
    }

    private fun renderDailyBreakdown(sales: List<SaleFS>) {
        binding.layoutDailySales.removeAllViews()
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
            val dayTotal = sales.filter { val d = it.saleDate?.toDate()?.time ?: 0L; d >= dayStartMs && d < dayEndMs }.sumOf { it.totalAmount }
            if (dayTotal > 0) {
                hasData = true
                val dayLabel = sdf.format(Date(dayStartMs))
                addClickableRow(binding.layoutDailySales, dayLabel, FormatUtil.montant(dayTotal)) {
                    val intent = Intent(this, SaleDayDetailActivity::class.java)
                    intent.putExtra("dayStart", dayStartMs)
                    intent.putExtra("selectedUserId", currentUserId)
                    startActivity(intent)
                }
            }
        }
        if (!hasData) {
            addText(binding.layoutDailySales, "Aucune vente ces 7 derniers jours")
        }
    }

    private fun renderMonthlyBreakdown(sales: List<SaleFS>) {
        binding.layoutMonthlySales.removeAllViews()
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
            val monthTotal = sales.filter { val d = it.saleDate?.toDate()?.time ?: 0L; d >= startOfMonth && d < startOfNextMonth }.sumOf { it.totalAmount }
            if (monthTotal > 0) {
                hasData = true
                val monthLabel = sdf.format(Date(startOfMonth))
                addClickableRow(binding.layoutMonthlySales, monthLabel, FormatUtil.montant(monthTotal)) {
                    val intent = Intent(this, SaleMonthBreakdownActivity::class.java)
                    intent.putExtra("monthStart", startOfMonth)
                    intent.putExtra("selectedUserId", currentUserId)
                    startActivity(intent)
                }
            }
        }
        if (!hasData) {
            addText(binding.layoutMonthlySales, "Aucune vente cette année")
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
            setBackgroundColor(ContextCompat.getColor(this@SalesDashboardActivity, R.color.grey_medium))
        }
        layout.addView(divider)
    }

    private fun addText(layout: LinearLayout, text: String) {
        val tv = TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@SalesDashboardActivity, R.color.grey_medium))
        }
        layout.addView(tv)
    }
}
