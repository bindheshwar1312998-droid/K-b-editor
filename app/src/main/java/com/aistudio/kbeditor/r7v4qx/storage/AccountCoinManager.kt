package com.aistudio.kbeditor.r7v4qx.storage

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.time.LocalDate

data class ProAccountState(
    val isLoggedIn: Boolean = true,
    val userName: String = "",
    val userEmail: String = "",
    val isProPlan: Boolean = true,
    val dailyCoins: Int = 50,
    val lastResetDate: String = ""
)

class AccountCoinManager(context: Context) {

    private val prefs = context.getSharedPreferences("kb_pro_account_prefs", Context.MODE_PRIVATE)

    fun loadState(): ProAccountState {
        val today = LocalDate.now().toString()
        val isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false)
        val name = prefs.getString(KEY_NAME, "") ?: ""
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val lastDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        var coins = prefs.getInt(KEY_COINS, 50)

        if (isLoggedIn && lastDate != today) {
            coins = DAILY_PRO_COINS
            prefs.edit()
                .putInt(KEY_COINS, coins)
                .putString(KEY_LAST_DATE, today)
                .apply()
        }

        return ProAccountState(
            isLoggedIn = isLoggedIn,
            userName = name,
            userEmail = email,
            isProPlan = true,
            dailyCoins = coins,
            lastResetDate = if (isLoggedIn) today else lastDate
        )
    }

    fun login(name: String, email: String): ProAccountState {
        val today = LocalDate.now().toString()
        val cleanName = name.trim().ifBlank { "Pro Creator" }
        val cleanEmail = email.trim().ifBlank { "creator@kbeditor.pro" }
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_NAME, cleanName)
            .putString(KEY_EMAIL, cleanEmail)
            .putInt(KEY_COINS, DAILY_PRO_COINS)
            .putString(KEY_LAST_DATE, today)
            .apply()
        val updated = loadState()
        syncToFirestore(updated)
        return updated
    }

    fun logout(): ProAccountState {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, false)
            .apply()
        return loadState()
    }

    fun spendCoins(cost: Int): Pair<Boolean, ProAccountState> {
        val current = loadState()
        if (!current.isLoggedIn || current.dailyCoins < cost) {
            return false to current
        }
        val remaining = (current.dailyCoins - cost).coerceAtLeast(0)
        prefs.edit().putInt(KEY_COINS, remaining).apply()
        val updated = loadState()
        syncToFirestore(updated)
        return true to updated
    }

    private fun syncToFirestore(state: ProAccountState) {
        if (!state.isLoggedIn || state.userEmail.isBlank()) return
        runCatching {
            val docId = state.userEmail.lowercase().replace(Regex("[^a-z0-9]"), "_")
            val payload = hashMapOf(
                "userName" to state.userName,
                "userEmail" to state.userEmail,
                "dailyCoins" to state.dailyCoins,
                "lastResetDate" to state.lastResetDate,
                "isProPlan" to state.isProPlan
            )
            Firebase.firestore.collection("kb_users").document(docId).set(payload)
        }
    }

    companion object {
        const val DAILY_PRO_COINS = 50
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_COINS = "daily_coins"
        private const val KEY_LAST_DATE = "last_reset_date"
    }
}
