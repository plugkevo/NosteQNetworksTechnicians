package com.kevannTechnologies.nosteqTech

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.kevannTechnologies.nosteqTech.ui.theme.NosteqTheme
import com.kevannTechnologies.nosteqTech.ui.viewmodel.ProfileViewModel
import com.kevannTechnologies.nosteqTech.viewmodel.LoginState
import com.kevannTechnologies.nosteqTech.viewmodel.LoginViewModel
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

    val profileViewModel: ProfileViewModel = viewModel()
    val isDarkMode by profileViewModel.isDarkMode.collectAsState()

    val loginViewModel = remember { LoginViewModel(context) }

    var startDestination by remember { mutableStateOf("login") }

    LaunchedEffect(Unit) {
        val email = loginViewModel.prefs?.getString("user_email", null)
        val password = loginViewModel.prefs?.getString("user_password", null)

        if (email != null && password != null && loginViewModel.isSessionValid()) {
            loginViewModel.restoreSession()
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
                            icon = { Icon(Icons.Filled.Home, null) },
                            label = { Text("Network") },
                            selected = currentRoute == "dashboard",
                            onClick = {
                                navController.navigate("dashboard") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )

                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.WifiOff, null) },
                            label = { Text("LOS") },
                            selected = currentRoute == "los",
                            onClick = {
                                navController.navigate("los") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )

                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.Warning, null) },
                            label = { Text("Offline") },
                            selected = currentRoute == "offline",
                            onClick = {
                                navController.navigate("offline") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )

                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.Warning, null) },
                            label = { Text("Power Fail") },
                            selected = currentRoute == "powerfail",
                            onClick = {
                                navController.navigate("powerfail") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
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
                        onNavigateToProfile = {
                            navController.navigate("profile")
                        },
                        onNavigateToMap = {
                            navController.navigate("map")
                        }
                    )
                }

                composable("los") {
                    LosFragment(
                        onRouterClick = { routerId ->
                            navController.navigate("details/$routerId")
                        }
                    )
                }

                composable("offline") {
                    OfflineFragment(
                        onRouterClick = { routerId ->
                            navController.navigate("details/$routerId")
                        }
                    )
                }

                composable("powerfail") {
                    PowerFailFragment(
                        onRouterClick = { routerId ->
                            navController.navigate("details/$routerId")
                        }
                    )
                }

                composable("map") {
                    MapScreen()
                }

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
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("details/{routerId}") { backStackEntry ->
                    val routerId =
                        backStackEntry.arguments?.getString("routerId") ?: return@composable

                    RouterDetailsScreen(
                        routerId = routerId,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
