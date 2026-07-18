package com.matelaspro.app.ui.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.R
import com.matelaspro.app.data.firestore.ExpenseFS
import com.matelaspro.app.data.firestore.FournisseurPaiementFS
import com.matelaspro.app.data.firestore.ProductFS
import com.matelaspro.app.data.firestore.SaleFS
import com.matelaspro.app.databinding.ActivityDashboardBinding
import com.matelaspro.app.service.SessionManager
import com.matelaspro.app.ui.sale.SaleDayDetailActivity
import com.matelaspro.app.ui.sale.SaleMonthBreakdownActivity
import com.matelaspro.app.util.FormatUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var app: MatelasProApp
    private var currentUserId: String = ""
    private var detailUserId: String = ""
    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = SessionManager.currentUserId
        isAdmin = SessionManager.isAdmin
        detailUserId = if (isAdmin) "" else currentUserId

        app = application as MatelasProApp
        setupListeners()
        observeData()

        if (!isAdmin) showUserMode()
        loadAllData()
    }

    override fun onResume() {
        super.onResume()
        loadAllData()
    }

    private fun showUserMode() {
        binding.cardStock.visibility = View.GONE
        binding.labelLowStock.visibility = View.GONE
        binding.layoutLowStock.visibility = View.GONE
        binding.labelFournisseurDebt.visibility = View.GONE
        binding.layoutFournisseurDebt.visibility = View.GONE
        binding.btnAuditLog.visibility = View.GONE
        binding.cardBalanceActuelle.visibility = View.GONE
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        if (isAdmin) {
            binding.btnAuditLog.setOnClickListener { showAuditLog() }
        }
    }

    private fun observeData() {
        if (isAdmin) {
            lifecycleScope.launch {
                app.firestoreService.productsFlow().collect { products ->
                    val totalValue = products.sumOf { it.sellingPrice * it.quantity }
                    binding.textStockValue.text = FormatUtil.montant(totalValue)
                }
            }
            lifecycleScope.launch {
                app.firestoreService.lowStockFlow().collect { list ->
                    renderLowStock(list)
                }
            }
        }
    }

    private fun loadAllData() {
        lifecycleScope.launch {
            val allSales = getSales()
            val allExpenses = getExpenses()
            val allPaiements = getAllPaiements()
            updateToday(allSales, allExpenses)
            updateMonth(allSales, allExpenses)
            renderDailyBreakdown(allSales, allExpenses)
            renderMonthlyBreakdown(allSales, allExpenses)
            renderYearTotal(allSales, allExpenses)
            renderBalanceActuelle(allSales, allExpenses, allPaiements)
            renderWeeklyProfit(allSales)
            if (isAdmin) loadFournisseurDebt()
        }
    }

    private suspend fun getSales(): List<SaleFS> {
        val all = app.firestoreService.getAllSales()
        return if (isAdmin) all else all.filter { it.userId == currentUserId }
    }

    private suspend fun getExpenses(): List<ExpenseFS> {
        val all = app.firestoreService.getAllExpenses()
        return if (isAdmin) all else all.filter { it.userId == currentUserId }
    }

    private suspend fun getAllPaiements(): List<FournisseurPaiementFS> {
        return app.firestoreService.getAllPaiements()
    }

    private fun updateToday(sales: List<SaleFS>, expenses: List<ExpenseFS>) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis

        val daySales = sales.filter { (it.saleDate?.toDate()?.time ?: 0L) in startOfDay..endOfDay }
        val dayExpenses = expenses.filter { (it.createdAt?.toDate()?.time ?: 0L) in startOfDay..endOfDay }

        val sTotal = daySales.sumOf { it.totalAmount }
        val pTotal = daySales.sumOf { it.profit }
        val eTotal = dayExpenses.sumOf { it.amount }
        val bal = sTotal - eTotal

        binding.textTodaySales.text = FormatUtil.montant(sTotal)
        binding.textTodayProfit.text = FormatUtil.montant(pTotal)
        binding.textTodayExpense.text = FormatUtil.montant(eTotal)
        binding.textTodayBalance.text = FormatUtil.montant(bal)

        binding.cardToday.setOnClickListener {
            val intent = Intent(this, SaleDayDetailActivity::class.java)
            intent.putExtra("dayStart", startOfDay)
            intent.putExtra("selectedUserId", detailUserId)
            startActivity(intent)
        }
    }

    private fun updateMonth(sales: List<SaleFS>, expenses: List<ExpenseFS>) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startOfMonth = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val startOfNextMonth = cal.timeInMillis

        val monthSales = sales.filter { (it.saleDate?.toDate()?.time ?: 0L) in startOfMonth until startOfNextMonth }
        val monthExpenses = expenses.filter { (it.createdAt?.toDate()?.time ?: 0L) in startOfMonth until startOfNextMonth }

        val sTotal = monthSales.sumOf { it.totalAmount }
        val pTotal = monthSales.sumOf { it.profit }
        val eTotal = monthExpenses.sumOf { it.amount }
        val bal = sTotal - eTotal

        binding.textMonthSales.text = FormatUtil.montant(sTotal)
        binding.textMonthProfit.text = FormatUtil.montant(pTotal)
        binding.textMonthExpense.text = FormatUtil.montant(eTotal)
        binding.textMonthBalance.text = FormatUtil.montant(bal)

        binding.cardMonth.setOnClickListener {
            val intent = Intent(this, SaleMonthBreakdownActivity::class.java)
            intent.putExtra("monthStart", startOfMonth)
            intent.putExtra("selectedUserId", detailUserId)
            startActivity(intent)
        }
    }

    private fun renderDailyBreakdown(sales: List<SaleFS>, expenses: List<ExpenseFS>) {
        binding.layoutLast7Days.removeAllViews()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        var hasData = false
        for (i in 0 until 7) {
            val dayStartMs = todayStart - i * 86400000L
            val dayEndMs = dayStartMs + 86400000L
            val daySales = sales.filter { (it.saleDate?.toDate()?.time ?: 0L) >= dayStartMs && (it.saleDate?.toDate()?.time ?: 0L) < dayEndMs }
            val dayExpenses = expenses.filter { (it.createdAt?.toDate()?.time ?: 0L) >= dayStartMs && (it.createdAt?.toDate()?.time ?: 0L) < dayEndMs }
            val sTotal = daySales.sumOf { it.totalAmount }
            val pTotal = daySales.sumOf { it.profit }
            val eTotal = dayExpenses.sumOf { it.amount }
            val bal = sTotal - eTotal
            if (sTotal > 0 || pTotal > 0 || eTotal > 0) {
                hasData = true
                val label = sdf.format(Date(dayStartMs))
                addPeriodBlock(binding.layoutLast7Days, label, sTotal, pTotal, eTotal, bal, dayStartMs)
            }
        }
        if (!hasData) {
            addText(binding.layoutLast7Days, "Aucune activité ces 7 derniers jours")
        }
    }

    private fun renderMonthlyBreakdown(sales: List<SaleFS>, expenses: List<ExpenseFS>) {
        binding.layoutMonthlyBreakdown.removeAllViews()
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
            val monthSales = sales.filter { (it.saleDate?.toDate()?.time ?: 0L) >= startOfMonth && (it.saleDate?.toDate()?.time ?: 0L) < startOfNextMonth }
            val monthExpenses = expenses.filter { (it.createdAt?.toDate()?.time ?: 0L) >= startOfMonth && (it.createdAt?.toDate()?.time ?: 0L) < startOfNextMonth }
            val sTotal = monthSales.sumOf { it.totalAmount }
            val pTotal = monthSales.sumOf { it.profit }
            val eTotal = monthExpenses.sumOf { it.amount }
            val bal = sTotal - eTotal
            if (sTotal > 0 || pTotal > 0 || eTotal > 0) {
                hasData = true
                val label = sdf.format(Date(startOfMonth))
                addMonthBlock(binding.layoutMonthlyBreakdown, label, sTotal, pTotal, eTotal, bal, startOfMonth)
            }
        }
        if (!hasData) {
            addText(binding.layoutMonthlyBreakdown, "Aucune activité cette année")
        }
    }

    private fun renderYearTotal(sales: List<SaleFS>, expenses: List<ExpenseFS>) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val yearStart = cal.timeInMillis
        cal.set(Calendar.DAY_OF_YEAR, 366); cal.add(Calendar.YEAR, 1)
        val yearEnd = cal.timeInMillis

        val yearSales = sales.filter { (it.saleDate?.toDate()?.time ?: 0L) >= yearStart && (it.saleDate?.toDate()?.time ?: 0L) < yearEnd }
        val yearExpenses = expenses.filter { (it.createdAt?.toDate()?.time ?: 0L) >= yearStart && (it.createdAt?.toDate()?.time ?: 0L) < yearEnd }

        binding.textYearSales.text = FormatUtil.montant(yearSales.sumOf { it.totalAmount })
        binding.textYearProfit.text = FormatUtil.montant(yearSales.sumOf { it.profit })
        binding.textYearExpense.text = FormatUtil.montant(yearExpenses.sumOf { it.amount })
        binding.textYearBalance.text = FormatUtil.montant(
            yearSales.sumOf { it.totalAmount } - yearExpenses.sumOf { it.amount }
        )
    }

    private fun renderBalanceActuelle(sales: List<SaleFS>, expenses: List<ExpenseFS>, paiements: List<FournisseurPaiementFS>) {
        val prefs = getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE)
        val soldeInitial = prefs.getFloat("solde_initial", 0f).toDouble()
        val totalSales = sales.sumOf { it.totalAmount }
        val totalExpenses = expenses.sumOf { it.amount }
        val totalPaiements = paiements.sumOf { it.montant }
        val balance = soldeInitial + totalSales - totalExpenses - totalPaiements

        binding.textBalanceActuelle.text = FormatUtil.montant(balance)
        val color = if (balance >= 0) R.color.green else R.color.red
        binding.textBalanceActuelle.setTextColor(ContextCompat.getColor(this, color))
    }

    private fun renderWeeklyProfit(sales: List<SaleFS>) {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.SUNDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val weekStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_WEEK, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        val weekEnd = cal.timeInMillis

        val weekSales = sales.filter { (it.saleDate?.toDate()?.time ?: 0L) in weekStart..weekEnd }
        val weekProfit = weekSales.sumOf { it.profit }
        val tenth = weekProfit / 10.0

        binding.textWeeklyProfit.text = FormatUtil.montant(weekProfit)
        binding.textWeeklyProfitTenth.text = FormatUtil.montant(tenth)
    }

    private fun renderLowStock(list: List<ProductFS>) {
        binding.layoutLowStock.removeAllViews()
        if (list.isEmpty()) { addText(binding.layoutLowStock, "Aucun produit en stock faible"); return }
        for (item in list) {
            val extra = categoryExtra(item.category, item.epaisseur, item.longueur, item.description)
            val row = createRow("${item.name}$extra", "Stock: ${item.quantity}")
            val red = ContextCompat.getColor(this, R.color.red)
            row.findViewById<TextView>(R.id.text_row_title)?.setTextColor(red)
            row.findViewById<TextView>(R.id.text_row_subtitle)?.setTextColor(red)
            binding.layoutLowStock.addView(row); addDivider(binding.layoutLowStock)
        }
    }

    private fun loadFournisseurDebt() {
        lifecycleScope.launch {
            val allEntries = app.firestoreService.getAllCreditEntries()
            val creditMap = mutableMapOf<String, Double>()
            for (e in allEntries) {
                if (e.quantity > 0 && !e.isOverridden) creditMap[e.fournisseur] = (creditMap[e.fournisseur] ?: 0.0) + e.quantity * e.sellingPrice
            }
            val allPaiementList = app.firestoreService.getAllPaiements()
            val paiementMap = mutableMapOf<String, Double>()
            for (p in allPaiementList) paiementMap[p.fournisseurId] = (paiementMap[p.fournisseurId] ?: 0.0) + p.montant
            val fournisseurs = app.firestoreService.allFournisseursFlow().first()

            binding.layoutFournisseurDebt.removeAllViews()
            val sortedCredits = creditMap.filter { it.value > 0 }.entries.sortedByDescending { it.value }
            if (sortedCredits.isEmpty()) { addText(binding.layoutFournisseurDebt, "Aucune dette fournisseur"); return@launch }
            for ((fournisseur, totalCredit) in sortedCredits) {
                val totalPaye = fournisseurs.filter { it.name == fournisseur }.sumOf { f -> paiementMap[f.id] ?: 0.0 }
                val reste = totalCredit - totalPaye
                val color = if (reste > 0) ContextCompat.getColor(this@DashboardActivity, R.color.red) else ContextCompat.getColor(this@DashboardActivity, R.color.green)
                val row = createRow(fournisseur, "Dû: ${FormatUtil.montant(reste)}")
                row.findViewById<TextView>(R.id.text_row_title)?.setTextColor(color)
                binding.layoutFournisseurDebt.addView(row); addDivider(binding.layoutFournisseurDebt)
            }
        }
    }

    private fun showAuditLog() {
        lifecycleScope.launch {
            val logs = app.firestoreService.getAllAuditLogs()
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
            val sb = StringBuilder()
            if (logs.isEmpty()) sb.append("Aucun événement d'audit")
            else for (log in logs.take(50)) {
                sb.append("[${sdf.format(Date(log.createdAt?.toDate()?.time ?: 0L))}] ${log.action} - ${log.tableName}#${log.recordId}\n")
                if (log.detail.isNotEmpty()) sb.append("  ${log.detail}\n"); sb.append("\n")
            }
            android.app.AlertDialog.Builder(this@DashboardActivity).setTitle("Journal d'audit").setMessage(sb.toString()).setPositiveButton("OK", null).show()
        }
    }

    private fun categoryExtra(category: String, epaisseur: Double, longueur: Double, description: String): String = when {
        category == "Matelas" && epaisseur > 0 -> " | ${epaisseur}cm"
        category == "Pieces de bois" && longueur > 0 -> " | ${longueur}m"
        category == "Meuble" && description.isNotEmpty() -> " | $description"
        else -> ""
    }

    private fun addPeriodBlock(layout: LinearLayout, title: String, sTotal: Double, pTotal: Double, eTotal: Double, bal: Double, dayStartMs: Long) {
        val block = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val titleRow = TextView(this).apply {
            text = title
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.primary))
            setPadding(0, 8, 0, 4)
            isClickable = true
            isFocusable = true
            val outValue = TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            foreground = ContextCompat.getDrawable(this@DashboardActivity, outValue.resourceId)
            setOnClickListener {
                val intent = Intent(this@DashboardActivity, SaleDayDetailActivity::class.java)
                intent.putExtra("dayStart", dayStartMs)
                intent.putExtra("selectedUserId", detailUserId)
                startActivity(intent)
            }
        }
        block.addView(titleRow)

        block.addView(createMetric2x2("Ventes", sTotal, R.color.primary, "Bénéfice", pTotal, R.color.green))
        block.addView(createMetric2x2("Dépenses", eTotal, R.color.red, "Balance", bal, R.color.primary))

        layout.addView(block)
        addDivider(layout)
    }

    private fun addMonthBlock(layout: LinearLayout, title: String, sTotal: Double, pTotal: Double, eTotal: Double, bal: Double, monthStartMs: Long) {
        val block = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val titleRow = TextView(this).apply {
            text = title
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.primary))
            setPadding(0, 8, 0, 4)
            isClickable = true
            isFocusable = true
            val outValue = TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            foreground = ContextCompat.getDrawable(this@DashboardActivity, outValue.resourceId)
            setOnClickListener {
                val intent = Intent(this@DashboardActivity, SaleMonthBreakdownActivity::class.java)
                intent.putExtra("monthStart", monthStartMs)
                intent.putExtra("selectedUserId", detailUserId)
                startActivity(intent)
            }
        }
        block.addView(titleRow)

        block.addView(createMetric2x2("Ventes", sTotal, R.color.primary, "Bénéfice", pTotal, R.color.green))
        block.addView(createMetric2x2("Dépenses", eTotal, R.color.red, "Balance", bal, R.color.primary))

        layout.addView(block)
        addDivider(layout)
    }

    private fun createMetric2x2(label1: String, amount1: Double, color1: Int, label2: String, amount2: Double, color2: Int): View {
        val row = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.HORIZONTAL
        }
        row.addView(createMetricCell(label1, amount1, color1))
        row.addView(createMetricCell(label2, amount2, color2))
        return row
    }

    private fun createMetricCell(label: String, amount: Double, colorRes: Int): View {
        val cell = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }
        cell.addView(TextView(this).apply {
            text = label
            textSize = 10f
            setTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.grey_medium))
        })
        cell.addView(TextView(this).apply {
            text = FormatUtil.montant(amount)
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@DashboardActivity, colorRes))
        })
        return cell
    }

    private fun addText(layout: LinearLayout, text: String) {
        layout.addView(TextView(this).apply {
            this.text = text; textSize = 13f
            setTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.grey_medium))
        })
    }

    private fun createRow(title: String, subtitle: String): View {
        val row = layoutInflater.inflate(R.layout.item_dashboard_row, null) as LinearLayout
        row.findViewById<TextView>(R.id.text_row_title).text = title
        row.findViewById<TextView>(R.id.text_row_subtitle).text = subtitle
        return row
    }

    private fun addDivider(layout: LinearLayout) {
        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { setMargins(0, 4, 0, 4) }
            setBackgroundColor(ContextCompat.getColor(this@DashboardActivity, R.color.grey_medium))
        })
    }
}
