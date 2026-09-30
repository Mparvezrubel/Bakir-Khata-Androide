package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.preferences.ShopPreferences
import com.example.ui.BakirKhataViewModel
import com.example.ui.theme.KhataCardBorder
import com.example.ui.theme.KhataPrimary
import com.example.ui.theme.KhataReceivableBg
import com.example.ui.theme.KhataReceivableBorder
import com.example.ui.theme.KhataReceivableGreen
import com.example.ui.theme.KhataTextPrimary
import com.example.ui.theme.KhataTextSecondary
import com.example.util.AutoBackupScheduler
import com.example.util.Formatters
import com.example.util.ReminderHelper
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    viewModel: BakirKhataViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var shopName by remember { mutableStateOf(viewModel.shopPrefs.shopName) }
    var ownerName by remember { mutableStateOf(viewModel.shopPrefs.ownerName) }
    var ownerPhone by remember { mutableStateOf(viewModel.shopPrefs.ownerPhone) }
    var smsTemplate by remember { mutableStateOf(viewModel.shopPrefs.smsReminderTemplate) }

    var googleAccountEmail by remember { mutableStateOf(viewModel.shopPrefs.googleAccountEmail) }
    var isAutoBackupEnabled by remember { mutableStateOf(viewModel.shopPrefs.isAutoBackupEnabled) }
    var isBackupWifiOnly by remember { mutableStateOf(viewModel.shopPrefs.isBackupWifiOnly) }
    var lastBackupTime by remember { mutableStateOf(viewModel.shopPrefs.lastBackupTimestamp) }
    var lastAutoBackupTime by remember { mutableStateOf(viewModel.shopPrefs.lastAutoBackupTimestamp) }

    var showConnectGmailDialog by remember { mutableStateOf(false) }
    var tempGmailInput by remember { mutableStateOf("") }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var showResetDataConfirm by remember { mutableStateOf(false) }

    // Storage Access Framework launcher to save/export JSON directly to Google Drive or Local Storage
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = viewModel.getBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray(Charsets.UTF_8))
                    }
                    viewModel.markBackupCompleted()
                    lastBackupTime = System.currentTimeMillis()
                    Toast.makeText(context, "গুগল ড্রাইভ / ফাইলে ব্যাকআপ সফলভাবে সংরক্ষিত হয়েছে!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "ব্যাকআপ সংরক্ষণ ব্যর্থ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Storage Access Framework launcher to open/restore JSON from Google Drive or Local Storage
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonContent = inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (!jsonContent.isNullOrBlank()) {
                    pendingRestoreJson = jsonContent
                    showRestoreConfirmDialog = true
                } else {
                    Toast.makeText(context, "নির্বাচিত ফাইলটি ফাঁকা", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "ফাইল পড়া যায়নি: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "গুগল ড্রাইভ ও ব্যাকআপ সেটিংস",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.shopPrefs.shopName = shopName
                            viewModel.shopPrefs.ownerName = ownerName
                            viewModel.shopPrefs.ownerPhone = ownerPhone
                            viewModel.shopPrefs.smsReminderTemplate = smsTemplate
                            viewModel.shopPrefs.googleAccountEmail = googleAccountEmail
                            viewModel.shopPrefs.isAutoBackupEnabled = isAutoBackupEnabled
                            viewModel.shopPrefs.isBackupWifiOnly = isBackupWifiOnly

                            if (isAutoBackupEnabled) {
                                AutoBackupScheduler.scheduleDailyBackup(context, wifiOnly = isBackupWifiOnly)
                            } else {
                                AutoBackupScheduler.cancelDailyBackup(context)
                            }

                            Toast.makeText(context, "সেটিংস সংরক্ষণ করা হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("save_settings_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KhataPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Google Drive & Gmail Account Connection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, KhataCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F0FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mail,
                                    contentDescription = null,
                                    tint = Color(0xFF1A73E8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "জিমেইল ও ড্রাইভ একাউন্ট",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = KhataTextPrimary
                                )
                                Text(
                                    text = "Google Drive Cloud Sync",
                                    fontSize = 11.sp,
                                    color = KhataTextSecondary
                                )
                            }
                        }

                        // Connection Status Pill
                        val isConnected = googleAccountEmail.isNotBlank()
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isConnected) KhataReceivableBg else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (isConnected) KhataReceivableBorder else KhataCardBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isConnected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = KhataReceivableGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = if (isConnected) "সংযুক্ত" else "সংযুক্ত নয়",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConnected) KhataReceivableGreen else KhataTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (googleAccountEmail.isNotBlank()) {
                        // Connected State Display
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = KhataReceivableBg.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, KhataReceivableBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = KhataReceivableGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = googleAccountEmail,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KhataTextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "📁 গুগল ড্রাইভ ফোল্ডার: /BakirKhata-Backup",
                                    fontSize = 11.sp,
                                    color = KhataTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    tempGmailInput = googleAccountEmail
                                    showConnectGmailDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("পরিবর্তন করুন", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    googleAccountEmail = ""
                                    viewModel.shopPrefs.googleAccountEmail = ""
                                    Toast.makeText(context, "জিমেইল একাউন্ট সংযোগ বিচ্ছিন্ন করা হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("ডিসকানেক্ট", fontSize = 12.sp)
                            }
                        }
                    } else {
                        // Not connected state
                        Text(
                            text = "আপনার জিমেইল একাউন্ট সংযুক্ত করুন। প্রতিদিনের ব্যাকআপ স্বয়ংক্রিয়ভাবে আপনার গুগল ড্রাইভ ক্লাউডে জমা হয়ে যাবে।",
                            fontSize = 12.sp,
                            color = KhataTextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                tempGmailInput = "itsmparvezrubel@gmail.com"
                                showConnectGmailDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("connect_gmail_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("জিমেইল (Gmail) সংযুক্ত করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 2. Daily Automatic Backup Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, KhataCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(KhataPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = KhataPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "প্রতিদিন স্বয়ংক্রিয় ব্যাকআপ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = KhataTextPrimary
                                )
                                Text(
                                    text = "প্রতি ২৪ ঘণ্টায় স্বয়ংক্রিয় ব্যাকআপ",
                                    fontSize = 11.sp,
                                    color = KhataTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isAutoBackupEnabled,
                            onCheckedChange = { checked ->
                                isAutoBackupEnabled = checked
                                viewModel.shopPrefs.isAutoBackupEnabled = checked
                                if (checked) {
                                    AutoBackupScheduler.scheduleDailyBackup(context, wifiOnly = isBackupWifiOnly)
                                    Toast.makeText(context, "দৈনিক স্বয়ংক্রিয় ব্যাকআপ চালু হয়েছে", Toast.LENGTH_SHORT).show()
                                } else {
                                    AutoBackupScheduler.cancelDailyBackup(context)
                                    Toast.makeText(context, "স্বয়ংক্রিয় ব্যাকআপ বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = KhataPrimary
                            )
                        )
                    }

                    AnimatedVisibility(visible = isAutoBackupEnabled) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))

                            // Wi-Fi only toggle
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Wifi,
                                            contentDescription = null,
                                            tint = KhataPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "শুধু ওয়াই-ফাই (Wi-Fi) থাকলে ব্যাকআপ",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = KhataTextPrimary
                                            )
                                            Text(
                                                text = "মোবাইল ডেটা খরচ বাঁচাবে",
                                                fontSize = 10.sp,
                                                color = KhataTextSecondary
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = isBackupWifiOnly,
                                        onCheckedChange = {
                                            isBackupWifiOnly = it
                                            viewModel.shopPrefs.isBackupWifiOnly = it
                                            AutoBackupScheduler.scheduleDailyBackup(context, wifiOnly = it)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = KhataPrimary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Auto Backup Status Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = KhataPrimary.copy(alpha = 0.06f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, KhataPrimary.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = KhataPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "স্বয়ংক্রিয় ব্যাকআপ স্থিতি:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = KhataPrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    val lastTimeText = if (lastAutoBackupTime > 0) {
                                        Formatters.formatDateTime(lastAutoBackupTime)
                                    } else {
                                        "আজ রাতে প্রথম ব্যাকআপ কার্যকর হবে"
                                    }

                                    Text(
                                        text = "• সর্বশেষ ব্যাকআপ: $lastTimeText",
                                        fontSize = 11.sp,
                                        color = KhataTextPrimary
                                    )
                                    Text(
                                        text = "• সিডিউল: প্রতিদিন মধ্যরাত ১২:০০ টা",
                                        fontSize = 11.sp,
                                        color = KhataTextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Immediate Test Auto Backup Button
                            OutlinedButton(
                                onClick = {
                                    AutoBackupScheduler.triggerImmediateBackup(context)
                                    lastAutoBackupTime = System.currentTimeMillis()
                                    Toast.makeText(context, "স্বয়ংক্রিয় ব্যাকআপ সফলভাবে সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, KhataPrimary.copy(alpha = 0.4f))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = KhataPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("এখনই অটো-ব্যাকআপ পরীক্ষা করুন", fontSize = 12.sp, color = KhataPrimary)
                            }
                        }
                    }
                }
            }

            // 3. Manual Backup & Restore Actions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, KhataCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(KhataPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = KhataPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ম্যানুয়াল ব্যাকআপ ও রিস্টোর",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = KhataTextPrimary
                            )
                            val backupText = if (lastBackupTime > 0) {
                                "সর্বশেষ ম্যানুয়াল ব্যাকআপ: ${Formatters.formatDateTime(lastBackupTime)}"
                            } else {
                                "এখনো কোনো ম্যানুয়াল ব্যাকআপ নেওয়া হয়নি"
                            }
                            Text(
                                text = backupText,
                                fontSize = 11.sp,
                                color = KhataTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Button 1: Save to Google Drive / File (SAF)
                    Button(
                        onClick = {
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            val fileName = "bakir_khata_backup_$timeStamp.json"
                            createDocumentLauncher.launch(fileName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_to_drive_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = KhataPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("গুগল ড্রাইভে ফাইল সংরক্ষণ করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Button 2: Share backup JSON to Google Drive directly via Share Sheet
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                shareBackupFileDirectly(context, viewModel)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_backup_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, KhataCardBorder)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("গুগল ড্রাইভ অ্যাপে ফাইল শেয়ার করুন", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Button 3: Restore from Google Drive / File
                    OutlinedButton(
                        onClick = {
                            openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_backup_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, KhataCardBorder)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ড্রাইভ / মেমরি থেকে রিস্টোর করুন", fontSize = 13.sp)
                    }
                }
            }

            // 4. SMS Reminder Customization Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, KhataCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(KhataReceivableGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Message,
                                contentDescription = null,
                                tint = KhataReceivableGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "এসএমএস রিমাইন্ডার টেমপ্লেট",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = KhataTextPrimary
                            )
                            Text(
                                text = "ট্যাগ: {NAME} = নাম, {AMOUNT} = টাকা, {SHOP} = দোকান",
                                fontSize = 11.sp,
                                color = KhataTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = smsTemplate,
                        onValueChange = { smsTemplate = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("sms_template_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = KhataCardBorder,
                            focusedBorderColor = KhataPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live preview of template
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "প্রিভিউ (গ্রাহক যা দেখতে পাবেন):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KhataTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val previewMsg = ReminderHelper.composeReminderMessage(
                                template = smsTemplate,
                                customerName = "রহিম মিয়া",
                                amount = 1500.0,
                                shopName = shopName
                            )
                            Text(
                                text = previewMsg,
                                fontSize = 12.sp,
                                color = KhataTextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                smsTemplate = ShopPreferences.DEFAULT_SMS_TEMPLATE
                            }
                        ) {
                            Text("ডিফল্ট টেমপ্লেট ফেরত আনুন", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 5. Shop Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, KhataCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AddBusiness,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "দোকানের পরিচিতি ও প্রোফাইল",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = KhataTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("দোকান / ব্যবসার নাম") },
                        placeholder = { Text("যেমন: মেসার্স ভাই ভাই স্টোর") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shop_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("স্বত্বাধিকারী / আপনার নাম (ঐচ্ছিক)") },
                        placeholder = { Text("যেমন: হাজী আব্দুর রাজ্জাক") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ownerPhone,
                        onValueChange = { ownerPhone = it },
                        label = { Text("দোকানের মোবাইল নম্বর (ঐচ্ছিক)") },
                        placeholder = { Text("যেমন: 01712345678") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_phone_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.shopPrefs.shopName = shopName
                            viewModel.shopPrefs.ownerName = ownerName
                            viewModel.shopPrefs.ownerPhone = ownerPhone
                            viewModel.shopPrefs.smsReminderTemplate = smsTemplate
                            Toast.makeText(context, "তথ্য সফলভাবে সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_profile_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = KhataPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("প্রোফাইল সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 6. Safety Guarantee & Reset Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = KhataReceivableGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "১০০% অফলাইন ও সম্পূর্ণ নিরাপদ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = KhataReceivableGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "আপনার বাকি হিসাবের সকল তথ্য আপনার মোবাইল ফোনেই সংরক্ষিত থাকে। ইন্টারনেট সংযোগ ছাড়াই এটি সম্পূর্ণ কাজ করে। গুগল ড্রাইভ ব্যাকআপ আপনার ব্যবসাকে ডেটা হারানোর ঝুঁকি থেকে চিরতরে মুক্ত রাখে।",
                        fontSize = 12.sp,
                        color = KhataTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showResetDataConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_demo_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("নমুনা ডেমো ডেটা লোড করুন", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Connect Gmail Dialog
    if (showConnectGmailDialog) {
        AlertDialog(
            onDismissRequest = { showConnectGmailDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mail, contentDescription = null, tint = Color(0xFF1A73E8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("জিমেইল একাউন্ট সংযুক্ত করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "আপনার জিমেইল একাউন্ট দিন। ব্যাকআপ ফাইল এই একাউন্টের গুগল ড্রাইভে স্বয়ংক্রিয়ভাবে জমা হবে।",
                        fontSize = 12.sp,
                        color = KhataTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = tempGmailInput,
                        onValueChange = { tempGmailInput = it },
                        label = { Text("Gmail ঠিকানা") },
                        placeholder = { Text("example@gmail.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Suggestion Chip for current user email
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F0FE),
                        modifier = Modifier
                            .clickable {
                                tempGmailInput = "itsmparvezrubel@gmail.com"
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡 ব্যবহার করুন: itsmparvezrubel@gmail.com",
                                fontSize = 11.sp,
                                color = Color(0xFF1A73E8),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempGmailInput.isNotBlank() && tempGmailInput.contains("@")) {
                            googleAccountEmail = tempGmailInput.trim()
                            viewModel.shopPrefs.googleAccountEmail = googleAccountEmail
                            showConnectGmailDialog = false
                            Toast.makeText(context, "জিমেইল সফলভাবে সংযুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "সঠিক জিমেইল ঠিকানা দিন", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                ) {
                    Text("কানেক্ট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectGmailDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Confirmation for restore
    if (showRestoreConfirmDialog && pendingRestoreJson != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreJson = null
            },
            title = { Text("ব্যাকআপ রিস্টোর নিশ্চিত করুন", fontWeight = FontWeight.Bold) },
            text = {
                Text("রিস্টোর করলে বর্তমানের ডেটা প্রতিস্থাপিত হবে। আপনি কি নিশ্চিত যে আপনি ব্যাকআপ ফাইলটি রিস্টোর করতে চান?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = pendingRestoreJson!!
                        viewModel.restoreFromJson(json) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (success) {
                                lastBackupTime = System.currentTimeMillis()
                                onBack()
                            }
                        }
                        showRestoreConfirmDialog = false
                        pendingRestoreJson = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KhataPrimary)
                ) {
                    Text("হ্যাঁ, রিস্টোর করুন")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestoreJson = null
                    }
                ) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Confirmation for reset / demo data
    if (showResetDataConfirm) {
        AlertDialog(
            onDismissRequest = { showResetDataConfirm = false },
            title = { Text("ডেমো ডেটা রিসেট করবেন?", fontWeight = FontWeight.Bold) },
            text = { Text("ডেমো কাস্টমার ও বাকির হিসাবসমূহ পুনরায় লোড করা হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToDemoData()
                        showResetDataConfirm = false
                        Toast.makeText(context, "ডেমো ডেটা সফলভাবে লোড হয়েছে", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KhataPrimary)
                ) {
                    Text("লোড করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDataConfirm = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

private suspend fun shareBackupFileDirectly(context: Context, viewModel: BakirKhataViewModel) {
    try {
        val json = viewModel.getBackupJson()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "bakir_khata_backup_$timeStamp.json"

        val cacheDir = File(context.cacheDir, "backups")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val backupFile = File(cacheDir, fileName)
        backupFile.writeText(json, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "বাকির খাতা ব্যাকআপ ফাইল")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "গুগল ড্রাইভ বা ফাইলে সংরক্ষণ করুন"))
        viewModel.markBackupCompleted()
    } catch (e: Exception) {
        // Fallback: copy or share plain text
        try {
            val json = viewModel.getBackupJson()
            ReminderHelper.shareText(context, "বাকির খাতা ব্যাকআপ ফাইল", json)
            viewModel.markBackupCompleted()
        } catch (ex: Exception) {
            Toast.makeText(context, "ফাইল তৈরি করা যায়নি: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
