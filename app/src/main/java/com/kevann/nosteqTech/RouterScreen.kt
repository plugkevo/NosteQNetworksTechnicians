package com.kevann.nosteqTech

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect // Import LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kevann.nosteqTech.data.api.SpeedTestResult
import com.kevann.nosteqTech.ui.theme.NosteqRed
import com.kevann.nosteqTech.ui.theme.NosteqTheme
import com.kevann.nosteqTech.viewmodel.NetworkState
import com.kevann.nosteqTech.viewmodel.NetworkViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouterDetailsScreen(
    routerId: String, // This is now the ONU SN
    onBackClick: () -> Unit,
    viewModel: NetworkViewModel = viewModel()
) {
    val networkState = viewModel.networkState.collectAsState().value
    val liveStatus = viewModel.selectedOnuStatus.collectAsState().value
    val signalInfo = viewModel.selectedOnuSignal.collectAsState().value // Collect signal info
    val gpsCoordinates = viewModel.gpsCoordinates.collectAsState().value
    val speedProfile = viewModel.selectedOnuSpeedProfile.collectAsState().value
    val speedTestResult = viewModel.speedTestResult.collectAsState(initial = null).value

    val onu = viewModel.getOnuById(routerId)

    val uniqueId = onu?.uniqueExternalId

    LaunchedEffect(routerId) {
        Log.d("[v0] Router Details", "routerId changed to: $routerId - Clearing speed test result")
        viewModel.clearSpeedTestResult()
    }

    if (uniqueId != null) {
        LaunchedEffect(uniqueId) {
            viewModel.fetchOnuFullStatus(uniqueId)
            viewModel.fetchOnuSignal(uniqueId)
            viewModel.fetchOnuSpeedProfile(uniqueId)  // Fetch speed profile
            viewModel.fetchGpsCoordinates()
        }
    }

    val context = LocalContext.current

    LaunchedEffect(gpsCoordinates) {
        if (uniqueId != null) {
            val gps = gpsCoordinates[uniqueId]
            Log.d("[v0]", "GPS Coordinates for $uniqueId: Lat=${gps?.first}, Long=${gps?.second}")
        }
    }

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
                        onu.status.contains("online", ignoreCase = true) -> Color(0xFF2E7D32).copy(alpha = 0.1f)
                        onu.status.contains("offline", ignoreCase = true) -> NosteqRed.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Text(
                    text = "Status: ${onu.status}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        onu.status.contains("online", ignoreCase = true) -> Color(0xFF2E7D32)
                        onu.status.contains("offline", ignoreCase = true) -> NosteqRed
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "IP: ${liveStatus?.ipAddress ?: onu.ipAddress ?: "N/A"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "MAC: ${onu.uniqueExternalId ?: "N/A"}", // Using uniqueExternalId as MAC/ID placeholder if MAC is missing
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                text = "Last Seen: ${liveStatus?.lastUpTime ?: onu.lastSeen}",
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
                            val dist = liveStatus?.distance ?: onu.distance
                            TelemetryItem(
                                label = "Distance",
                                value = if (dist != null) "${dist}m" else "Loading...",
                                isCritical = false
                            )
                        }
                    } else {
                        // Fallback to original display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val rx = liveStatus?.rxPower ?: onu.rxPower
                            val tx = liveStatus?.txPower ?: onu.txPower
                            val dist = liveStatus?.distance ?: onu.distance

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
                            TelemetryItem(
                                label = "Distance",
                                value = if (dist != null) "${dist}m" else "Loading...",
                                isCritical = false
                            )
                        }
                    }
                }
            }

            if (liveStatus?.runState != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Live Status: ${liveStatus.runState}", fontWeight = FontWeight.Bold)
                        if (liveStatus.lastDownCause != null) {
                            Text("Last Down Cause: ${liveStatus.lastDownCause}")
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
                    if (onu.odbName != null) {
                        Text(
                            text = "ODB: ${onu.odbName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (onu.phoneNumber != null) {
                        Text(
                            text = "Customer Phone: ${onu.phoneNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (onu.phoneNumber != null) {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${onu.phoneNumber}")
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
                            Log.d("[v0]", "Opening Google Maps with GPS: ${gps.first}, ${gps.second}")
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
                            Log.d("[v0]", "GPS coordinates not available for $uniqueId")
                            Toast.makeText(context, "GPS coordinates not available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (uniqueId != null && gpsCoordinates.containsKey(uniqueId)) "Navigate (GPS)" else "Navigate")
                }

                OutlinedButton(
                    onClick = {
                        if (speedTestResult?.isLoading == true) {
                            Toast.makeText(context, "Speed test in progress...", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.runSpeedTest()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        when {
                            speedTestResult?.isLoading == true -> "Testing..."
                            speedTestResult?.downloadSpeedMbps != null ->
                                "Speed: %.1f Mbps".format(speedTestResult.downloadSpeedMbps)
                            else -> "Speed Test"
                        }
                    )
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

            Button(
                onClick = {
                    Toast.makeText(context, "Ticket Marked Resolved", Toast.LENGTH_SHORT).show()
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mark Resolved")
            }
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
