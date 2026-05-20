package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.data.local.GalleryImageDao
import com.example.data.model.GalleryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class GalleryRepository(private val dao: GalleryImageDao) {

    val allImages: Flow<List<GalleryImage>> = dao.getAllImages()
    val favoriteImages: Flow<List<GalleryImage>> = dao.getFavoriteImages()

    suspend fun getById(id: Int): GalleryImage? = withContext(Dispatchers.IO) {
        dao.getImageById(id)
    }

    suspend fun insert(image: GalleryImage): Long = withContext(Dispatchers.IO) {
        dao.insertImage(image)
    }

    suspend fun update(image: GalleryImage) = withContext(Dispatchers.IO) {
        dao.updateImage(image)
    }

    suspend fun delete(image: GalleryImage) = withContext(Dispatchers.IO) {
        // If image file exists in internal storage, delete it as well to free up space
        if (image.filePath.startsWith("/")) {
            val file = File(image.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        dao.deleteImage(image)
    }

    // Helper, copies an input stream to internal files storage
    suspend fun saveImageFromStream(context: Context, inputStream: InputStream, mimeType: String, originalSize: Long): FileResult = withContext(Dispatchers.IO) {
        try {
            val extension = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                else -> "jpg"
            }
            val fileName = "photo_${System.currentTimeMillis()}.$extension"
            val destFile = File(context.filesDir, fileName)
            
            destFile.outputStream().use { output ->
                inputStream.use { input ->
                    input.copyTo(output)
                }
            }

            // Read dimensions
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeFile(destFile.absolutePath, options)
            
            FileResult.Success(
                filePath = destFile.absolutePath,
                width = options.outWidth,
                height = options.outHeight,
                sizeBytes = if (originalSize > 0) originalSize else destFile.length()
            )
        } catch (e: Exception) {
            FileResult.Error(e.message ?: "Unknown error while saving file")
        }
    }

    // Helper, compresses and saves drawing bitmap to internal files storage
    suspend fun saveDrawingBitmap(context: Context, bitmap: Bitmap, title: String): FileResult = withContext(Dispatchers.IO) {
        try {
            val fileName = "sketch_${System.currentTimeMillis()}.png"
            val destFile = File(context.filesDir, fileName)
            
            destFile.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }

            FileResult.Success(
                filePath = destFile.absolutePath,
                width = bitmap.width,
                height = bitmap.height,
                sizeBytes = destFile.length()
            )
        } catch (e: Exception) {
            FileResult.Error(e.message ?: "Unknown error while saving sketch")
        }
    }
}

sealed interface FileResult {
    data class Success(val filePath: String, val width: Int, val height: Int, val sizeBytes: Long) : FileResult
    data class Error(val message: String) : FileResult
}
