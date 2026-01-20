package com.example.cryptosignals.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignalsResponse(
    val ok: Boolean = false,
    val signals: List<SignalItem> = emptyList()
)

@Serializable
data class SignalItem(
    val symbol: String? = null,
    val timeframe: String? = null,
    val score: Double? = null,
    val reasons: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class HealthResponse(
    val ok: Boolean = false,
    val app: String? = null,
    @SerialName("interval_minutes") val intervalMinutes: Int? = null,
    @SerialName("top_n") val topN: Int? = null,
    val timeframes: String? = null
)
