package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.KhataBackupData
import com.example.util.AutoBackupScheduler
import com.example.util.DriveBackupStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** SAF-based Drive sync. Android shows its folder consent picker once; no OAuth token is stored. */
@Composable
fun DriveBackupFab(viewModel: BakirKhataViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showActions by remember { mutableStateOf(false) }
    var restoreJson by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    var folderReady by remember { mutableStateOf(DriveBackupStorage.hasFolder(context)) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            try {
                DriveBackupStorage.saveFolder(context, uri)
                folderReady = true
                if (viewModel.shopPrefs.isAutoBackupEnabled) {
                    AutoBackupScheduler.scheduleDailyBackup(context, viewModel.shopPrefs.isBackupWifiOnly)
                }
                scope.launch {
                    try {
                        val restored = viewModel.restoreDriveBackupIfLocalDatabaseEmpty()
                        if (!restored && viewModel.shopPrefs.isAutoBackupEnabled) {
                            AutoBackupScheduler.triggerImmediateBackup(context)
                        }
                        val message = when {
                            restored -> "Drive-এর ব্যাকআপ থেকে খালি হিসাবটি রিস্টোর হয়েছে।"
                            viewModel.shopPrefs.isAutoBackupEnabled -> "Drive ফোল্ডার যুক্ত হয়েছে; ব্যাকআপ শুরু হয়েছে।"
                            else -> "Drive ফোল্ডার যুক্ত হয়েছে। স্বয়ংক্রিয় ব্যাকআপ সেটিংস চালু করুন।"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    } catch (error: Exception) {
                        Toast.makeText(context, "Drive sync ব্যর্থ: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (error: Exception) {
                Toast.makeText(context, "Drive ফোল্ডারের অনুমতি সংরক্ষণ হয়নি: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    FloatingActionButton(onClick = { showActions = true }, modifier = modifier) {
        Icon(Icons.Default.CloudSync, contentDescription = "Google Drive backup settings")
    }

    if (showActions) {
        AlertDialog(
            onDismissRequest = { showActions = false },
            title = { Text("Google Drive ব্যাকআপ") },
            text = {
                Text(
                    if (folderReady) "Drive ফোল্ডার অনুমোদিত। অটো-ব্যাকআপ চালু থাকলে প্রতিদিন sync হবে; খালি ডেটাবেসে থাকলে ফোল্ডার যোগের পর ব্যাকআপ restore হবে।"
                    else "Google Drive-এর জন্য Gmail ঠিকানা নয়, Android-এর ফোল্ডার অনুমতি দরকার। একবার Drive-এর একটি ফোল্ডার বেছে দিন।"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showActions = false
                    folderPicker.launch(null)
                }) { Text(if (folderReady) "ফোল্ডার পরিবর্তন" else "Drive ফোল্ডার বাছুন") }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = folderReady,
                    onClick = {
                        showActions = false
                        scope.launch {
                            try {
                                val json = withContext(Dispatchers.IO) { DriveBackupStorage.readLatestBackup(context) }
                                if (json.isNullOrBlank()) {
                                    Toast.makeText(context, "Drive ফোল্ডারে ব্যাকআপ নেই।", Toast.LENGTH_LONG).show()
                                } else {
                                    withContext(Dispatchers.IO) { KhataBackupData.fromJsonString(json) }
                                    restoreJson = json
                                    showRestoreConfirmation = true
                                }
                            } catch (error: Exception) {
                                Toast.makeText(context, "Drive ব্যাকআপ পড়া যায়নি: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) { Text("Drive থেকে রিস্টোর") }
            }
        )
    }

    if (showRestoreConfirmation && restoreJson != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmation = false; restoreJson = null },
            title = { Text("Drive ব্যাকআপ রিস্টোর করবেন?") },
            text = { Text("এতে এই ফোনের বর্তমান হিসাব Drive-এর ব্যাকআপ দিয়ে প্রতিস্থাপিত হবে।") },
            confirmButton = {
                TextButton(onClick = {
                    val json = restoreJson ?: return@TextButton
                    showRestoreConfirmation = false
                    restoreJson = null
                    viewModel.restoreFromJson(json) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }) { Text("রিস্টোর করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmation = false; restoreJson = null }) { Text("বাতিল") }
            }
        )
    }
}
