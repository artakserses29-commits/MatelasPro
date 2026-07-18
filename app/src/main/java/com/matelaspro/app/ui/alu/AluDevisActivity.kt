package com.matelaspro.app.ui.alu

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.matelaspro.app.data.entity.AluProduct
import com.matelaspro.app.databinding.ActivityAluDevisBinding
import com.matelaspro.app.databinding.DialogClientInfoBinding
import com.matelaspro.app.databinding.ItemAluProductDevisBinding
import com.matelaspro.app.databinding.ItemDevisItemBinding
import java.text.NumberFormat
import java.util.Currency
import kotlin.math.ceil
import kotlin.math.roundToLong

data class DevisItem(
    val product: AluProduct,
    var surface: Double = product.surface
) {
    val quantity: Int get() = ceil(surface / product.surface).toInt()
    val montantBrut: Double get() = (surface / product.surface) * product.prixUnitaire
    val prixTotal: Double get() = roundToNearest(montantBrut)
}

private fun roundToNearest(value: Double): Double {
    return (value / 100.0).roundToLong() * 100.0
}

class AluDevisActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAluDevisBinding
    private lateinit var viewModel: AluViewModel
    private lateinit var productAdapter: ProductCardAdapter
    private lateinit var devisAdapter: DevisItemsAdapter
    private val devisItems = mutableListOf<DevisItem>()
    private var editingDevisId: Long? = null
    private var editingClientName: String = ""
    private var editingClientAddress: String = ""
    private var editingClientPhone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAluDevisBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[AluViewModel::class.java]

        editingDevisId = intent.getLongExtra("devis_id", -1L).let { if (it == -1L) null else it }

        binding.btnBack.setOnClickListener { finish() }

        if (editingDevisId != null) {
            binding.toolbar.title = "Modifier devis"
            binding.btnSaveDevis.text = "Mettre à jour"
        }

        productAdapter = ProductCardAdapter { product ->
            val existing = devisItems.find { it.product.id == product.id }
            if (existing != null) {
                existing.surface += product.surface
                devisAdapter.notifyDataSetChanged()
            } else {
                devisItems.add(DevisItem(product))
            }
            devisAdapter.notifyDataSetChanged()
            updateTotal()
        }
        binding.recyclerProducts.layoutManager = LinearLayoutManager(this)
        binding.recyclerProducts.adapter = productAdapter

        devisAdapter = DevisItemsAdapter(
            items = devisItems,
            onTotalChanged = { updateTotal() }
        )
        binding.recyclerDevis.layoutManager = LinearLayoutManager(this)
        binding.recyclerDevis.adapter = devisAdapter

        binding.btnSaveDevis.setOnClickListener { showClientDialog() }

        viewModel.allProducts.observe(this) { products ->
            productAdapter.submitList(products)
        }

        if (editingDevisId != null) {
            viewModel.getDevisById(editingDevisId!!) { devis ->
                if (devis != null) {
                    editingClientName = devis.clientName
                    editingClientAddress = devis.clientAddress
                    editingClientPhone = devis.clientPhone
                    loadDevisItems(devis)
                }
            }
        }
    }

    private fun loadDevisItems(devis: com.matelaspro.app.data.entity.AluDevis) {
        val lines = devis.items.split("\n")
        for (line in lines) {
            val parts = line.split("|")
            if (parts.size >= 6) {
                val productId = parts[0].toLongOrNull() ?: 0L
                val name = parts[1]
                val surface = parts[3].toDoubleOrNull() ?: 0.0
                val pu = parts[4].toDoubleOrNull() ?: 0.0
                val product = AluProduct(id = productId, name = name, surface = surface, prixUnitaire = pu)
                devisItems.add(DevisItem(product, surface))
            }
        }
        devisAdapter.notifyDataSetChanged()
        updateTotal()
    }

    private fun updateTotal() {
        val format = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
        val total = devisItems.sumOf { it.prixTotal }
        binding.textTotal.text = "Total: ${format.format(total)}"
    }

    private fun showClientDialog() {
        if (devisItems.isEmpty()) {
            Toast.makeText(this, "Ajoutez des produits au devis", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogBinding = DialogClientInfoBinding.inflate(layoutInflater)

        if (editingDevisId != null) {
            dialogBinding.editNom.setText(editingClientName)
            dialogBinding.editAdresse.setText(editingClientAddress)
            dialogBinding.editTelephone.setText(editingClientPhone)
        }

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Confirmer") { _, _ ->
                val nom = dialogBinding.editNom.text.toString().trim()
                val adresse = dialogBinding.editAdresse.text.toString().trim()
                val telephone = dialogBinding.editTelephone.text.toString().trim()
                if (nom.isEmpty()) {
                    Toast.makeText(this, "Le nom du client est requis", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val total = devisItems.sumOf { it.prixTotal }

                if (editingDevisId != null) {
                    viewModel.updateDevis(
                        editingDevisId!!, devisItems.toList(), total,
                        clientName = nom, clientAddress = adresse, clientPhone = telephone
                    )
                    Toast.makeText(this, "Devis mis à jour !", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.saveDevis(nom, adresse, telephone, devisItems.toList(), total)
                    devisItems.clear()
                    devisAdapter.notifyDataSetChanged()
                    updateTotal()
                    Toast.makeText(this, "Devis enregistré !", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}

private class ProductCardAdapter(
    private val onClick: (AluProduct) -> Unit
) : RecyclerView.Adapter<ProductCardAdapter.ViewHolder>() {
    private var items = listOf<AluProduct>()

    fun submitList(list: List<AluProduct>) { items = list; notifyDataSetChanged() }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAluProductDevisBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = items[position]
        val format = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
        holder.binding.textProductName.text = product.name
        holder.binding.textSurface.text = "${product.surface}"
        holder.binding.textPrixUnitaire.text = format.format(product.prixUnitaire)
        holder.binding.root.setOnClickListener { onClick(product) }
    }

    class ViewHolder(val binding: ItemAluProductDevisBinding) : RecyclerView.ViewHolder(binding.root)
}

class DevisItemsAdapter(
    private val items: MutableList<DevisItem>,
    private val onTotalChanged: () -> Unit
) : RecyclerView.Adapter<DevisItemsAdapter.ViewHolder>() {

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDevisItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val fmt = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("MGA") }
        holder.binding.textProductName.text = item.product.name
        holder.binding.textPrixUnitaire.text = fmt.format(item.product.prixUnitaire)
        holder.binding.textQuantity.text = item.quantity.toString()
        holder.binding.textPrixTotal.text = fmt.format(item.prixTotal)
        holder.binding.editSurface.removeTextChangedListener(holder.surfaceWatcher)
        holder.binding.editSurface.setText(if (item.surface == item.product.surface) "" else item.surface.toString())
        holder.binding.editSurface.hint = item.product.surface.toString()
        holder.binding.editSurface.addTextChangedListener(holder.surfaceWatcher)
        holder.currentItem = item
        holder.currentFormat = fmt
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.attachListeners()
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.detachListeners()
    }

    inner class ViewHolder(val binding: ItemDevisItemBinding) : RecyclerView.ViewHolder(binding.root) {
        var currentItem: DevisItem? = null
        var currentFormat: NumberFormat? = null

        private fun refreshAll() {
            val item = currentItem ?: return
            val fmt = currentFormat
            binding.textQuantity.text = item.quantity.toString()
            binding.textPrixTotal.text = fmt?.format(item.prixTotal) ?: item.prixTotal.toString()
            onTotalChanged()
        }

        val surfaceWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val item = currentItem ?: return
                val input = s.toString().trim()
                if (input.isEmpty()) {
                    item.surface = item.product.surface
                } else {
                    val v = input.toDoubleOrNull()
                    if (v != null && v > 0) item.surface = v
                }
                refreshAll()
            }
        }

        fun attachListeners() {
            binding.editSurface.addTextChangedListener(surfaceWatcher)
            binding.btnMinus.setOnClickListener {
                val item = currentItem ?: return@setOnClickListener
                val step = item.product.surface
                if (item.surface - step >= 0.01) {
                    item.surface -= step
                    binding.editSurface.removeTextChangedListener(surfaceWatcher)
                    binding.editSurface.setText(if (item.surface == item.product.surface) "" else item.surface.toString())
                    binding.editSurface.addTextChangedListener(surfaceWatcher)
                    refreshAll()
                }
            }
            binding.btnPlus.setOnClickListener {
                val item = currentItem ?: return@setOnClickListener
                item.surface += item.product.surface
                binding.editSurface.removeTextChangedListener(surfaceWatcher)
                binding.editSurface.setText(if (item.surface == item.product.surface) "" else item.surface.toString())
                binding.editSurface.addTextChangedListener(surfaceWatcher)
                refreshAll()
            }
            binding.btnDelete.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val ctx = binding.root.context
                    AlertDialog.Builder(ctx)
                        .setTitle("Supprimer")
                        .setMessage("Supprimer ${items[pos].product.name} du devis ?")
                        .setPositiveButton("Supprimer") { _, _ ->
                            items.removeAt(pos)
                            notifyDataSetChanged()
                            onTotalChanged()
                        }
                        .setNegativeButton("Annuler", null)
                        .show()
                }
            }
        }

        fun detachListeners() {
            binding.editSurface.removeTextChangedListener(surfaceWatcher)
        }
    }
}
