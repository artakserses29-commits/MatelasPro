package com.matelaspro.app.ui.sale

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager

import com.matelaspro.app.MainViewModel
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.R
import com.matelaspro.app.data.entity.Product
import com.matelaspro.app.data.entity.Sale
import com.matelaspro.app.databinding.ActivitySaleBinding
import com.matelaspro.app.databinding.DialogSaleBinding
import com.matelaspro.app.util.FormatUtil
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Locale

class SaleActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySaleBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: SaleAdapter
    private var productList: List<Product> = emptyList()
    private var allSalesList: List<Sale> = emptyList()
    private var searchQuery: String = ""
    private var filterDateStart: Long = -1L
    private var filterDateEnd: Long = -1L
    private var currentUserId: Long = 0
    private var isAdmin: Boolean = false
    private var userStockProductIds: Set<Long> = emptySet()
    private var userStockQtys: Map<Long, Int> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        val loginPrefs = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        currentUserId = loginPrefs.getLong("currentUserId", 0)
        isAdmin = loginPrefs.getBoolean("isAdmin", false)
        super.onCreate(savedInstanceState)
        binding = ActivitySaleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = SaleAdapter(
            onCancel = { sale -> cancelSale(sale) }
        )
        binding.recyclerViewSales.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSales.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.fabAddSale.setOnClickListener { showSaleDialog() }

        binding.swipeRefresh.setOnRefreshListener {
            observeData()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString() ?: ""
                filterSales()
            }
        })

        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        binding.textDateStart.setOnClickListener {
            val cal = Calendar.getInstance()
            if (filterDateStart > 0) cal.timeInMillis = filterDateStart
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(Calendar.YEAR, y); cal.set(Calendar.MONTH, m); cal.set(Calendar.DAY_OF_MONTH, d)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                filterDateStart = cal.timeInMillis
                binding.textDateStart.text = sdfDate.format(cal.time)
                binding.textDateStart.setTextColor(android.graphics.Color.parseColor("#212121"))
                filterSales()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
        binding.textDateEnd.setOnClickListener {
            val cal = Calendar.getInstance()
            if (filterDateEnd > 0) cal.timeInMillis = filterDateEnd
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(Calendar.YEAR, y); cal.set(Calendar.MONTH, m); cal.set(Calendar.DAY_OF_MONTH, d)
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                filterDateEnd = cal.timeInMillis
                binding.textDateEnd.text = sdfDate.format(cal.time)
                binding.textDateEnd.setTextColor(android.graphics.Color.parseColor("#212121"))
                filterSales()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
        binding.btnClearDateFilter.setOnClickListener {
            filterDateStart = -1L
            filterDateEnd = -1L
            binding.textDateStart.text = "Date début"
            binding.textDateStart.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
            binding.textDateEnd.text = "Date fin"
            binding.textDateEnd.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
            filterSales()
        }
    }

    private fun observeData() {
        viewModel.allSales.observe(this) { sales ->
            allSalesList = if (isAdmin) sales else sales.filter { it.userId == currentUserId }
            filterSales()
        }
        viewModel.allProducts.observe(this) { products ->
            if (isAdmin) {
                productList = products
            } else {
                lifecycleScope.launch {
                    val app = application as MatelasProApp
                    val userStocks = app.userStockRepository.getByUserId(currentUserId)
                    userStockProductIds = userStocks.filter { it.quantity > 0 }.map { it.productId }.toSet()
                    userStockQtys = userStocks.associate { it.productId to it.quantity }
                    productList = products.filter { it.id in userStockProductIds }
                    adapter.productMap = productList.associateBy { it.id }
                }
            }
            adapter.productMap = productList.associateBy { it.id }
        }
    }

    private fun filterSales() {
        var filtered = allSalesList
        if (filterDateStart > 0) {
            filtered = filtered.filter { it.saleDate >= filterDateStart }
        }
        if (filterDateEnd > 0) {
            filtered = filtered.filter { it.saleDate <= filterDateEnd }
        }
        val q = searchQuery.lowercase()
        val productMap = productList.associateBy { it.id }
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter { sale ->
                val p = productMap[sale.productId]
                sale.productName.lowercase().contains(q) ||
                sale.totalAmount.toString().contains(q) ||
                sale.quantity.toString().contains(q) ||
                sale.unitPrice.toString().contains(q) ||
                sale.profit.toString().contains(q) ||
                p?.category?.lowercase()?.contains(q) == true ||
                p?.fournisseur?.lowercase()?.contains(q) == true ||
                p?.description?.lowercase()?.contains(q) == true ||
                p?.epaisseur?.toString()?.contains(q) == true ||
                p?.longueur?.toString()?.contains(q) == true ||
                p?.prixUnitaireCm?.toString()?.contains(q) == true ||
                p?.purchasePrice?.toString()?.contains(q) == true
            }
        }
        adapter.submitList(filtered)
        val empty = filtered.isEmpty()
        binding.textEmptyState.visibility = if (empty) View.VISIBLE else View.GONE
        binding.recyclerViewSales.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun showSaleDialog() {
        if (productList.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_products), Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBinding = DialogSaleBinding.inflate(layoutInflater)
        data class SaleItem(val left: String, val right: String)
        val saleItems = mutableListOf(SaleItem("Choisir...", ""))
        saleItems.addAll(productList.map { p ->
            val left = when {
                p.category == "Matelas" && p.epaisseur > 0 -> "${p.name} - ${p.epaisseur}cm"
                p.category == "Pieces de bois" && p.longueur > 0 -> "${p.name} ${p.longueur.toLong()}m"
                p.category == "Meuble" && p.description.isNotEmpty() -> "${p.name} - ${p.description}"
                else -> p.name
            }
            val right = when {
                p.category == "Matelas" && p.prixUnitaireCm > 0 -> "%,d MGA/cm".format(p.prixUnitaireCm.toLong())
                (p.category == "Pieces de bois" || p.category == "Meuble") && p.purchasePrice > 0 -> "%,d MGA".format(p.purchasePrice.toLong())
                else -> ""
            }
            SaleItem(left, right)
        })
        val spinnerAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, saleItems.map { it.left }) {
            override fun getDropDownView(pos: Int, convertView: View?, parent: ViewGroup): View {
                val v = convertView ?: LayoutInflater.from(this@SaleActivity).inflate(R.layout.item_product_dropdown, parent, false)
                v.findViewById<TextView>(R.id.textLeft).text = if (pos < saleItems.size) saleItems[pos].left else ""
                v.findViewById<TextView>(R.id.textRight).text = if (pos < saleItems.size) saleItems[pos].right else ""
                return v
            }
        }
        dialogBinding.spinnerProduct.adapter = spinnerAdapter

        val currencyFormat = NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance("MGA")
        }

        var selectedProduct: Product? = null
        var productSelected = false
        dialogBinding.spinnerProduct.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    selectedProduct = productList[position - 1]
                    productSelected = true
                    dialogBinding.editUnitPrice.setText("")
                    dialogBinding.textPrixUnitaire.setText(currencyFormat.format(selectedProduct!!.sellingPrice))
                } else {
                    selectedProduct = null
                    productSelected = false
                    dialogBinding.editUnitPrice.setText("")
                    dialogBinding.textPrixUnitaire.setText("")
                }
                if (selectedProduct != null) updateBenefice(dialogBinding, selectedProduct!!)
                else {
                    dialogBinding.textBenefice.setText("")
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val saleTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                selectedProduct?.let { updateBenefice(dialogBinding, it) }
            }
        }
        dialogBinding.editQuantity.addTextChangedListener(saleTextWatcher)
        dialogBinding.editUnitPrice.addTextChangedListener(saleTextWatcher)
        FormatUtil.applyMoneyFormat(dialogBinding.editUnitPrice)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.save), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                dialogBinding.layoutQuantity.error = null
                dialogBinding.layoutUnitPrice.error = null
                dialogBinding.textProductError.visibility = android.view.View.GONE
                dialogBinding.cardProduct.strokeColor = android.graphics.Color.parseColor("#9E9E9E")

                var hasError = false

                if (!productSelected || selectedProduct == null) {
                    dialogBinding.textProductError.text = "Veuillez choisir un produit"
                    dialogBinding.textProductError.visibility = android.view.View.VISIBLE
                    dialogBinding.cardProduct.strokeColor = android.graphics.Color.parseColor("#FF0000")
                    hasError = true
                }

                val product = selectedProduct ?: return@setOnClickListener
                val quantity = dialogBinding.editQuantity.text.toString().toIntOrNull() ?: 0
                val unitPriceText = dialogBinding.editUnitPrice.text.toString().trim()
                val unitPrice = FormatUtil.parseMontant(unitPriceText)

                if (quantity <= 0) {
                    dialogBinding.layoutQuantity.error = "Quantité invalide"
                    hasError = true
                }
                val availableStock = if (isAdmin) product.quantity else userStockQtys[product.id] ?: 0
                if (quantity > availableStock) {
                    dialogBinding.layoutQuantity.error = "Stock insuffisant ($availableStock)"
                    hasError = true
                }
                if (unitPriceText.isEmpty()) {
                    dialogBinding.layoutUnitPrice.error = "Prix de vente requis"
                    hasError = true
                } else if (unitPrice < product.sellingPrice) {
                    dialogBinding.layoutUnitPrice.error = "Prix doit être ≥ ${product.sellingPrice}"
                    hasError = true
                }
                if (hasError) return@setOnClickListener

                viewModel.recordSale(
                    productId = product.id,
                    productName = product.name,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    purchasePrice = product.sellingPrice,
                    userId = currentUserId
                )
                if (isAdmin) {
                    viewModel.updateProduct(product.copy(quantity = product.quantity - quantity))
                } else {
                    lifecycleScope.launch {
                        val app = application as MatelasProApp
                        app.userStockRepository.addQuantity(currentUserId, product.id, -quantity)
                    }
                }
                Toast.makeText(this, getString(R.string.sale_recorded), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun updateBenefice(binding: DialogSaleBinding, product: Product?) {
        if (product == null) { binding.textBenefice.setText(""); return }
        val qty = binding.editQuantity.text.toString().toIntOrNull() ?: 0
        val sellPrice = FormatUtil.parseMontant(binding.editUnitPrice.text.toString())
        val benefice = (sellPrice - product.sellingPrice) * qty
        val format = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
        binding.textBenefice.setText(format.format(benefice as Double))
    }

    private fun cancelSale(sale: Sale) {
        val now = System.currentTimeMillis()
        val thirtyMinutes = 30 * 60 * 1000L
        if (now - sale.saleDate > thirtyMinutes) {
            Toast.makeText(this, "Délai d'annulation dépassé (30 min)", Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Annuler la vente")
            .setMessage("Voulez-vous vraiment annuler la vente de ${sale.productName} ? (Le stock sera remis à jour)")
            .setPositiveButton("Annuler la vente") { _, _ ->
                lifecycleScope.launch {
                    val app = application as MatelasProApp
                    app.saleRepository.delete(sale)
                    app.auditLogRepository.insert("CANCEL", "sales", sale.id, sale.productName)

                    val product = app.productRepository.getProductById(sale.productId)
                    if (product != null) {
                        app.productRepository.insert(product.copy(quantity = product.quantity + sale.quantity))
                    }

                    val userStock = app.userStockRepository.getByUserAndProduct(sale.userId, sale.productId)
                    if (userStock != null) {
                        app.userStockRepository.upsert(userStock.copy(quantity = userStock.quantity + sale.quantity))
                    }

                    Toast.makeText(this@SaleActivity, "Vente annulée", Toast.LENGTH_SHORT).show()
                    observeData()
                }
            }
            .setNegativeButton("Non", null)
            .show()
    }
}
