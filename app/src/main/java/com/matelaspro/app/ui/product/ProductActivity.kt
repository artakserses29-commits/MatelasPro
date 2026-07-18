package com.matelaspro.app.ui.product

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager

import com.matelaspro.app.MainViewModel
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.R
import com.matelaspro.app.data.entity.CreditEntry
import com.matelaspro.app.data.entity.Product
import com.matelaspro.app.databinding.ActivityProductBinding
import java.util.Calendar
import com.matelaspro.app.databinding.DialogProductBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProductActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: ProductAdapter
    private var categories = listOf("Matelas", "Pieces de bois", "Meuble")
    private var selectedCategory: String = ""
    private var allProductsList: List<Product> = emptyList()
    private var fournisseurNames = listOf<String>()
    private var filterDateStart: Long = -1L
    private var filterDateEnd: Long = -1L
    private var filterFournisseur: String? = null
    private var lastCategory: String = ""
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductBinding.inflate(layoutInflater)
        setContentView(binding.root)
        filterDateStart = intent.getLongExtra("filter_date_start", -1L)
        filterDateEnd = intent.getLongExtra("filter_date_end", -1L)
        filterFournisseur = intent.getStringExtra("filter_fournisseur")

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setupRecyclerView()
        setupListeners()
        setupCategoryFilter()
        loadFournisseurs()
        if (filterDateStart > 0 && filterDateEnd > 0) {
            loadFilteredProducts()
        } else {
            observeData()
        }
    }

    private fun loadFilteredProducts() {
        lifecycleScope.launch {
            val repo = (application as MatelasProApp).productRepository
            val saleRepo = (application as MatelasProApp).saleRepository
            val products = if (filterFournisseur != null) {
                repo.getProductsByDateRangeAndFournisseur(filterDateStart, filterDateEnd, filterFournisseur!!)
            } else {
                repo.getProductsByDateRange(filterDateStart, filterDateEnd)
            }
            allProductsList = products.map { p ->
                val sold = saleRepo.getTotalQuantitySoldByProductId(p.id)
                p.copy(quantity = p.quantity + sold)
            }
            filterProducts()
            binding.spinnerCategory.visibility = android.view.View.GONE
            binding.textCategoryLabel.visibility = android.view.View.GONE
        }
    }

    private fun loadFournisseurs() {
        val repo = (application as MatelasProApp).fournisseurRepository
        repo.allFournisseurs.observe(this) { list ->
            fournisseurNames = list.map { it.name }
        }
    }

    private fun setupCategoryFilter() {
        val items = listOf("Tous") + categories
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        binding.spinnerCategory.adapter = adapter
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = if (position == 0) "" else categories[position - 1]
                filterProducts()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun filterProducts() {
        var filtered = if (selectedCategory.isEmpty()) allProductsList
        else allProductsList.filter { it.category == selectedCategory }
        if (filterDateStart > 0) {
            filtered = filtered.filter { it.createdAt >= filterDateStart }
        }
        if (filterDateEnd > 0) {
            filtered = filtered.filter { it.createdAt <= filterDateEnd }
        }
        if (searchQuery.isNotEmpty()) {
            val q = searchQuery.lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.fournisseur.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.epaisseur.toString().contains(q) ||
                it.longueur.toString().contains(q) ||
                it.prixUnitaireCm.toString().contains(q) ||
                it.purchasePrice.toString().contains(q) ||
                it.sellingPrice.toString().contains(q)
            }
        }
        adapter.submitList(filtered)
        if (filtered.isEmpty()) {
            binding.textEmptyState.visibility = View.VISIBLE
            binding.recyclerViewProducts.visibility = View.GONE
        } else {
            binding.textEmptyState.visibility = View.GONE
            binding.recyclerViewProducts.visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(
            onEdit = { product -> showProductDialog(product) },
            onDelete = { product -> deleteProduct(product) }
        )
        binding.recyclerViewProducts.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewProducts.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAddProduct.setOnClickListener {
            showProductDialog(null)
        }
        binding.btnBack.setOnClickListener { finish() }

        binding.swipeRefresh.setOnRefreshListener {
            observeData()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString() ?: ""
                filterProducts()
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
                filterProducts()
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
                filterProducts()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
        binding.btnClearDateFilter.setOnClickListener {
            filterDateStart = -1L
            filterDateEnd = -1L
            binding.textDateStart.text = "Date début"
            binding.textDateStart.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
            binding.textDateEnd.text = "Date fin"
            binding.textDateEnd.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
            filterProducts()
        }
    }

    private fun observeData() {
        viewModel.allProducts.observe(this) { products ->
            allProductsList = products
            filterProducts()
        }
    }

    private fun showProductDialog(product: Product?) {
        val dialogBinding = DialogProductBinding.inflate(layoutInflater)
        val existingProduct = product
        var selectedExistingProduct: Product? = null

        val catItems = mutableListOf("Choisir...")
        catItems.addAll(categories)
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, catItems)
        dialogBinding.spinnerCategory.adapter = catAdapter

        val fournItems = mutableListOf("Choisir...")
        fournItems.addAll(fournisseurNames)
        val fournAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fournItems)
        dialogBinding.spinnerFournisseur.adapter = fournAdapter

        if (existingProduct != null) {
            dialogBinding.editName.setText(existingProduct.name)
            dialogBinding.editDescription.setText(existingProduct.description)
            dialogBinding.editQuantity.setText(existingProduct.quantity.toString())
            dialogBinding.editEpaisseur.setText(if (existingProduct.epaisseur > 0) existingProduct.epaisseur.toString() else "")
            dialogBinding.editPrixUnitaireCm.setText(if (existingProduct.prixUnitaireCm > 0) existingProduct.prixUnitaireCm.toString() else "")
            dialogBinding.editLongueur.setText(if (existingProduct.longueur > 0) existingProduct.longueur.toString() else "")
            dialogBinding.editPrixUnitaireBois.setText(if (existingProduct.purchasePrice > 0) existingProduct.purchasePrice.toString() else "")
            updatePrixTotal(dialogBinding)
            updatePrixTotalBois(dialogBinding)
            dialogBinding.textDialogTitle.text = getString(R.string.edit_product)
            val catIndex = categories.indexOf(existingProduct.category)
            if (catIndex >= 0) dialogBinding.spinnerCategory.setSelection(catIndex + 1)
            val fIndex = fournItems.indexOf(existingProduct.fournisseur)
            if (fIndex >= 0) dialogBinding.spinnerFournisseur.setSelection(fIndex)
            lastCategory = existingProduct.category
            onCategoryChanged(dialogBinding, existingProduct.category)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("fr", "FR"))
            dialogBinding.textStockDate.text = "Ajouté le: ${dateFormat.format(Date(existingProduct.createdAt))}"
        } else {
            dialogBinding.layoutDate.visibility = View.VISIBLE
            val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
            dialogBinding.editDate.setText(sdfDate.format(Date()))
            dialogBinding.editDate.setOnClickListener {
                val cal = Calendar.getInstance()
                DatePickerDialog(this, { _, y, m, d ->
                    cal.set(Calendar.YEAR, y)
                    cal.set(Calendar.MONTH, m)
                    cal.set(Calendar.DAY_OF_MONTH, d)
                    dialogBinding.editDate.setText(sdfDate.format(cal.time))
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }
            val prefillFournisseur = intent.getStringExtra("prefill_fournisseur")
            if (!prefillFournisseur.isNullOrEmpty()) {
                val fIndex = fournItems.indexOf(prefillFournisseur)
                if (fIndex >= 0) dialogBinding.spinnerFournisseur.setSelection(fIndex)
            }

            fun setEditFieldsEnabled(enabled: Boolean) {
                val alpha = if (enabled) 1.0f else 0.4f
                dialogBinding.layoutName.alpha = alpha
                dialogBinding.editName.isEnabled = enabled
                dialogBinding.editEpaisseur.isEnabled = enabled
                dialogBinding.editPrixUnitaireCm.isEnabled = enabled
                dialogBinding.editLongueur.isEnabled = enabled
                dialogBinding.editDescription.isEnabled = enabled
                dialogBinding.editQuantity.isEnabled = enabled
                dialogBinding.editPrixUnitaireBois.isEnabled = enabled
                dialogBinding.editDate.isEnabled = enabled
                dialogBinding.layoutEpaisseur.alpha = alpha
                dialogBinding.layoutPrixUnitaireCm.alpha = alpha
                dialogBinding.layoutLongueur.alpha = alpha
                dialogBinding.layoutDescription.alpha = alpha
                dialogBinding.layoutQuantity.alpha = alpha
                dialogBinding.layoutPrixUnitaireBois.alpha = alpha
                dialogBinding.layoutDate.alpha = alpha
            }
            setEditFieldsEnabled(false)
            dialogBinding.cardCategory.alpha = 0.4f
            dialogBinding.spinnerCategory.isEnabled = false

            class AcAdapter(val ctx: android.content.Context, val data: MutableList<android.util.Pair<String, String>>, val filtered: MutableList<android.util.Pair<String, String>>) : android.widget.BaseAdapter(), android.widget.Filterable {
                override fun getCount() = filtered.size
                override fun getItem(pos: Int) = filtered[pos].first
                override fun getItemId(pos: Int) = pos.toLong()
                override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
                    val v = convertView ?: LayoutInflater.from(ctx).inflate(R.layout.item_product_dropdown, parent, false)
                    v.findViewById<TextView>(R.id.textLeft).text = filtered[pos].first
                    v.findViewById<TextView>(R.id.textRight).text = filtered[pos].second
                    return v
                }
                override fun getFilter(): android.widget.Filter {
                    return object : android.widget.Filter() {
                        override fun performFiltering(constraint: CharSequence?): android.widget.Filter.FilterResults {
                            val q = constraint?.toString()?.lowercase() ?: ""
                            val res = if (q.isEmpty()) ArrayList(data) else ArrayList(data.filter { it.first.lowercase().contains(q) })
                            val fr = android.widget.Filter.FilterResults()
                            fr.values = res; fr.count = res.size
                            return fr
                        }
                        @Suppress("UNCHECKED_CAST")
                        override fun publishResults(constraint: CharSequence?, results: android.widget.Filter.FilterResults) {
                            filtered.clear()
                            val list = results.values as? ArrayList<android.util.Pair<String, String>> ?: ArrayList()
                            filtered.addAll(list)
                            notifyDataSetChanged()
                        }
                    }
                }
            }
            val acData = ArrayList<android.util.Pair<String, String>>()
            val acFiltered = ArrayList<android.util.Pair<String, String>>()
            val acAdapter = AcAdapter(this@ProductActivity, acData, acFiltered)
            dialogBinding.editName.setAdapter(acAdapter)
            val autocompleteProductList = mutableListOf<Product>()

            fun productLabel(p: Product): String {
                return when (p.category) {
                    "Matelas" -> {
                        val parts = mutableListOf(p.name)
                        if (p.epaisseur > 0) parts.add("${p.epaisseur}cm")
                        parts.joinToString(" - ")
                    }
                    "Pieces de bois" -> {
                        val parts = mutableListOf(p.name)
                        if (p.longueur > 0) parts.add("${p.longueur.toLong()}m")
                        parts.joinToString(" - ")
                    }
                    "Meuble" -> {
                        val parts = mutableListOf(p.name)
                        if (p.description.isNotEmpty()) parts.add(p.description)
                        parts.joinToString(" - ")
                    }
                    else -> p.name
                }
            }
            fun productRightText(p: Product): String {
                return when {
                    p.category == "Matelas" && p.prixUnitaireCm > 0 -> "%,d MGA/cm".format(p.prixUnitaireCm.toLong())
                    (p.category == "Pieces de bois" || p.category == "Meuble") && p.purchasePrice > 0 -> "%,d MGA".format(p.purchasePrice.toLong())
                    else -> ""
                }
            }

            fun loadAutoComplete() {
                val cp = dialogBinding.spinnerCategory.selectedItemPosition
                val fp = dialogBinding.spinnerFournisseur.selectedItemPosition
                if (cp <= 0 || fp <= 0) {
                    autocompleteProductList.clear()
                    acData.clear()
                    acFiltered.clear()
                    acAdapter.notifyDataSetChanged()
                    return
                }
                val cat = categories[cp - 1]
                val fourn = fournisseurNames[fp - 1]
                lifecycleScope.launch {
                    val repo = (application as MatelasProApp).productRepository
                    val list = repo.getProductsByFournisseurAndCategory(fourn, cat)
                    autocompleteProductList.clear()
                    autocompleteProductList.addAll(list)
                    acData.clear()
                    acData.addAll(list.map { android.util.Pair(productLabel(it), productRightText(it)) })
                    acFiltered.clear()
                    acFiltered.addAll(acData)
                    acAdapter.notifyDataSetChanged()
                }
            }

            dialogBinding.editName.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                val label = acAdapter.getItem(position) as? String ?: return@OnItemClickListener
                selectedExistingProduct = autocompleteProductList.find { productLabel(it) == label }
                selectedExistingProduct?.let { p ->
                    dialogBinding.editName.setText(p.name)
                    dialogBinding.editName.setSelection(p.name.length)
                    dialogBinding.editEpaisseur.setText(if (p.epaisseur > 0) p.epaisseur.toString() else "")
                    dialogBinding.editPrixUnitaireCm.setText(if (p.prixUnitaireCm > 0) p.prixUnitaireCm.toString() else "")
                    dialogBinding.editLongueur.setText(if (p.longueur > 0) p.longueur.toString() else "")
                    dialogBinding.editDescription.setText(p.description)
                    dialogBinding.editPrixUnitaireBois.setText(if (p.purchasePrice > 0) p.purchasePrice.toString() else "")
                    dialogBinding.editQuantity.requestFocus()
                }
            }

            dialogBinding.editName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (selectedExistingProduct != null) {
                        selectedExistingProduct = null
                        dialogBinding.editEpaisseur.setText("")
                        dialogBinding.editPrixUnitaireCm.setText("")
                        dialogBinding.editPrixUnitaire.setText("")
                        dialogBinding.editPrixTotal.setText("")
                        dialogBinding.editLongueur.setText("")
                        dialogBinding.editPrixUnitaireBois.setText("")
                        dialogBinding.editPrixTotalBois.setText("")
                        dialogBinding.editDescription.setText("")
                        dialogBinding.editPrixUnitaireBois.setText("")
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            dialogBinding.spinnerFournisseur.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val hasFournisseur = position > 0
                    dialogBinding.cardCategory.alpha = if (hasFournisseur) 1.0f else 0.4f
                    dialogBinding.spinnerCategory.isEnabled = hasFournisseur
                    if (!hasFournisseur) {
                        dialogBinding.spinnerCategory.setSelection(0)
                        dialogBinding.editEpaisseur.setText("")
                        dialogBinding.editPrixUnitaireCm.setText("")
                        dialogBinding.editPrixUnitaire.setText("")
                        dialogBinding.editPrixTotal.setText("")
                        dialogBinding.editLongueur.setText("")
                        dialogBinding.editPrixUnitaireBois.setText("")
                        dialogBinding.editPrixTotalBois.setText("")
                        dialogBinding.editDescription.setText("")
                        dialogBinding.editPrixUnitaireBois.setText("")
                        lastCategory = ""
                        setEditFieldsEnabled(false)
                    }
                    loadAutoComplete()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            // add mode category listener
            val catListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val cat = if (position > 0) categories[position - 1] else ""
                    if (cat != lastCategory) {
                        dialogBinding.editEpaisseur.setText("")
                        dialogBinding.editPrixUnitaireCm.setText("")
                        dialogBinding.editPrixUnitaire.setText("")
                        dialogBinding.editPrixTotal.setText("")
                        dialogBinding.editLongueur.setText("")
                        dialogBinding.editPrixUnitaireBois.setText("")
                        dialogBinding.editPrixTotalBois.setText("")
                        dialogBinding.editDescription.setText("")
                        dialogBinding.editPrixUnitaireBois.setText("")
                        lastCategory = cat
                        onCategoryChanged(dialogBinding, cat)
                    }
                    val hasCategory = position > 0
                    setEditFieldsEnabled(hasCategory)
                    if (!hasCategory) {
                        dialogBinding.editEpaisseur.setText("")
                        dialogBinding.editPrixUnitaireCm.setText("")
                        dialogBinding.editPrixUnitaire.setText("")
                        dialogBinding.editPrixTotal.setText("")
                        dialogBinding.editLongueur.setText("")
                        dialogBinding.editPrixUnitaireBois.setText("")
                        dialogBinding.editPrixTotalBois.setText("")
                        dialogBinding.editDescription.setText("")
                        dialogBinding.editPrixUnitaireBois.setText("")
                    }
                    dialogBinding.spinnerFournisseur.onItemSelectedListener?.let {
                        val fPos = dialogBinding.spinnerFournisseur.selectedItemPosition
                        if (fPos > 0) {
                            (it as? AdapterView.OnItemSelectedListener)?.onItemSelected(null, null, fPos, 0)
                        }
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            dialogBinding.spinnerCategory.onItemSelectedListener = catListener
            loadAutoComplete()
        }

        if (existingProduct != null) {
            dialogBinding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val cat = if (position > 0) categories[position - 1] else ""
                    if (cat != lastCategory) {
                        dialogBinding.editEpaisseur.setText("")
                        dialogBinding.editPrixUnitaireCm.setText("")
                        dialogBinding.editPrixUnitaire.setText("")
                        dialogBinding.editPrixTotal.setText("")
                        dialogBinding.editLongueur.setText("")
                        dialogBinding.editPrixUnitaireBois.setText("")
                        dialogBinding.editPrixTotalBois.setText("")
                        dialogBinding.editDescription.setText("")
                        dialogBinding.editPrixUnitaireBois.setText("")
                        lastCategory = cat
                        onCategoryChanged(dialogBinding, cat)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updatePrixTotal(dialogBinding)
                updatePrixTotalBois(dialogBinding)
            }
        }
        val boisTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updatePrixTotalBois(dialogBinding) }
        }
        dialogBinding.editEpaisseur.addTextChangedListener(textWatcher)
        dialogBinding.editQuantity.addTextChangedListener(textWatcher)
        dialogBinding.editPrixUnitaireCm.addTextChangedListener(textWatcher)
        dialogBinding.editPrixUnitaireBois.addTextChangedListener(boisTextWatcher)
        dialogBinding.editQuantity.addTextChangedListener(boisTextWatcher)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.save), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.editName.text.toString().trim()
                val fournisseurPos = dialogBinding.spinnerFournisseur.selectedItemPosition
                val fournisseur = if (fournisseurPos > 0) fournisseurNames[fournisseurPos - 1] else ""
                val description = dialogBinding.editDescription.text.toString().trim()
                val quantity = dialogBinding.editQuantity.text.toString().toIntOrNull() ?: 0
                val catPos = dialogBinding.spinnerCategory.selectedItemPosition
                val category = if (catPos > 0) categories[catPos - 1] else ""
                val epaisseur = dialogBinding.editEpaisseur.text.toString().toDoubleOrNull() ?: 0.0
                val prixUnitaireCm = dialogBinding.editPrixUnitaireCm.text.toString().toDoubleOrNull() ?: 0.0
                val longueur = dialogBinding.editLongueur.text.toString().toDoubleOrNull() ?: 0.0
                val purchasePrice = when (category) {
                    "Matelas" -> epaisseur * prixUnitaireCm
                    "Pieces de bois" -> dialogBinding.editPrixUnitaireBois.text.toString().toDoubleOrNull() ?: 0.0
                    "Meuble" -> dialogBinding.editPrixUnitaireBois.text.toString().toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }
                val sellingPrice = when (category) {
                    "Matelas" -> epaisseur * prixUnitaireCm
                    "Pieces de bois" -> purchasePrice
                    "Meuble" -> purchasePrice
                    else -> purchasePrice
                }

                dialogBinding.layoutName.error = null
                dialogBinding.layoutQuantity.error = null
                dialogBinding.layoutEpaisseur.error = null
                dialogBinding.textCategoryError.visibility = android.view.View.GONE
                dialogBinding.textFournisseurError.visibility = android.view.View.GONE
                dialogBinding.cardCategory.strokeColor = android.graphics.Color.parseColor("#9E9E9E")
                dialogBinding.cardFournisseur.strokeColor = android.graphics.Color.parseColor("#9E9E9E")

                var hasError = false
                if (name.isEmpty()) {
                    dialogBinding.layoutName.error = "Le nom est requis"
                    hasError = true
                }
                if (catPos <= 0) {
                    dialogBinding.textCategoryError.text = "Veuillez choisir une catégorie"
                    dialogBinding.textCategoryError.visibility = android.view.View.VISIBLE
                    dialogBinding.cardCategory.strokeColor = android.graphics.Color.parseColor("#FF0000")
                    hasError = true
                }
                if (fournisseurPos <= 0) {
                    dialogBinding.textFournisseurError.text = "Veuillez choisir un fournisseur"
                    dialogBinding.textFournisseurError.visibility = android.view.View.VISIBLE
                    dialogBinding.cardFournisseur.strokeColor = android.graphics.Color.parseColor("#FF0000")
                    hasError = true
                }
                if (quantity <= 0) {
                    dialogBinding.layoutQuantity.error = "Doit être > 0"
                    hasError = true
                }
                if (category == "Matelas" && epaisseur <= 0) {
                    dialogBinding.layoutEpaisseur.error = "Épaisseur requise"
                    hasError = true
                }
                if (category == "Meuble" && purchasePrice <= 0) {
                    dialogBinding.layoutPrixUnitaireBois.error = "Prix unitaire requis"
                    hasError = true
                }
                if (hasError) return@setOnClickListener

                if (existingProduct != null) {
                    viewModel.updateProduct(
                        existingProduct.copy(
                            name = name, category = category, fournisseur = fournisseur,
                            description = description, quantity = quantity,
                            epaisseur = epaisseur, prixUnitaireCm = prixUnitaireCm,
                            longueur = longueur, purchasePrice = purchasePrice,
                            sellingPrice = sellingPrice
                        )
                    )
                } else if (selectedExistingProduct != null) {
                    val app = application as MatelasProApp
                    val mergeProduct = selectedExistingProduct!!
                    val sameName = name.equals(mergeProduct.name, ignoreCase = true)
                    val sameEpaisseur = if (category == "Matelas") epaisseur == mergeProduct.epaisseur else true
                    val sameLongueur = if (category == "Pieces de bois") longueur == mergeProduct.longueur else true
                    val sameDescription = if (category == "Meuble") description == mergeProduct.description else true
                    val samePurchasePrice = purchasePrice == mergeProduct.purchasePrice
                    val samePrixUnitaireCm = prixUnitaireCm == mergeProduct.prixUnitaireCm

                    if (sameName && sameEpaisseur && sameLongueur && sameDescription && samePurchasePrice && samePrixUnitaireCm) {
                        lifecycleScope.launch {
                            val updatedQty = mergeProduct.quantity + quantity
                            val updated = mergeProduct.copy(quantity = updatedQty)
                            viewModel.updateProduct(updated)
                            val creditEntry = CreditEntry(
                                productId = mergeProduct.id,
                                productName = mergeProduct.name,
                                fournisseur = fournisseur,
                                category = category,
                                quantity = quantity,
                                sellingPrice = sellingPrice,
                                createdAt = System.currentTimeMillis()
                            )
                            app.creditEntryRepository.insert(creditEntry)
                            app.auditLogRepository.insert("MERGE", "products", mergeProduct.id, "Added qty $quantity to $name")
                            runOnUiThread {
                                Toast.makeText(this@ProductActivity, "Quantité ajoutée au produit existant", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                        val chosenDate = try {
                            val parsed = sdf.parse(dialogBinding.editDate.text.toString())
                            if (parsed != null) {
                                val cal = Calendar.getInstance().apply { time = parsed }
                                val now = Calendar.getInstance()
                                cal.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                                cal.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                                cal.set(Calendar.SECOND, now.get(Calendar.SECOND))
                                cal.set(Calendar.MILLISECOND, now.get(Calendar.MILLISECOND))
                                cal.timeInMillis
                            } else null
                        } catch (e: Exception) { null }
                        viewModel.insertProduct(name, category, fournisseur, description, quantity, purchasePrice, sellingPrice, epaisseur, prixUnitaireCm, longueur, chosenDate)
                    }
                } else {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                    val chosenDate = try {
                        val parsed = sdf.parse(dialogBinding.editDate.text.toString())
                        if (parsed != null) {
                            val cal = Calendar.getInstance().apply { time = parsed }
                            val now = Calendar.getInstance()
                            cal.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                            cal.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                            cal.set(Calendar.SECOND, now.get(Calendar.SECOND))
                            cal.set(Calendar.MILLISECOND, now.get(Calendar.MILLISECOND))
                            cal.timeInMillis
                        } else null
                    } catch (e: Exception) { null }
                    viewModel.insertProduct(name, category, fournisseur, description, quantity, purchasePrice, sellingPrice, epaisseur, prixUnitaireCm, longueur, chosenDate)
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun onCategoryChanged(binding: DialogProductBinding, category: String) {
        val isMatelas = category == "Matelas"
        val isBois = category == "Pieces de bois"
        val isMeuble = category == "Meuble"
        binding.layoutEpaisseur.visibility = if (isMatelas) View.VISIBLE else View.GONE
        binding.layoutPrixUnitaireCm.visibility = if (isMatelas) View.VISIBLE else View.GONE
        binding.layoutPrixUnitaire.visibility = if (isMatelas) View.VISIBLE else View.GONE
        binding.layoutPrixTotal.visibility = if (isMatelas) View.VISIBLE else View.GONE
        binding.layoutLongueur.visibility = if (isBois) View.VISIBLE else View.GONE
        binding.layoutPrixUnitaireBois.visibility = if (isBois) View.VISIBLE else View.GONE
        binding.layoutPrixTotalBois.visibility = if (isBois) View.VISIBLE else View.GONE
        binding.textStockDate.visibility = if (isMatelas || isBois || isMeuble) View.VISIBLE else View.GONE
        binding.layoutDescription.visibility = if (isMeuble) View.VISIBLE else View.GONE
        binding.layoutPrixUnitaireBois.visibility = if (isBois || isMeuble) View.VISIBLE else View.GONE
        if (isMeuble) binding.layoutPrixUnitaireBois.hint = "Prix unitaire"
        if (isBois) binding.layoutPrixUnitaireBois.hint = "Prix unitaire"
        if (isMatelas) updatePrixTotal(binding)
        if (isBois) updatePrixTotalBois(binding)
    }

    private fun updatePrixTotal(binding: DialogProductBinding) {
        val ep = binding.editEpaisseur.text.toString().toDoubleOrNull() ?: 0.0
        val qty = binding.editQuantity.text.toString().toIntOrNull() ?: 0
        val pu = binding.editPrixUnitaireCm.text.toString().toDoubleOrNull() ?: 0.0
        val prixUnitaire = ep * pu
        val prixTotal = prixUnitaire * qty
        binding.editPrixUnitaire.setText(if (prixUnitaire > 0) String.format("%.2f", prixUnitaire) else "")
        binding.editPrixTotal.setText(if (prixTotal > 0) String.format("%.2f", prixTotal) else "")
    }

    private fun updatePrixTotalBois(binding: DialogProductBinding) {
        val pu = binding.editPrixUnitaireBois.text.toString().toDoubleOrNull() ?: 0.0
        val qty = binding.editQuantity.text.toString().toIntOrNull() ?: 0
        val total = pu * qty
        binding.editPrixTotalBois.setText(if (total > 0) String.format("%.2f", total) else "")
    }

    private fun deleteProduct(product: Product) {
        if (product.quantity > 0) {
            AlertDialog.Builder(this)
                .setTitle("Suppression impossible")
                .setMessage("Impossible de supprimer ${product.name} car il a encore ${product.quantity} unité(s) en stock.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Supprimer le produit")
            .setMessage("Voulez-vous vraiment supprimer ${product.name} ?")
            .setPositiveButton("Supprimer") { _, _ ->
                lifecycleScope.launch {
                    val app = application as MatelasProApp
                    app.auditLogRepository.insert("DELETE", "products", product.id, product.name)
                    viewModel.deleteProduct(product)
                    Toast.makeText(this@ProductActivity, "Supprimé", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}
