package com.seldon.fleet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seldon.fleet.data.FavoritesRepository
import com.seldon.fleet.data.FleetRepository
import com.seldon.fleet.domain.Device
import com.seldon.fleet.domain.DeviceRepository
import com.seldon.fleet.domain.DeviceStatus
import com.seldon.fleet.domain.FavoriteRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FleetApp(FleetRepository(this), FavoritesRepository(this))
        }
    }
}

@Composable
fun FleetApp(deviceRepository: DeviceRepository, favoriteRepository: FavoriteRepository) {
    val devices = remember { deviceRepository.load() }
    var favorites by remember { mutableStateOf(favoriteRepository.load()) }
    var selectedStatus by remember { mutableStateOf<DeviceStatus?>(null) }
    var selectedDevice by remember { mutableStateOf<Device?>(null) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (selectedDevice == null) {
                FleetList(
                    devices = devices,
                    selectedStatus = selectedStatus,
                    favorites = favorites,
                    onStatusSelected = { selectedStatus = it },
                    onDeviceSelected = { selectedDevice = it },
                    onFavoriteToggle = { device ->
                        favorites = if (device.id in favorites) favorites - device.id else favorites + device.id
                        favoriteRepository.save(favorites)
                    }
                )
            } else {
                DeviceDetail(
                    device = selectedDevice!!,
                    isFavorite = selectedDevice!!.id in favorites,
                    onBack = { selectedDevice = null },
                    onFavoriteToggle = {
                        val id = selectedDevice!!.id
                        favorites = if (id in favorites) favorites - id else favorites + id
                        favoriteRepository.save(favorites)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FleetList(
    devices: List<Device>,
    selectedStatus: DeviceStatus?,
    favorites: Set<String>,
    onStatusSelected: (DeviceStatus?) -> Unit,
    onDeviceSelected: (Device) -> Unit,
    onFavoriteToggle: (Device) -> Unit
) {
    val counts = devices.groupingBy { it.decision().status }.eachCount()
    val visibleDevices = devices.filter { selectedStatus == null || it.decision().status == selectedStatus }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Frota") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Text(
                "${devices.size} DISPOSITIVOS · LIDOS DE ASSETS",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { onStatusSelected(null) },
                    label = { Text("Todos ${devices.size}") }
                )
                DeviceStatus.entries.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { onStatusSelected(status) },
                        label = { Text("${statusLabel(status)} ${counts[status] ?: 0}") }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleDevices, key = { it.id }) { device ->
                    DeviceRow(device, device.id in favorites, onDeviceSelected, onFavoriteToggle)
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: Device,
    isFavorite: Boolean,
    onDeviceSelected: (Device) -> Unit,
    onFavoriteToggle: (Device) -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable { onDeviceSelected(device) }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(device.id, fontWeight = FontWeight.Bold)
                Text("${device.model} · ${device.soc}")
                Text("${device.batteryLevelPct}%")
            }
            StatusChip(device.decision().status)
            IconButton(onClick = { onFavoriteToggle(device) }) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Favorito"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceDetail(
    device: Device,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Frota") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp)) {
            Text(device.id, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${device.model} · ${device.soc} · tier ${device.tier}")
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(device.decision().status)
                Spacer(Modifier.weight(1f))
                AssistChip(
                    onClick = onFavoriteToggle,
                    label = { Text(if (isFavorite) "Favorito" else "Favoritar") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null
                        )
                    }
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(device.decision().reason, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(20.dp))
            DetailLine("Consumo normalizado", device.energyMah?.let { "%.1f mAh".format(it) } ?: "Sem dados")
            DetailLine("Capacidade", device.batteryCapacityMah?.let { "$it mAh" } ?: "Sem dados")
            DetailLine("Memória", "${device.ramMb} MB")
            DetailLine("Tela ligada", device.screenOnMinutes?.let { "$it min" } ?: "Sem dados")
            DetailLine("Quadros perdidos", device.jankFramesPct?.let { "$it %" } ?: "Sem dados")
            DetailLine("Falhas (7d)", device.crashCount7d?.toString() ?: "Sem dados")
            DetailLine("Nível de API", device.androidApi.toString())
            Spacer(Modifier.height(12.dp))
            Text("ÚLTIMO CONTATO · ${device.lastSeen}", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Text(value)
    }
}

@Composable
private fun StatusChip(status: DeviceStatus) {
    AssistChip(
        onClick = {},
        label = { Text(statusLabel(status)) },
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            containerColor = statusColor(status).copy(alpha = 0.18f)
        )
    )
}

private fun statusLabel(status: DeviceStatus): String = when (status) {
    DeviceStatus.OK -> "OK"
    DeviceStatus.ATENCAO -> "Atenção"
    DeviceStatus.CRITICO -> "Crítico"
    DeviceStatus.INDETERMINADO -> "Sem dados"
}

private fun statusColor(status: DeviceStatus): Color = when (status) {
    DeviceStatus.OK -> Color(0xFF2E7D32)
    DeviceStatus.ATENCAO -> Color(0xFFF9A825)
    DeviceStatus.CRITICO -> Color(0xFFC62828)
    DeviceStatus.INDETERMINADO -> Color(0xFF616161)
}

private fun Device.decision() = com.seldon.fleet.domain.ClassifyDevice()(this)
