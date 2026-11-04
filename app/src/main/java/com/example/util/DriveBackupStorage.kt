package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.example.data.model.KhataBackupData
import com.example.data.preferences.ShopPreferences
import java.io.IOException

/** Accesses a user-selected Google Drive (or other SAF provider) folder without storing credentials. */
object DriveBackupStorage {
    private const val BACKUP_FILE = "bakir_khata_drive_auto_backup.json"
    private const val MIME_TYPE = "application/json"

    fun saveFolder(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        ShopPreferences(context).driveBackupFolderUri = uri.toString()
    }

    fun clearFolder(context: Context) {
        val prefs = ShopPreferences(context)
        prefs.driveBackupFolderUri?.let { value ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(value),
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
        prefs.driveBackupFolderUri = null
    }

    fun hasFolder(context: Context): Boolean = !ShopPreferences(context).driveBackupFolderUri.isNullOrBlank()

    /** Writes the latest backup and dated snapshot to the selected provider folder. */
    fun writeBackup(context: Context, json: String, timestamp: Long = System.currentTimeMillis()) {
        val tree = folderUri(context) ?: throw IOException("Choose a Google Drive backup folder first")
        writeDocument(context, tree, BACKUP_FILE, json)
        val date = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
            .format(java.util.Date(timestamp))
        writeDocument(context, tree, "backup_$date.json", json)
    }

    /** Returns the latest cloud snapshot, or null if no backup exists yet. */
    fun readLatestBackup(context: Context): String? {
        val tree = folderUri(context) ?: return null
        val parentId = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                if (cursor.getString(nameColumn) == BACKUP_FILE) {
                    val document = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(idColumn))
                    return context.contentResolver.openInputStream(document)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                }
            }
        }
        return null
    }

    private fun folderUri(context: Context): Uri? =
        ShopPreferences(context).driveBackupFolderUri?.let(Uri::parse)

    private fun writeDocument(context: Context, tree: Uri, name: String, content: String) {
        val resolver = context.contentResolver
        val parentId = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
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

        val temp = DocumentsContract.createDocument(resolver, tree, MIME_TYPE, "$name.tmp")
            ?: throw IOException("Could not create backup in the selected Drive folder")
        try {
            resolver.openOutputStream(temp, "wt")?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                ?: throw IOException("Could not open the Drive backup file")
            existing?.let { old ->
                if (!DocumentsContract.deleteDocument(resolver, old)) throw IOException("Could not replace the previous Drive backup")
            }
            if (DocumentsContract.renameDocument(resolver, temp, name) == null) {
                throw IOException("Could not finalize the Drive backup file")
            }
        } catch (error: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, temp) }
            throw error
        }
    }

    /** Validates the cloud file before it is allowed to replace local database contents. */
    fun validatedBackup(context: Context): KhataBackupData? =
        readLatestBackup(context)?.let(KhataBackupData::fromJsonString)
}
