package com.kevann.nosteqTech


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
import com.kevann.nosteqTech.data.api.OnuDetail


import com.kevann.nosteqTech.ui.theme.NosteqRed
import com.kevann.nosteqTech.ui.theme.NosteqTheme
import com.kevann.nosteqTech.ui.theme.NosteqYellow
import com.kevann.nosteqTech.viewmodel.NetworkState
import com.kevann.nosteqTech.viewmodel.NetworkViewModel



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDashboardScreen(
    onRouterClick: (String) -> Unit,
    viewModel: NetworkViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    val networkState by viewModel.networkState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchAllOnus()
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
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (val state = networkState) {
            is NetworkState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading ONUs...")
                    }
                }
            }
            is NetworkState.Error -> {
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
                            "Error Loading Data",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.fetchAllOnus() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            is NetworkState.Success -> {
                val filteredOnus = state.onus.filter { onu ->
                    (selectedFilter == null || onu.status.equals(selectedFilter, ignoreCase = true)) &&
                            (onu.name.contains(searchQuery, ignoreCase = true) ||
                                    onu.sn.contains(searchQuery, ignoreCase = true))
                }

                if (filteredOnus.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No ONUs found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredOnus.size) { index ->
                            val onu = filteredOnus[index]
                            OnuCard(onu = onu, onClick = { onRouterClick(onu.sn) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnuCard(onu: OnuDetail, onClick: () -> Unit) {
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
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = when (onu.status.lowercase()) {
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
                    text = "${onu.sn} • ${onu.model ?: "Unknown Model"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Last seen: ${onu.lastSeen}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (onu.status.equals("Los", ignoreCase = true)) NosteqRed else MaterialTheme.colorScheme.onSurfaceVariant
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
