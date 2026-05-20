package com.example.data.local

import androidx.room.*
import com.example.data.model.GalleryImage
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryImageDao {
    @Query("SELECT * FROM gallery_images ORDER BY dateAdded DESC")
    fun getAllImages(): Flow<List<GalleryImage>>

    @Query("SELECT * FROM gallery_images WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavoriteImages(): Flow<List<GalleryImage>>

    @Query("SELECT * FROM gallery_images WHERE id = :id")
    suspend fun getImageById(id: Int): GalleryImage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: GalleryImage): Long

    @Update
    suspend fun updateImage(image: GalleryImage)

    @Delete
    suspend fun deleteImage(image: GalleryImage)
}
