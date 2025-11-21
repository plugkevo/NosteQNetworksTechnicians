package com.kevann.nosteqTech



import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kevann.nosteqTech.ui.theme.NosteqTheme
import com.kevann.nosteqTech.viewmodel.NetworkViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NosteqTheme {
                NosteqApp()
            }
        }
    }
}

@Composable
fun NosteqApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != "login" &&
            currentRoute?.startsWith("details") == false

    val networkViewModel: NetworkViewModel = viewModel()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("Network") },
                        selected = currentRoute == "dashboard",
                        onClick = {
                            navController.navigate("dashboard") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                        label = { Text("Map") },
                        selected = currentRoute == "map",
                        onClick = {
                            navController.navigate("map") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        label = { Text("Profile") },
                        selected = currentRoute == "profile",
                        onClick = {
                            navController.navigate("profile") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("dashboard") {
                NetworkDashboardScreen(
                    onRouterClick = { routerId ->
                        navController.navigate("details/$routerId")
                    },
                    viewModel = networkViewModel
                )
            }
            composable("map") { MapScreen() }
            composable("profile") {
                ProfileScreen(
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                )
            }
            composable("details/{routerId}") { backStackEntry ->
                val routerId = backStackEntry.arguments?.getString("routerId") ?: return@composable
                RouterDetailsScreen(
                    routerId = routerId,
                    onBackClick = { navController.popBackStack() },
                    viewModel = networkViewModel
                )
            }
        }
    }
}