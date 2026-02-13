package com.kevannTechnologies.nosteqTech

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerFailFragment(
    onBack: () -> Unit = {},
    onRouterClick: (String) -> Unit,
    viewModel: NetworkViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }

    val networkState by viewModel.networkState.collectAsState()
    val powerFailOnu by viewModel.powerFailOnu.collectAsState()
    val profileData by profileViewModel.profileData.collectAsState()
    val onuStatuses by viewModel.onuStatuses.collectAsState()

    // 1. Initial Load
    LaunchedEffect(Unit) {
        profileViewModel.fetchUserProfile()
        viewModel.fetchAllOnus()
        viewModel.fetchOnuStatuses()
    }

    // 2. Sync Filters
    LaunchedEffect(searchQuery) {
        viewModel.setSearchQuery(searchQuery)
    }

    LaunchedEffect(profileData) {
        profileData?.let {
            viewModel.setUserRole(it.role.orEmpty())
            viewModel.setUserServiceArea(it.serviceArea.orEmpty())
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        /** ---------------- Header ---------------- */
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Power Fail ONUs", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = {
                viewModel.fetchAllOnus(forceRefresh = true)
                viewModel.fetchOnuStatuses()
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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

        /** ---------------- Content Logic ---------------- */
        when (networkState) {
            is NetworkState.Loading -> PfLoadingState()
            is NetworkState.Error -> PfErrorState()
            is NetworkState.Success -> {
                // If statuses are empty, it means we haven't grouped the ONUs yet.
                // Stay in LoadingState to prevent the empty flicker.
                if (onuStatuses.isEmpty()) {
                    PfLoadingState()
                } else if (powerFailOnu.isEmpty()) {
                    EmptyPowerFailState()
                } else {
                    PowerFailList(powerFailOnu, onuStatuses, viewModel, onRouterClick)
                }
            }
        }
    }
}

/** ---------------- Sub-Composables ---------------- */

@Composable
fun PowerFailList(
    onus: List<com.kevannTechnologies.nosteqTech.data.api.OnuDetail>,
    statuses: Map<String, com.kevannTechnologies.nosteqTech.data.api.OnuStatus>,
    viewModel: NetworkViewModel,
    onRouterClick: (String) -> Unit
) {
    var isPaging by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = onus,
            key = { _, onu -> onu.uniqueExternalId ?: onu.sn }
        ) { index, onu ->
            if (index >= onus.size - 5 && !isPaging) {
                SideEffect {
                    isPaging = true
                    viewModel.loadMoreOnu()
                    isPaging = false
                }
            }

            OnuCard(
                onu = onu,
                liveStatus = statuses[onu.sn],
                onClick = { onRouterClick(onu.uniqueExternalId ?: "") }
            )
        }
    }
}

@Composable
fun PfLoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyPowerFailState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Warning, null, tint = NosteqRed, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("No Power Fail devices found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PfErrorState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Failed to load ONUs", color = NosteqRed)
    }
}