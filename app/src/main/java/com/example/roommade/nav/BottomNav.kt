package com.example.roommade.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomTab(
    val route: Route,
    val label: String,
    val icon: ImageVector
)

val bottomTabs = listOf(
    BottomTab(Route.Start, "홈", Icons.Filled.Home),
    BottomTab(Route.RoomCategory, "생성", Icons.Filled.AddCircle),
    BottomTab(Route.Cart, "장바구니", Icons.Filled.ShoppingCart),
    BottomTab(Route.Library, "보관함", Icons.Filled.CollectionsBookmark),
    BottomTab(Route.Settings, "설정", Icons.Filled.Settings)
)
