package com.ahm.mydalil.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import com.ahm.mydalil.ui.screens.bookmarks.BookmarkScreen
import com.ahm.mydalil.ui.screens.search.SearchScreen
import com.ahm.mydalil.ui.viewmodel.SearchViewModel

private sealed class Screen {
    data object Search : Screen()
    data object Bookmarks : Screen()
}

@Composable
fun AppNavigator(viewModel: SearchViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Search) }

    when (currentScreen) {
        is Screen.Search -> SearchScreen(
            viewModel = viewModel,
            onNavigateToBookmarks = { currentScreen = Screen.Bookmarks }
        )
        is Screen.Bookmarks -> {
            // Handle back press to navigate from Bookmarks to Search
            BackHandler { currentScreen = Screen.Search }
            BookmarkScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = Screen.Search }
            )
        }
    }
}
