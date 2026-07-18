package com.matelaspro.app.ui.expense

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.R
import com.matelaspro.app.data.firestore.ExpenseFS
import com.matelaspro.app.databinding.ActivityUserExpenseDetailBinding
import com.matelaspro.app.util.FormatUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserExpenseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserExpenseDetailBinding
    private lateinit var app: MatelasProApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserExpenseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = intent.getStringExtra("userId") ?: ""
        val userName = intent.getStringExtra("userName") ?: "Utilisateur"

        binding.toolbar.title = "$userName - Dépenses"
        binding.btnBack.setOnClickListener { finish() }

        app = application as MatelasProApp

        lifecycleScope.launch {
            val expenses = app.firestoreService.getExpensesByUserList(userId)
            renderExpenses(expenses)
        }
    }

    private fun renderExpenses(expenses: List<ExpenseFS>) {
        val total = expenses.sumOf { it.amount }
        binding.textTotalExpense.text = FormatUtil.montant(total)

        val sdfDay = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        val sdfMonth = SimpleDateFormat("MM/yyyy", Locale.FRANCE)
        val sdfFull = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

        // Par jour
        binding.layoutByDay.removeAllViews()
        val byDay = expenses.groupBy { sdfDay.format(Date(it.createdAt?.toDate()?.time ?: 0L)) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.key }
        if (byDay.isEmpty()) addText(binding.layoutByDay, "Aucune dépense")
        else for ((day, amt) in byDay) addRow(binding.layoutByDay, day, FormatUtil.montant(amt))

        // Par mois
        binding.layoutByMonth.removeAllViews()
        val byMonth = expenses.groupBy { sdfMonth.format(Date(it.createdAt?.toDate()?.time ?: 0L)) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.key }
        if (byMonth.isEmpty()) addText(binding.layoutByMonth, "Aucune dépense")
        else for ((month, amt) in byMonth) addRow(binding.layoutByMonth, month, FormatUtil.montant(amt))

        // Détail
        binding.layoutDetail.removeAllViews()
        if (expenses.isEmpty()) addText(binding.layoutDetail, "Aucune dépense")
        else for (e in expenses.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }) {
            val desc = if (e.description.isNotEmpty()) e.description else "Sans description"
            addRow(binding.layoutDetail, desc, "${FormatUtil.montant(e.amount)} - ${sdfFull.format(Date(e.createdAt?.toDate()?.time ?: 0L))}")
        }
    }

    private fun addText(layout: LinearLayout, text: String) {
        layout.addView(TextView(this).apply {
            this.text = text; textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.grey_medium))
        })
    }

    private fun addRow(layout: LinearLayout, title: String, subtitle: String) {
        val row = layoutInflater.inflate(R.layout.item_dashboard_row, null) as LinearLayout
        row.findViewById<TextView>(R.id.text_row_title).text = title
        row.findViewById<TextView>(R.id.text_row_subtitle).text = subtitle
        layout.addView(row)
        addDivider(layout)
    }

    private fun addDivider(layout: LinearLayout) {
        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { setMargins(0, 4, 0, 4) }
            setBackgroundColor(ContextCompat.getColor(this@UserExpenseDetailActivity, R.color.grey_medium))
        })
    }
}
