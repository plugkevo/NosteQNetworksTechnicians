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
import kotlinx.coroutines.delay


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
    val displayedOnuCount by viewModel.displayedOnuCount.collectAsState()
    val losOnu by viewModel.losOnu.collectAsState()
    val onuStatuses by viewModel.onuStatuses.collectAsState()

    LaunchedEffect("fetchProfile") {
        profileViewModel.fetchUserProfile()
        if (networkState is NetworkState.Loading) {
            viewModel.fetchAllOnus()
        }
    }

    LaunchedEffect("fetchStatuses") {
        delay(500)
        viewModel.fetchOnuStatuses()
    }

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
        // Header with title and refresh button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "LOS ONUs",
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

        Spacer(modifier = Modifier.height(8.dp))

        /* -------------------- CONTENT -------------------- */

        when {
            networkState is NetworkState.Loading && losOnu.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            losOnu.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = NosteqRed,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No LOS devices found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = losOnu,
                        key = { _, onu -> onu.uniqueExternalId ?: onu.sn }
                    ) { index, onu ->

                        // Pagination trigger (SAFE)
                        if (index >= losOnu.size - 5) {
                            LaunchedEffect(index) {
                                viewModel.loadMoreOnu()
                            }
                        }

                        OnuCard(
                            onu = onu,
                            liveStatus = onuStatuses[onu.sn],
                            onClick = {
                                onRouterClick(onu.sn)
                            }
                        )
                    }
                }
            }
        }
    }
}
