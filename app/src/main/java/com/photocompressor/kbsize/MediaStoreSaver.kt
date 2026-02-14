package com.photocompressor.kbsize

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * MediaStoreSaver saves compressed images to device storage using MediaStore API.
 * Supports scoped storage for Android 10+.
 */
class MediaStoreSaver {

    companion object {
        private const val TAG = "MediaStoreSaver"
        private const val FOLDER_NAME = "PhotoCompressor"
    }

    /**
     * Save compressed image data to MediaStore in Pictures/PhotoCompressor folder.
     * Returns the Uri of the saved file.
     */
    suspend fun saveImage(
        context: Context,
        imageData: ByteArray,
        fileName: String = "compressed_${System.currentTimeMillis()}.jpg"
    ): kotlin.Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$FOLDER_NAME")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val imageUri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext kotlin.Result.failure(IOException("Failed to create MediaStore entry"))

            // Write image data
            contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                outputStream.write(imageData)
                outputStream.flush()
            } ?: return@withContext kotlin.Result.failure(IOException("Failed to open output stream"))

            // Mark as not pending (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(imageUri, contentValues, null, null)
            }

            Log.d(TAG, "Image saved successfully: $imageUri")
            kotlin.Result.success(imageUri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save image", e)
            kotlin.Result.failure(e)
        }
    }
}
