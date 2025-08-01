package com.ahm.mydalil.util

import android.content.Context
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Represents the window size class for the app's window size.
 */
enum class WindowSizeClass { COMPACT, MEDIUM, EXPANDED }

/**
 * Calculates the window size class based on the current window size.
 */
fun calculateWindowSizeClass(context: Context, configuration: Configuration): WindowSizeClass {
    val windowMetrics = context.getSystemService(Context.WINDOW_SERVICE)
        .let { it as WindowManager }
        .currentWindowMetrics
    
    val widthDp = windowMetrics.bounds.width() / 
                 context.resources.displayMetrics.density
    
    return when {
        widthDp < 600f -> WindowSizeClass.COMPACT
        widthDp < 840f -> WindowSizeClass.MEDIUM
        else -> WindowSizeClass.EXPANDED
    }
}

/**
 * Extension function to easily get the window size class in composables.
 */
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    
    return remember(configuration) {
        calculateWindowSizeClass(context, configuration)
    }
}

/**
 * Common constants used throughout the app.
 */
object Constants {
    const val DATABASE_NAME = "mydalil_database"
    const val PREFERENCES_NAME = "user_preferences"
    const val HADITHS_DB_FILE = "hadiths_db.json"
    const val VERSES_DB_FILE = "verses_db.json"
    const val SEARCH_DEBOUNCE_MS = 300L
    const val PAGINATION_PAGE_SIZE = 30
    const val PAGINATION_PREFETCH_DISTANCE = 5
}
