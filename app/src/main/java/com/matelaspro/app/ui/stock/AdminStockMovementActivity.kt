package com.matelaspro.app.ui.stock

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.R
import com.matelaspro.app.data.entity.Product
import com.matelaspro.app.data.entity.User
import com.matelaspro.app.data.entity.UserStock
import com.matelaspro.app.databinding.ActivityAdminStockMovementBinding
import com.matelaspro.app.util.FormatUtil
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency

class AdminStockMovementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminStockMovementBinding
    private lateinit var app: MatelasProApp
    private var users: List<User> = emptyList()
    private var products: List<Product> = emptyList()
    private var selectedUser: User? = null
    private var selectedProduct: Product? = null
    private var userStockMap: Map<Long, Int> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminStockMovementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as MatelasProApp

        binding.toolbar.setNavigationOnClickListener { finish() }

        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            users = app.userRepository.getAllUsersList().filter { !it.isAdmin }
            products = app.productRepository.getAllProductsSuspend()

            setupUserSpinner()
            setupProductSpinner()

            binding.btnTransfer.setOnClickListener { transferStock() }
        }
    }

    private fun setupUserSpinner() {
        val userNames = users.map { it.name }.toMutableList().apply { add(0, "Choisir...") }
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, userNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerUser.adapter = adapter
        binding.spinnerUser.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedUser = if (position > 0) users[position - 1] else null
                if (selectedUser != null) loadUserStock() else binding.layoutUserStock.removeAllViews()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupProductSpinner() {
        data class DropdownItem(val left: String, val right: String)
        val items = mutableListOf(DropdownItem("Choisir...", ""))
        val currencyFormat = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
        items.addAll(products.map { p ->
            val left = when {
                p.category == "Matelas" && p.epaisseur > 0 -> "${p.name} - ${p.epaisseur}cm"
                p.category == "Pieces de bois" && p.longueur > 0 -> "${p.name} ${p.longueur.toLong()}m"
                p.category == "Meuble" && p.description.isNotEmpty() -> "${p.name} - ${p.description}"
                else -> p.name
            }
            val right = when {
                p.category == "Matelas" && p.prixUnitaireCm > 0 -> "${currencyFormat.format(p.prixUnitaireCm)}/cm"
                else -> currencyFormat.format(p.sellingPrice)
            }
            DropdownItem(left, right)
        })
        val adapter = object : android.widget.ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items.map { it.left }) {
            override fun getDropDownView(pos: Int, convertView: View?, parent: ViewGroup): View {
                val v = convertView ?: LayoutInflater.from(this@AdminStockMovementActivity).inflate(R.layout.item_product_dropdown, parent, false)
                v.findViewById<TextView>(R.id.textLeft).text = if (pos < items.size) items[pos].left else ""
                v.findViewById<TextView>(R.id.textRight).text = if (pos < items.size) items[pos].right else ""
                return v
            }
        }
        binding.spinnerProduct.adapter = adapter
        binding.spinnerProduct.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedProduct = if (position > 0) products[position - 1] else null
                if (selectedProduct != null) {
                    binding.textStockDisponible.text = "Stock disponible: ${selectedProduct!!.quantity}"
                    binding.textStockDisponible.visibility = TextView.VISIBLE
                } else {
                    binding.textStockDisponible.visibility = TextView.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadUserStock() {
        lifecycleScope.launch {
            val userId = selectedUser!!.id
            val userStocks = app.userStockRepository.getByUserId(userId)
            userStockMap = userStocks.associate { it.productId to it.quantity }

            binding.layoutUserStock.removeAllViews()
            if (userStocks.isEmpty()) {
                val tv = TextView(this@AdminStockMovementActivity).apply {
                    text = "Aucun stock pour cet utilisateur"
                    textSize = 13f
                    setTextColor(resources.getColor(R.color.grey_medium, theme))
                }
                binding.layoutUserStock.addView(tv)
            } else {
                for (us in userStocks.sortedByDescending { it.quantity }) {
                    val product = products.find { it.id == us.productId }
                    val name = product?.name ?: "Produit #${us.productId}"
                    addUserStockRow(name, us.quantity)
                }
            }
        }
    }

    private fun addUserStockRow(productName: String, quantity: Int) {
        val row = layoutInflater.inflate(R.layout.item_dashboard_row, null) as LinearLayout
        row.findViewById<TextView>(R.id.text_row_title).text = productName
        row.findViewById<TextView>(R.id.text_row_subtitle).text = "Qté: $quantity"
        binding.layoutUserStock.addView(row)
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { setMargins(0, 4, 0, 4) }
            setBackgroundColor(resources.getColor(R.color.grey_medium, theme))
        }
        binding.layoutUserStock.addView(divider)
    }

    private fun transferStock() {
        val user = selectedUser ?: run { Toast.makeText(this, "Choisir un utilisateur", Toast.LENGTH_SHORT).show(); return }
        val product = selectedProduct ?: run { Toast.makeText(this, "Choisir un produit", Toast.LENGTH_SHORT).show(); return }
        val qtyText = binding.editQuantity.text.toString().trim()
        if (qtyText.isEmpty()) { Toast.makeText(this, "Saisir une quantité", Toast.LENGTH_SHORT).show(); return }
        val qty = qtyText.toIntOrNull()
        if (qty == null || qty <= 0) { Toast.makeText(this, "Quantité invalide", Toast.LENGTH_SHORT).show(); return }
        if (qty > product.quantity) { Toast.makeText(this, "Stock insuffisant (${product.quantity})", Toast.LENGTH_SHORT).show(); return }

        lifecycleScope.launch {
            val now = System.currentTimeMillis()
            val existing = app.userStockRepository.getByUserAndProduct(user.id, product.id)
            if (existing != null) {
                app.userStockRepository.upsert(existing.copy(quantity = existing.quantity + qty, updatedAt = now))
            } else {
                app.userStockRepository.upsert(com.matelaspro.app.data.entity.UserStock(userId = user.id, productId = product.id, quantity = qty, updatedAt = now))
            }
            app.productRepository.update(product.copy(quantity = product.quantity - qty))

            Toast.makeText(this@AdminStockMovementActivity, "$qty ${product.name} transféré à ${user.name}", Toast.LENGTH_SHORT).show()
            binding.editQuantity.setText("")
            loadUserStock()

            val updatedProduct = app.productRepository.getProductById(product.id)
            if (updatedProduct != null) {
                val idx = products.indexOfFirst { it.id == product.id }
                if (idx >= 0) products = products.toMutableList().apply { set(idx, updatedProduct) }
                binding.textStockDisponible.text = "Stock disponible: ${updatedProduct.quantity}"
            }
        }
    }
}
