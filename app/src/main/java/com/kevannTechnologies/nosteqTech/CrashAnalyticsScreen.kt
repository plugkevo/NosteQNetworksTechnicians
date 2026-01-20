package com.kevannTechnologies.nosteqTech

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kevannTechnologies.nosteqTech.data.api.cache.CacheAnalytics
import com.kevannTechnologies.nosteqTech.data.api.cache.CacheMetrics


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheAnalyticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val analytics = remember { CacheAnalytics(context) }
    val metrics = remember { mutableStateOf(CacheMetrics()) }

    LaunchedEffect(Unit) {
        metrics.value = analytics.getMetrics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cache Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cache Hit/Miss Stats
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cache Performance", style = MaterialTheme.typography.headlineSmall)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Cache Hits", style = MaterialTheme.typography.labelMedium)
                            Text(metrics.value.cacheHits.toString(), style = MaterialTheme.typography.headlineMedium)
                        }
                        Column {
                            Text("Cache Misses", style = MaterialTheme.typography.labelMedium)
                            Text(metrics.value.cacheMisses.toString(), style = MaterialTheme.typography.headlineMedium)
                        }
                        Column {
                            Text("Hit Rate", style = MaterialTheme.typography.labelMedium)
                            Text("${String.format("%.1f", analytics.getCacheHitRate())}%", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }

            // API Call Stats
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("API Usage", style = MaterialTheme.typography.headlineSmall)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("API Calls", style = MaterialTheme.typography.labelMedium)
                            Text(metrics.value.apiCalls.toString(), style = MaterialTheme.typography.headlineMedium)
                        }
                        Column {
                            Text("Calls/Hour Limit", style = MaterialTheme.typography.labelMedium)
                            Text("3", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Text("Status: Within limit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            // ONU Stats
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Data Statistics", style = MaterialTheme.typography.headlineSmall)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Total ONUs Served", style = MaterialTheme.typography.labelMedium)
                            Text(metrics.value.totalOnusServed.toString(), style = MaterialTheme.typography.headlineMedium)
                        }
                        Column {
                            Text("Last Updated", style = MaterialTheme.typography.labelMedium)
                            val lastUpdated = java.text.SimpleDateFormat("HH:mm:ss").format(metrics.value.lastUpdated)
                            Text(lastUpdated, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        metrics.value = analytics.getMetrics()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Refresh")
                }

                Button(
                    onClick = {
                        analytics.resetMetrics()
                        metrics.value = CacheMetrics()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
            }
        }
    }
}
