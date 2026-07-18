package com.matelaspro.app.ui.stock

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.R
import com.matelaspro.app.databinding.ActivityUserStockBinding
import com.matelaspro.app.service.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserStockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserStockBinding
    private lateinit var app: MatelasProApp
    private var currentUserId: String = ""
    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserStockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = SessionManager.currentUserId
        isAdmin = SessionManager.isAdmin

        app = application as MatelasProApp

        binding.toolbar.setNavigationOnClickListener { finish() }
        showSkeleton()
        loadStock()
    }

    private fun loadStock() {
        lifecycleScope.launch {
            val userStocks = app.firestoreService.getUserStockByUserId(currentUserId)
            val products = app.firestoreService.getAllProducts()

            binding.layoutStock.removeAllViews()

            if (userStocks.isEmpty()) {
                val tv = TextView(this@UserStockActivity).apply {
                    text = "Aucun stock disponible"
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.grey_medium, theme))
                }
                binding.layoutStock.addView(tv)
            } else {
                var total = 0
                for (us in userStocks.sortedByDescending { it.quantity }) {
                    val product = products.find { it.id == us.productId }
                    val name = product?.name ?: "Produit #${us.productId}"
                    val extra = when {
                        product?.category == "Matelas" && product.epaisseur > 0 -> " - ${product.epaisseur}cm"
                        product?.category == "Pieces de bois" && product.longueur > 0 -> " ${product.longueur.toLong()}m"
                        product?.category == "Meuble" && product.description.isNotEmpty() -> " - ${product.description}"
                        else -> ""
                    }
                    total += us.quantity
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
                    val dateStr = sdf.format(Date(us.updatedAt?.toDate()?.time ?: 0L))
                    val row = layoutInflater.inflate(R.layout.item_dashboard_row, null) as LinearLayout
                    row.findViewById<TextView>(R.id.text_row_title).text = "$name$extra"
                    row.findViewById<TextView>(R.id.text_row_subtitle).text = "Qté: ${us.quantity}  |  $dateStr"
                    binding.layoutStock.addView(row)
                    val divider = android.view.View(this@UserStockActivity).apply {
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1
                        ).apply { setMargins(0, 4, 0, 4) }
                        setBackgroundColor(resources.getColor(R.color.grey_medium, theme))
                    }
                    binding.layoutStock.addView(divider)
                }
                val tvTotal = android.widget.TextView(this@UserStockActivity).apply {
                    text = "Total articles: $total"
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(resources.getColor(R.color.primary, theme))
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = 8 }
                }
                binding.layoutStock.addView(tvTotal)
            }
            hideSkeleton()
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
}
