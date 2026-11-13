package com.nawasena.pemandoo.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorage {

    private const val PHOTO_DIRECTORY = "landmark_photos"

    /**
     * Copies a gallery image into the private app storage. This is important
     * because the temporary read permission granted by the gallery picker
     * disappears after the activity is closed.
     */
    fun copyToAppStorage(context: Context, source: Uri): String? {
        return try {
            val directory = File(context.filesDir, PHOTO_DIRECTORY).apply { mkdirs() }
            val destination = File(directory, "landmark_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(source)?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            if (destination.length() > 0) destination.absolutePath else null
        } catch (exception: Exception) {
            null
        }
    }

    /**
     * Creates a file and a content:// URI for the system camera app.
     */
    fun createCameraFile(context: Context): Pair<File, Uri>? {
        return try {
            val directory = File(
                context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
                PHOTO_DIRECTORY
            ).apply { mkdirs() }
            val file = File(directory, "camera_${UUID.randomUUID()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            file to uri
        } catch (exception: Exception) {
            null
        }
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val file = File(path)
            if (file.exists()) file.delete()
        } catch (_: Exception) {
            // Nothing to do: an orphaned file is harmless.
        }
    }
}
