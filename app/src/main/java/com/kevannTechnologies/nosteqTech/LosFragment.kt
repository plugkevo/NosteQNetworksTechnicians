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
fun LosFragment(
    onBack: () -> Unit = {},
    onRouterClick: (String) -> Unit,
    viewModel: NetworkViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }

    val networkState by viewModel.networkState.collectAsState()
    val profileData by profileViewModel.profileData.collectAsState()
    val losOnu by viewModel.losOnu.collectAsState()
    val onuStatuses by viewModel.onuStatuses.collectAsState()

    // 1. Initial Data Fetch
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
            viewModel.setUserRole(it.role ?: "")
            viewModel.setUserServiceArea(it.serviceArea ?: "")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        /** ---------------- Header ---------------- */
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LOS ONUs", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.fetchAllOnus(forceRefresh = true); viewModel.fetchOnuStatuses() }) {
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
            is NetworkState.Loading ->LosLoadingState()
            is NetworkState.Error -> LosErrorState()
            is NetworkState.Success -> {
                // GUARD: Even if ONUs are loaded from cache, we must wait for statuses
                // to know which ones are actually in "LOS" state.
                if (onuStatuses.isEmpty()) {
                    LosLoadingState()
                } else if (losOnu.isEmpty()) {
                    EmptyLosState()
                } else {
                    LosOnuList(losOnu, onuStatuses, viewModel, onRouterClick)
                }
            }
        }
    }
}

/** ---------------- Sub-Composables for Clarity ---------------- */

@Composable
fun LosOnuList(
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
            // Simple Pagination
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
fun LosLoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyLosState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Warning, null, tint = NosteqRed, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("No LOS devices found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}