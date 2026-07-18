package com.matelaspro.app.ui.sale

import android.content.Context
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.R
import com.matelaspro.app.data.entity.Product
import com.matelaspro.app.util.FormatUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaleDayDetailActivity : AppCompatActivity() {

    private lateinit var binding: com.matelaspro.app.databinding.ActivitySaleDayDetailBinding
    private lateinit var app: MatelasProApp
    private var currentUserId: Long = 0
    private var dayStart: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = com.matelaspro.app.databinding.ActivitySaleDayDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dayStart = intent.getLongExtra("dayStart", 0)
        if (dayStart == 0L) { finish(); return }

        val loginPrefs = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        currentUserId = intent.getLongExtra("selectedUserId", loginPrefs.getLong("currentUserId", 0))

        app = application as MatelasProApp

        val sdf = SimpleDateFormat("EEEE dd/MM/yyyy", Locale.FRANCE)
        binding.toolbar.title = sdf.format(Date(dayStart))
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadDaySales()
    }

    private fun loadDaySales() {
        lifecycleScope.launch {
            val allSales = withContext(Dispatchers.IO) { app.saleRepository.getAllSalesList() }
            val filtered = if (currentUserId == 0L) allSales else allSales.filter { it.userId == currentUserId }
            val products = withContext(Dispatchers.IO) { app.productRepository.getAllProductsSuspend() }
            val productMap = products.associateBy { it.id }

            val dayEnd = dayStart + 86400000L
            val daySales = filtered.filter { it.saleDate >= dayStart && it.saleDate < dayEnd }
                .sortedByDescending { it.saleDate }

            val total = daySales.sumOf { it.totalAmount }
            binding.textDayTotal.text = FormatUtil.montant(total)

            binding.layoutDaySales.removeAllViews()
            if (daySales.isEmpty()) {
                val tv = TextView(this@SaleDayDetailActivity).apply {
                    text = "Aucune vente ce jour"
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.grey_medium, theme))
                }
                binding.layoutDaySales.addView(tv)
            } else {
                val timeSdf = SimpleDateFormat("HH:mm", Locale.FRANCE)
                for (sale in daySales) {
                    val card = layoutInflater.inflate(R.layout.item_sale_detail_card, null)
                    card.findViewById<TextView>(R.id.textProductName).text = sale.productName
                    val product = productMap[sale.productId]
                    val attrs = productAttrs(product)
                    card.findViewById<TextView>(R.id.textProductAttrs).text = attrs
                    card.findViewById<TextView>(R.id.textQuantity).text = "Qté: ${sale.quantity} x ${FormatUtil.montant(sale.unitPrice)}"
                    card.findViewById<TextView>(R.id.textAmount).text = FormatUtil.montant(sale.totalAmount)
                    card.findViewById<TextView>(R.id.textProfit).text = "Profit: ${FormatUtil.montant(sale.profit)}"
                    card.findViewById<TextView>(R.id.textTime).text = timeSdf.format(Date(sale.saleDate))
                    binding.layoutDaySales.addView(card)
                }
            }
        }
    }

    private fun productAttrs(product: Product?): String {
        if (product == null) return ""
        val parts = mutableListOf<String>()
        if (product.epaisseur > 0) parts.add("${product.epaisseur}cm")
        if (product.longueur > 0) parts.add("${product.longueur}m")
        if (product.description.isNotEmpty()) parts.add(product.description)
        return parts.joinToString(" | ")
    }
}
