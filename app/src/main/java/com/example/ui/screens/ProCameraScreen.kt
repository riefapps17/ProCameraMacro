package com.example.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.model.AspectRatioSetting
import com.example.model.DriveMode
import com.example.model.MeteringMode
import com.example.ui.components.AfAeLockButtons
import com.example.ui.components.FocusPeakingOverlayView
import com.example.ui.components.GridOverlayView
import com.example.ui.components.HistogramView
import com.example.ui.components.LevelMeterView
import com.example.ui.components.ProDialsView
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TapFocusTargetRing
import com.example.ui.components.TopBarControls
import com.example.ui.components.ZoomControlBar
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentLight
import com.example.ui.theme.RawBadgeColor
import com.example.viewmodel.CameraViewModel

@Composable
fun ProCameraScreen(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val settings by viewModel.settings.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val isTopBarExpanded by viewModel.isTopBarExpanded.collectAsState()
    val zoomRatio by viewModel.zoomRatio.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val histogramData by viewModel.histogramData.collectAsState()
    val simulatedPreview by viewModel.simulatedPreview.collectAsState()
    val focusPeakingBitmap by viewModel.focusPeakingBitmap.collectAsState()
    val isLiveCameraActive by viewModel.isLiveCameraActive.collectAsState()
    val cameraStatusMessage by viewModel.cameraStatusMessage.collectAsState()
    val focusPoint by viewModel.focusPoint.collectAsState()
    val macroSharpnessScore by viewModel.macroSharpnessScore.collectAsState()

    val rollAngle by viewModel.rollAngle.collectAsState()
    val pitchAngle by viewModel.pitchAngle.collectAsState()

    val isCapturing by viewModel.isCapturing.collectAsState()
    val isBracketingRunning by viewModel.isBracketingRunning.collectAsState()
    val bracketingProgressText by viewModel.bracketingProgressText.collectAsState()
    val countdownSeconds by viewModel.countdownSeconds.collectAsState()

    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
    val showGalleryScreen by viewModel.showGalleryScreen.collectAsState()

    if (showGalleryScreen) {
        GalleryScreen(
            photos = photos,
            onBack = { viewModel.setShowGalleryScreen(false) },
            onDeletePhoto = { id -> viewModel.deletePhoto(id) }
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("pro_camera_screen")
    ) {
        val portraitViewfinderRatio = when (settings.aspectRatio) {
            AspectRatioSetting.RATIO_4_3 -> 3f / 4f
            AspectRatioSetting.RATIO_16_9 -> 9f / 16f
            AspectRatioSetting.RATIO_1_1 -> 1f
            AspectRatioSetting.RATIO_3_2 -> 2f / 3f
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 50.dp, bottom = 150.dp),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .aspectRatio(portraitViewfinderRatio)
                    .fillMaxSize()
                    .clipToBounds()
                    .background(Color.Black)
                    // Tap to Focus and Meter
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                val normX = (offset.x / size.width).coerceIn(0f, 1f)
                                val normY = (offset.y / size.height).coerceIn(0f, 1f)
                                viewModel.onScreenTapped(normX, normY)
                            },
                            onLongPress = {
                                // Long press toggles both AF/AE lock
                                viewModel.toggleAfLock()
                                viewModel.toggleAeLock()
                            }
                        )
                    }
                    // Pinch to Zoom
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom != 1.0f) {
                                val delta = (zoom - 1.0f) * 3f
                                viewModel.adjustZoomDelta(delta)
                            }
                        }
                    }
                    .testTag("viewfinder_box")
            ) {
                val boxWidthPx = constraints.maxWidth.toFloat()
                val boxHeightPx = constraints.maxHeight.toFloat()

                // Real Hardware Camera Surface via PreviewView with FIT_CENTER
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FIT_CENTER
                            viewModel.bindPreviewView(this, lifecycleOwner)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Simulated Preview fallback
                if (!isLiveCameraActive) {
                    simulatedPreview?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Simulasi Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Tap focus ring overlay
                focusPoint?.let { pt ->
                    val ringX = (pt.x * boxWidthPx) - 32.dp.value
                    val ringY = (pt.y * boxHeightPx) - 32.dp.value
                    TapFocusTargetRing(
                        point = pt,
                        isAfLocked = settings.isAfLocked,
                        isAeLocked = settings.isAeLocked,
                        modifier = Modifier.offset { IntOffset(ringX.toInt(), ringY.toInt()) }
                    )
                }

                // Focus Peaking & Zebra Stripes Highlights Overlay
                if (settings.isFocusPeakingEnabled || settings.isMacroMode || settings.isZebraEnabled) {
                    FocusPeakingOverlayView(peakingBitmap = focusPeakingBitmap)
                }

                // Horizon Level Meter Overlay
                if (settings.isLevelMeterEnabled) {
                    LevelMeterView(rollAngle = rollAngle, pitchAngle = pitchAngle)
                }

                // Composition Grid Overlay
                GridOverlayView(gridType = settings.gridType)

                // Camera hardware focus & metering status message badge
                cameraStatusMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xD9161820))
                            .border(1.dp, GoldAccent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = msg,
                            color = if (msg.contains("Terkunci") || msg.contains("Tajam")) GoldAccentLight else Color.Yellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Shutter Flash Animation Effect
                if (isCapturing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    )
                }

                // Countdown Timer Overlay
                if (countdownSeconds > 0) {
                    Text(
                        text = "$countdownSeconds",
                        color = GoldAccent,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Focus Bracketing Active Progress Overlay
                if (isBracketingRunning) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF161820))
                                .border(1.5.dp, GoldAccent, RoundedCornerShape(16.dp))
                                .padding(horizontal = 24.dp, vertical = 20.dp)
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = GoldAccent,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Focus Bracketing Berjalan",
                                color = GoldAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = bracketingProgressText ?: "Membidik irisan fokus...",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = "Tahan kamera tetap stabil",
                                color = Color(0x88FFFFFF),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Top Bar Hidden Drawer Controls (Ditarik kebawah untuk menampilkan icon)
        TopBarControls(
            settings = settings,
            isExpanded = isTopBarExpanded,
            onToggleExpanded = { viewModel.toggleTopBarExpanded() },
            onFormatClick = {
                val nextFmt = when (settings.captureFormat) {
                    com.example.model.CaptureFormat.RAW_JPEG -> com.example.model.CaptureFormat.RAW_ONLY
                    com.example.model.CaptureFormat.RAW_ONLY -> com.example.model.CaptureFormat.JPEG_ONLY
                    com.example.model.CaptureFormat.JPEG_ONLY -> com.example.model.CaptureFormat.RAW_JPEG
                }
                viewModel.setCaptureFormat(nextFmt)
            },
            onRatioClick = {
                val nextRatio = when (settings.aspectRatio) {
                    AspectRatioSetting.RATIO_4_3 -> AspectRatioSetting.RATIO_16_9
                    AspectRatioSetting.RATIO_16_9 -> AspectRatioSetting.RATIO_1_1
                    AspectRatioSetting.RATIO_1_1 -> AspectRatioSetting.RATIO_3_2
                    AspectRatioSetting.RATIO_3_2 -> AspectRatioSetting.RATIO_4_3
                }
                viewModel.setAspectRatio(nextRatio)
            },
            onMacroClick = { viewModel.toggleMacroMode() },
            onFocusPeakingClick = { viewModel.toggleFocusPeaking() },
            onMeteringClick = {
                val nextMode = when (settings.meteringMode) {
                    MeteringMode.MATRIX -> MeteringMode.CENTER_WEIGHTED
                    MeteringMode.CENTER_WEIGHTED -> MeteringMode.SPOT
                    MeteringMode.SPOT -> MeteringMode.MATRIX
                }
                viewModel.setMeteringMode(nextMode)
            },
            onGridClick = {
                val nextGrid = when (settings.gridType) {
                    com.example.model.GridType.NONE -> com.example.model.GridType.RULE_OF_THIRDS
                    com.example.model.GridType.RULE_OF_THIRDS -> com.example.model.GridType.GOLDEN_RATIO
                    com.example.model.GridType.GOLDEN_RATIO -> com.example.model.GridType.SQUARE
                    com.example.model.GridType.SQUARE -> com.example.model.GridType.NONE
                }
                viewModel.setGridType(nextGrid)
            },
            onFlashClick = { viewModel.toggleFlashMode() },
            onSettingsClick = { viewModel.setShowSettingsDialog(true) },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Separate AF / AE Lock Floating Bar
        AfAeLockButtons(
            isAfLocked = settings.isAfLocked,
            isAeLocked = settings.isAeLocked,
            onToggleAfLock = { viewModel.toggleAfLock() },
            onToggleAeLock = { viewModel.toggleAeLock() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = if (isTopBarExpanded) 110.dp else 55.dp, end = 8.dp)
        )

        // Real-Time RGB & Luminance Histogram Overlay (HANYA MUNCUL JIKA DIAKTIFKAN DI SETTING)
        if (settings.isHistogramEnabled) {
            HistogramView(
                data = histogramData,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = if (isTopBarExpanded) 110.dp else 60.dp, start = 12.dp)
            )
        }

        // Live Mode Banner (Macro Active / Focus Peaking / Focus Bracketing Indicator)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (isTopBarExpanded) 110.dp else 60.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (settings.isMacroMode) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xDDFFB800))
                        .clickable { viewModel.triggerAutoFocus() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "MAKRO AF • Ketajaman $macroSharpnessScore%",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (settings.isFocusBracketingEnabled) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xEEFFB800))
                        .border(1.dp, Color.White, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "FOCUS STACK (${settings.focusBracketingCount.count}x)",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Always-active Vertical Zoom Bar (portrait top-to-bottom layout, no auto-hide)
        ZoomControlBar(
            zoomRatio = zoomRatio,
            onZoomChange = { viewModel.setZoom(it) },
            onZoomDelta = { viewModel.adjustZoomDelta(it) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        )

        // Bottom Controls Container (Pro Dials + Shutter Trigger Row)
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ProDialsView(
                settings = settings,
                activeTab = activeTab,
                onTabSelected = { viewModel.toggleControlTab(it) },
                onShutterSelected = { viewModel.setShutterSpeed(it) },
                onIsoSelected = { viewModel.setIso(it) },
                onFocusSelected = { viewModel.setFocusDistance(it) },
                onManualFocusChanged = { viewModel.setManualFocusDistanceFactor(it) },
                onManualFocusDelta = { viewModel.adjustManualFocusDelta(it) },
                onToggleContinuousFocus = { viewModel.setContinuousManualFocus(it) },
                onToggleFocusBracketing = { viewModel.setFocusBracketingEnabled(it) },
                onFocusBracketingCountSelected = { viewModel.setFocusBracketingCount(it) },
                onFocusStepSizeSelected = { viewModel.setFocusStepSize(it) },
                onApertureSelected = { viewModel.setAperture(it) },
                onEvChanged = { viewModel.setExposureCompensation(it) },
                onWbSelected = { viewModel.setWhiteBalance(it) },
                onKelvinChanged = { viewModel.setCustomKelvin(it) }
            )

            // Symmetric Shutter Trigger & Gallery Bar (Shutter strictly centered)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0E12))
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                val latestPhoto = photos.firstOrNull()
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF222530))
                        .border(1.5.dp, GoldAccent, RoundedCornerShape(10.dp))
                        .clickable { viewModel.setShowGalleryScreen(true) }
                        .testTag("gallery_thumbnail_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (latestPhoto != null) {
                        AsyncImage(
                            model = latestPhoto.filePath,
                            contentDescription = "Galeri Foto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Buka Galeri",
                            tint = GoldAccent
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, GoldAccent, CircleShape)
                        .clickable { viewModel.capturePhoto() }
                        .testTag("shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(if (isCapturing || isBracketingRunning) GoldAccent else Color(0xFF161820))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBracketingRunning) "BURST"
                            else when (settings.driveMode) {
                                DriveMode.SINGLE -> if (settings.captureFormat.isRaw) "RAW" else "JPG"
                                DriveMode.BURST_3 -> "3x\nBURST"
                                DriveMode.BURST_5 -> "5x\nBURST"
                                DriveMode.BURST_10 -> "10x\nBURST"
                                DriveMode.FOCUS_STACK -> "STACK\n5x"
                            },
                            color = if (isCapturing || isBracketingRunning) Color.Black else RawBadgeColor,
                            fontSize = if (settings.driveMode != DriveMode.SINGLE && !isBracketingRunning) 9.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.toggleCameraFacing() },
                    modifier = Modifier
                        .size(52.dp)
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF222530))
                        .testTag("switch_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Ganti Kamera Depan Belakang",
                        tint = Color.White
                    )
                }
            }
        }

        // Settings Dialog
        if (showSettingsDialog) {
            SettingsDialog(
                settings = settings,
                onDismiss = { viewModel.setShowSettingsDialog(false) },
                onDriveModeSelected = { viewModel.setDriveMode(it) },
                onJpegQualitySelected = { viewModel.setJpegQuality(it) },
                onResolutionSelected = { viewModel.setResolution(it) },
                onAspectRatioSelected = { viewModel.setAspectRatio(it) },
                onMeteringModeSelected = { viewModel.setMeteringMode(it) },
                onFocusPeakingColorSelected = { viewModel.setFocusPeakingColor(it) },
                onHistogramToggle = { viewModel.toggleHistogram() },
                onLevelMeterToggle = { viewModel.toggleLevelMeter() },
                onZebraToggle = { viewModel.toggleZebraStripes() }
            )
        }
    }
}
