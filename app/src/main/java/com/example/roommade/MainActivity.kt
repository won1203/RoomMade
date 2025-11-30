package com.example.roommade

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.roommade.nav.Route
import com.example.roommade.ui.*
import com.example.roommade.vm.FloorPlanViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                val nav = rememberNavController()
                val context = LocalContext.current
                val vm: FloorPlanViewModel = viewModel(
                    factory = FloorPlanViewModel.provideFactory(context)
                )
                NavHost(nav, startDestination = Route.Start.path) {
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
                            onNext = { nav.navigate(Route.StructureArea.path) },
                            onBack = { nav.popBackStack() },
                            vm = vm
                        )
                    }
                    composable(Route.StructureArea.path) {
                        StructureAreaScreen(
                            onNext = { nav.navigate(Route.StructureInventory.path) },
                            onBack = { nav.popBackStack() },
                            vm = vm
                        )
                    }
                    composable(Route.StructureInventory.path) {
                        StructureInventoryScreen(
                            onNext = { nav.navigate(Route.Concept.path) },
                            onBack = { nav.popBackStack() },
                            vm = vm
                        )
                    }
                    composable(Route.StructureLayout.path) {
                        StructureLayoutScreen(
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
                    composable(Route.Plans.path) {
                        PlanSelectionScreen(
                            onSelectPlan = { nav.navigate(Route.Result.path) },
                            onBack = { nav.popBackStack() },
                            vm = vm
                        )
                    }
                    composable(Route.Result.path) {
                        ResultScreen(
                            onSearch = { navigateToShoppingWeb() },
                            onBack = { nav.popBackStack() },
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
                            onGoPlacement = { nav.navigate(Route.Placement.path) }
                        )
                    }
                    composable(Route.Placement.path) {
                        PlacementScreen(
                            onBack = { nav.popBackStack() },
                            onRender = { vm.markPlacementRendered() },
                            onGenerateAi = { nav.navigate(Route.AiImage.path) },
                            vm = vm
                        )
                    }
                    composable(Route.AiImage.path) {
                        AiImageScreen(
                            vm = vm,
                            onBack = { nav.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
