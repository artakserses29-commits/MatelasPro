package com.matelaspro.app.util

import android.widget.EditText
import java.text.NumberFormat
import java.util.Locale

object FormatUtil {
    private val nf = NumberFormat.getNumberInstance(Locale.FRANCE).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    private val nfNoFrac = NumberFormat.getNumberInstance(Locale.FRANCE).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    private val nfInput = NumberFormat.getNumberInstance(Locale.FRANCE).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
        isGroupingUsed = true
    }

    fun montant(v: Double): String = nf.format(v) + " MGA"
    fun montantSansDevise(v: Double): String = nfNoFrac.format(v)

    fun parseMontant(valeur: String): Double {
        val clean = valeur.replace("[^\\d,\\.]".toRegex(), "").replace(",", ".").trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    fun applyMoneyFormat(editText: EditText) {
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val raw = editText.text.toString()
                val clean = raw.replace("[^\\d]".toRegex(), "")
                if (clean.isNotEmpty()) {
                    try {
                        editText.setText(nfInput.format(clean.toLong()) + " MGA")
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
