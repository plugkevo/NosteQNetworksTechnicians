package com.kevann.nosteqTech



import android.content.Intent
import android.net.Uri
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
import com.kevann.nosteqTech.viewmodel.NetworkViewModel



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouterDetailsScreen(
    routerId: String, // This is now the ONU SN
    onBackClick: () -> Unit,
    viewModel: NetworkViewModel = viewModel()
) {
    val onu = viewModel.getOnuById(routerId)
    val context = LocalContext.current

    if (onu == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ONU not found", style = MaterialTheme.typography.titleLarge)
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
            Text(
                text = "IP: ${onu.ipAddress ?: "N/A"}",
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

            Spacer(modifier = Modifier.height(24.dp))

            // Optical Telemetry
            Text(
                text = "Optical Telemetry",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TelemetryItem(
                        label = "Rx Power",
                        value = if (onu.rxPower != null) "${onu.rxPower} dBm" else "N/A",
                        isCritical = (onu.rxPower ?: -100.0) < -27.0
                    )
                    TelemetryItem(
                        label = "Tx Power",
                        value = if (onu.txPower != null) "${onu.txPower} dBm" else "N/A",
                        isCritical = false
                    )
                    TelemetryItem(
                        label = "Distance",
                        value = if (onu.distance != null) "${onu.distance}m" else "N/A",
                        isCritical = false
                    )
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { Toast.makeText(context, "Calling Customer...", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call Customer")
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
                        // Using zone as address placeholder since address isn't in OnuDetail
                        val address = onu.address ?: onu.zoneName ?: ""
                        val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Google Maps not installed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Navigate")
                }

                OutlinedButton(
                    onClick = { Toast.makeText(context, "Pinging ${onu.ipAddress ?: "device"}...", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ping")
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
