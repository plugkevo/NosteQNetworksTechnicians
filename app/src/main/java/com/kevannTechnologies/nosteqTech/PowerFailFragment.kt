package com.kevannTechnologies.nosteqTech

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
fun PowerFailFragment(
    onBack: () -> Unit = {},
    onRouterClick: (String) -> Unit,
    viewModel: NetworkViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }

    // ---- Collect state ----
    val networkState by viewModel.networkState.collectAsState()
    val powerFailOnu by viewModel.powerFailOnu.collectAsState()
    val profileData by profileViewModel.profileData.collectAsState()
    val onuStatuses by viewModel.onuStatuses.collectAsState()

    // ---- Initial load (runs once) ----
    LaunchedEffect(Unit) {
        profileViewModel.fetchUserProfile()
        viewModel.fetchAllOnus()
        delay(400)
        viewModel.fetchOnuStatuses()
    }

    // ---- Search ----
    LaunchedEffect(searchQuery) {
        viewModel.setSearchQuery(searchQuery)
    }

    // ---- Role & service area ----
    LaunchedEffect(profileData) {
        profileData?.let {
            viewModel.setUserRole(it.role.orEmpty())
            viewModel.setUserServiceArea(it.serviceArea.orEmpty())
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ---- Header ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Power Fail ONUs",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {
                viewModel.fetchAllOnus(forceRefresh = true)
                viewModel.fetchOnuStatuses()
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        // ---- Search Bar ----
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = {},
            active = false,
            onActiveChange = {},
            placeholder = { Text("Search SN, Name, or Username") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {}

        Spacer(modifier = Modifier.height(8.dp))

        // ---- Content ----
        when (networkState) {
            is NetworkState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is NetworkState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Failed to load ONUs")
                }
            }

            is NetworkState.Success -> {
                if (powerFailOnu.isEmpty()) {
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
                        items(powerFailOnu.size) { index ->
                            val onu = powerFailOnu[index]

                            // ---- Pagination ----
                            if (index >= powerFailOnu.size - 5) {
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
}
