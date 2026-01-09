package com.kevann.nosteqTech

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kevann.nosteqTech.ui.theme.NosteqRed
import com.kevann.nosteqTech.ui.theme.NosteqTheme
import com.kevann.nosteqTech.viewmodel.NetworkState
import com.kevann.nosteqTech.viewmodel.NetworkViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouterDetailsScreen(
    routerId: String, // This is now the ONU SN
    onBackClick: () -> Unit = {},
    viewModel: NetworkViewModel = viewModel()
) {
    val networkState = viewModel.networkState.collectAsState().value
    val onuStatuses by viewModel.onuStatuses.collectAsState() // collect live statuses
    val signalInfo = viewModel.selectedOnuSignal.collectAsState().value
    val gpsCoordinates = viewModel.gpsCoordinates.collectAsState().value
    val speedProfile = viewModel.selectedOnuSpeedProfile.collectAsState().value
    val speedTestResult = viewModel.speedTestResult.collectAsState(initial = null).value
    val liveOnuStatus = onuStatuses[routerId]?.status ?: "Loading..."

    val onu = viewModel.getOnuById(routerId)

    val uniqueId = onu?.uniqueExternalId

    LaunchedEffect(routerId) {
        viewModel.clearSpeedTestResult()
    }

    if (uniqueId != null) {
        LaunchedEffect(uniqueId) {
            viewModel.fetchOnuFullStatus(uniqueId)
            viewModel.fetchOnuSignal(uniqueId)
            viewModel.fetchOnuSpeedProfile(uniqueId)
            viewModel.fetchGpsCoordinates()
            viewModel.fetchLiveOnuStatus(uniqueId)
        }
    }

    val context = LocalContext.current

    if (onu == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ONU not found", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Looking for SN: $routerId",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "State: ${when(networkState) {
                        is NetworkState.Loading -> "Loading..."
                        is NetworkState.Success -> "Loaded ${(networkState as NetworkState.Success).onus.size} ONUs"
                        is NetworkState.Error -> "Error: ${(networkState as NetworkState.Error).message}"
                    }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBackClick) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = onu.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier.padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        liveOnuStatus?.contains("power fail", ignoreCase = true) == true -> Color(0xFFF57C00).copy(alpha = 0.1f)
                        liveOnuStatus?.contains("los", ignoreCase = true) == true -> Color(0xFFF57C00).copy(alpha = 0.1f)
                        liveOnuStatus?.contains("offline", ignoreCase = true) == true -> NosteqRed.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Text(
                    text = "Status: ${liveOnuStatus ?: "Loading..."}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        liveOnuStatus?.contains("online", ignoreCase = true) == true -> Color(0xFF2E7D32)
                        liveOnuStatus?.contains("power fail", ignoreCase = true) == true -> Color(0xFFF57C00)
                        liveOnuStatus?.contains("los", ignoreCase = true) == true -> Color(0xFFF57C00)
                        liveOnuStatus?.contains("offline", ignoreCase = true) == true -> NosteqRed
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SN: ${onu.sn}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onu.phoneNumber != null) {
                Text(
                    text = "Phone: ${onu.phoneNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Username: ${onu.username ?: "N/A"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Optical Telemetry
            Text(
                text = "Optical Telemetry (Live)", // Updated title
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (signalInfo != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (signalInfo.signalQuality.lowercase()) {
                            "very good" -> Color(0xFF2E7D32).copy(alpha = 0.1f)
                            "warning" -> Color(0xFFF57C00).copy(alpha = 0.1f)
                            "critical" -> NosteqRed.copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Signal Quality: ${signalInfo.signalQuality}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (signalInfo.signalQuality.lowercase()) {
                                    "very good" -> Color(0xFF2E7D32)
                                    "warning" -> Color(0xFFF57C00)
                                    "critical" -> NosteqRed
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = signalInfo.signalValue,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (signalInfo?.signal1490 != null || signalInfo?.signal1310 != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (signalInfo.signal1490 != null) {
                                TelemetryItem(
                                    label = "1490nm (Rx)",
                                    value = signalInfo.signal1490,
                                    isCritical = false
                                )
                            }
                            if (signalInfo.signal1310 != null) {
                                TelemetryItem(
                                    label = "1310nm (Tx)",
                                    value = signalInfo.signal1310,
                                    isCritical = false
                                )
                            }
                        }
                    } else {
                        // Fallback to original display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val rx = onu.rxPower
                            val tx = onu.txPower

                            TelemetryItem(
                                label = "Rx Power",
                                value = if (rx != null) "$rx dBm" else "Loading...",
                                isCritical = (rx ?: -100.0) < -27.0
                            )
                            TelemetryItem(
                                label = "Tx Power",
                                value = if (tx != null) "$tx dBm" else "Loading...",
                                isCritical = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Customer Info
            Text(
                text = "Location & Zone",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Zone: ${onu.zoneName ?: "Unknown"}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (onu.username != null) {
                        Text(
                            text = "Customer Phone: ${onu.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (onu.username != null) {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${onu.username}")
                                }
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (onu.phoneNumber != null) "Call ${onu.phoneNumber}" else "Call Customer")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Actions
            Text(
                text = "Technician Actions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val gps = uniqueId?.let { gpsCoordinates[it] }

                        if (gps != null) {
                            val uriString = "geo:${gps.first},${gps.second}?q=${gps.first},${gps.second}(${Uri.encode(onu.name)})"
                            val gmmIntentUri = Uri.parse(uriString)
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Google Maps not installed", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "GPS coordinates not available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (uniqueId != null && gpsCoordinates.containsKey(uniqueId)) "Navigate (GPS)" else "Navigate")
                }

                Button(
                    onClick = {
                        if (speedTestResult?.isLoading == true) {
                            Toast.makeText(context, "Speed test in progress...", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.runSpeedTest()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Speed Test")
                }
            }

            // Display speed test results if available
            if (speedTestResult?.downloadSpeedMbps != null || speedTestResult?.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (speedTestResult.error != null)
                            NosteqRed.copy(alpha = 0.1f)
                        else
                            Color(0xFF2E7D32).copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (speedTestResult.downloadSpeedMbps != null) {
                            Text(
                                text = "Download Speed: %.2f Mbps".format(speedTestResult.downloadSpeedMbps),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        if (speedTestResult.error != null) {
                            Text(
                                text = "Error: ${speedTestResult.error}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NosteqRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun TelemetryItem(label: String, value: String, isCritical: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isCritical) NosteqRed else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RouterDetailsScreenPreview() {
    NosteqTheme {
        RouterDetailsScreen(routerId = "1", onBackClick = {})
    }
}
