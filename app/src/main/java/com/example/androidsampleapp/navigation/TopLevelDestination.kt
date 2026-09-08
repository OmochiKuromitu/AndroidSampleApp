package com.example.androidsampleapp.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.androidsampleapp.R

/**
 * Navigation bar のタブ。ここに 1 件足せばタブが 1 つ増える。
 */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME("home", R.string.tab_home, Icons.Filled.Home),
    SEARCH("search", R.string.tab_search, Icons.Filled.Search),
    PROFILE("profile", R.string.tab_profile, Icons.Filled.Person),
}
