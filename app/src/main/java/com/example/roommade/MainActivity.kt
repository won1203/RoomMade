package com.example.roommade

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.roommade.nav.Route
import com.example.roommade.nav.bottomTabs
import com.example.roommade.ui.*
import com.example.roommade.vm.FloorPlanViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoomMadeTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()
                    val backStack by nav.currentBackStackEntryAsState()
                    val currentRoute = backStack?.destination?.route
                    val context = LocalContext.current
                    val vm: FloorPlanViewModel = viewModel(
                        factory = FloorPlanViewModel.provideFactory(context)
                    )
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                bottomTabs.forEach { tab ->
                                    NavigationBarItem(
                                        selected = currentRoute?.startsWith(tab.route.path) == true,
                                        onClick = {
                                            when (tab.route) {
                                                Route.Start -> {
                                                    nav.navigate(Route.Start.path) {
                                                        popUpTo(Route.Start.path) { inclusive = true }
                                                        launchSingleTop = true
                                                        restoreState = false
                                                    }
                                                }
                                                Route.RoomCategory -> {
                                                    nav.navigate(Route.RoomCategory.path) {
                                                        popUpTo(Route.Start.path) { inclusive = false }
                                                        launchSingleTop = true
                                                        restoreState = false
                                                    }
                                                }
                                                else -> {
                                                    nav.navigate(tab.route.path) {
                                                        popUpTo(Route.Start.path) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
                                        },
                                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                                        label = { Text(tab.label) }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = nav,
                            startDestination = Route.Start.path,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            fun navigateToShoppingWeb() {
                                val encoded = Uri.encode(vm.shoppingSearchQuery())
                                nav.navigate("${Route.ShoppingWeb.path}?query=$encoded")
                            }
                            composable(Route.Start.path) {
                                StartScreen(
                                    onStartManual = { nav.navigate(Route.RoomCategory.path) }
                                )
                            }
                            composable(Route.RoomCategory.path) {
                                RoomCategoryScreen(
                                    onNext = { nav.navigate(Route.Concept.path) },
                                    onBack = { nav.popBackStack() },
                                    vm = vm
                                )
                            }
                            composable(Route.Concept.path) {
                                ConceptInputScreen(
                                    onNext = { nav.navigate(Route.ConceptAnalyzing.path) },
                                    onBack = { nav.popBackStack() },
                                    vm = vm
                                )
                            }
                            composable(Route.ConceptAnalyzing.path) {
                                ConceptAnalyzingScreen(
                                    onAnalyzed = {
                                        nav.navigate(Route.ConceptResult.path) {
                                            popUpTo(Route.ConceptAnalyzing.path) { inclusive = true }
                                        }
                                    },
                                    onCancel = { nav.popBackStack() },
                                    vm = vm
                                )
                            }
                            composable(Route.ConceptResult.path) {
                                ConceptResultScreen(
                                    onSearch = { navigateToShoppingWeb() },
                                    onBack = {
                                        nav.popBackStack(Route.Concept.path, inclusive = false)
                                    },
                                    vm = vm
                                )
                            }
                            composable(
                                route = "${Route.ShoppingWeb.path}?query={query}",
                                arguments = listOf(
                                    navArgument("query") {
                                        type = NavType.StringType
                                        defaultValue = ""
                                    }
                                )
                            ) { entry ->
                                val rawQuery = entry.arguments?.getString("query")?.let { Uri.decode(it) }.orEmpty()
                                ShoppingWebViewScreen(
                                    query = rawQuery,
                                    vm = vm,
                                    onBack = { nav.popBackStack() },
                                    onGoPlacement = { nav.navigate(Route.ExampleRoom.path) }
                                )
                            }
                            composable(Route.ExampleRoom.path) {
                                ExampleRoomSelectionScreen(
                                    vm = vm,
                                    onBack = { nav.popBackStack() },
                                    onGenerate = { nav.navigate(Route.AiImage.path) }
                                )
                            }
                            composable(Route.AiImage.path) {
                                AiImageScreen(
                                    vm = vm,
                                    onBack = { nav.popBackStack() }
                                )
                            }
                            composable(Route.Cart.path) {
                                CartScreen(
                                    vm = vm,
                                    onSearchMore = { navigateToShoppingWeb() }
                                )
                            }
                            composable(Route.Library.path) {
                                LibraryScreen(
                                    vm = vm,
                                    onOpenGenerate = {
                                        nav.navigate(Route.RoomCategory.path) {
                                            popUpTo(Route.Start.path) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
