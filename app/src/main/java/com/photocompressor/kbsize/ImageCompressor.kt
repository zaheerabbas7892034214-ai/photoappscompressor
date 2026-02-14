package com.photocompressor.kbsize

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * ImageCompressor handles image compression using binary search algorithm
 * to achieve target file size in KB.
 */
class ImageCompressor {

    companion object {
        private const val TAG = "ImageCompressor"
        private const val MIN_QUALITY = 20
        private const val MAX_QUALITY = 100
        private const val MAX_ATTEMPTS = 10
        private const val DIMENSION_SCALE_FACTOR = 0.9f
    }

    data class Result(
        val compressedData: ByteArray,
        val originalSizeKB: Int,
        val compressedSizeKB: Int,
        val compressionPercentage: Int,
        val finalWidth: Int,
        val finalHeight: Int
    )

    /**
     * Compress image to target size using binary search on quality.
     * If quality alone can't reach target, gradually downscale dimensions.
     */
    suspend fun compressToTargetSize(
        context: Context,
        uri: Uri,
        targetSizeKB: Int
    ): kotlin.Result<Result> = withContext(Dispatchers.Default) {
        try {
            // Decode original image with proper options
            val originalBitmap = decodeBitmapFromUri(context, uri)
                ?: return@withContext kotlin.Result.failure(Exception("Failed to decode image"))

            val originalSizeKB = calculateOriginalSize(context, uri)
            Log.d(TAG, "Original size: ${originalSizeKB}KB, Target: ${targetSizeKB}KB")

            var currentBitmap = originalBitmap
            var attempts = 0
            var compressedData: ByteArray? = null
            val targetSizeBytes = targetSizeKB * 1024

            // First, try binary search on quality with original dimensions
            compressedData = binarySearchQuality(
                currentBitmap,
                targetSizeBytes,
                MIN_QUALITY,
                MAX_QUALITY
            )

            // If still too large at minimum quality, downscale dimensions
            while (compressedData.size > targetSizeBytes && attempts < MAX_ATTEMPTS) {
                val newWidth = (currentBitmap.width * DIMENSION_SCALE_FACTOR).roundToInt()
                val newHeight = (currentBitmap.height * DIMENSION_SCALE_FACTOR).roundToInt()

                if (newWidth < 50 || newHeight < 50) {
                    Log.w(TAG, "Image too small to compress further")
                    break
                }

                Log.d(TAG, "Downscaling to ${newWidth}x${newHeight}")
                val scaledBitmap = Bitmap.createScaledBitmap(currentBitmap, newWidth, newHeight, true)
                
                if (currentBitmap != originalBitmap) {
                    currentBitmap.recycle()
                }
                currentBitmap = scaledBitmap

                // Try binary search again with new dimensions
                compressedData = binarySearchQuality(
                    currentBitmap,
                    targetSizeBytes,
                    MIN_QUALITY,
                    MAX_QUALITY
                )

                attempts++
            }

            val compressedSizeKB = compressedData.size / 1024
            val compressionPercentage = if (originalSizeKB > 0) {
                ((originalSizeKB - compressedSizeKB) * 100 / originalSizeKB).coerceIn(0, 100)
            } else 0

            Log.d(TAG, "Compression complete: ${compressedSizeKB}KB (${compressionPercentage}% reduction)")

            // Clean up
            if (currentBitmap != originalBitmap) {
                currentBitmap.recycle()
            }
            originalBitmap.recycle()

            kotlin.Result.success(
                Result(
                    compressedData = compressedData,
                    originalSizeKB = originalSizeKB,
                    compressedSizeKB = compressedSizeKB,
                    compressionPercentage = compressionPercentage,
                    finalWidth = currentBitmap.width,
                    finalHeight = currentBitmap.height
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Compression failed", e)
            kotlin.Result.failure(e)
        }
    }

    /**
     * Binary search algorithm to find optimal JPEG quality.
     */
    private fun binarySearchQuality(
        bitmap: Bitmap,
        targetSizeBytes: Int,
        minQuality: Int,
        maxQuality: Int
    ): ByteArray {
        var low = minQuality
        var high = maxQuality
        var bestData: ByteArray? = null
        var bestDiff = Int.MAX_VALUE

        while (low <= high) {
            val mid = (low + high) / 2
            val data = compressBitmap(bitmap, mid)

            val diff = data.size - targetSizeBytes
            val absDiff = kotlin.math.abs(diff)

            // Keep track of best result
            if (absDiff < bestDiff) {
                bestDiff = absDiff
                bestData = data
            }

            when {
                data.size > targetSizeBytes -> {
                    // Too large, decrease quality
                    high = mid - 1
                }
                data.size < targetSizeBytes -> {
                    // Too small, increase quality (try to get closer)
                    low = mid + 1
                }
                else -> {
                    // Perfect match
                    return data
                }
            }
        }

        return bestData ?: compressBitmap(bitmap, minQuality)
    }

    /**
     * Compress bitmap to JPEG with specified quality.
     */
    private fun compressBitmap(bitmap: Bitmap, quality: Int): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Decode bitmap from URI with efficient sampling.
     */
    private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // First decode with inJustDecodeBounds to get dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)

                // Calculate inSampleSize for memory efficiency
                options.inSampleSize = calculateInSampleSize(options, 2048, 2048)
                options.inJustDecodeBounds = false

                // Now decode the actual bitmap
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode bitmap", e)
            null
        }
    }

    /**
     * Calculate appropriate sample size for bitmap decoding.
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Calculate original file size in KB.
     */
    private fun calculateOriginalSize(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.available()
                bytes / 1024
            } ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate original size", e)
            0
        }
    }
}
