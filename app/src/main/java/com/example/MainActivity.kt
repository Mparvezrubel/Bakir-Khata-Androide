package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.BakirKhataViewModel
import com.example.ui.screens.BackupSettingsScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme

sealed interface Screen {
    data object Dashboard : Screen
    data class CustomerDetail(val customerId: Long) : Screen
    data object Settings : Screen
}

class MainActivity : ComponentActivity() {

    private val viewModel: BakirKhataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BakirKhataApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun BakirKhataApp(viewModel: BakirKhataViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            is Screen.Dashboard -> {
                DashboardScreen(
                    viewModel = viewModel,
                    onCustomerClick = { customerId ->
                        viewModel.selectCustomer(customerId)
                        currentScreen = Screen.CustomerDetail(customerId)
                    },
                    onNavigateToSettings = {
                        currentScreen = Screen.Settings
                    }
                )
            }
            is Screen.CustomerDetail -> {
                CustomerDetailScreen(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.selectCustomer(null)
                        currentScreen = Screen.Dashboard
                    }
                )
            }
            is Screen.Settings -> {
                BackupSettingsScreen(
                    viewModel = viewModel,
                    onBack = {
                        currentScreen = Screen.Dashboard
                    }
                )
            }
        }
    }
}
