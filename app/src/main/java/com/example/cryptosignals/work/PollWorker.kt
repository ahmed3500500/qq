import kotlinx.coroutines.flow.firstOrNull

package com.example.cryptosignals.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cryptosignals.net.Api
import com.example.cryptosignals.ui.AppSettings

class PollWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val settings = AppSettings(applicationContext)
        val server = settings.serverFlow.firstOrNull() ?: "http://37.49.228.169:8000"
        val notifs = settings.notifsFlow.firstOrNull() ?: true
        if (!notifs) return Result.success()

        val strongOnly = settings.strongOnlyFlow.firstOrNull() ?: true
        val minScore = settings.minScoreFlow.firstOrNull() ?: 70.0

        val api = Api(server)
        val resp = api.opportunities(limit = 20).getOrNull() ?: return Result.retry()

        val seen = settings.loadSeen().toMutableSet()
        var any = false
        for (s in resp.signals) {
            val score = s.score ?: 0.0
            if (strongOnly && score < minScore) continue
            val key = "${'$'}{s.symbol}|${'$'}{s.timeframe}|${'$'}{s.createdAt}|${'$'}score"
            val hash = key.hashCode().toString()
            if (seen.contains(hash)) continue
            seen.add(hash)
            any = true
            val title = applicationContext.getString(com.example.cryptosignals.R.string.notif_title)
            val text = "${'$'}{s.symbol ?: "?"}  ${'$'}{s.timeframe ?: ""}  score=${'$'}{String.format("%.1f", score)}\n${'$'}{s.reasons ?: ""}"
            Notifier.notify(applicationContext, title, text, hash.hashCode())
        }
        if (any) settings.saveSeen(seen)
        return Result.success()
    }
}
