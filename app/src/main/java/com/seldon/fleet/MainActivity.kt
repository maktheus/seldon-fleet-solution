package com.seldon.fleet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.seldon.fleet.data.FleetRepository
import com.seldon.fleet.domain.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContent { FleetApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val devices = remember { FleetRepository(context).load() }
    val classifier = remember { ClassifyDevice() }
    val decisions = remember(devices) { devices.associateBy({ it.id }, { classifier.invoke(it) }) }
    val prefs = remember { context.getSharedPreferences("favorites", 0) }
    var favorites by remember {
        mutableStateOf(
            prefs.getStringSet("ids", emptySet()) ?: emptySet()
        )
    }
    val nav = rememberNavController()
    MaterialTheme {
        NavHost(nav, "list") {
            composable("list") {
                FleetList(
                    devices,
                    decisions,
                    favorites,
                    { id ->
                        favorites = favorites.toMutableSet()
                            .apply { if (!add(id)) remove(id) }; prefs.edit()
                        .putStringSet("ids", favorites).apply()
                    },
                    { id -> nav.navigate("detail/$id") })
            }
            composable("detail/{id}") { back ->
                devices.find { it.id == back.arguments?.getString("id") }
                    ?.let { DeviceDetail(it, decisions.getValue(it.id)) }
            }
        }
    }
}

private fun statusLabel(s: DeviceStatus) = when (s) {
    DeviceStatus.OK -> "OK"; DeviceStatus.ATENCAO -> "ATENÇÃO"; DeviceStatus.CRITICO -> "CRÍTICO"; DeviceStatus.INDETERMINADO -> "SEM DADOS"
}

private fun statusColor(s: DeviceStatus) = when (s) {
    DeviceStatus.OK -> Color(0xFF15803D); DeviceStatus.ATENCAO -> Color(0xFFB45309); DeviceStatus.CRITICO -> Color(
        0xFFB91C1C
    ); DeviceStatus.INDETERMINADO -> Color(0xFF64748B)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetList(
    devices: List<Device>,
    decisions: Map<String, Decision>,
    favorites: Set<String>,
    toggle: (String) -> Unit,
    open: (String) -> Unit
) {
    var filter by remember { mutableStateOf<DeviceStatus?>(null) }
    val counts =
        DeviceStatus.entries.associateWith { s -> devices.count { decisions.getValue(it.id).status == s } }
    val shown = devices.filter { filter == null || decisions.getValue(it.id).status == filter }
    Scaffold(topBar = { TopAppBar(title = { Text("Seldon Fleet") }) }) { pad ->
        Column(Modifier
            .padding(pad)
            .padding(horizontal = 16.dp)) {
            Text("${devices.size} dispositivos", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DeviceStatus.entries.forEach { s ->
                    FilterChip(
                        selected = filter == s,
                        onClick = { filter = if (filter == s) null else s },
                        label = { Text("${statusLabel(s)} ${counts.getValue(s)}") })
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    shown,
                    key = { it.id }) { d ->
                    val decision = decisions.getValue(d.id); Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable { open(d.id) }) {
                    Row(
                        Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                d.id,
                                style = MaterialTheme.typography.titleMedium
                            ); Text("${d.model} · bateria ${d.batteryLevelPct}%")
                        }; Text(
                        if (favorites.contains(d.id)) "★" else "☆",
                        Modifier
                            .clickable { toggle(d.id) }
                            .padding(8.dp),
                        color = Color(0xFFF59E0B)
                    ); Text(
                        statusLabel(decision.status),
                        color = statusColor(decision.status),
                        modifier = Modifier.padding(8.dp)
                    )
                    }
                }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetail(device: Device, decision: Decision) {
    Scaffold(topBar = { TopAppBar(title = { Text(device.id) }) }) { p ->
        Column(
            Modifier
                .padding(p)
                .padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(device.model, style = MaterialTheme.typography.headlineSmall); Text(
            statusLabel(
                decision.status
            ), color = statusColor(decision.status), style = MaterialTheme.typography.titleLarge
        ); Text(decision.reason); HorizontalDivider(); Text("SoC: ${device.soc}"); Text("RAM: ${device.ramMb} MB · Android API ${device.androidApi}"); Text(
            "Bateria: ${device.batteryLevelPct}% de ${device.batteryCapacityMah ?: "sem capacidade"} mAh"
        ); Text("Consumo normalizado: ${decision.energyMah?.let { "%.1f mAh".format(it) } ?: "indisponível"}"); Text(
            "Tempo de tela: ${device.screenOnMinutes} min"
        ); Text("Frames perdidos: ${device.jankFramesPct}% · falhas recentes: ${device.crashCount7d}"); Text(
            "Último contato: ${device.lastSeen}"
        )
        }
    }
}
