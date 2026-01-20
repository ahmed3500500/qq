package com.example.cryptosignals.ui

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull

private val Context.dataStore by preferencesDataStore(name = "settings")

object PrefKeys {
    val SERVER = stringPreferencesKey("server")
    val DARK = booleanPreferencesKey("dark")
    val NOTIFS = booleanPreferencesKey("notifs")
    val STRONG_ONLY = booleanPreferencesKey("strong_only")
    val MIN_SCORE = doublePreferencesKey("min_score")
    val SEEN_HASHES = stringPreferencesKey("seen_hashes")
}

class AppSettings(private val ctx: Context) {
    val serverFlow: Flow<String> = ctx.dataStore.data.map { it[PrefKeys.SERVER] ?: "http://37.49.228.169:8000" }
    val darkFlow: Flow<Boolean> = ctx.dataStore.data.map { it[PrefKeys.DARK] ?: true }
    val notifsFlow: Flow<Boolean> = ctx.dataStore.data.map { it[PrefKeys.NOTIFS] ?: true }
    val strongOnlyFlow: Flow<Boolean> = ctx.dataStore.data.map { it[PrefKeys.STRONG_ONLY] ?: true }
    val minScoreFlow: Flow<Double> = ctx.dataStore.data.map { it[PrefKeys.MIN_SCORE] ?: 70.0 }

    suspend fun setServer(v: String) = ctx.dataStore.edit { it[PrefKeys.SERVER] = v }
    suspend fun setDark(v: Boolean) = ctx.dataStore.edit { it[PrefKeys.DARK] = v }
    suspend fun setNotifs(v: Boolean) = ctx.dataStore.edit { it[PrefKeys.NOTIFS] = v }
    suspend fun setStrongOnly(v: Boolean) = ctx.dataStore.edit { it[PrefKeys.STRONG_ONLY] = v }
    suspend fun setMinScore(v: Double) = ctx.dataStore.edit { it[PrefKeys.MIN_SCORE] = v }

    suspend fun loadSeen(): Set<String> {
        val s = ctx.dataStore.data.map { it[PrefKeys.SEEN_HASHES] ?: "" }.firstOrNull() ?: ""
        return s.split('|').filter { it.isNotBlank() }.toSet()
    }
    suspend fun saveSeen(seen: Set<String>) = ctx.dataStore.edit { it[PrefKeys.SEEN_HASHES] = seen.take(500).joinToString("|") }
}
