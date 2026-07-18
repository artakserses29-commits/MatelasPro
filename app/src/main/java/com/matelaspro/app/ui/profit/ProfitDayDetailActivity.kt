package com.matelaspro.app.ui.profit

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.R
import com.matelaspro.app.util.FormatUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfitDayDetailActivity : AppCompatActivity() {

    private lateinit var binding: com.matelaspro.app.databinding.ActivityProfitDayDetailBinding
    private lateinit var app: MatelasProApp
    private var currentUserId: Long = 0
    private var isAdmin: Boolean = false
    private var dayStart: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = com.matelaspro.app.databinding.ActivityProfitDayDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dayStart = intent.getLongExtra("dayStart", 0)
        if (dayStart == 0L) { finish(); return }

        val loginPrefs = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        currentUserId = intent.getLongExtra("selectedUserId", loginPrefs.getLong("currentUserId", 0))
        isAdmin = loginPrefs.getBoolean("isAdmin", false)

        app = application as MatelasProApp

        val sdf = SimpleDateFormat("EEEE dd/MM/yyyy", Locale.FRANCE)
        binding.toolbar.title = sdf.format(Date(dayStart))
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadDayProfit()
    }

    private fun loadDayProfit() {
        lifecycleScope.launch {
            val allSales = withContext(Dispatchers.IO) { app.saleRepository.getAllSalesList() }
            val filtered = if (currentUserId == 0L) allSales else allSales.filter { it.userId == currentUserId }

            val dayEnd = dayStart + 86400000L
            val daySales = filtered.filter { it.saleDate >= dayStart && it.saleDate < dayEnd }
                .sortedByDescending { it.saleDate }

            val totalProfit = daySales.sumOf { it.profit }
            binding.textDayTotal.text = FormatUtil.montant(totalProfit)

            binding.layoutDayProfit.removeAllViews()
            if (daySales.isEmpty()) {
                val tv = TextView(this@ProfitDayDetailActivity).apply {
                    text = "Aucun profit ce jour"
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.grey_medium, theme))
                }
                binding.layoutDayProfit.addView(tv)
            } else {
                val timeSdf = SimpleDateFormat("HH:mm", Locale.FRANCE)
                for (sale in daySales) {
                    val card = layoutInflater.inflate(R.layout.item_profit_detail_card, null)
                    card.findViewById<TextView>(R.id.textProductName).text = sale.productName
                    card.findViewById<TextView>(R.id.textQuantity).text = "Qté: ${sale.quantity}"
                    card.findViewById<TextView>(R.id.textProfit).text = FormatUtil.montant(sale.profit)
                    card.findViewById<TextView>(R.id.textAmount).text = "Vente: ${FormatUtil.montant(sale.totalAmount)}"
                    card.findViewById<TextView>(R.id.textTime).text = timeSdf.format(Date(sale.saleDate))
                    binding.layoutDayProfit.addView(card)
                }
            }
        }
    }
}
