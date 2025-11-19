package com.kevann.nosteqTech


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kevann.nosteqTech.ui.theme.NosteqTheme

@Composable
fun MapScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text("Map Visualization Placeholder")
        // In a real app, Google Maps Compose would go here
    }
}
@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    NosteqTheme {
        MapScreen()
    }
}