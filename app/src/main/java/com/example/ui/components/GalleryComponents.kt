package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.GalleryImage
import com.example.ui.viewmodel.StatusMessage
import kotlin.math.roundToInt

// Point structure for handdrawn paint strokes
data class StrokePoint(
    val points: List<Offset>,
    val color: Color,
    val width: Float
)

// List of lovely colors for painters
val PresetPaintColors = listOf(
    Color(0xFFFF2E93), // Cyber Neon Pink
    Color(0xFF8E97FD), // Indigo Purple
    Color(0xFF3DDC84), // Android Jade Green
    Color(0xFFFFC107), // Golden Yellow
    Color(0xFF00D2FF), // Neon Cyan
    Color(0xFFFFFFFF), // Pure White
    Color(0xFF000000)  // Pitch Black
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GallerySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("গ্যালারিতে ছবির নাম বা ক্যাটাগরি খুজুন...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .testTag("gallery_search_input")
    )
}

@Composable
fun CategoryTabs(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("All", "Photos", "Sketches", "Favorites")
    val bengaliLabels = mapOf(
        "All" to "সব ছবি",
        "Photos" to "সংগৃহীত ছবি",
        "Sketches" to "আমার ডিজাইন",
        "Favorites" to "পছন্দের তালিকা"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { category ->
            val isSelected = selectedCategory == category
            val label = bengaliLabels[category] ?: category
            
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    selectedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .testTag("category_chip_$category")
            )
        }
    }
}

@Composable
fun GalleryGrid(
    images: List<GalleryImage>,
    onImageClick: (GalleryImage) -> Unit,
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Empty Gallery",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "গ্যালারি খালি রয়েছে!",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ডান দিকের প্লাস আইকনে (+) চাপ দিয়ে নতুন ছবি ডাউনলোড করতে পারেন অথবা নিজের আঁকা স্কেচ গ্যালারিতে সংরক্ষণ করতে পারেন।",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(images, key = { it.id }) { image ->
                ImageCard(
                    image = image,
                    onClick = { onImageClick(image) }
                )
            }
        }
    }
}

