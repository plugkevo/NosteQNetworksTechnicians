package com.kevann.nosteqTech

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kevann.nosteqTech.data.api.OnuDetail
import com.kevann.nosteqTech.ui.theme.NosteqTheme
import com.kevann.nosteqTech.ui.viewmodel.ProfileState
import com.kevann.nosteqTech.ui.viewmodel.ProfileViewModel
import com.kevann.nosteqTech.viewmodel.NetworkState
import com.kevann.nosteqTech.viewmodel.NetworkViewModel

import kotlinx.coroutines.delay

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    profileViewModel: ProfileViewModel,  // receive as required parameter instead of default
    networkViewModel: NetworkViewModel = viewModel(),  // Add NetworkViewModel to access ONUs
    onNavigateToAnalytics: (() -> Unit)? = null  // Add analytics navigation callback
) {
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var editPhoneNumber by remember { mutableStateOf("") }
    var isEditingPhone by remember { mutableStateOf(false) }
    var showPhoneUpdateSuccess by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val profileState by profileViewModel.profileState.collectAsState()
    val profileData by profileViewModel.profileData.collectAsState()
    val updatePhoneState by profileViewModel.updatePhoneState.collectAsState()
    val updatePasswordState by profileViewModel.updatePasswordState.collectAsState()
    val onusManagedCount by profileViewModel.onusManagedCount.collectAsState()
    val isDarkMode by profileViewModel.isDarkMode.collectAsState()

    // Fetch profile on screen load
    LaunchedEffect(Unit) {
        profileViewModel.fetchUserProfile()
    }

    var shouldCalculateCount by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000)
        networkViewModel.fetchAllOnus()
        shouldCalculateCount = true
    }

    LaunchedEffect(profileData) {
        if (profileData != null && profileData!!.serviceArea.isNotEmpty()) {
            profileViewModel.calculateManagedOnuCountFromCache(profileData!!.serviceArea)
            println("[v0] Profile - Service area loaded: ${profileData!!.serviceArea}")
        }
    }

    val networkState by networkViewModel.networkState.collectAsState()
    LaunchedEffect(networkState, shouldCalculateCount) {
        if (shouldCalculateCount && networkState is NetworkState.Success && profileData != null) {
            val allOnus = (networkState as NetworkState.Success).onus
            profileViewModel.calculateManagedOnuCount(allOnus, profileData!!.serviceArea)
            println("[v0] Profile - Calculating managed ONUs: Total ONUs=${allOnus.size}, Service Area=${profileData!!.serviceArea}, Count=${profileViewModel.onusManagedCount.value}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Avatar
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = profileData?.name ?: "Loading...",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    modifier = Modifier.padding(top = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        profileData?.role?.equals("admin", ignoreCase = true) == true -> MaterialTheme.colorScheme.errorContainer
                        profileData?.role?.equals("technician", ignoreCase = true) == true -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = profileData?.role?.uppercase() ?: "N/A",
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            profileData?.role?.equals("admin", ignoreCase = true) == true -> MaterialTheme.colorScheme.onErrorContainer
                            profileData?.role?.equals("technician", ignoreCase = true) == true -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "ID: ${profileData?.id ?: "N/A"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    text = "Service Area: ${profileData?.serviceArea ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Work Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(value = onusManagedCount.toString(), label = "ONUs Managed")  // Use dynamic count
                    StatItem(value = "156", label = "Issues Resolved")
                    StatItem(value = "24m", label = "Avg Response")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Contact Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Email - Read only (Admin controlled)
                ReadOnlyInfoField(icon = Icons.Default.Email, label = "Email", value = profileData?.email ?: "N/A")

                // Phone - Editable by technician
                if (isEditingPhone) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editPhoneNumber,
                            onValueChange = { editPhoneNumber = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.weight(1f),
                            leadingIcon = { Icon(Icons.Default.Phone, null) }
                        )
                        IconButton(
                            onClick = {
                                if (editPhoneNumber.isNotBlank()) {
                                    profileViewModel.updatePhoneNumber(editPhoneNumber)
                                }
                            },
                            enabled = updatePhoneState !is ProfileState.Loading
                        ) {
                            Icon(Icons.Default.Check, "Save")
                        }
                        IconButton(onClick = { isEditingPhone = false }) {
                            Icon(Icons.Default.Close, "Cancel")
                        }
                    }
                    if (updatePhoneState is ProfileState.Error) {
                        Text(
                            text = (updatePhoneState as ProfileState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (updatePhoneState is ProfileState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.End)
                        )
                    }
                } else {
                    EditableInfoField(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = profileData?.phoneNumber ?: "N/A",
                        onEdit = {
                            editPhoneNumber = profileData?.phoneNumber ?: ""
                            isEditingPhone = true
                        }
                    )
                }

                // Service Area - Read only (Admin controlled)
                ReadOnlyInfoField(icon = Icons.Default.LocationOn, label = "Service Area", value = profileData?.serviceArea ?: "N/A")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Settings Section Header
        Text(
            text = "Settings & Preferences",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        ListItem(
            headlineContent = { Text("Push Notifications") },
            supportingContent = { Text("Receive alerts for urgent issues") },
            leadingContent = { Icon(Icons.Default.Notifications, null) },
            trailingContent = {
                Switch(
                    checked = true,
                    onCheckedChange = { }
                )
            }
        )

        ListItem(
            headlineContent = { Text("Dark Mode") },
            leadingContent = { Icon(Icons.Default.DarkMode, null) },
            trailingContent = {
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { newValue ->
                        profileViewModel.updateThemePreference(newValue)
                    }
                )
            }
        )

        ListItem(
            headlineContent = { Text("Change Password") },
            leadingContent = { Icon(Icons.Default.Lock, null) },
            modifier = Modifier.clickable { showChangePasswordDialog = true }
        )

        ListItem(
            headlineContent = { Text("Sync Data") },
            supportingContent = { Text("Last synced: 5 minutes ago") },
            leadingContent = { Icon(Icons.Default.Sync, null) },
            modifier = Modifier.clickable { }
        )

        if (onNavigateToAnalytics != null) {
            ListItem(
                headlineContent = { Text("Cache Analytics") },
                supportingContent = { Text("Monitor data sync performance") },
                leadingContent = { Icon(Icons.Default.Analytics, null) },
                modifier = Modifier.clickable { onNavigateToAnalytics() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Help & Support",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        ListItem(
            headlineContent = { Text("App Version") },
            supportingContent = { Text("v1.0.5") },
            leadingContent = { Icon(Icons.Default.Info, null) }
        )

        ListItem(
            headlineContent = { Text("Contact Support") },
            leadingContent = { Icon(Icons.Default.Help, null) },
            modifier = Modifier.clickable { }
        )

        ListItem(
            headlineContent = { Text("About") },
            leadingContent = { Icon(Icons.Default.Info, null) },
            modifier = Modifier.clickable { showAboutDialog = true }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.ExitToApp, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Developed by Kevann Technologies ❤️",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            viewModel = profileViewModel,
            updatePasswordState = updatePasswordState,
            onSuccess = {
                showChangePasswordDialog = false
                profileViewModel.resetUpdatePasswordState()
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }

    if (showPhoneUpdateSuccess) {
        LaunchedEffect(Unit) {
            showPhoneUpdateSuccess = false
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ReadOnlyInfoField(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "(Admin managed)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun EditableInfoField(icon: ImageVector, label: String, value: String, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, "Edit")
        }
    }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    viewModel: ProfileViewModel,
    updatePasswordState: ProfileState,
    onSuccess: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(updatePasswordState) {
        when (updatePasswordState) {
            is ProfileState.Success -> onSuccess()
            is ProfileState.Error -> errorMessage = (updatePasswordState as ProfileState.Error).message
            else -> {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    enabled = updatePasswordState !is ProfileState.Loading
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    enabled = updatePasswordState !is ProfileState.Loading
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = updatePasswordState !is ProfileState.Loading
                )
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (updatePasswordState is ProfileState.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.changePassword(currentPassword, newPassword, confirmPassword)
                },
                enabled = updatePasswordState !is ProfileState.Loading
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About SmartOlt Technician") },
        text = {
            Column {
                Text(
                    text = "App Version",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "v1.0.5",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Build Number",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "2024.01.1005",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Description",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "NosteQ Networks Technician app is a comprehensive mobile application designed to help field technicians manage and monitor network devices (ONUs) efficiently. View device status, perform diagnostics, and manage customer information all in one place.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Developer",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Kevann Technologies",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    NosteqTheme {
        ProfileScreen(
            onLogout = {},
            profileViewModel = ProfileViewModel()
        )
    }
}
