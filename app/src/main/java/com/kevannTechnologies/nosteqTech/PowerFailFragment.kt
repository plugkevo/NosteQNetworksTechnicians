package com.kevannTechnologies.nosteqTech

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kevannTechnologies.nosteqTech.ui.theme.NosteqRed
import com.kevannTechnologies.nosteqTech.ui.viewmodel.ProfileViewModel
import com.kevannTechnologies.nosteqTech.viewmodel.NetworkState
import com.kevannTechnologies.nosteqTech.viewmodel.NetworkViewModel
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerFailFragment(
    onRouterClick: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToOnline: () -> Unit = {},
    onNavigateToLos: () -> Unit = {},
    onNavigateToOffline: () -> Unit = {},
    onNavigateToPowerFail: () -> Unit = {},
    viewModel: NetworkViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }

    val networkState by viewModel.networkState.collectAsState()
    val profileData by profileViewModel.profileData.collectAsState()
    val onuStatuses by viewModel.onuStatuses.collectAsState()
    val displayedOnuCount by viewModel.displayedOnuCount.collectAsState()

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

    // Calculate counts for each status
    val allOnus = when (networkState) {
        is NetworkState.Success -> (networkState as NetworkState.Success).onus
        else -> emptyList()
    }

    val userRole = profileData?.role ?: ""
    val userServiceArea = profileData?.serviceArea ?: ""

    val roleFilteredOnus = if (userRole.equals("technician", ignoreCase = true) && userServiceArea.isNotEmpty()) {
        allOnus.filter { onu ->
            val onuZone = onu.zoneName ?: ""
            ZoneConfig.isOnuInZone(onuZone, userServiceArea)
        }
    } else {
        allOnus
    }

    val onlineCount = roleFilteredOnus.count { onuStatuses[it.sn]?.status?.lowercase() == "online" }
    val losCount = roleFilteredOnus.count { onuStatuses[it.sn]?.status?.lowercase() == "los" }
    val offlineCount = roleFilteredOnus.count { onuStatuses[it.sn]?.status?.lowercase() == "offline" }
    val powerFailCount = roleFilteredOnus.count { onuStatuses[it.sn]?.status?.lowercase() == "power fail" }

    // Determine chip labels based on role
    val isTechnician = userRole.equals("technician", ignoreCase = true)
    val onlineLabel = if (isTechnician) "Online" else "Online ($onlineCount)"
    val losLabel = "LOS ($losCount)"
    val offlineLabel = if (isTechnician) "Offline" else "Offline ($offlineCount)"
    val powerFailLabel = if (isTechnician) "Power Fail" else "Power Fail ($powerFailCount)"

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with title and refresh button
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
            placeholder = { Text("Search SN, Name, or Username") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {}

        // Filter Chips with navigation and counts - 2x2 grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // First row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = false,
                    onClick = { onNavigateToOnline() },
                    label = { Text(onlineLabel) },
                    leadingIcon = { Icon(Icons.Default.WifiOff, null) }
                )
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = false,
                    onClick = { onNavigateToLos() },
                    label = { Text(losLabel) },
                    leadingIcon = { Icon(Icons.Default.WifiOff, null) }
                )
            }
            // Second row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = false,
                    onClick = { onNavigateToOffline() },
                    label = { Text(offlineLabel) },
                    leadingIcon = { Icon(Icons.Default.Warning, null) }
                )
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = true,
                    onClick = {},
                    label = { Text(powerFailLabel) },
                    leadingIcon = { Icon(Icons.Default.Warning, null) }
                )
            }
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
            // Filter by Power Fail status
            val powerFailFilteredOnus = roleFilteredOnus.filter { onu ->
                val status = onuStatuses[onu.sn]?.status?.lowercase() ?: "unknown"
                status == "power fail"
            }

            // Apply search filter - search by SN, Name, or Username (username with "starts with")
            val searchFilteredOnus = if (searchQuery.isNotEmpty()) {
                powerFailFilteredOnus.filter { onu ->
                    onu.name.contains(searchQuery, ignoreCase = true) ||
                            onu.sn.contains(searchQuery, ignoreCase = true) ||
                            onu.username?.startsWith(searchQuery, ignoreCase = true) == true
                }
            } else {
                powerFailFilteredOnus
            }

            // Apply pagination
            val displayOnus = searchFilteredOnus.take(displayedOnuCount)

            if (displayOnus.isEmpty()) {
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
                            "No Power Fail devices found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
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
            }
        }
    }
}
