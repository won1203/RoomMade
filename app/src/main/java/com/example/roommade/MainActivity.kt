package com.example.roommade

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
                    composable(Route.Start.path) {
                        StartScreen(
                            onStartManual = { nav.navigate(Route.StructureArea.path) }
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
                            onNext = { nav.navigate(Route.StructureLayout.path) },
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
                            onNext = { nav.navigate(Route.Catalog.path) },
                            onBack = {
                                nav.popBackStack(Route.Concept.path, inclusive = false)
                            },
                            vm = vm
                        )
                    }
                    composable(Route.Catalog.path) {
                        RecommendationCatalogScreen(
                            onNext = { nav.navigate(Route.Plans.path) },
                            onBack = { nav.popBackStack() },
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
                            onShop = { nav.navigate(Route.Shopping.path) },
                            onBack = { nav.popBackStack() },
                            vm = vm
                        )
                    }
                    composable(Route.Shopping.path) {
                        ShoppingScreen(
                            onDone = { nav.popBackStack(Route.StructureArea.path, false) },
                            onBack = { nav.popBackStack() },
                            vm = vm
                        )
                    }
                }
            }
        }
    }
}
