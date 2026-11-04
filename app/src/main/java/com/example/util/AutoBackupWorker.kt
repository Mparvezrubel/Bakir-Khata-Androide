package com.example.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase
import com.example.data.model.KhataBackupData
import com.example.data.preferences.ShopPreferences
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Creates scheduled, on-device backup snapshots. */
class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        val prefs = ShopPreferences(applicationContext)
        if (!prefs.isAutoBackupEnabled) return Result.success()

        val dao = AppDatabase.getDatabase(applicationContext).khataDao()
        val backupJson = KhataBackupData(
            shopName = prefs.shopName,
            customers = dao.getAllCustomersDirect(),
            transactions = dao.getAllTransactionsDirect()
        ).toJsonString()

        val backupDir = File(applicationContext.filesDir, BACKUP_DIRECTORY)
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            error("Could not create the backup directory")
        }

        val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        writeAtomically(File(backupDir, PRIMARY_BACKUP), backupJson)
        writeAtomically(File(backupDir, "backup_$date.json"), backupJson)
        pruneOldDailyBackups(backupDir)

        val completedAt = System.currentTimeMillis()
        prefs.lastAutoBackupTimestamp = completedAt
        prefs.lastBackupTimestamp = completedAt
        Result.success()
    } catch (error: Exception) {
        error.printStackTrace()
        Result.retry()
    }

    private fun writeAtomically(destination: File, content: String) {
        val temporary = File(destination.parentFile, "${destination.name}.tmp")
        temporary.writeText(content, Charsets.UTF_8)
        if (destination.exists() && !destination.delete()) {
            temporary.delete()
            error("Could not replace the previous backup")
        }
        if (!temporary.renameTo(destination)) {
            temporary.delete()
            error("Could not finish writing the backup")
        }
    }

    private fun pruneOldDailyBackups(directory: File) {
        val cutoff = System.currentTimeMillis() - RETENTION_DAYS * DAY_MILLIS
        directory.listFiles { file -> file.name.matches(DAILY_BACKUP_PATTERN) }
            ?.filter { it.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }

    companion object {
        private const val BACKUP_DIRECTORY = "auto_backups"
        private const val PRIMARY_BACKUP = "bakir_khata_drive_auto_backup.json"
        private const val RETENTION_DAYS = 30
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        private val DAILY_BACKUP_PATTERN = Regex("backup_\\d{8}\\.json")
    }
}

object AutoBackupScheduler {
    private const val UNIQUE_WORK_NAME = "DAILY_AUTO_DRIVE_BACKUP"

    fun scheduleDailyBackup(context: Context, wifiOnly: Boolean = false) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 2,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag("bakir_khata_auto_backup")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )
    }

    fun triggerImmediateBackup(context: Context) {
        val oneTimeWork = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .addTag("bakir_khata_immediate_backup")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "IMMEDIATE_AUTO_BACKUP",
            ExistingWorkPolicy.REPLACE,
            oneTimeWork
        )
    }

    fun cancelDailyBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
