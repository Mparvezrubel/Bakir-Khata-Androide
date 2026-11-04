package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Stores backups in the SAF folder explicitly selected by the user. */
object DriveBackupStorage {
    private const val PREFS = "drive_backup_settings"
    private const val FOLDER_URI = "backup_folder_uri"
    private const val LATEST_FILE = "bakir_khata_drive_auto_backup.json"
    private const val MIME_TYPE = "application/json"
    private const val GRANT_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    private const val RETENTION_DAYS = 30L
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    private const val DAILY_PREFIX = "backup_"
    private val dailyNamePattern = Regex("backup_(\\d{8})\\.json")
    private val timestampFormat get() = SimpleDateFormat("yyyyMMdd", Locale.US)

    fun saveFolder(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri, GRANT_FLAGS)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(FOLDER_URI, uri.toString()).commit()
    }

    fun clearFolder(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(FOLDER_URI, null)?.let { saved ->
            runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(saved), GRANT_FLAGS) }
        }
        prefs.edit().remove(FOLDER_URI).commit()
    }

    fun hasFolder(context: Context): Boolean = folderUri(context) != null

    fun writeBackup(context: Context, json: String, timestamp: Long = System.currentTimeMillis()) {
        val tree = folderUri(context) ?: throw IOException("Select a Drive folder first")
        val day = timestampFormat.format(Date(timestamp))
        // Write dated copy before latest pointer so a failure doesn't destroy the previous latest backup.
        writeDocument(context, tree, "${DAILY_PREFIX}$day.json", json)
        writeDocument(context, tree, LATEST_FILE, json)
        pruneOldDailyBackups(context, tree, timestamp)
    }

    fun readLatestBackup(context: Context): String? {
        val tree = folderUri(context) ?: return null
        val latest = findDocument(context, tree, LATEST_FILE) ?: return null
        return context.contentResolver.openInputStream(latest)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: throw IOException("Could not read the latest Drive backup")
    }

    private fun folderUri(context: Context): Uri? {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(FOLDER_URI, null) ?: return null
        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null
        val hasPersistedGrant = context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
        }
        return uri.takeIf { hasPersistedGrant }
    }

    private fun findDocument(context: Context, tree: Uri, name: String): Uri? {
        val parentId = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val cursor = context.contentResolver.query(children, columns, null, null, null)
            ?: throw IOException("Drive provider did not return folder contents")
        cursor.use {
            val idColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (it.moveToNext()) {
                if (it.getString(nameColumn) == name) {
                    return DocumentsContract.buildDocumentUriUsingTree(tree, it.getString(idColumn))
                }
            }
        }
        return null
    }

    private fun writeDocument(context: Context, tree: Uri, name: String, content: String) {
        val resolver = context.contentResolver
        val temporary = DocumentsContract.createDocument(resolver, tree, MIME_TYPE, ".$name.${System.nanoTime()}.tmp")
            ?: throw IOException("Could not create a temporary Drive backup")
        try {
            resolver.openOutputStream(temporary, "wt")?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                ?: throw IOException("Could not write the Drive backup")

            val old = findDocument(context, tree, name)
            val oldStaging = old?.let {
                DocumentsContract.renameDocument(resolver, it, ".$name.${System.nanoTime()}.old")
                    ?: throw IOException("Could not safely stage the previous Drive backup")
            }
            val saved = DocumentsContract.renameDocument(resolver, temporary, name)
            if (saved == null) {
                oldStaging?.let { DocumentsContract.renameDocument(resolver, it, name) }
                throw IOException("Could not finalize the Drive backup")
            }
            oldStaging?.let { DocumentsContract.deleteDocument(resolver, it) }
        } catch (error: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, temporary) }
            throw error
        }
    }

    private fun pruneOldDailyBackups(context: Context, tree: Uri, now: Long) {
        val parentId = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val cursor = context.contentResolver.query(children, columns, null, null, null) ?: return
        val cutoff = now - RETENTION_DAYS * DAY_MILLIS
        cursor.use {
            val idColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (it.moveToNext()) {
                val name = it.getString(nameColumn) ?: continue
                val match = dailyNamePattern.matchEntire(name) ?: continue
                val date = runCatching { timestampFormat.parse(match.groupValues[1])?.time }.getOrNull() ?: continue
                if (date < cutoff) {
                    val document = DocumentsContract.buildDocumentUriUsingTree(tree, it.getString(idColumn))
                    DocumentsContract.deleteDocument(context.contentResolver, document)
                }
            }
        }
    }
}
