package com.kevannTechnologies.nosteqTech

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kevannTechnologies.nosteqTech.data.api.OnuDetail
import com.kevannTechnologies.nosteqTech.data.api.OnuStatus
import com.kevannTechnologies.nosteqTech.ui.theme.NosteqRed
import com.kevannTechnologies.nosteqTech.ui.theme.NosteqYellow
import com.kevannTechnologies.nosteqTech.ui.viewmodel.ProfileViewModel
import com.kevannTechnologies.nosteqTech.viewmodel.NetworkState
import com.kevannTechnologies.nosteqTech.viewmodel.NetworkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDashboardScreen(
    onRouterClick: (String) -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    networkViewModel: NetworkViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    val networkState by networkViewModel.networkState.collectAsState()
    val onlineOnu by networkViewModel.onlineOnu.collectAsState()
    val profileData by profileViewModel.profileData.collectAsState()
    val onuStatuses by networkViewModel.onuStatuses.collectAsState()

    LaunchedEffect(Unit) {
        profileViewModel.fetchUserProfile()
        networkViewModel.fetchAllOnus()
        networkViewModel.fetchOnuStatuses()
    }

    LaunchedEffect(searchQuery) {
        networkViewModel.setSearchQuery(searchQuery)
    }

    LaunchedEffect(profileData) {
        profileData?.let {
            networkViewModel.setUserRole(it.role.orEmpty())
            networkViewModel.setUserServiceArea(it.serviceArea.orEmpty())
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        /** ---------------- Header ---------------- */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Network Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Row {
                IconButton(onClick = {

                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Profile") },
                            onClick = {
                                showMenu = false
                                onNavigateToProfile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Map") },
                            onClick = {
                                showMenu = false
                                onNavigateToMap()
                            }
                        )
                    }
                }
            }
        }

        /** ---------------- Search Bar ---------------- */
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = {},
            active = false,
            onActiveChange = {},
            placeholder = { Text("Search SN, Name, or Username") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {}

        Spacer(modifier = Modifier.height(8.dp))

        /** ---------------- Content ---------------- */
        when (networkState) {
            is NetworkState.Loading -> LoadingIndicator()
            is NetworkState.Error -> LosErrorState()
            is NetworkState.Success -> {
                // If statuses are still fetching, keep showing loader to prevent "Empty State" flicker
                if (onuStatuses.isEmpty()) {
                    LoadingIndicator()
                } else if (onlineOnu.isEmpty()) {
                    EmptyState()
                } else {
                    OnuList(onlineOnu, networkViewModel, onRouterClick)
                }
            }
        }
    }
}

@Composable
fun OnuList(
    onus: List<OnuDetail>,
    viewModel: NetworkViewModel,
    onRouterClick: (String) -> Unit
) {
    var isLoadingMore by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(onus) { index, onu ->
            if (index >= onus.lastIndex - 3 && !isLoadingMore) {
                isLoadingMore = true
                LaunchedEffect(index) {
                    viewModel.loadMoreOnu()
                    isLoadingMore = false
                }
            }

            OnuCard(
                onu = onu,
                onClick = { onRouterClick(onu.uniqueExternalId.orEmpty()) }
            )
        }
    }
}

@Composable
fun OnuCard(onu: OnuDetail, onClick: () -> Unit, liveStatus: OnuStatus? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val status = liveStatus?.status ?: "Online"
            Box(
                modifier = Modifier.size(12.dp).background(
                    color = when (status.lowercase()) {
                        "los" -> NosteqRed
                        "offline" -> Color.Gray
                        "online" -> Color.Green
                        else -> NosteqYellow
                    },
                    shape = MaterialTheme.shapes.small
                )
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(onu.name, fontWeight = FontWeight.Bold)
                Text("${onu.sn} • ${onu.onuTypeName ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                Text("Zone: ${onu.zoneName ?: "Unknown"}", style = MaterialTheme.typography.labelSmall)
                Text("Status: $status", style = MaterialTheme.typography.labelSmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = NosteqRed, modifier = Modifier.size(64.dp))
            Text("No Online ONUs found")
        }
    }
}

@Composable
fun LosErrorState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.WifiOff, contentDescription = null, tint = NosteqRed, modifier = Modifier.size(64.dp))
    }
}