package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gallery_images")
data class GalleryImage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val filePath: String, // Can be a local file path inside app's internal files, or an HTTPS sample URL
    val mimeType: String, // "image/jpeg", "image/png", or "image/webp"
    val dateAdded: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val tags: String = "", // Comma-separated tags
    val width: Int = 0,
    val height: Int = 0,
    val fileSizeBytes: Long = 0L
)
