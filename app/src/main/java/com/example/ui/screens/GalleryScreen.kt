package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RawOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CapturedPhoto
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentLight
import com.example.ui.theme.RawBadgeColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    photos: List<CapturedPhoto>,
    onBack: () -> Unit,
    onDeletePhoto: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPhoto by remember { mutableStateOf<CapturedPhoto?>(null) }
    var photoToDelete by remember { mutableStateOf<CapturedPhoto?>(null) }
    var showExifSheet by remember { mutableStateOf(false) }
    var showRawEditor by remember { mutableStateOf(false) }

    // Editor Sliders State
    var exposureEdit by remember { mutableFloatStateOf(0f) }
    var contrastEdit by remember { mutableFloatStateOf(0f) }
    var highlightsEdit by remember { mutableFloatStateOf(0f) }
    var shadowsEdit by remember { mutableFloatStateOf(0f) }
    var tempEdit by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Galeri Foto RAW Pro (${photos.size})",
                        color = GoldAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("gallery_back_button")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali ke Kamera",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0E12))
            )
        },
        containerColor = Color(0xFF0D0E12),
        modifier = modifier.testTag("gallery_screen")
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (photos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada foto yang diambil.\nAmbil foto profesional dengan mode RAW!",
                        color = Color(0x88FFFFFF),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(photos, key = { it.id }) { photo ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { selectedPhoto = photo }
                                .testTag("photo_item_${photo.id}")
                        ) {
                            AsyncImage(
                                model = photo.filePath,
                                contentDescription = photo.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (photo.isStacked) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(GoldAccent)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "STACK ${photo.stackedShotCount}x",
                                            color = Color.Black,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (photo.captureFormat.isRaw) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(RawBadgeColor)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "RAW",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            // Quick delete icon button on grid item
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xBB000000))
                                    .clickable { photoToDelete = photo }
                                    .testTag("grid_delete_button_${photo.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Foto",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Fullscreen Inspection Modal View with Pinch-To-Zoom & Pan
            selectedPhoto?.let { photo ->
                var scale by remember(photo.id) { mutableFloatStateOf(1f) }
                var offsetX by remember(photo.id) { mutableFloatStateOf(0f) }
                var offsetY by remember(photo.id) { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    // Zoomable and Pannable Image Container with Pinch & Double-Tap
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .pointerInput(photo.id) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1.2f) {
                                            scale = 1.0f
                                            offsetX = 0f
                                            offsetY = 0f
                                        } else {
                                            scale = 3.0f
                                        }
                                    }
                                )
                            }
                            .pointerInput(photo.id) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1.0f, 10.0f)
                                    if (newScale == 1.0f) {
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                                        val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                                        offsetX = (offsetX + pan.x * scale).coerceIn(-maxOffsetX, maxOffsetX)
                                        offsetY = (offsetY + pan.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
                                    }
                                    scale = newScale
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = photo.filePath,
                            contentDescription = photo.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offsetX
                                    translationY = offsetY
                                },
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Floating Zoom Indicator Pill
                    if (scale > 1.05f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xDD161820))
                                .border(1.dp, GoldAccent, RoundedCornerShape(16.dp))
                                .clickable {
                                    scale = 1.0f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", scale)}x Zoom (Ketuk 2x untuk reset)",
                                color = GoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Top Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xAA000000))
                            .padding(12.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedPhoto = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Tutup", tint = Color.White)
                        }

                        Text(
                            text = photo.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Row {
                            IconButton(onClick = { showRawEditor = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit RAW", tint = GoldAccent)
                            }
                            IconButton(onClick = { showExifSheet = true }) {
                                Icon(Icons.Default.Info, contentDescription = "EXIF Data", tint = GoldAccent)
                            }
                            IconButton(
                                onClick = {
                                    photoToDelete = photo
                                },
                                modifier = Modifier.testTag("detail_delete_button")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus Foto", tint = Color.Red)
                            }
                        }
                    }

                    // Bottom EXIF Quick Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xCC0D0E12))
                            .padding(12.dp)
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ExifBadge(label = "ISO", value = photo.iso)
                        ExifBadge(label = "SHUTTER", value = photo.shutterSpeed)
                        ExifBadge(label = "APERTURE", value = photo.aperture)
                        ExifBadge(label = "FOCAL", value = photo.focalLength)
                        ExifBadge(label = "SIZE", value = "${photo.fileSizeMb} MB")
                    }
                }

                // EXIF Sheet
                if (showExifSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showExifSheet = false },
                        containerColor = Color(0xFF161820)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text("Data Informasi EXIF Foto", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                            Spacer(modifier = Modifier.height(16.dp))

                            ExifDetailRow("Nama File", photo.title)
                            ExifDetailRow("Format File", photo.captureFormat.label)
                            ExifDetailRow("Resolusi Dimensi", "${photo.width} x ${photo.height} Pixels")
                            ExifDetailRow("Ukuran File JPEG", "${photo.fileSizeMb} MB (${photo.jpegQualityPercent}% Quality)")
                            photo.rawSizeMb?.let { ExifDetailRow("Ukuran File DNG RAW", "$it MB (14-Bit Sensor Data)") }
                            ExifDetailRow("Kecepatan Rana (Shutter)", photo.shutterSpeed)
                            ExifDetailRow("Sensitivitas ISO", photo.iso)
                            ExifDetailRow("Bukaan Lensa (Aperture)", photo.aperture)
                            ExifDetailRow("Panjang Fokus", photo.focalLength)
                            ExifDetailRow("Keseimbangan Putih (WB)", photo.whiteBalance)
                            ExifDetailRow("Waktu Pengambilan", SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(photo.timestamp)))
                            ExifDetailRow("Mode Makro", if (photo.isMacro) "Aktif (Jarak Dekat)" else "Standard")
                            if (photo.isStacked) {
                                ExifDetailRow("Focus Stacking", "${photo.stackedShotCount} Irisan Fokus (Depth Blended All-in-Focus)")
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(
                                onClick = { showExifSheet = false },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("TUTUP", color = GoldAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // RAW Editor Sheet
                if (showRawEditor) {
                    ModalBottomSheet(
                        onDismissRequest = { showRawEditor = false },
                        containerColor = Color(0xFF161820)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text("Pengembang & Editor Foto RAW (14-Bit DNG)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                            Text("Proses data sensor mentah tanpa kehilangan detail", fontSize = 11.sp, color = Color(0x88FFFFFF))

                            Spacer(modifier = Modifier.height(16.dp))

                            EditorSliderRow("Eksposur (EV)", exposureEdit) { exposureEdit = it }
                            EditorSliderRow("Kontras", contrastEdit) { contrastEdit = it }
                            EditorSliderRow("Highlight (Bayangan Terang)", highlightsEdit) { highlightsEdit = it }
                            EditorSliderRow("Shadows (Area Gelap)", shadowsEdit) { shadowsEdit = it }
                            EditorSliderRow("Temperatur Warna (K)", tempEdit) { tempEdit = it }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    exposureEdit = 0f
                                    contrastEdit = 0f
                                    highlightsEdit = 0f
                                    shadowsEdit = 0f
                                    tempEdit = 0f
                                }) {
                                    Text("RESET", color = Color.White)
                                }
                                TextButton(onClick = { showRawEditor = false }) {
                                    Text("SIMPAN RAW EDIT", color = GoldAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Delete Confirmation Dialog
            photoToDelete?.let { targetPhoto ->
                AlertDialog(
                    onDismissRequest = { photoToDelete = null },
                    containerColor = Color(0xFF161822),
                    titleContentColor = GoldAccent,
                    textContentColor = Color.White,
                    title = {
                        Text(
                            text = "Konfirmasi Hapus Foto",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Apakah Anda yakin ingin menghapus file foto ini secara permanen?",
                                fontSize = 14.sp,
                                color = Color(0xEEFFFFFF)
                            )
                            Text(
                                text = targetPhoto.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = GoldAccentLight
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onDeletePhoto(targetPhoto.id)
                                if (selectedPhoto?.id == targetPhoto.id) {
                                    selectedPhoto = null
                                }
                                photoToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            modifier = Modifier.testTag("confirm_delete_button")
                        ) {
                            Text("Hapus Permanen", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { photoToDelete = null },
                            modifier = Modifier.testTag("cancel_delete_button")
                        ) {
                            Text("Batal", color = Color(0xCCFFFFFF), fontWeight = FontWeight.Medium)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ExifBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0x88FFFFFF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = GoldAccentLight, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ExifDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xAAFFFFFF), fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun EditorSliderRow(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 12.sp)
            Text(String.format(Locale.US, "%+.1f", value), color = GoldAccent, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -3.0f..3.0f,
            colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent)
        )
    }
}
