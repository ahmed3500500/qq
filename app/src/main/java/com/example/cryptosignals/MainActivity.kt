package com.example.cryptosignals

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cryptosignals.net.Api
import com.example.cryptosignals.net.SignalItem
import com.example.cryptosignals.ui.AppSettings
import com.example.cryptosignals.work.PollWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Schedule polling every 15 minutes
        val req = PeriodicWorkRequestBuilder<PollWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "poll_opportunities",
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )

        setContent { AppRoot() }
    }
}

@Composable
private fun AppRoot() {
    val ctx = LocalContext.current
    val settings = remember { AppSettings(ctx) }

    val dark by settings.darkFlow.collectAsState(initial = true)
    val server by settings.serverFlow.collectAsState(initial = "http://37.49.228.169:8000")

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            MainScreen(server = server, settings = settings)
        }
    }
}

@Composable
private fun MainScreen(server: String, settings: AppSettings) {
    var tab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Header()

        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.tab_opportunities)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.tab_latest)) })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.tab_settings)) })
        }

        when (tab) {
            0 -> SignalsList(server = server, endpoint = "opportunities")
            1 -> SignalsList(server = server, endpoint = "latest")
            else -> SettingsScreen(server = server, settings = settings)
        }
    }
}

@Composable
private fun Header() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.header_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${'$'}server",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SignalsList(server: String, endpoint: String) {
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<SignalItem>>(emptyList()) }
    var lastUpdate by remember { mutableStateOf<String?>(null) }

    fun load() {
        loading = true
        error = null
        val api = Api(server)
        val res = if (endpoint == "latest") api.latest(50) else api.opportunities(50)
        val r = res.getOrNull()
        if (r == null) {
            error = stringResource(R.string.network_error)
        } else {
            items = r.signals
            lastUpdate = java.time.LocalTime.now().toString().substring(0,8)
        }
        loading = false
    }

    LaunchedEffect(endpoint, server) { load() }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { load() }, enabled = !loading) {
                Text(stringResource(R.string.refresh))
            }
            Spacer(Modifier.width(12.dp))
            if (lastUpdate != null) {
                Text("${'$'}{stringResource(R.string.last_update)}: ${'$'}lastUpdate", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (loading) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(8.dp))

        if (!loading && items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { s ->
                    SignalCard(s)
                }
            }
        }
    }
}

@Composable
private fun SignalCard(s: SignalItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(s.symbol ?: "?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(s.timeframe ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "score: ${'$'}{s.score ?: 0.0}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (!s.reasons.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(s.reasons!!, style = MaterialTheme.typography.bodySmall)
            }
            if (!s.createdAt.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(s.createdAt!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsScreen(server: String, settings: AppSettings) {
    val ctx = LocalContext.current
    val dark by settings.darkFlow.collectAsState(initial = true)
    val notifs by settings.notifsFlow.collectAsState(initial = true)
    val strongOnly by settings.strongOnlyFlow.collectAsState(initial = true)
    val minScore by settings.minScoreFlow.collectAsState(initial = 70.0)

    var serverText by remember { mutableStateOf(server) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.server), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = serverText,
            onValueChange = { serverText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.server_hint)) }
        )
        Button(onClick = {
            kotlinx.coroutines.MainScope().launch { settings.setServer(serverText.trim()) }
        }) { Text("OK") }

        Divider()

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.dark_mode))
            Switch(checked = dark, onCheckedChange = { v -> kotlinx.coroutines.MainScope().launch { settings.setDark(v) } })
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.notifications))
            Switch(checked = notifs, onCheckedChange = { v -> kotlinx.coroutines.MainScope().launch { settings.setNotifs(v) } })
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.notify_strong_only))
            Switch(checked = strongOnly, onCheckedChange = { v -> kotlinx.coroutines.MainScope().launch { settings.setStrongOnly(v) } })
        }

        val minScoreLabel = stringResource(R.string.min_score)
        val minScoreValue = String.format("%.0f", minScore)
        Text("$minScoreLabel: $minScoreValue")
        Slider(
            value = minScore.toFloat(),
            onValueChange = { v -> kotlinx.coroutines.MainScope().launch { settings.setMinScore(v.toDouble()) } },
            valueRange = 0f..100f
        )

        Divider()

        Text(
            text = "Scheduler runs every 15 minutes via WorkManager.\nNo login. Arabic/English automatic.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
