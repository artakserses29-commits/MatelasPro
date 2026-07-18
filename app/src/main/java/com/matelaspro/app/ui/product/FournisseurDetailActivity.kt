package com.matelaspro.app.ui.product

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.data.firestore.CreditEntryFS
import com.matelaspro.app.data.firestore.FournisseurPaiementFS
import com.matelaspro.app.databinding.ActivityFournisseurDetailBinding
import com.matelaspro.app.databinding.ItemFournisseurHistoryBinding
import com.matelaspro.app.util.FormatUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class HistoryItem(
    val type: Int,
    val dateLabel: String,
    val montant: Double,
    val timestamp: Long = 0L,
    val startOfDay: Long = 0L,
    val endOfDay: Long = 0L
) {
    companion object {
        const val TYPE_CREDIT = 0
        const val TYPE_PAIEMENT = 1
    }
}

class FournisseurDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFournisseurDetailBinding
    private lateinit var app: MatelasProApp
    private var fournisseur: com.matelaspro.app.data.firestore.FournisseurFS? = null
    private var fournisseurId: String = ""
    private var allHistoryItems = listOf<HistoryItem>()
    private var showAllHistory = false
    private val PAGE_SIZE = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFournisseurDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        app = application as MatelasProApp

        fournisseurId = intent.getStringExtra("fournisseur_id") ?: ""
        if (fournisseurId.isEmpty()) { finish(); return }

        binding.btnBack.setOnClickListener { finish() }

        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)

        binding.btnAjouterProduit.setOnClickListener {
            val name = fournisseur?.name ?: return@setOnClickListener
            val i = Intent(this, ProductActivity::class.java)
            i.putExtra("prefill_fournisseur", name)
            startActivity(i)
        }

        binding.btnPayer.setOnClickListener { showPaiementDialog() }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        binding.skeleton.root.apply {
            visibility = android.view.View.VISIBLE
            startShimmer()
        }
        lifecycleScope.launch {
            try {
                fournisseur = app.firestoreService.getFournisseurById(fournisseurId)
                val f = fournisseur ?: return@launch
                binding.textFournisseurName.text = f.name

                val allCredits = app.firestoreService.getCreditEntriesByFournisseur(f.name)
                val activeCredits = allCredits.filter { it.quantity > 0 && !it.isOverridden }
                val totalADevoir = activeCredits.sumOf { it.sellingPrice * it.quantity }
                val montantVerse = app.firestoreService.getTotalPayeByFournisseur(f.id)
                val reste = totalADevoir - montantVerse

                binding.textReste.text = FormatUtil.montant(reste)
                binding.skeleton.root.apply {
                    stopShimmer()
                    visibility = android.view.View.GONE
                }

                app.firestoreService.getPaiementsByFournisseurFlow(f.id).collect { list ->
                    buildHistory(activeCredits, list)
                }
            } catch (_: CancellationException) {
                binding.skeleton.root.stopShimmer()
            } catch (e: Exception) {
                binding.skeleton.root.stopShimmer()
                Toast.makeText(this@FournisseurDetailActivity, "Erreur: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun buildHistory(credits: List<CreditEntryFS>, paiements: List<FournisseurPaiementFS>) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        val items = mutableListOf<HistoryItem>()

        val creditsByDate = credits.groupBy { p ->
            val cal = Calendar.getInstance().apply { timeInMillis = p.createdAt?.toDate()?.time ?: 0L }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        for ((startOfDay, prods) in creditsByDate) {
            val cal = Calendar.getInstance().apply { timeInMillis = startOfDay }
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endOfDay = cal.timeInMillis
            val total = prods.sumOf { it.sellingPrice * it.quantity }
            items.add(HistoryItem(
                type = HistoryItem.TYPE_CREDIT,
                dateLabel = sdf.format(Date(startOfDay)),
                montant = total,
                timestamp = startOfDay,
                startOfDay = startOfDay,
                endOfDay = endOfDay
            ))
        }

        for (p in paiements) {
            items.add(HistoryItem(
                type = HistoryItem.TYPE_PAIEMENT,
                dateLabel = sdf.format(Date(p.createdAt?.toDate()?.time ?: 0L)),
                montant = p.montant,
                timestamp = p.createdAt?.toDate()?.time ?: 0L
            ))
        }

        items.sortByDescending { it.timestamp }
        allHistoryItems = items
        showAllHistory = false
        displayHistory()
    }

    private fun displayHistory() {
        val displayList = if (showAllHistory) allHistoryItems
        else allHistoryItems.take(PAGE_SIZE)

        val hasMore = allHistoryItems.size > PAGE_SIZE && !showAllHistory

        binding.recyclerHistory.adapter = HistoryAdapter(displayList, fournisseur?.name ?: "", hasMore) {
            showAllHistory = true
            displayHistory()
        }
        binding.textEmptyHistory.visibility = if (allHistoryItems.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerHistory.visibility = if (allHistoryItems.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showPaiementDialog() {
        val editMontant = EditText(this).apply {
            hint = "Montant"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        FormatUtil.applyMoneyFormat(editMontant)
        val ll = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
            addView(editMontant, android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        AlertDialog.Builder(this)
            .setTitle("Nouveau paiement")
            .setView(ll)
            .setPositiveButton("Valider", null)
            .setNegativeButton("Annuler", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val montant = FormatUtil.parseMontant(editMontant.text.toString())
                        if (montant <= 0) {
                            editMontant.error = "Montant invalide"
                            editMontant.requestFocus()
                            return@setOnClickListener
                        }
                        lifecycleScope.launch {
                            try {
                                val soldeInitial = getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE).getFloat("solde_initial", 0f).toDouble()
                                val sales = app.firestoreService.getAllSales()
                                val expenses = app.firestoreService.getAllExpenses()
                                val paiements = app.firestoreService.getAllPaiements()
                                val currentBalance = soldeInitial + sales.sumOf { it.totalAmount } - expenses.sumOf { it.amount } - paiements.sumOf { it.montant }
                                if (currentBalance - montant < 0) {
                                    editMontant.error = "Balance insuffisante (${com.matelaspro.app.util.FormatUtil.montant(currentBalance)})"
                                    return@launch
                                }
                                app.firestoreService.setPaiement(null,
                                    FournisseurPaiementFS(fournisseurId = fournisseurId, montant = montant)
                                )
                                dismiss()
                                loadData()
                            } catch (_: CancellationException) {
                            } catch (e: Exception) {
                                editMontant.error = "Erreur lors du paiement"
                            }
                        }
                    }
                }
                show()
            }
    }
}

class HistoryAdapter(
    private val items: List<HistoryItem>,
    private val fournisseurName: String,
    private val hasMore: Boolean = false,
    private val onShowAll: (() -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val TYPE_ITEM = 0
    private val TYPE_SHOW_ALL = 1

    override fun getItemCount() = items.size + if (hasMore) 1 else 0
    override fun getItemViewType(position: Int): Int {
        return if (position == items.size && hasMore) TYPE_SHOW_ALL else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_SHOW_ALL) {
            val tv = TextView(parent.context).apply {
                text = "Voir tout l'historique"
                setTextColor(parent.context.getColor(com.matelaspro.app.R.color.primary))
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 12, 0, 12)
            }
            return object : RecyclerView.ViewHolder(tv) {}
        }
        val binding = ItemFournisseurHistoryBinding.inflate(android.view.LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        if (getItemViewType(pos) == TYPE_SHOW_ALL) {
            holder.itemView.setOnClickListener { onShowAll?.invoke() }
            return
        }
        val item = items[pos]
        val h = holder as ViewHolder
        h.binding.textHistoryMontant.text = FormatUtil.montant(item.montant)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
        h.binding.textHistoryDate.text = sdf.format(Date(item.timestamp))
        when (item.type) {
            HistoryItem.TYPE_CREDIT -> {
                h.binding.textHistoryType.text = "Crédit"
                h.binding.textHistoryType.setTextColor(h.itemView.context.getColor(com.matelaspro.app.R.color.red))
                h.binding.textHistoryMontant.setTextColor(h.itemView.context.getColor(com.matelaspro.app.R.color.red))
                h.binding.root.setOnClickListener {
                    val ctx = h.itemView.context
                    val intent = Intent(ctx, FournisseurCreditDetailActivity::class.java).apply {
                        putExtra("start_of_day", item.startOfDay)
                        putExtra("end_of_day", item.endOfDay)
                        putExtra("fournisseur", fournisseurName)
                    }
                    ctx.startActivity(intent)
                }
            }
            HistoryItem.TYPE_PAIEMENT -> {
                h.binding.textHistoryType.text = "Paiement"
                h.binding.textHistoryType.setTextColor(h.itemView.context.getColor(com.matelaspro.app.R.color.green))
                h.binding.textHistoryMontant.setTextColor(h.itemView.context.getColor(com.matelaspro.app.R.color.green))
                h.binding.root.setOnClickListener(null)
            }
        }
    }

    class ViewHolder(val binding: ItemFournisseurHistoryBinding) : RecyclerView.ViewHolder(binding.root)
}
