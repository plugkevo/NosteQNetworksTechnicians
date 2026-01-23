package com.kevannTechnologies.nosteqTech



import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kevannTechnologies.nosteqTech.data.api.OnuDetail
import com.kevannTechnologies.nosteqTech.data.api.OnuStatus
import com.kevannTechnologies.nosteqTech.ui.theme.NosteqRed
import com.kevannTechnologies.nosteqTech.ui.theme.NosteqTheme
import com.kevannTechnologies.nosteqTech.ui.theme.NosteqYellow
import com.kevannTechnologies.nosteqTech.ui.viewmodel.ProfileViewModel
import com.kevannTechnologies.nosteqTech.viewmodel.NetworkState
import com.kevannTechnologies.nosteqTech.viewmodel.NetworkViewModel

import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDashboardScreen(
    onRouterClick: (String) -> Unit,
    viewModel: NetworkViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    val networkState by viewModel.networkState.collectAsState()
    val profileData by profileViewModel.profileData.collectAsState()
    val onuStatuses by viewModel.onuStatuses.collectAsState() // collect live statuses
    val displayedOnuCount by viewModel.displayedOnuCount.collectAsState() // Collect pagination state

    // Always fetch profile data to ensure role-based filtering works
    LaunchedEffect(Unit) {
        profileViewModel.fetchUserProfile()
        if (networkState is NetworkState.Loading) {
            viewModel.fetchAllOnus()
        }
    }

    LaunchedEffect(Unit) {
        delay(500)
        viewModel.fetchOnuStatuses()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Network Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.fetchAllOnus() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        // Search Bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = {},
            active = false,
            onActiveChange = {},
            placeholder = { Text("Search SN or Name") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {}

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "Los",
                onClick = { selectedFilter = if (selectedFilter == "Los") null else "Los" },
                label = { Text("Critical (LOS)") },
                leadingIcon = { if (selectedFilter == "Los") Icon(Icons.Default.WifiOff, null) else null }
            )
            FilterChip(
                selected = selectedFilter == "Offline",
                onClick = { selectedFilter = if (selectedFilter == "Offline") null else "Offline" },
                label = { Text("Offline") },
                leadingIcon = { if (selectedFilter == "Offline") Icon(Icons.Default.Warning, null) else null }
            )
            FilterChip(
                selected = selectedFilter == "Power fail",
                onClick = { selectedFilter = if (selectedFilter == "Power fail") null else "Power fail" },
                label = { Text("Power Fail") },
                leadingIcon = { if (selectedFilter == "Power fail") Icon(Icons.Default.Warning, null) else null }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (networkState is NetworkState.Loading && (networkState as? NetworkState.Success)?.onus?.isEmpty() != false) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val allOnus = when (networkState) {
                is NetworkState.Success -> (networkState as NetworkState.Success).onus
                else -> emptyList()
            }

            // Role-based filtering: Technicians see only ONUs in their assigned zone
            val userRole = profileData?.role ?: ""
            val userServiceArea = profileData?.serviceArea ?: ""

            val roleFilteredOnus = if (userRole.equals("technician", ignoreCase = true) && userServiceArea.isNotEmpty()) {
                allOnus.filter { onu ->
                    val onuZone = onu.zoneName ?: ""
                    ZoneConfig.isOnuInZone(onuZone, userServiceArea)
                }
            } else {
                // Admin and other roles see all ONUs
                allOnus
            }

            // Apply status filter if selected
            val statusFilteredOnus = if (selectedFilter != null) {
                roleFilteredOnus.filter { onu ->
                    val status = onuStatuses[onu.sn]?.status?.lowercase() ?: "unknown"
                    when (selectedFilter!!.lowercase()) {
                        "los" -> status == "los"
                        "offline" -> status == "offline"
                        "power fail" -> status == "power fail"
                        else -> true
                    }
                }
            } else {
                roleFilteredOnus
            }

            // Apply search filter
            val searchFilteredOnus = if (searchQuery.isNotEmpty()) {
                statusFilteredOnus.filter { onu ->
                    onu.name.contains(searchQuery, ignoreCase = true) ||
                            onu.sn.contains(searchQuery, ignoreCase = true)
                }
            } else {
                statusFilteredOnus
            }

            // Apply pagination
            val displayOnus = searchFilteredOnus.take(displayedOnuCount)

            if (displayOnus.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayOnus.size) { index ->
                        val onu = displayOnus[index]
                        val liveStatus = onuStatuses[onu.sn]

                        if (index >= displayOnus.size - 5) {
                            LaunchedEffect(Unit) {
                                viewModel.loadMoreOnu()
                            }
                        }

                        OnuCard(
                            onu = onu,
                            liveStatus = liveStatus,
                            onClick = { onRouterClick(onu.uniqueExternalId ?: "") }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = NosteqRed,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No ONUs found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnuCard(onu: OnuDetail, onClick: () -> Unit, liveStatus: OnuStatus? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator
            val statusString = liveStatus?.status ?: "Unknown"

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = when (statusString.lowercase()) {
                            "los" -> NosteqRed
                            "offline" -> Color.Gray
                            "online" -> Color.Green
                            else -> NosteqYellow
                        },
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = onu.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${onu.sn} • ${onu.model ?: "Unknown"} ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Zone: ${onu.zoneName ?: "Unknown"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Status: $statusString",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NetworkDashboardScreenPreview() {
    NosteqTheme {
        NetworkDashboardScreen(onRouterClick = {})
    }
}
