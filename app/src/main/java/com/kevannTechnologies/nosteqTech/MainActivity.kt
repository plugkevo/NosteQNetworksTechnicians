package com.kevannTechnologies.nosteqTech

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
import com.kevannTechnologies.nosteqTech.ui.theme.NosteqTheme
import com.kevannTechnologies.nosteqTech.ui.viewmodel.ProfileViewModel
import com.kevannTechnologies.nosteqTech.viewmodel.LoginState
import com.kevannTechnologies.nosteqTech.viewmodel.LoginViewModel
import com.kevannTechnologies.nosteqTech.viewmodel.NetworkViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            NosteqTheme {
                NosteqApp(this)
            }
        }
    }
}

@Composable
fun NosteqApp(context: android.content.Context) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != "login" &&
            currentRoute?.startsWith("details") == false

    val networkViewModel: NetworkViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val isDarkMode by profileViewModel.isDarkMode.collectAsState()

    val loginViewModel = LoginViewModel(context)

    var startDestination by remember { mutableStateOf("login") }

    LaunchedEffect(Unit) {
        val email = loginViewModel.prefs?.getString("user_email", null)
        val password = loginViewModel.prefs?.getString("user_password", null)

        if (email != null && password != null && loginViewModel.isSessionValid()) {
            loginViewModel.restoreSession()
            // Wait a bit for Firebase to complete authentication
            delay(500)
            startDestination = if (loginViewModel.loginState.value is LoginState.Success) {
                "dashboard"
            } else {
                "login"
            }
        } else {
            startDestination = "login"
        }

        profileViewModel.fetchUserProfile()
        profileViewModel.fetchThemePreference()
    }

    NosteqTheme(darkTheme = isDarkMode) {
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
                startDestination = startDestination,
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
                            loginViewModel.logout()
                            navController.navigate("login") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        },
                        profileViewModel = profileViewModel,
                        onNavigateToAnalytics = {
                            navController.navigate("cache_analytics")
                        }
                    )
                }
                composable("cache_analytics") {
                    CacheAnalyticsScreen(
                        onBack = {
                            navController.popBackStack()
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
}
