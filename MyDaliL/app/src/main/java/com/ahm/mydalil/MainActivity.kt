package com.ahm.mydalil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.ahm.mydalil.data.local.datastore.UserPreferences
import com.ahm.mydalil.data.repository.VerseRepository
import com.ahm.mydalil.ui.navigation.AppNavigator
import com.ahm.mydalil.ui.theme.MyDalilTheme
import com.ahm.mydalil.ui.viewmodel.SearchViewModel
//import com.ahm.mydalil.ui.viewmodel.SearchViewModelFactory
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // Add this annotation
class MainActivity : ComponentActivity() {
    // Hilt will now provide the ViewModel automatically
    private val viewModel: SearchViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        //WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

//        // Manual Dependency Injection
//        val repository = VerseRepository.getInstance(this)
//        val userPreferences = UserPreferences(this)
//        val viewModel: SearchViewModel by viewModels {
//            SearchViewModelFactory(
//                repository,
//                userPreferences
//            )
//        }

        setContent {
            MyDalilTheme {
                AppNavigator(viewModel = viewModel)
            }
        }
    }
}