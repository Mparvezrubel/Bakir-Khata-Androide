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
import com.example.data.model.Customer
import com.example.data.model.Transaction
import com.example.data.preferences.ShopPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val prefs = ShopPreferences(applicationContext)

            if (!prefs.isAutoBackupEnabled) {
                return Result.success()
            }

            val customers: List<Customer> = db.khataDao().getAllCustomersDirect()
            val transactions: List<Transaction> = db.khataDao().getAllTransactionsDirect()

            val rootJson = JSONObject().apply {
                put("version", 1)
                put("exportTimestamp", System.currentTimeMillis())
                put("shopName", prefs.shopName)
                put("ownerName", prefs.ownerName)
                put("ownerPhone", prefs.ownerPhone)
                put("googleAccount", prefs.googleAccountEmail)
                put("backupType", "DAILY_AUTO_BACKUP")

                val customersArray = JSONArray()
                customers.forEach { c ->
                    customersArray.put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("phone", c.phone)
                        put("address", c.address)
                        put("type", c.customerType.name)
                        put("createdAt", c.createdAt)
                        put("updatedAt", c.updatedAt)
                    })
                }
                put("customers", customersArray)

                val transactionsArray = JSONArray()
                transactions.forEach { t ->
                    transactionsArray.put(JSONObject().apply {
                        put("id", t.id)
                        put("customerId", t.customerId)
                        put("amount", t.amount)
                        put("type", t.type.name)
                        put("note", t.note)
                        put("billNumber", t.billNumber)
                        put("date", t.date)
                        put("dueDate", t.dueDate ?: JSONObject.NULL)
                    })
                }
                put("transactions", transactionsArray)
            }

            // Save to auto-backup directory in app storage
            val backupDir = File(applicationContext.filesDir, "auto_backups").apply {
                if (!exists()) mkdirs()
            }

            // Primary auto-backup file
            val backupFile = File(backupDir, "bakir_khata_drive_auto_backup.json")
            backupFile.writeText(rootJson.toString(2), Charsets.UTF_8)

            // Also keep rolling daily backup
            val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val dailyFile = File(backupDir, "backup_$dateStr.json")
            dailyFile.writeText(rootJson.toString(2), Charsets.UTF_8)

            // Update preference timestamps
            val now = System.currentTimeMillis()
            prefs.lastAutoBackupTimestamp = now
            prefs.lastBackupTimestamp = now

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
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
