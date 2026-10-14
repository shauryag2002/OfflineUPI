package com.offlineupi.ui.home

import android.content.Context

class RecentPayeeStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getRecentPayees(): List<String> {
        val raw = prefs.getString(KEY_RECENT_PAYEES, "").orEmpty()
        if (raw.isBlank()) {
            return emptyList()
        }
        return raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }
    }

    fun savePayee(payee: String) {
        val normalized = payee.trim().lowercase()
        if (normalized.isBlank()) {
            return
        }

        val updated = mutableListOf(normalized)
        updated.addAll(getRecentPayees().filterNot { it.equals(normalized, ignoreCase = true) })

        prefs.edit()
            .putString(KEY_RECENT_PAYEES, updated.take(MAX_ITEMS).joinToString(separator = "\n"))
            .apply()
    }

    companion object {
        private const val PREF_NAME = "offline_upi_prefs"
        private const val KEY_RECENT_PAYEES = "recent_payees"
        private const val MAX_ITEMS = 12
    }
}
