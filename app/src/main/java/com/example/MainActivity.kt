package com.example

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.BakirKhataViewModel
import com.example.ui.screens.BackupSettingsScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AutoBackupScheduler
import com.example.util.DriveBackupStorage
import kotlinx.coroutines.launch

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
        if (viewModel.shopPrefs.isAutoBackupEnabled) {
            AutoBackupScheduler.scheduleDailyBackup(this, viewModel.shopPrefs.isBackupWifiOnly)
        }
        setContent {
            MyApplicationTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    BakirKhataApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun BakirKhataApp(viewModel: BakirKhataViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(targetState = currentScreen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen_transition") { screen ->
            when (screen) {
                is Screen.Dashboard -> DashboardScreen(
                    viewModel = viewModel,
                    onCustomerClick = { id -> viewModel.selectCustomer(id); currentScreen = Screen.CustomerDetail(id) },
                    onNavigateToSettings = { currentScreen = Screen.Settings }
                )
                is Screen.CustomerDetail -> CustomerDetailScreen(viewModel) {
                    viewModel.selectCustomer(null)
                    currentScreen = Screen.Dashboard
                }
                is Screen.Settings -> BackupSettingsScreen(viewModel) { currentScreen = Screen.Dashboard }
            }
        }
        if (currentScreen == Screen.Settings) {
            DriveBackupControls(viewModel, Modifier.align(Alignment.BottomCenter).padding(12.dp))
        }
    }
}

@Composable
private fun DriveBackupControls(viewModel: BakirKhataViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var folderConfigured by remember { mutableStateOf(DriveBackupStorage.hasFolder(context)) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            try {
                DriveBackupStorage.saveFolder(context, uri)
                folderConfigured = true
                if (viewModel.shopPrefs.isAutoBackupEnabled) {
                    AutoBackupScheduler.scheduleDailyBackup(context, viewModel.shopPrefs.isBackupWifiOnly)
                    scope.launch {
                        val restored = runCatching { viewModel.restoreDriveBackupIfLocalDatabaseEmpty() }.getOrDefault(false)
                        if (!restored) AutoBackupScheduler.triggerImmediateBackup(context)
                        Toast.makeText(context, if (restored) "Drive-এর ডেটা স্বয়ংক্রিয়ভাবে রিস্টোর হয়েছে" else "Drive ফোল্ডার সংরক্ষিত; অটো-ব্যাকআপ চালু হয়েছে", Toast.LENGTH_LONG).show()
                    }
                } else {
                    scope.launch {
                        val restored = runCatching { viewModel.restoreDriveBackupIfLocalDatabaseEmpty() }.getOrDefault(false)
                        Toast.makeText(context, if (restored) "Drive-এর ডেটা স্বয়ংক্রিয়ভাবে রিস্টোর হয়েছে" else "Drive ফোল্ডার সংরক্ষিত হয়েছে", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (error: Exception) {
                Toast.makeText(context, "ফোল্ডার সংরক্ষণ করা যায়নি: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shadowElevation = 6.dp, shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (folderConfigured) "Drive sync প্রস্তুত — ব্যাকআপ/রিস্টোর স্বয়ংক্রিয়" else "স্বয়ংক্রিয় Drive sync চালু করতে একবার ফোল্ডার বাছুন", style = MaterialTheme.typography.labelMedium)
                OutlinedButton(onClick = { folderPicker.launch(null) }) {
                    Text(if (folderConfigured) "Drive ফোল্ডার পরিবর্তন" else "Drive ফোল্ডার নির্বাচন")
                }
            }
        }
    }
}
