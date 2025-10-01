package com.example.roommade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
                val vm: FloorPlanViewModel = viewModel()
                NavHost(nav, startDestination = Route.Start.path) {
                    composable(Route.Start.path) {
                        StartScreen(
                            onStartManual = { nav.navigate(Route.RoomSize.path) },
                            onStartPhoto = {
                                // 사진 보조 입력 화면로 이동 (구현했을 경우)
                                // nav.navigate("capture")
                                // 없다면 바로 RoomSize로 연결해도 OK:
                                nav.navigate(Route.RoomSize.path)
                            }
                        )
                    }
                    composable(Route.RoomSize.path) { RoomSizeScreen(onNext = { nav.navigate(Route.Inventory.path) }, vm = vm) }
                    composable(Route.Inventory.path) { InventoryScreen(onNext = { nav.navigate(Route.Edit.path) }, vm = vm) }
                    composable(Route.Edit.path) { EditFloorplanScreen(onNext = { nav.navigate(Route.Style.path) }, vm = vm) }
                    composable(Route.Style.path) {
                        StylePreferenceScreen(
                            onNext = { nav.navigate(Route.StyleCatalog.path) },
                            onBack = { nav.popBackStack() },
                            vm = vm
                        )
                    }
                    composable(Route.StyleCatalog.path) {
                        StyleCatalogScreen(
                            onNext = { nav.navigate(Route.Recommend.path) },
                            onBack = { nav.popBackStack() },
                            vm = vm
                        )
                    }
                    composable(Route.Recommend.path) {
                        RecommendScreen(
                            onSelectPlan = { nav.navigate(Route.Result.path) },
                            onBack = { nav.popBackStack() },
                            vm = vm
                        )
                    }
                    composable(Route.Result.path) {
                        ResultScreen(onShop = { nav.navigate(Route.Shopping.path) }, onBack = { nav.popBackStack() }, vm = vm)
                    }
                    composable(Route.Shopping.path) {
                        ShoppingScreen(onDone = { nav.popBackStack(Route.RoomSize.path, false) }, onBack = { nav.popBackStack() }, vm = vm)
                    }
                }
            }
        }
    }
}
