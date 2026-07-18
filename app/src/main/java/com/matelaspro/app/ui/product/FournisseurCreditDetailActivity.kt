package com.matelaspro.app.ui.product

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.data.entity.CreditEntry
import com.matelaspro.app.databinding.ActivityFournisseurCreditDetailBinding
import com.matelaspro.app.databinding.ItemFournisseurProductBinding
import com.matelaspro.app.util.FormatUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FournisseurCreditDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFournisseurCreditDetailBinding
    private lateinit var app: MatelasProApp
    private var fournisseurName = ""
    private var startOfDay: Long = -1L
    private var endOfDay: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFournisseurCreditDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        app = application as MatelasProApp

        binding.btnBack.setOnClickListener { finish() }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        startOfDay = intent.getLongExtra("start_of_day", -1L)
        endOfDay = intent.getLongExtra("end_of_day", -1L)
        fournisseurName = intent.getStringExtra("fournisseur") ?: ""

        if (startOfDay < 0 || endOfDay < 0) { finish(); return }

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        binding.toolbar.title = "Crédit du ${sdf.format(Date(startOfDay))}"

        loadEntries()
    }

    private fun loadEntries() {
        lifecycleScope.launch {
            val entries = app.creditEntryRepository.getActiveByDateRangeAndFournisseur(startOfDay, endOfDay, fournisseurName)
            binding.recyclerView.adapter = CreditProductAdapter(entries,
                { entry -> showInfoDialog(entry) },
                { entry -> showCorrigerDialog(entry) },
                { entry -> showDeleteConfirmDialog(entry) })
            binding.textEmptyState.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun showDeleteConfirmDialog(entry: CreditEntry) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer le crédit")
            .setMessage("Voulez-vous vraiment supprimer le crédit de ${entry.productName} ?")
            .setPositiveButton("Supprimer") { _, _ ->
                lifecycleScope.launch {
                    app.auditLogRepository.insert("DELETE", "credit_entries", entry.id, entry.productName)
                    val linkedProduct = if (entry.productId != null) {
                        app.productRepository.getProductById(entry.productId!!)
                    } else null
                    app.creditEntryRepository.deleteById(entry.id)
                    if (linkedProduct != null) {
                        app.productRepository.delete(linkedProduct)
                    }
                    loadEntries()
                    Toast.makeText(this@FournisseurCreditDetailActivity, "Supprimé", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showInfoDialog(entry: CreditEntry) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
        val msg = buildString {
            append("Produit: ${entry.productName}\n")
            append("Catégorie: ${entry.category}\n")
            when (entry.category) {
                "Matelas" -> if (entry.epaisseur > 0) append("Épaisseur: ${entry.epaisseur}cm\n")
                "Pieces de bois" -> if (entry.longueur > 0) append("Longueur: ${entry.longueur}\n")
                "Meuble" -> if (entry.description.isNotEmpty()) append("Description: ${entry.description}\n")
            }
            append("Quantité: ${entry.quantity}\n")
            append("Prix unitaire: ${FormatUtil.montant(entry.sellingPrice)}\n")
            append("Total: ${FormatUtil.montant(entry.sellingPrice * entry.quantity)}\n")
            append("Date: ${sdf.format(Date(entry.createdAt))}\n")
            if (entry.notes.isNotEmpty()) {
                append("\nHistorique des modifications:\n${entry.notes}")
            }
        }
        AlertDialog.Builder(this)
            .setTitle("Détail du crédit")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showCorrigerDialog(entry: CreditEntry) {
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        val editName = EditText(this).apply {
            hint = "Nom du produit"
            setText(entry.productName)
        }
        val editQty = EditText(this).apply {
            hint = "Nouvelle quantité"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(entry.quantity.toString())
        }
        val editPrice = EditText(this).apply {
            hint = "Nouveau prix unitaire"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(entry.sellingPrice.toBigDecimal().stripTrailingZeros().toPlainString())
        }
        val editDate = EditText(this).apply {
            hint = "Date (jj/mm/aaaa)"
            inputType = android.text.InputType.TYPE_CLASS_DATETIME
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
            setText(sdf.format(Date(entry.createdAt)))
            isFocusable = false
            setOnClickListener {
                val cal = Calendar.getInstance().apply { timeInMillis = entry.createdAt }
                DatePickerDialog(this@FournisseurCreditDetailActivity, { _, y, m, d ->
                    cal.set(Calendar.YEAR, y)
                    cal.set(Calendar.MONTH, m)
                    cal.set(Calendar.DAY_OF_MONTH, d)
                    setText(SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(cal.time))
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }
        }

        // Category-specific fields
        val editCategoryField = EditText(this).apply {
            when (entry.category) {
                "Matelas" -> {
                    hint = "Épaisseur (cm)"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                    if (entry.epaisseur > 0) setText(entry.epaisseur.toBigDecimal().stripTrailingZeros().toPlainString())
                }
                "Pieces de bois" -> {
                    hint = "Longueur"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                    if (entry.longueur > 0) setText(entry.longueur.toBigDecimal().stripTrailingZeros().toPlainString())
                }
                "Meuble" -> {
                    hint = "Description"
                    if (entry.description.isNotEmpty()) setText(entry.description)
                }
            }
        }

        // Prix par cm for Matelas
        val editPrixCm = if (entry.category == "Matelas") {
            EditText(this).apply {
                hint = "Prix par cm"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                val initialPrixCm = if (entry.epaisseur > 0) entry.sellingPrice / entry.epaisseur else 0.0
                if (initialPrixCm > 0) setText(initialPrixCm.toBigDecimal().stripTrailingZeros().toPlainString())
            }
        } else null

        // Live total preview
        val textTotal = TextView(this).apply {
            text = "Total: 0.00 €"
            textSize = 16f
            setTextColor(0xFFB71C1C.toInt())
        }

        fun updateTotal() {
            val qty = editQty.text.toString().toIntOrNull() ?: 0
            val price = editPrice.text.toString().toDoubleOrNull() ?: 0.0
            textTotal.text = "Total: ${FormatUtil.montant(qty * price)}"
        }

        val qtyWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateTotal() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        editQty.addTextChangedListener(qtyWatcher)
        editPrice.addTextChangedListener(qtyWatcher)

        // Auto-calculate selling price for Matelas
        if (entry.category == "Matelas" && editPrixCm != null) {
            val matelasWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val ep = editCategoryField.text.toString().toDoubleOrNull() ?: 0.0
                    val cm = editPrixCm.text.toString().toDoubleOrNull() ?: 0.0
                    if (ep > 0 && cm > 0) {
                        val calculated = ep * cm
                        editPrice.setText(calculated.toBigDecimal().stripTrailingZeros().toPlainString())
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
            editCategoryField.addTextChangedListener(matelasWatcher)
            editPrixCm.addTextChangedListener(matelasWatcher)
        }

        listOfNotNull(editName, editQty, editPrice, editDate, editCategoryField, editPrixCm, textTotal).forEach { et ->
            ll.addView(et, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) })
        }

        // Initial total
        updateTotal()

        val dialog = AlertDialog.Builder(this)
            .setTitle("Corriger le crédit")
            .setView(ll)
            .setPositiveButton("Valider", null)
            .setNegativeButton("Annuler", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newName = editName.text.toString().trim()
                val newQty = editQty.text.toString().toIntOrNull() ?: 0
                val newPrice = editPrice.text.toString().toDoubleOrNull() ?: 0.0
                if (newName.isEmpty()) { Toast.makeText(this@FournisseurCreditDetailActivity, "Nom invalide", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                if (newQty <= 0) { Toast.makeText(this@FournisseurCreditDetailActivity, "Quantité invalide", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                if (newPrice <= 0) { Toast.makeText(this@FournisseurCreditDetailActivity, "Prix invalide", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

                lifecycleScope.launch {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                    val chosenDate = try {
                        val parsed = sdf.parse(editDate.text.toString())
                        if (parsed != null) {
                            val cal = Calendar.getInstance().apply { time = parsed }
                            val now = Calendar.getInstance()
                            cal.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                            cal.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                            cal.set(Calendar.SECOND, now.get(Calendar.SECOND))
                            cal.set(Calendar.MILLISECOND, now.get(Calendar.MILLISECOND))
                            cal.timeInMillis
                        } else System.currentTimeMillis()
                    } catch (e: Exception) { System.currentTimeMillis() }
                    val now = System.currentTimeMillis()
                    val sdfFull = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

                    val changes = mutableListOf<String>()
                    if (newName != entry.productName) changes.add("Nom: ${entry.productName} → $newName")
                    if (newQty != entry.quantity) changes.add("Qté: ${entry.quantity} → $newQty")
                    if (newPrice != entry.sellingPrice) changes.add("Prix: ${FormatUtil.montant(entry.sellingPrice)} → ${FormatUtil.montant(newPrice)}")

                    val newEpaisseur = if (entry.category == "Matelas") {
                        editCategoryField.text.toString().toDoubleOrNull() ?: 0.0
                    } else entry.epaisseur
                    val newLongueur = if (entry.category == "Pieces de bois") {
                        editCategoryField.text.toString().toDoubleOrNull() ?: 0.0
                    } else entry.longueur
                    val newDescription = if (entry.category == "Meuble") {
                        editCategoryField.text.toString().trim()
                    } else entry.description

                    if (entry.category == "Matelas" && newEpaisseur != entry.epaisseur) changes.add("Épaisseur: ${entry.epaisseur} → $newEpaisseur")
                    if (entry.category == "Pieces de bois" && newLongueur != entry.longueur) changes.add("Longueur: ${entry.longueur} → $newLongueur")
                    if (entry.category == "Meuble" && newDescription != entry.description) changes.add("Description: ${entry.description} → $newDescription")

                    val modLine = if (changes.isNotEmpty()) {
                        "${sdfFull.format(Date(now))} — ${changes.joinToString(", ")}"
                    } else {
                        "${sdfFull.format(Date(now))} — Aucun changement"
                    }
                    val notes = if (entry.notes.isEmpty()) modLine else "${entry.notes}\n$modLine"

                    // Mark original as overridden
                    app.creditEntryRepository.update(entry.copy(isOverridden = true))

                    // Create avoir (hidden, negative qty)
                    app.creditEntryRepository.insert(entry.copy(
                        id = 0,
                        quantity = -entry.quantity,
                        sellingPrice = entry.sellingPrice,
                        createdAt = now
                    ))

                    // Create corrected entry
                    val newCorrectionCount = entry.correctionCount + 1
                    app.creditEntryRepository.insert(entry.copy(
                        id = 0,
                        productName = newName,
                        quantity = newQty,
                        sellingPrice = newPrice,
                        epaisseur = newEpaisseur,
                        longueur = newLongueur,
                        description = newDescription,
                        createdAt = chosenDate,
                        notes = notes,
                        productId = entry.productId,
                        isOverridden = false,
                        correctionCount = newCorrectionCount
                    ))

                    // Update product in stock if linked
                    val pid = entry.productId
                    if (pid != null) {
                        val product = app.productRepository.getProductById(pid)
                        if (product != null) {
                            val sold = app.saleRepository.getTotalQuantitySoldByProductId(pid)
                            val newStock = newQty - sold
                            if (newStock >= 0) {
                                val newPrixCm = editPrixCm?.text?.toString()?.toDoubleOrNull() ?: product.prixUnitaireCm
                                val newPurchasePrice = when (entry.category) {
                                    "Matelas" -> product.purchasePrice
                                    else -> newPrice
                                }
                                app.productRepository.update(product.copy(
                                    name = newName,
                                    quantity = newStock,
                                    sellingPrice = newPrice,
                                    purchasePrice = newPurchasePrice,
                                    prixUnitaireCm = newPrixCm,
                                    epaisseur = if (entry.category == "Matelas") newEpaisseur else product.epaisseur,
                                    longueur = if (entry.category == "Pieces de bois") newLongueur else product.longueur,
                                    description = if (entry.category == "Meuble") newDescription else product.description
                                ))
                            }
                        }
                    }

                    app.auditLogRepository.insert("CORRECT", "credit_entries", entry.id, entry.productName)
                    loadEntries()
                    Toast.makeText(this@FournisseurCreditDetailActivity, "Crédit corrigé", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }
}

class CreditProductAdapter(
    private val items: List<CreditEntry>,
    private val onInfo: (CreditEntry) -> Unit,
    private val onCorriger: (CreditEntry) -> Unit,
    private val onDelete: (CreditEntry) -> Unit
) : RecyclerView.Adapter<CreditProductAdapter.ViewHolder>() {

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFournisseurProductBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        val p = items[pos]
        val extra = when {
            p.category == "Matelas" && p.epaisseur > 0 -> " | ${p.epaisseur}cm"
            p.category == "Pieces de bois" && p.longueur > 0 -> " | ${p.longueur}m"
            p.category == "Meuble" && p.description.isNotEmpty() -> " | ${p.description}"
            else -> ""
        }
        holder.binding.textProductName.text = p.productName + extra
        holder.binding.textProductInfo.text = "Qté: ${p.quantity} | ${FormatUtil.montant(p.sellingPrice * p.quantity)}"
        holder.binding.textTotal.text = FormatUtil.montant(p.sellingPrice * p.quantity)

        if (p.notes.isNotEmpty()) {
            holder.binding.btnInfo.visibility = View.VISIBLE
            holder.binding.btnInfo.setOnClickListener { onInfo(p) }
        } else {
            holder.binding.btnInfo.visibility = View.GONE
        }

        if (p.correctionCount >= 2) {
            holder.binding.btnCorriger.visibility = View.GONE
        } else {
            holder.binding.btnCorriger.visibility = View.VISIBLE
            holder.binding.btnCorriger.setOnClickListener { onCorriger(p) }
        }

        holder.binding.btnDelete.setOnClickListener { onDelete(p) }
    }

    class ViewHolder(val binding: ItemFournisseurProductBinding) : RecyclerView.ViewHolder(binding.root)
}