@Composable
fun ImageCard(
    image: GalleryImage,
    onClick: () -> Unit
) {
    val isLocal = image.filePath.startsWith("/")
    val imageTypeLabel = when {
        image.filePath.contains("sketch_") -> "Sketch"
        isLocal -> "Local"
        else -> "Sample"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .testTag("gallery_item_card_${image.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                AsyncImage(
                    model = image.filePath,
                    contentDescription = image.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Tags / Badges layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (imageTypeLabel == "Sketch") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = imageTypeLabel,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (image.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(12.dp)
            ) {
                Text(
                    text = image.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (image.description.isBlank()) "কোন বিবরণ নেই" else image.description,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// In-App Painting Canvas drawing Screen
@Composable
fun CanvasDrawingScreen(
    onDismiss: () -> Unit,
    onSave: (Bitmap, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var strokesState = remember { mutableStateListOf<StrokePoint>() }
    var currentPoints = remember { mutableStateListOf<Offset>() }
    var strokeColor by remember { mutableStateOf(PresetPaintColors[0]) }
    var strokeWidth by remember { mutableStateOf(12f) }

    // Metadata form values
    var showFormDialog by remember { mutableStateOf(false) }
    var sketchTitle by remember { mutableStateOf("") }
    var sketchDesc by remember { mutableStateOf("") }
    var sketchTags by remember { mutableStateOf("") }

    // Canvas size tracking
    var canvasSize by remember { mutableStateOf(android.util.Size(100, 100)) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("drawing_back_button")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }

                Text(
                    text = "ক্যানভাস চিত্রকর্ম",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = {
                        if (strokesState.isNotEmpty()) {
                            showFormDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    enabled = strokesState.isNotEmpty(),
                    modifier = Modifier.testTag("drawing_save_trigger")
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("সংরক্ষণ", fontSize = 12.sp)
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                // Formatting Controls: Palette Selection + Width Slider
                Text(
                    text = "তুলির সাইজ ও রং নির্বাচন:",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PresetPaintColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (strokeColor == color) 3.dp else 1.dp,
                                    color = if (strokeColor == color) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { strokeColor = color }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.Brush, contentDescription = "Brush Size", tint = MaterialTheme.colorScheme.primary)
                    Slider(
                        value = strokeWidth,
                        onValueChange = { strokeWidth = it },
                        valueRange = 4f..40f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = "${strokeWidth.roundToInt()} px", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Clear & Undo operations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        onClick = {
                            if (strokesState.isNotEmpty()) {
                                strokesState.removeLast()
                            }
                        },
                        enabled = strokesState.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("পূর্ববর্তী ভুল মুছুন")
                    }

                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        onClick = { strokesState.clear() },
                        enabled = strokesState.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ক্যানভাস ক্লিয়ার")
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFEBEBEB)) // Elegant light cardboard back for painting focus
                .pointerInput(strokeColor, strokeWidth) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPoints.add(offset)
                        },
                        onDrag = { change, _ ->
                            currentPoints.add(change.position)
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                strokesState.add(
                                    StrokePoint(
                                        points = currentPoints.toList(),
                                        color = strokeColor,
                                        width = strokeWidth
                                    )
                                )
                                currentPoints.clear()
                            }
                        }
                    )
                }
        ) {
            ComposeCanvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // Gather canvas layout structure dimensions dynamically
                        this.size.let {
                            canvasSize = android.util.Size(it.width, it.height)
                        }
                    }
            ) {
                // Paint existing complete strokes
                strokesState.forEach { stroke ->
                    if (stroke.points.size > 1) {
                        for (i in 0 until stroke.points.size - 1) {
                            drawLine(
                                color = stroke.color,
                                start = stroke.points[i],
                                end = stroke.points[i + 1],
                                strokeWidth = stroke.width,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Paint currently active ongoing stroke
                if (currentPoints.size > 1) {
                    for (i in 0 until currentPoints.size - 1) {
                        drawLine(
                            color = strokeColor,
                            start = currentPoints[i],
                            end = currentPoints[i + 1],
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Quick instruction overlay when empty
            if (strokesState.isEmpty() && currentPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Gesture,
                            contentDescription = "Draw Guide",
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "আপনার আঙুল দিয়ে ক্যানভাসে ছবি আঁকুন",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Save Form input dialog
    if (showFormDialog) {
        AlertDialog(
            onDismissRequest = { showFormDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showFormDialog = false
                        // Generate Android SDK standard Bitmap
                        val w = canvasSize.width
                        val h = canvasSize.height
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val sdkCanvas = Canvas(bmp)
                        sdkCanvas.drawColor(android.graphics.Color.WHITE) // Always write drawings to a solid clean white back
                        
                        val paint = Paint().apply {
                            isAntiAlias = true
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            style = Paint.Style.STROKE
                        }

                        strokesState.forEach { stroke ->
                            paint.color = stroke.color.toArgb()
                            paint.strokeWidth = stroke.width
                            val path = Path()
                            if (stroke.points.isNotEmpty()) {
                                path.moveTo(stroke.points[0].x, stroke.points[0].y)
                                for (i in 1 until stroke.points.size) {
                                    path.lineTo(stroke.points[i].x, stroke.points[i].y)
                                }
                                sdkCanvas.drawPath(path, paint)
                            }
                        }

                        onSave(bmp, sketchTitle, sketchDesc, sketchTags)
                    },
                    modifier = Modifier.testTag("submit_drawing_form")
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFormDialog = false }) {
                    Text("বাতিল")
                }
            },
            title = { Text("চিত্রকর্ম সংরক্ষণ", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("আপনার আঁকা ছবিটির জন্য বিবরণ যোগ করুন:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    
                    OutlinedTextField(
                        value = sketchTitle,
                        onValueChange = { sketchTitle = it },
                        label = { Text("ছবির শিরোনাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sketch_title_input")
                    )

                    OutlinedTextField(
                        value = sketchDesc,
                        onValueChange = { sketchDesc = it },
                        label = { Text("ছবির ছোট বিবরণ") },
                        modifier = Modifier.fillMaxWidth().testTag("sketch_desc_input")
                    )

                    OutlinedTextField(
                        value = sketchTags,
                        onValueChange = { sketchTags = it },
                        label = { Text("ট্যাগসমূহ (কমা দিয়ে যেমন: sketch, flower)") },
                        singleLine = true,
                        placeholder = { Text("যেমন: sketch, art") },
                        modifier = Modifier.fillMaxWidth().testTag("sketch_tags_input")
                    )
                }
            }
        )
    }
}

// Media Detail screen + multi formats Exporter and metadata modification Tool
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailsDialog(
    image: GalleryImage,
    onDismiss: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
    onSaveDetails: (String, String, String) -> Unit,
    onShare: () -> Unit,
    onExport: (String, Int) -> Unit, // Format (JPEG, PNG, WEBP), Quality (0-100)
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(image.title) }
    var editDesc by remember { mutableStateOf(image.description) }
    var editTags by remember { mutableStateOf(image.tags) }

    // Export section controls
    var showExportOptions by remember { mutableStateOf(false) }
    var chosenFormat by remember { mutableStateOf("JPEG") }
    var exportQuality by remember { mutableStateOf(90f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false // Enables full width beautiful display sheet
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dialog Navigation Top AppBar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("detail_close_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    Text(
                        text = "ছবির বিস্তির্ণ বিবরণ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onFavoriteToggle, modifier = Modifier.testTag("detail_fav_button")) {
                            Icon(
                                imageVector = if (image.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite Toggle",
                                tint = if (image.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = onShare, modifier = Modifier.testTag("detail_share_button")) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        IconButton(onClick = onDelete, modifier = Modifier.testTag("detail_delete_button")) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Full resolution Immersive Display Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                    ) {
                        AsyncImage(
                            model = image.filePath,
                            contentDescription = image.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Metadata attributes labels card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ফরম্যাট", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text(
                                    text = image.mimeType.split("/").last().uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Divider(modifier = Modifier.width(1.dp).height(24.dp).align(Alignment.CenterVertically))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("আকার", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                val dimText = if (image.width > 0 && image.height > 0) "${image.width}x${image.height}" else "Unknown"
                                Text(
                                    text = dimText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Divider(modifier = Modifier.width(1.dp).height(24.dp).align(Alignment.CenterVertically))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("সাইজ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                val kb = image.fileSizeBytes / 1024L
                                val sizeText = if (kb > 1024L) String.format("%.2f MB", kb / 1024f) else "$kb KB"
                                Text(
                                    text = sizeText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Details block & Form
                    if (!isEditing) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = image.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(onClick = { isEditing = true }, modifier = Modifier.testTag("detail_edit_trigger")) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Details", tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = if (image.description.isBlank()) "কোন বিবরণ নেই" else image.description,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Tag list layout
                            if (image.tags.isNotBlank()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    image.tags.split(",").forEach { tag ->
                                        if (tag.trim().isNotEmpty()) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text("#${tag.trim()}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Editable Input fields
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                label = { Text("ছবির শিরোনাম") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("edit_title_input")
                            )

                            OutlinedTextField(
                                value = editDesc,
                                onValueChange = { editDesc = it },
                                label = { Text("ছবির বিবরণ বা ক্যাপশন") },
                                modifier = Modifier.fillMaxWidth().testTag("edit_desc_input")
                            )

                            OutlinedTextField(
                                value = editTags,
                                onValueChange = { editTags = it },
                                label = { Text("ট্যাগসমূহ (যেমন: nature, sketch)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("edit_tags_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isEditing = false
                                        onSaveDetails(editTitle, editDesc, editTags)
                                    },
                                    modifier = Modifier.weight(1f).testTag("edit_save_button")
                                ) {
                                    Text("সংরক্ষণ করুন")
                                }

                                OutlinedButton(
                                    onClick = {
                                        isEditing = false
                                        editTitle = image.title
                                        editDesc = image.description
                                        editTags = image.tags
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("বাতিল")
                                }
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // Export Trigger & Exporter options Panel
                    if (!showExportOptions) {
                        Button(
                            onClick = { showExportOptions = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("export_panel_trigger")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Export Icon")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("গ্যালারি বা এসডি কার্ডে এক্সপোর্ট করুন", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("পছন্দের এক্সপোর্ট ফরম্যাট নির্বাচন করুন:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    IconButton(onClick = { showExportOptions = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Export Options", modifier = Modifier.size(16.dp))
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val formats = listOf("JPEG", "PNG", "WEBP")
                                    formats.forEach { form ->
                                        val isChosen = chosenFormat == form
                                        Button(
                                            onClick = { chosenFormat = form },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                contentColor = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("export_format_$form")
                                        ) {
                                            Text(form, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (chosenFormat != "PNG") {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("ছবির কোয়ালিটি (Quality):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                            Text("${exportQuality.roundToInt()}%", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Slider(
                                            value = exportQuality,
                                            onValueChange = { exportQuality = it },
                                            valueRange = 30f..100f
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        showExportOptions = false
                                        onExport(chosenFormat, exportQuality.roundToInt())
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("export_execute_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Confirm Export", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("পাবলিক Pictures ফোল্ডারে এক্সপোর্ট করুন", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom Snack Message Indicator
@Composable
fun StatusOverlayMessage(
    message: StatusMessage,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto clear after 4 seconds
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(4000)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isSuccess) Color(0xFF1E3A1E) else Color(0xFF4A1E1E),
                contentColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(6.dp),
            modifier = Modifier
                .widthIn(max = 500.dp)
                .border(
                    width = 1.dp,
                    color = if (message.isSuccess) Color(0xFF326E32) else Color(0xFF8C3232),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (message.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = "Status Icon",
                    tint = if (message.isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onFinished,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Status",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
