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

class AutoBackupWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            val prefs = ShopPreferences(applicationContext)
            if (!prefs.isAutoBackupEnabled) return Result.success()

            val dao = AppDatabase.getDatabase(applicationContext).khataDao()
            val now = System.currentTimeMillis()
            val json = KhataBackupData(
                shopName = prefs.shopName,
                customers = dao.getAllCustomersDirect(),
                transactions = dao.getAllTransactionsDirect(),
                exportedAt = now
            ).toJsonString()

            val directory = File(applicationContext.filesDir, BACKUP_DIRECTORY)
            if (!directory.exists() && !directory.mkdirs()) error("Could not create local backup directory")
            val day = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(now))
            writeAtomically(File(directory, PRIMARY_BACKUP), json)
            writeAtomically(File(directory, "backup_$day.json"), json)

            // Keep a local backup even before the user grants Drive-folder access.
            if (DriveBackupStorage.hasFolder(applicationContext)) {
                DriveBackupStorage.writeBackup(applicationContext, json, now)
            }
            pruneOldBackups(directory, now)
            prefs.lastAutoBackupTimestamp = now
            prefs.lastBackupTimestamp = now
            Result.success()
        } catch (error: Exception) {
            error.printStackTrace()
            Result.retry()
        }
    }

    private fun writeAtomically(destination: File, content: String) {
        val temporary = File(destination.parentFile, "${destination.name}.tmp")
        temporary.writeText(content, Charsets.UTF_8)
        if (destination.exists() && !destination.delete()) {
            temporary.delete()
            error("Could not replace the previous local backup")
        }
        if (!temporary.renameTo(destination)) {
            temporary.delete()
            error("Could not finalize the local backup")
        }
    }

    private fun pruneOldBackups(directory: File, now: Long) {
        val cutoff = now - RETENTION_DAYS * DAY_MILLIS
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
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS, 2, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag("bakir_khata_auto_backup")
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun triggerImmediateBackup(context: Context) {
        val request = OneTimeWorkRequestBuilder<AutoBackupWorker>().addTag("bakir_khata_immediate_backup").build()
        WorkManager.getInstance(context).enqueueUniqueWork("IMMEDIATE_AUTO_BACKUP", ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelDailyBackup(context: Context) { WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME) }
}
