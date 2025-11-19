package com.kevann.nosteqTech


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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

import com.kevann.nosteqTech.ui.theme.NosteqRed
import com.kevann.nosteqTech.ui.theme.NosteqTheme
import com.kevann.nosteqTech.ui.theme.NosteqYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDashboardScreen(onRouterClick: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<RouterStatus?>(null) }

    val filteredRouters = MockData.routers.filter { router ->
        (selectedFilter == null || router.status == selectedFilter) &&
                (router.customerName.contains(searchQuery, ignoreCase = true) ||
                        router.customerId.contains(searchQuery, ignoreCase = true))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = {},
            active = false,
            onActiveChange = {},
            placeholder = { Text("Search Customer ID or Name") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {}

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == RouterStatus.LOS,
                onClick = { selectedFilter = if (selectedFilter == RouterStatus.LOS) null else RouterStatus.LOS },
                label = { Text("Critical (LOS)") },
                leadingIcon = { if (selectedFilter == RouterStatus.LOS) Icon(Icons.Default.WifiOff, null) else null }
            )
            FilterChip(
                selected = selectedFilter == RouterStatus.LATENCY,
                onClick = { selectedFilter = if (selectedFilter == RouterStatus.LATENCY) null else RouterStatus.LATENCY },
                label = { Text("Latency") },
                leadingIcon = { if (selectedFilter == RouterStatus.LATENCY) Icon(Icons.Default.Warning, null) else null }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Router List
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredRouters) { router ->
                RouterCard(router = router, onClick = { onRouterClick(router.id) })
            }
        }
    }
}

@Composable
fun RouterCard(router: Router, onClick: () -> Unit) {
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
                        color = when (router.status) {
                            RouterStatus.LOS -> NosteqRed
                            RouterStatus.LATENCY -> NosteqYellow
                            RouterStatus.OFFLINE -> Color.Gray
                            RouterStatus.ONLINE -> Color.Green
                        },
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = router.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${router.customerId} • ${router.model}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = router.lastSeen,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (router.status == RouterStatus.LOS) NosteqRed else MaterialTheme.colorScheme.onSurfaceVariant
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