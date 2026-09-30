package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class ShopPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bakir_khata_prefs", Context.MODE_PRIVATE)

    var shopName: String
        get() = prefs.getString(KEY_SHOP_NAME, "আমার দোকান") ?: "আমার দোকান"
        set(value) = prefs.edit().putString(KEY_SHOP_NAME, value.trim()).apply()

    var ownerName: String
        get() = prefs.getString(KEY_OWNER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OWNER_NAME, value.trim()).apply()

    var ownerPhone: String
        get() = prefs.getString(KEY_OWNER_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OWNER_PHONE, value.trim()).apply()

    var smsReminderTemplate: String
        get() = prefs.getString(KEY_SMS_TEMPLATE, DEFAULT_SMS_TEMPLATE) ?: DEFAULT_SMS_TEMPLATE
        set(value) = prefs.edit().putString(KEY_SMS_TEMPLATE, value.trim()).apply()

    var lastBackupTimestamp: Long
        get() = prefs.getLong(KEY_LAST_BACKUP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP, value).apply()

    // Google Drive & Gmail Auto-Backup Properties
    var googleAccountEmail: String
        get() = prefs.getString(KEY_GOOGLE_ACCOUNT_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GOOGLE_ACCOUNT_EMAIL, value.trim()).apply()

    var isAutoBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, value).apply()

    var isBackupWifiOnly: Boolean
        get() = prefs.getBoolean(KEY_BACKUP_WIFI_ONLY, false)
        set(value) = prefs.edit().putBoolean(KEY_BACKUP_WIFI_ONLY, value).apply()

    var lastAutoBackupTimestamp: Long
        get() = prefs.getLong(KEY_LAST_AUTO_BACKUP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_AUTO_BACKUP, value).apply()

    companion object {
        private const val KEY_SHOP_NAME = "key_shop_name"
        private const val KEY_OWNER_NAME = "key_owner_name"
        private const val KEY_OWNER_PHONE = "key_owner_phone"
        private const val KEY_SMS_TEMPLATE = "key_sms_template"
        private const val KEY_LAST_BACKUP = "key_last_backup"

        private const val KEY_GOOGLE_ACCOUNT_EMAIL = "key_google_account_email"
        private const val KEY_AUTO_BACKUP_ENABLED = "key_auto_backup_enabled"
        private const val KEY_BACKUP_WIFI_ONLY = "key_backup_wifi_only"
        private const val KEY_LAST_AUTO_BACKUP = "key_last_auto_backup"

        const val DEFAULT_SMS_TEMPLATE =
            "আসসালামু আলাইকুম {NAME} ভাই/ম্যাডাম, {SHOP}-এ আপনার মোট বাকি ৳{AMOUNT}। অনুগ্রহ করে দ্রুত পরিশোধের অনুরোধ রইল। ধন্যবাদ।"
    }
}
