package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Stores backups in a user-selected SAF folder (Google Drive or another document provider). */
object DriveBackupStorage {
    private const val PREFS = "drive_backup_settings"
    private const val FOLDER_URI = "backup_folder_uri"
    private const val LATEST_FILE = "bakir_khata_drive_auto_backup.json"
    private const val MIME_TYPE = "application/json"
    private const val GRANT_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    fun saveFolder(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri, GRANT_FLAGS)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(FOLDER_URI, uri.toString()).apply()
    }

    fun clearFolder(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(FOLDER_URI, null)?.let { saved ->
            runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(saved), GRANT_FLAGS) }
        }
        prefs.edit().remove(FOLDER_URI).apply()
    }

    fun hasFolder(context: Context): Boolean = folderUri(context) != null

    fun writeBackup(context: Context, json: String, timestamp: Long = System.currentTimeMillis()) {
        val tree = folderUri(context) ?: throw IOException("Select a Google Drive folder in backup settings first")
        writeDocument(context, tree, LATEST_FILE, json)
        val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(timestamp))
        writeDocument(context, tree, "backup_$date.json", json)
    }

    fun readLatestBackup(context: Context): String? {
        val tree = folderUri(context) ?: return null
        val parentId = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )
        context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                if (cursor.getString(nameColumn) == LATEST_FILE) {
                    val document = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(idColumn))
                    return context.contentResolver.openInputStream(document)
                        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                }
            }
        }
        return null
    }

    private fun folderUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val value = prefs.getString(FOLDER_URI, null) ?: return null
        return runCatching { Uri.parse(value) }.getOrNull()
    }

    private fun writeDocument(context: Context, tree: Uri, name: String, content: String) {
        val resolver = context.contentResolver
        val parentId = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )
        var existing: Uri? = null
        resolver.query(children, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                if (cursor.getString(nameColumn) == name) {
                    existing = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(idColumn))
                    break
                }
            }
        }

        val tempName = ".${name}.${System.currentTimeMillis()}.tmp"
        val temporary = DocumentsContract.createDocument(resolver, tree, MIME_TYPE, tempName)
            ?: throw IOException("Could not create a backup in the selected folder")
        try {
            resolver.openOutputStream(temporary, "wt")?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                ?: throw IOException("Could not write the backup to the selected folder")
            existing?.let { old ->
                if (!DocumentsContract.deleteDocument(resolver, old)) throw IOException("Could not replace the previous backup")
            }
            if (DocumentsContract.renameDocument(resolver, temporary, name) == null) {
                throw IOException("Could not finalize the Drive backup")
            }
        } catch (error: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, temporary) }
            throw error
        }
    }
}
