package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.GalleryDatabase
import com.example.data.model.GalleryImage
import com.example.data.repository.GalleryRepository
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GalleryViewModel
import com.example.ui.viewmodel.GalleryViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup Room Database Repository and ViewModel
        val database = GalleryDatabase.getDatabase(applicationContext)
        val repository = GalleryRepository(database.galleryImageDao())
        
        val viewModel: GalleryViewModel by viewModels {
            GalleryViewModelFactory(repository)
        }

        setContent {
            MyApplicationTheme {
                GalleryAppScreen(viewModel)
            }
        }
    }
}

@Composable
fun GalleryAppScreen(viewModel: GalleryViewModel) {
    val context = LocalContext.current
    
    // ViewModel states
    val searchByText by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeTab by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val filteredImagesList by viewModel.filteredImages.collectAsStateWithLifecycle()
    val overlayAlertMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    // Screen navigation triggers
    var selectedImageForDetail by remember { mutableStateOf<GalleryImage?>(null) }
    var showDrawingCanvas by remember { mutableStateOf(false) }

    // Unified Android System PickVisualMedia Launcher (no storage permissions required)
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.importImageFromUri(context, uri)
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!showDrawingCanvas) {
            // Main Gallery Screen
            Scaffold(
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "ছবি গ্যালারি",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "সংরক্ষণ করুন এবং সহজে শেয়ার করুন",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Search Control
                        GallerySearchBar(
                            query = searchByText,
                            onQueryChange = { viewModel.setSearchQuery(it) }
                        )

                        // Categories filtering
                        CategoryTabs(
                            selectedCategory = activeTab,
                            onCategorySelected = { viewModel.setSelectedCategory(it) }
                        )
                    }
                },
                floatingActionButton = {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Action 1: Import photo
                        FloatingActionButton(
                            onClick = {
                                pickImageLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("fab_import_photo")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.PhotoAlbum, contentDescription = "Import Photo")
                                Text("ছবি ইম্পোর্ট", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Action 2: Open in-app painter
                        FloatingActionButton(
                            onClick = { showDrawingCanvas = true },
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("fab_draw_canvas")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Brush, contentDescription = "Draw Freehand")
                                Text("ছবি আঁকুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
                ) {
                    GalleryGrid(
                        images = filteredImagesList,
                        onImageClick = { image ->
                            selectedImageForDetail = image
                        }
                    )
                }
            }
        } else {
            // Immersive Fullscreen Finger Sketch board Canvas view
            CanvasDrawingScreen(
                onDismiss = { showDrawingCanvas = false },
                onSave = { bitmap, title, desc, tags ->
                    viewModel.saveSketch(context, bitmap, title, desc, tags)
                    showDrawingCanvas = false
                }
            )
        }

        // Expanded metadata detailed editor & sharing/export panels overlay
        selectedImageForDetail?.let { image ->
            ImageDetailsDialog(
                image = image,
                onDismiss = { selectedImageForDetail = null },
                onFavoriteToggle = { viewModel.toggleFavorite(image) },
                onDelete = {
                    viewModel.deleteImage(image)
                    selectedImageForDetail = null
                },
                onSaveDetails = { t, d, tg ->
                    viewModel.updateImageDetails(image.id, t, d, tg)
                    // Retrieve updated state
                    selectedImageForDetail = image.copy(title = t, description = d, tags = tg)
                },
                onShare = { viewModel.shareImage(context, image) },
                onExport = { format, q ->
                    viewModel.exportImage(context, image, format, q)
                }
            )
        }

        // Custom animated status updates / snack messages (always shown over drawings or grids)
        overlayAlertMessage?.let { status ->
            StatusOverlayMessage(
                message = status,
                onFinished = { viewModel.clearStatusMessage() }
            )
        }
    }
}
