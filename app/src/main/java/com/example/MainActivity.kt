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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.KhataBackupData
import com.example.ui.BakirKhataViewModel
import com.example.ui.screens.BackupSettingsScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AutoBackupScheduler
import com.example.util.DriveBackupStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        if (viewModel.shopPrefs.isAutoBackupEnabled && DriveBackupStorage.hasFolder(this)) {
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
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_transition"
        ) { screen ->
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
    var folderUri by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<String?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            try {
                DriveBackupStorage.saveFolder(context, uri)
                folderUri = uri.toString()
                if (viewModel.shopPrefs.isAutoBackupEnabled) {
                    AutoBackupScheduler.scheduleDailyBackup(context, viewModel.shopPrefs.isBackupWifiOnly)
                    AutoBackupScheduler.triggerImmediateBackup(context)
                }
                Toast.makeText(context, "Drive ফোল্ডার সংরক্ষিত; ব্যাকআপ শুরু হয়েছে", Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                Toast.makeText(context, "ফোল্ডার সংরক্ষণ করা যায়নি: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shadowElevation = 6.dp, shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (folderUri != null || DriveBackupStorage.hasFolder(context)) "Google Drive ফোল্ডার প্রস্তুত" else "প্রথমে Drive ফোল্ডার নির্বাচন করুন", style = MaterialTheme.typography.labelMedium)
                OutlinedButton(onClick = { folderPicker.launch(null) }) { Text("Drive ফোল্ডার নির্বাচন / পরিবর্তন") }
                Button(
                    enabled = folderUri != null || DriveBackupStorage.hasFolder(context),
                    onClick = {
                        if (viewModel.shopPrefs.isAutoBackupEnabled) {
                            AutoBackupScheduler.triggerImmediateBackup(context)
                            Toast.makeText(context, "Drive ব্যাকআপ কিউ করা হয়েছে", Toast.LENGTH_SHORT).show()
                        } else Toast.makeText(context, "আগে স্বয়ংক্রিয় ব্যাকআপ চালু করুন", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("এখনই Drive-এ ব্যাকআপ") }
                OutlinedButton(
                    enabled = folderUri != null || DriveBackupStorage.hasFolder(context),
                    onClick = {
                        scope.launch {
                            try {
                                val json = withContext(Dispatchers.IO) { DriveBackupStorage.readLatestBackup(context) }
                                if (json.isNullOrBlank()) {
                                    Toast.makeText(context, "Drive ফোল্ডারে ব্যাকআপ পাওয়া যায়নি", Toast.LENGTH_LONG).show()
                                } else {
                                    withContext(Dispatchers.IO) { KhataBackupData.fromJsonString(json) }
                                    pendingRestore = json
                                    showRestoreDialog = true
                                }
                            } catch (error: Exception) {
                                Toast.makeText(context, "Drive ব্যাকআপ পড়া যায়নি: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) { Text("Drive থেকে রিস্টোর") }
            }
        }
    }

    if (showRestoreDialog && pendingRestore != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false; pendingRestore = null },
            title = { Text("Drive ব্যাকআপ রিস্টোর করবেন?") },
            text = { Text("বর্তমান হিসাবের জায়গায় Drive-এর ব্যাকআপ বসবে। আগে বর্তমান ডেটার কপি রাখুন।") },
            confirmButton = {
                TextButton(onClick = {
                    val json = pendingRestore ?: return@TextButton
                    showRestoreDialog = false
                    pendingRestore = null
                    viewModel.restoreFromJson(json) { ok, message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }) { Text("রিস্টোর করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false; pendingRestore = null }) { Text("বাতিল") }
            }
        )
    }
}
