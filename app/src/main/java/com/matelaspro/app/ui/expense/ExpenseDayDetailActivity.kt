package com.matelaspro.app.ui.expense

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.matelaspro.app.MatelasProApp
import com.matelaspro.app.R
import com.matelaspro.app.service.SessionManager
import com.matelaspro.app.util.FormatUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseDayDetailActivity : AppCompatActivity() {

    private lateinit var binding: com.matelaspro.app.databinding.ActivityExpenseDayDetailBinding
    private lateinit var app: MatelasProApp
    private var currentUserId: String = ""
    private var dayStart: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = com.matelaspro.app.databinding.ActivityExpenseDayDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dayStart = intent.getLongExtra("dayStart", 0)
        if (dayStart == 0L) { finish(); return }

        currentUserId = intent.getStringExtra("selectedUserId") ?: SessionManager.currentUserId
        app = application as MatelasProApp

        val sdf = SimpleDateFormat("EEEE dd/MM/yyyy", Locale.FRANCE)
        binding.toolbar.title = sdf.format(Date(dayStart))
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadDayExpenses()
    }

    private fun loadDayExpenses() {
        lifecycleScope.launch {
            val allExpenses = if (currentUserId.isEmpty()) app.firestoreService.getAllExpenses()
            else app.firestoreService.getExpensesByUserList(currentUserId)

            val dayEnd = dayStart + 86400000L
            val dayExpenses = allExpenses.filter { (it.createdAt?.toDate()?.time ?: 0L) >= dayStart && (it.createdAt?.toDate()?.time ?: 0L) < dayEnd }
                .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }

            val userMap = if (SessionManager.isAdmin) {
                app.firestoreService.getAllUsers().associate { it.id to it.name }
            } else emptyMap()

            val total = dayExpenses.sumOf { it.amount }
            binding.textDayTotal.text = FormatUtil.montant(total)

            binding.layoutDayExpenses.removeAllViews()
            if (dayExpenses.isEmpty()) {
                val tv = TextView(this@ExpenseDayDetailActivity).apply {
                    text = "Aucune dépense ce jour"
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.grey_medium, theme))
                }
                binding.layoutDayExpenses.addView(tv)
            } else {
                val timeSdf = SimpleDateFormat("HH:mm", Locale.FRANCE)
                for (expense in dayExpenses) {
                    val card = layoutInflater.inflate(R.layout.item_expense_detail_card, null)
                    card.findViewById<TextView>(R.id.textAmount).text = FormatUtil.montant(expense.amount)
                    card.findViewById<TextView>(R.id.textDescription).text =
                        if (expense.description.isNotEmpty()) expense.description else "Sans description"
                    card.findViewById<TextView>(R.id.textTime).text = timeSdf.format(Date(expense.createdAt?.toDate()?.time ?: 0L))
                    if (SessionManager.isAdmin) {
                        val tvUser = card.findViewById<TextView>(R.id.textUser)
                        tvUser.text = userMap[expense.userId] ?: "Utilisateur #${expense.userId}"
                        tvUser.visibility = TextView.VISIBLE
                    }
                    binding.layoutDayExpenses.addView(card)
                }
            }
        }
    }
}
