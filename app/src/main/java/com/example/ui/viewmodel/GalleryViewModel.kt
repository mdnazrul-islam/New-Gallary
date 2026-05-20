package com.example.ui.viewmodel

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.data.model.GalleryImage
import com.example.data.repository.FileResult
import com.example.data.repository.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class GalleryViewModel(private val repository: GalleryRepository) : ViewModel() {

    // Filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All") // "All", "Photos", "Sketches", "Favorites"
    val selectedCategory = _selectedCategory.asStateFlow()

    // Status / Message overlay
    private val _statusMessage = MutableStateFlow<StatusMessage?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    // Load actual images
    val allImages: StateFlow<List<GalleryImage>> = repository.allImages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered gallery items
    val filteredImages: StateFlow<List<GalleryImage>> = combine(
        allImages,
        _searchQuery,
        _selectedCategory
    ) { list, query, category ->
        var result = list

        // Category filter
        result = when (category) {
            "Favorites" -> result.filter { it.isFavorite }
            "Sketches" -> result.filter { it.filePath.contains("sketch_") }
            "Photos" -> result.filter { !it.filePath.contains("sketch_") }
            else -> result
        }

        // Search text filter
        if (query.isNotBlank()) {
            result = result.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.tags.contains(query, ignoreCase = true)
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Pre-populate with beautiful sample pictures if completely empty on first run
        viewModelScope.launch {
            allImages.first { it.isNotEmpty() || allImages.value.isEmpty() }
            if (allImages.value.isEmpty()) {
                prePopulateSamples()
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showMessage(message: String, isSuccess: Boolean = true) {
        _statusMessage.value = StatusMessage(message, isSuccess)
    }

    // Toggle Favorite
    fun toggleFavorite(image: GalleryImage) {
        viewModelScope.launch {
            repository.update(image.copy(isFavorite = !image.isFavorite))
        }
    }

    // Delete Item
    fun deleteImage(image: GalleryImage) {
        viewModelScope.launch {
            repository.delete(image)
            showMessage("ছবিটি সফলভাবে মুছে ফেলা হয়েছে", isSuccess = true)
        }
    }

    // Update Details (Title, Description, Tags)
    fun updateImageDetails(id: Int, title: String, description: String, tags: String) {
        viewModelScope.launch {
            val existing = repository.getById(id)
            if (existing != null) {
                repository.update(
                    existing.copy(
                        title = title.trim(),
                        description = description.trim(),
                        tags = tags.trim()
                    )
                )
                showMessage("ছবির বিবরণ আপডেট করা হয়েছে")
            }
        }
    }

    // Import from Device PickVisualMedia Uri
    fun importImageFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                
                // Get original size if possible
                var sizeBytes = 0L
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst()) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }

                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    showMessage("ছবি লোড করতে ব্যর্থ হয়েছে", isSuccess = false)
                    return@launch
                }

                val flowResult = repository.saveImageFromStream(context, inputStream, mimeType, sizeBytes)
                when (flowResult) {
                    is FileResult.Success -> {
                        val galleryImage = GalleryImage(
                            title = "Photo / ছবি",
                            description = "গ্যালারি থেকে সংগৃহীত চিত্র",
                            filePath = flowResult.filePath,
                            mimeType = mimeType,
                            width = flowResult.width,
                            height = flowResult.height,
                            fileSizeBytes = flowResult.sizeBytes,
                            tags = "imported, photo"
                        )
                        repository.insert(galleryImage)
                        showMessage("ছবিটি সফলভাবে গ্যালারিতে সংরক্ষণ করা হয়েছে!")
                    }
                    is FileResult.Error -> {
                        showMessage("ডিভাইস থেকে সেভ করতে সমস্যা: ${flowResult.message}", isSuccess = false)
                    }
                }
            } catch (e: Exception) {
                showMessage("ছবি ইম্পোর্ট করতে সমস্যা: ${e.localizedMessage}", isSuccess = false)
            }
        }
    }

    // Save sketch in-house Canvas Painting
    fun saveSketch(context: Context, bitmap: Bitmap, title: String, desc: String, tags: String) {
        viewModelScope.launch {
            val result = repository.saveDrawingBitmap(context, bitmap, title)
            when (result) {
                is FileResult.Success -> {
                    val galleryImage = GalleryImage(
                        title = title.ifBlank { "আমার চিত্রকর্ম" },
                        description = desc.ifBlank { "ইন-অ্যাপ ক্যানভাসে আঁকা চিত্র" },
                        filePath = result.filePath,
                        mimeType = "image/png",
                        width = result.width,
                        height = result.height,
                        fileSizeBytes = result.sizeBytes,
                        tags = if (tags.isNotBlank()) "sketch, $tags" else "sketch, handdrawn"
                    )
                    repository.insert(galleryImage)
                    showMessage("আপনার স্কেচ গ্যালারিতে সংরক্ষণ করা হয়েছে!")
                }
                is FileResult.Error -> {
                    showMessage("স্কেচ সেভ করতে সমস্যা: ${result.message}", isSuccess = false)
                }
            }
        }
    }

    // Export format and Quality option to Public Gallery (Pictures/GalleryExport)
    fun exportImage(context: Context, image: GalleryImage, formatName: String, quality: Int) {
        viewModelScope.launch {
            val bitmap = loadBitmapForImage(context, image)
            if (bitmap == null) {
                showMessage("ছবির তথ্য পুনরুদ্ধার করা যায়নি", isSuccess = false)
                return@launch
            }

            val compressFormat = when (formatName) {
                "PNG" -> Bitmap.CompressFormat.PNG
                "WEBP" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                else -> Bitmap.CompressFormat.JPEG
            }

            val mimeType = when (formatName) {
                "PNG" -> "image/png"
                "WEBP" -> "image/webp"
                else -> "image/jpeg"
            }

            val fileExt = formatName.lowercase()
            val fileName = "Export_${image.title.replace(" ", "_")}_${System.currentTimeMillis()}.$fileExt"

            val success = withContext(Dispatchers.IO) {
                try {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GalleryExport")
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                    }

                    val insertUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (insertUri != null) {
                        resolver.openOutputStream(insertUri).use { outStream ->
                            if (outStream != null) {
                                bitmap.compress(compressFormat, quality, outStream)
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                            resolver.update(insertUri, contentValues, null, null)
                        }
                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
            }

            if (success) {
                showMessage("সফলভাবে public ছবিতে এক্সপোর্ট করা হয়েছে: Pictures/GalleryExport")
            } else {
                showMessage("ছবি এক্সপোর্ট করতে সমস্যা হয়েছে", isSuccess = false)
            }
        }
    }

    // Sharing via normal Android Share sheet
    fun shareImage(context: Context, image: GalleryImage) {
        viewModelScope.launch {
            try {
                val file = if (image.filePath.startsWith("/")) {
                    File(image.filePath)
                } else {
                    // It's a sample URL image. Let's download first, then share!
                    val bitmap = loadBitmapForImage(context, image)
                    if (bitmap == null) {
                        showMessage("শেয়ার করার জন্য ছবিটি লোড করা যায়নি", isSuccess = false)
                        return@launch
                    }
                    val tempFile = File(context.cacheDir, "share_temp.png")
                    withContext(Dispatchers.IO) {
                        tempFile.outputStream().use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                    tempFile
                }

                if (!file.exists()) {
                    showMessage("ফাইলটি খুঁজে পাওয়া যায়নি", isSuccess = false)
                    return@launch
                }

                // Get share URI using FileProvider
                val authority = "com.aistudio.gallery.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    type = image.mimeType
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooserIntent = Intent.createChooser(shareIntent, "ছবি শেয়ার করুন")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
            } catch (e: Exception) {
                showMessage("শেয়ার করতে ব্যর্থ: ${e.localizedMessage}", isSuccess = false)
            }
        }
    }

    // Helper, retrieves a bitmap from local files or downloads from network via Coil
    private suspend fun loadBitmapForImage(context: Context, image: GalleryImage): Bitmap? {
        return withContext(Dispatchers.IO) {
            if (image.filePath.startsWith("/")) {
                // Local absolute path
                BitmapFactory.decodeFile(image.filePath)
            } else {
                // Online content url
                try {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(image.filePath)
                        .allowHardware(false) // Required for software compress operations
                        .build()
                    val result = loader.execute(request)
                    if (result is SuccessResult) {
                        (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    // Pre-populate database with beautiful, curated, high-quality photograph placeholders
    private suspend fun prePopulateSamples() {
        val natureSample = GalleryImage(
            title = "Mountain Oasis / পার্বত্যাঞ্চল",
            description = "সবুজ উপত্যকা ও নীল হ্রদ পরিবৃত একটি চমৎকার প্রাকৃতিক দৃশ্য।",
            filePath = "https://images.unsplash.com/photo-1426604966848-d7adac402bff?w=800",
            mimeType = "image/jpeg",
            tags = "nature, mountain, water, travel",
            width = 1200,
            height = 800,
            fileSizeBytes = 245000
        )
        val spaceSample = GalleryImage(
            title = "Stardust Nebula / কসমস নিহারিকা",
            description = "মহাজাগতিক ধূলিকণা ও নক্ষত্রমন্ডলীতে ঘেরা আমাদের সুন্দর ছায়াপথ।",
            filePath = "https://images.unsplash.com/photo-1464802686167-b939a6910659?w=800",
            mimeType = "image/jpeg",
            tags = "space, galaxy, stars, cosmic",
            width = 1200,
            height = 800,
            fileSizeBytes = 389000
        )
        val citySample = GalleryImage(
            title = "City Neon Lights / মহানগরী",
            description = "অগণিত নিয়ন বাতিতে আলোকিত এবং জীবন্ত আধুনিক শহর।",
            filePath = "https://images.unsplash.com/photo-1519501025264-65ba15a82390?w=800",
            mimeType = "image/jpeg",
            tags = "city, architecture, skyscraper, night",
            width = 1200,
            height = 800,
            fileSizeBytes = 294000
        )
        val abstractSample = GalleryImage(
            title = "Fluid Dream / কাল্পনিক বিমূর্ত",
            description = "রং ও স্বপ্নের গভীর স্রোতে ভেসে চলা আকর্ষণীয় মডার্ন বিমূর্ত চিত্রকর্ম।",
            filePath = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?w=800",
            mimeType = "image/jpeg",
            tags = "abstract, art, colorful, fluid",
            width = 1200,
            height = 800,
            fileSizeBytes = 432000
        )

        repository.insert(natureSample)
        repository.insert(spaceSample)
        repository.insert(citySample)
        repository.insert(abstractSample)
    }
}

data class StatusMessage(
    val text: String,
    val isSuccess: Boolean = true
)

class GalleryViewModelFactory(private val repository: GalleryRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            return GalleryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
