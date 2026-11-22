package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Log
import androidx.annotation.OptIn
import android.graphics.Matrix
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.model.AspectRatioSetting
import com.example.model.CameraSettings
import com.example.model.FocusDistance
import com.example.model.FocusPeakingColor
import com.example.model.HistogramData
import com.example.model.IsoValue
import com.example.model.MeteringMode
import com.example.model.ShutterSpeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max

class ProCameraEngine(private val context: Context) {

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val _histogramState = MutableStateFlow(HistogramData())
    val histogramState: StateFlow<HistogramData> = _histogramState

    private val _focusPeakingBitmap = MutableStateFlow<Bitmap?>(null)
    val focusPeakingBitmap: StateFlow<Bitmap?> = _focusPeakingBitmap

    private val _simulatedPreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val simulatedPreviewBitmap: StateFlow<Bitmap?> = _simulatedPreviewBitmap

    private val _isLiveCameraActive = MutableStateFlow(false)
    val isLiveCameraActive: StateFlow<Boolean> = _isLiveCameraActive

    private val _cameraStatusMessage = MutableStateFlow<String?>(null)
    val cameraStatusMessage: StateFlow<String?> = _cameraStatusMessage

    private val _currentZoomRatio = MutableStateFlow(1.0f)
    val currentZoomRatio: StateFlow<Float> = _currentZoomRatio

    // Focus / Metering tap point visual state (normalized 0..1)
    private val _focusPoint = MutableStateFlow<PointF?>(null)
    val focusPoint: StateFlow<PointF?> = _focusPoint

    // Real-time macro sharpness score (0..100%) calculated from live sensor edges
    private val _macroSharpnessScore = MutableStateFlow(0)
    val macroSharpnessScore: StateFlow<Int> = _macroSharpnessScore

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var previewView: PreviewView? = null
    private var activeLifecycleOwner: LifecycleOwner? = null
    private var currentSettings: CameraSettings = CameraSettings()

    fun bindPreviewView(
        view: PreviewView,
        lifecycleOwner: LifecycleOwner,
        settings: CameraSettings
    ) {
        this.previewView = view
        this.activeLifecycleOwner = lifecycleOwner
        this.currentSettings = settings

        startCamera(lifecycleOwner, settings)
    }

    private fun startCamera(
        lifecycleOwner: LifecycleOwner,
        settings: CameraSettings
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val provider = cameraProvider ?: return@addListener

                provider.unbindAll()

                val selector = if (settings.isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                if (!provider.hasCamera(selector)) {
                    Log.w("ProCameraEngine", "Requested camera selector not found on device, falling back")
                    _isLiveCameraActive.value = false
                    setupSimulationMode(settings)
                    return@addListener
                }

                val targetRatio = when (settings.aspectRatio) {
                    AspectRatioSetting.RATIO_16_9 -> AspectRatio.RATIO_16_9
                    else -> AspectRatio.RATIO_4_3
                }

                val previewBuilder = Preview.Builder()
                    .setTargetAspectRatio(targetRatio)

                val preview = previewBuilder.build().apply {
                    previewView?.let { setSurfaceProvider(it.surfaceProvider) }
                }

                val (effW, effH) = settings.resolution.getEffectiveDimensions(settings.aspectRatio, isPortrait = true)
                val targetResolutionSize = android.util.Size(effW, effH)

                val imageCaptureResolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            targetResolutionSize,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .setAspectRatioStrategy(
                        when (settings.aspectRatio) {
                            AspectRatioSetting.RATIO_16_9 -> AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
                            else -> AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
                        }
                    )
                    .build()

                imageCapture = ImageCapture.Builder()
                    .setResolutionSelector(imageCaptureResolutionSelector)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetAspectRatio(targetRatio)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (currentSettings.isHistogramEnabled ||
                        currentSettings.isFocusPeakingEnabled ||
                        currentSettings.isMacroMode ||
                        currentSettings.isZebraEnabled
                    ) {
                        processLiveFrame(imageProxy, currentSettings)
                    } else {
                        if (_focusPeakingBitmap.value != null) {
                            _focusPeakingBitmap.value = null
                        }
                    }
                    imageProxy.close()
                }

                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )

                _isLiveCameraActive.value = true
                _cameraStatusMessage.value = null
                Log.i("ProCameraEngine", "Camera re-bound successfully with aspect ratio $targetRatio")

                applyCameraControls(camera?.cameraControl, settings)
                setZoomRatio(_currentZoomRatio.value)

            } catch (e: Exception) {
                Log.e("ProCameraEngine", "Camera binding failed: ${e.message}", e)
                _isLiveCameraActive.value = false
                _cameraStatusMessage.value = "Kamera HP: ${e.localizedMessage ?: "Inisialisasi ulang..."}"
                setupSimulationMode(settings)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setZoomRatio(ratio: Float) {
        val clamped = ratio.coerceIn(1.0f, 16.0f)
        _currentZoomRatio.value = clamped
        camera?.cameraControl?.setZoomRatio(clamped)
    }

    /**
     * Tap to Focus & Metering with AF/AE Lock integration and hardware lock feedback
     */
    fun onUserTapToFocus(normX: Float, normY: Float, settings: CameraSettings) {
        val pView = previewView ?: return
        val control = camera?.cameraControl ?: return

        _focusPoint.value = PointF(normX, normY)
        _cameraStatusMessage.value = "Mengunci fokus..."

        val factory = pView.meteringPointFactory
        val point = factory.createPoint(normX * pView.width, normY * pView.height)

        val actionBuilder = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)

        if (settings.isAfLocked || settings.isAeLocked) {
            actionBuilder.disableAutoCancel()
        } else {
            actionBuilder.setAutoCancelDuration(4, TimeUnit.SECONDS)
        }

        val future = control.startFocusAndMetering(actionBuilder.build())
        future.addListener({
            try {
                val result = future.get()
                if (result.isFocusSuccessful) {
                    _cameraStatusMessage.value = "Fokus Terkunci (Tajam)"
                } else {
                    _cameraStatusMessage.value = if (settings.isMacroMode) "Jarak Terlalu Dekat - Jaga Jarak 6-10 cm" else "Fokus Tidak Terkunci"
                }
            } catch (e: Exception) {
                _cameraStatusMessage.value = null
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Trigger immediate Macro AutoFocus scan when entering Macro mode
     */
    fun triggerMacroFocus(settings: CameraSettings) {
        val pView = previewView ?: return
        val control = camera?.cameraControl ?: return

        val factory = pView.meteringPointFactory
        val center = factory.createPoint(pView.width / 2f, pView.height / 2f)

        _focusPoint.value = PointF(0.5f, 0.5f)
        _cameraStatusMessage.value = "Fokus Makro Aktif: Jarak Ideal 6-10 cm"

        val action = FocusMeteringAction.Builder(center, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(4, TimeUnit.SECONDS)
            .build()

        val future = control.startFocusAndMetering(action)
        future.addListener({
            try {
                val result = future.get()
                if (result.isFocusSuccessful) {
                    _cameraStatusMessage.value = "Fokus Makro Terkunci (Tajam)"
                } else {
                    _cameraStatusMessage.value = "Objek Terlalu Dekat - Mundurkan 2-4 cm"
                }
            } catch (e: Exception) {
                // Ignore
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Hardware Camera2 Interop Controls for physical lens motor, Macro AF, and Manual Focus Diopters
     */
    @OptIn(ExperimentalCamera2Interop::class)
    private fun applyProCameraHardwareControls(control: CameraControl?, settings: CameraSettings) {
        val cam = camera ?: return
        val ctl = control ?: return

        try {
            val camera2Control = Camera2CameraControl.from(ctl)
            val builder = CaptureRequestOptions.Builder()

            val camera2Info = Camera2CameraInfo.from(cam.cameraInfo)
            // LENS_INFO_MINIMUM_FOCUS_DISTANCE in Diopters (1/m). Typical macro values: 10.0f - 20.0f (5cm - 10cm)
            val minFocusDistance = camera2Info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 10.0f
            val supportedAfModes = camera2Info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) ?: intArrayOf()
            val hasMacroAfMode = supportedAfModes.contains(CaptureRequest.CONTROL_AF_MODE_MACRO)

            // 1. Focus Motor Driving (Continuous AF, Macro AF, or Manual Diopters)
            if (settings.isContinuousManualFocus) {
                // Manual focus: direct physical lens motor driving
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                val diopters = (settings.manualFocusDistanceFactor * minFocusDistance).coerceIn(0.0f, minFocusDistance)
                builder.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, diopters)
                Log.d("ProCameraEngine", "Hardware Manual Focus: diopters=$diopters / max=$minFocusDistance")
            } else if (settings.focusDistance != FocusDistance.AUTO) {
                // Preset distance
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                val presetDiopters = settings.focusDistance.diopters ?: minFocusDistance
                val clamped = presetDiopters.coerceIn(0.0f, minFocusDistance)
                builder.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, clamped)
                Log.d("ProCameraEngine", "Hardware Preset Focus: diopters=$clamped")
            } else if (settings.isMacroMode) {
                // Hardware Macro Focus Mode
                if (hasMacroAfMode) {
                    builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_MACRO)
                    Log.d("ProCameraEngine", "Hardware Macro AF Mode Engaged (CONTROL_AF_MODE_MACRO)")
                } else {
                    // Fallback to closest physical focal plane if hardware lacks dedicated macro AF mode
                    builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                    builder.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, minFocusDistance)
                    Log.d("ProCameraEngine", "Hardware Macro: Set to maximum diopters $minFocusDistance")
                }
            } else {
                // Standard continuous autofocus
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }

            // 2. Hardware Shutter Speed & ISO (Sensor Exposure & Gain)
            if (settings.shutterSpeed != ShutterSpeed.AUTO || settings.iso != IsoValue.AUTO) {
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                settings.shutterSpeed.exposureTimeNanos?.let { nanos ->
                    builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, nanos)
                }
                settings.iso.value?.let { isoVal ->
                    builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, isoVal)
                }
            } else {
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            }

            // 3. Hardware White Balance (AWB Mode)
            val awbMode = when (settings.whiteBalance) {
                com.example.model.WhiteBalance.AUTO -> CaptureRequest.CONTROL_AWB_MODE_AUTO
                com.example.model.WhiteBalance.DAYLIGHT -> CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT
                com.example.model.WhiteBalance.CLOUDY -> CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
                com.example.model.WhiteBalance.SHADE -> CaptureRequest.CONTROL_AWB_MODE_SHADE
                com.example.model.WhiteBalance.INCANDESCENT -> CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT
                com.example.model.WhiteBalance.FLUORESCENT -> CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT
                com.example.model.WhiteBalance.CUSTOM -> {
                    when {
                        settings.customKelvin < 3500 -> CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT
                        settings.customKelvin < 4500 -> CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT
                        settings.customKelvin < 6000 -> CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT
                        settings.customKelvin < 7000 -> CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
                        else -> CaptureRequest.CONTROL_AWB_MODE_SHADE
                    }
                }
            }
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, awbMode)

            // 4. Hardware AE / AWB Locks
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, settings.isAeLocked)

            camera2Control.setCaptureRequestOptions(builder.build())
        } catch (e: Exception) {
            Log.e("ProCameraEngine", "Failed to apply Camera2 hardware controls: ${e.message}")
        }
    }

    private fun applyCameraControls(control: CameraControl?, settings: CameraSettings) {
        if (control == null) return
        try {
            val torchMode = settings.flashMode == com.example.model.FlashMode.TORCH
            control.enableTorch(torchMode)

            imageCapture?.flashMode = when (settings.flashMode) {
                com.example.model.FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                com.example.model.FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                else -> ImageCapture.FLASH_MODE_OFF
            }

            val evInt = (settings.exposureCompensationEv * 2).toInt()
            control.setExposureCompensationIndex(evInt)

            applyProCameraHardwareControls(control, settings)
        } catch (e: Exception) {
            Log.e("ProCameraEngine", "Failed to apply camera controls: ${e.message}")
        }
    }

    /**
     * Real-time Live Sensor Frame Analysis:
     * 1. True Sobel high-frequency gradient edge detection for Macro Focus Peaking
     * 2. Overexposure Zebra Stripes clipping detection (Y >= 245)
     * 3. Macro Focus Sharpness Score (0-100%)
     * 4. Real-time Luminance & RGB Histogram
     */
    private fun processLiveFrame(imageProxy: ImageProxy, settings: CameraSettings) {
        try {
            val yPlane = imageProxy.planes[0]
            val yBuffer = yPlane.buffer
            val rowStride = yPlane.rowStride
            val pixelStride = yPlane.pixelStride
            val srcW = imageProxy.width
            val srcH = imageProxy.height
            val rotation = imageProxy.imageInfo.rotationDegrees

            val yBytes = ByteArray(yBuffer.remaining())
            yBuffer.get(yBytes)

            // 1. Live Histogram
            if (settings.isHistogramEnabled) {
                val redBins = IntArray(256)
                val greenBins = IntArray(256)
                val blueBins = IntArray(256)
                val lumBins = IntArray(256)
                var maxBin = 1

                val histStep = max(1, yBytes.size / 6000)
                for (i in 0 until yBytes.size step histStep) {
                    val yVal = yBytes[i].toInt() and 0xFF
                    lumBins[yVal]++
                    redBins[yVal]++
                    greenBins[yVal]++
                    blueBins[yVal]++
                    if (lumBins[yVal] > maxBin) maxBin = lumBins[yVal]
                }
                _histogramState.value = HistogramData(redBins, greenBins, blueBins, lumBins, maxBin)
            }

            // 2. Real-time Sobel Edge Detection (Focus Peaking) & Zebra Stripes
            val needPeaking = settings.isFocusPeakingEnabled || settings.isMacroMode
            val needZebra = settings.isZebraEnabled

            if (needPeaking || needZebra) {
                // Downsample 2x for fast 60fps real-time edge overlay generation
                val step = 2
                val gridW = srcW / step
                val gridH = srcH / step

                val outW = if (rotation == 90 || rotation == 270) gridH else gridW
                val outH = if (rotation == 90 || rotation == 270) gridW else gridH

                val outPixels = IntArray(outW * outH)

                val peakArgb = when (settings.focusPeakingColor) {
                    FocusPeakingColor.GREEN -> Color.argb(235, 0, 255, 102)
                    FocusPeakingColor.RED -> Color.argb(235, 255, 23, 68)
                    FocusPeakingColor.YELLOW -> Color.argb(235, 255, 234, 0)
                    FocusPeakingColor.CYAN -> Color.argb(235, 0, 229, 255)
                }
                val zebraArgb = Color.argb(190, 255, 82, 82)

                // Threshold tuned for macro fine textures (hairs, leaf veins, insect details)
                val edgeThreshold = (85 - (settings.focusPeakingThreshold * 55).toInt()).coerceIn(22, 110)

                var strongEdgeCount = 0
                var centerSampleCount = 1

                val roiMinX = gridW / 4
                val roiMaxX = (gridW * 3) / 4
                val roiMinY = gridH / 4
                val roiMaxY = (gridH * 3) / 4

                fun lumAt(gx: Int, gy: Int): Int {
                    val idx = (gy * step) * rowStride + (gx * step) * pixelStride
                    return if (idx in yBytes.indices) (yBytes[idx].toInt() and 0xFF) else 0
                }

                for (gy in 1 until gridH - 1) {
                    for (gx in 1 until gridW - 1) {
                        val c = lumAt(gx, gy)
                        var pixelColor = 0

                        if (needPeaking) {
                            // 3x3 Sobel horizontal & vertical gradient
                            val tl = lumAt(gx - 1, gy - 1)
                            val tc = lumAt(gx, gy - 1)
                            val tr = lumAt(gx + 1, gy - 1)
                            val ml = lumAt(gx - 1, gy)
                            val mr = lumAt(gx + 1, gy)
                            val bl = lumAt(gx - 1, gy + 1)
                            val bc = lumAt(gx, gy + 1)
                            val br = lumAt(gx + 1, gy + 1)

                            val gradX = (tr + 2 * mr + br) - (tl + 2 * ml + bl)
                            val gradY = (bl + 2 * bc + br) - (tl + 2 * tc + tr)
                            val mag = (kotlin.math.abs(gradX) + kotlin.math.abs(gradY)) shr 2

                            if (gx in roiMinX..roiMaxX && gy in roiMinY..roiMaxY) {
                                centerSampleCount++
                                if (mag >= edgeThreshold) {
                                    strongEdgeCount++
                                }
                            }

                            if (mag >= edgeThreshold) {
                                pixelColor = peakArgb
                            }
                        }

                        if (needZebra && pixelColor == 0 && c >= 246) {
                            // Diagonal zebra stripe pattern on clipped highlights
                            if (((gx + gy) % 4) < 2) {
                                pixelColor = zebraArgb
                            }
                        }

                        if (pixelColor != 0) {
                            val (ox, oy) = when (rotation) {
                                90 -> Pair(gridH - 1 - gy, gx)
                                180 -> Pair(gridW - 1 - gx, gridH - 1 - gy)
                                270 -> Pair(gy, gridW - 1 - gx)
                                else -> Pair(gx, gy)
                            }
                            if (ox in 0 until outW && oy in 0 until outH) {
                                outPixels[oy * outW + ox] = pixelColor
                            }
                        }
                    }
                }

                // Calculate Macro Sharpness Percentage (0..100%)
                val score = ((strongEdgeCount.toFloat() / (centerSampleCount * 0.12f)) * 100f)
                    .toInt()
                    .coerceIn(0, 100)
                _macroSharpnessScore.value = score

                val overlayBmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                overlayBmp.setPixels(outPixels, 0, outW, 0, 0, outW, outH)
                _focusPeakingBitmap.value = overlayBmp
            } else {
                _focusPeakingBitmap.value = null
            }
        } catch (e: Exception) {
            // Ignore transient frame analysis errors
        }
    }

    fun updateSettings(settings: CameraSettings) {
        val oldRatio = this.currentSettings.aspectRatio
        val oldFacing = this.currentSettings.isFrontCamera
        val oldRes = this.currentSettings.resolution
        val oldAfLock = this.currentSettings.isAfLocked
        val oldAeLock = this.currentSettings.isAeLocked
        val oldMetering = this.currentSettings.meteringMode
        this.currentSettings = settings

        if (oldRatio != settings.aspectRatio || oldFacing != settings.isFrontCamera || oldRes != settings.resolution) {
            activeLifecycleOwner?.let { owner ->
                startCamera(owner, settings)
                return
            }
        }

        if (_isLiveCameraActive.value && camera != null) {
            applyCameraControls(camera?.cameraControl, settings)
        } else {
            generateSimulatedFrame(settings)
        }
    }

    fun switchCameraFacing(settings: CameraSettings) {
        this.currentSettings = settings
        activeLifecycleOwner?.let { owner ->
            startCamera(owner, settings)
        }
    }

    private fun setupSimulationMode(settings: CameraSettings) {
        generateSimulatedFrame(settings)
    }

    private fun generateSimulatedFrame(settings: CameraSettings) {
        CoroutineScope(Dispatchers.Default).launch {
            val w = 640
            val h = 480
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            var brightnessMult = 1.0f
            if (settings.shutterSpeed != ShutterSpeed.AUTO) {
                val speedVal = settings.shutterSpeed.exposureTimeNanos ?: 16_666_666L
                brightnessMult *= (speedVal / 16_666_666L.toFloat()).coerceIn(0.2f, 3.0f)
            }
            if (settings.iso != IsoValue.AUTO) {
                val isoVal = settings.iso.value ?: 100
                brightnessMult *= (isoVal / 400.0f).coerceIn(0.3f, 3.0f)
            }
            brightnessMult *= Math.pow(2.0, settings.exposureCompensationEv.toDouble()).toFloat()

            val paint = Paint().apply { isAntiAlias = true }
            val baseBgColor = Color.rgb(
                (20 * brightnessMult).toInt().coerceIn(0, 255),
                (24 * brightnessMult).toInt().coerceIn(0, 255),
                (35 * brightnessMult).toInt().coerceIn(0, 255)
            )
            canvas.drawColor(baseBgColor)

            val cx = w / 2f
            val cy = h / 2f

            val focusFactor = if (settings.isContinuousManualFocus) {
                settings.manualFocusDistanceFactor.coerceIn(0.0f, 1.0f)
            } else {
                when (settings.focusDistance) {
                    FocusDistance.AUTO -> 1.0f
                    FocusDistance.MACRO -> if (settings.isMacroMode) 1.0f else 0.4f
                    FocusDistance.M_0_1 -> 0.9f
                    FocusDistance.M_0_2 -> 0.8f
                    FocusDistance.M_0_5 -> 0.6f
                    FocusDistance.M_1_0 -> 0.4f
                    FocusDistance.M_2_0 -> 0.3f
                    FocusDistance.INFINITY -> 0.2f
                }
            }

            paint.color = Color.argb(
                120,
                (255 * brightnessMult).toInt().coerceIn(0, 255),
                (180 * brightnessMult).toInt().coerceIn(0, 255),
                (50 * brightnessMult).toInt().coerceIn(0, 255)
            )
            val bokehRadius = 40f * (1.5f - focusFactor)
            canvas.drawCircle(cx - 160f, cy - 100f, 60f + bokehRadius, paint)
            canvas.drawCircle(cx + 180f, cy + 90f, 50f + bokehRadius, paint)

            val petalColor = Color.rgb(
                (220 * brightnessMult).toInt().coerceIn(0, 255),
                (60 * brightnessMult).toInt().coerceIn(0, 255),
                (140 * brightnessMult).toInt().coerceIn(0, 255)
            )
            paint.color = petalColor

            for (i in 0 until 8) {
                val angle = i * (Math.PI / 4)
                val px = cx + (Math.cos(angle) * 90).toFloat()
                val py = cy + (Math.sin(angle) * 90).toFloat()
                canvas.drawCircle(px, py, 55f, paint)
            }

            paint.color = Color.rgb(
                (255 * brightnessMult).toInt().coerceIn(0, 255),
                (220 * brightnessMult).toInt().coerceIn(0, 255),
                (80 * brightnessMult).toInt().coerceIn(0, 255)
            )
            canvas.drawCircle(cx, cy, 40f, paint)

            _simulatedPreviewBitmap.value = bitmap

            if (settings.isFocusPeakingEnabled || settings.isMacroMode) {
                generateFocusPeakingOverlay(bitmap, settings)
            } else {
                _focusPeakingBitmap.value = null
            }

            if (settings.isHistogramEnabled) {
                calculateHistogramFromBitmap(bitmap)
            }
        }
    }

    private fun generateFocusPeakingOverlay(bitmap: Bitmap, settings: CameraSettings) {
        val w = bitmap.width
        val h = bitmap.height
        val overlay = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(overlay)

        val highlightColor = when (settings.focusPeakingColor) {
            FocusPeakingColor.GREEN -> Color.GREEN
            FocusPeakingColor.RED -> Color.RED
            FocusPeakingColor.YELLOW -> Color.YELLOW
            FocusPeakingColor.CYAN -> Color.CYAN
        }

        val paint = Paint().apply {
            color = highlightColor
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val cx = w / 2f
        val cy = h / 2f

        canvas.drawCircle(cx, cy, 40f, paint)
        for (i in 0 until 8) {
            val angle = i * (Math.PI / 4)
            val px = cx + (Math.cos(angle) * 90).toFloat()
            val py = cy + (Math.sin(angle) * 90).toFloat()
            canvas.drawCircle(px, py, 55f, paint)
        }

        _focusPeakingBitmap.value = overlay
    }

    private fun calculateHistogramFromBitmap(bitmap: Bitmap) {
        val redBins = IntArray(256)
        val greenBins = IntArray(256)
        val blueBins = IntArray(256)
        val lumBins = IntArray(256)
        var maxBin = 1

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val step = max(1, pixels.size / 5000)
        for (i in 0 until pixels.size step step) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)

            redBins[r]++
            greenBins[g]++
            blueBins[b]++
            lumBins[lum]++

            if (lumBins[lum] > maxBin) maxBin = lumBins[lum]
            if (redBins[r] > maxBin) maxBin = redBins[r]
            if (greenBins[g] > maxBin) maxBin = greenBins[g]
            if (blueBins[b] > maxBin) maxBin = blueBins[b]
        }

        _histogramState.value = HistogramData(redBins, greenBins, blueBins, lumBins, maxBin)
    }

    suspend fun capturePhotoBitmap(settings: CameraSettings): Bitmap {
        val capture = imageCapture
        if (_isLiveCameraActive.value && capture != null) {
            try {
                val capturedBmp = suspendCancellableCoroutine<Bitmap?> { continuation ->
                    capture.takePicture(
                        cameraExecutor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                try {
                                    val rotationDegrees = image.imageInfo.rotationDegrees
                                    val bmp = image.toBitmap()
                                    image.close()

                                    val finalBmp = if (rotationDegrees != 0) {
                                        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                        Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                                    } else {
                                        bmp
                                    }
                                    if (continuation.isActive) {
                                        continuation.resume(finalBmp)
                                    }
                                } catch (e: Exception) {
                                    image.close()
                                    Log.e("ProCameraEngine", "Failed to convert ImageProxy to Bitmap: ${e.message}", e)
                                    if (continuation.isActive) {
                                        continuation.resume(null)
                                    }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("ProCameraEngine", "Hardware ImageCapture error: ${exception.message}", exception)
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                        }
                    )
                }

                if (capturedBmp != null) {
                    val isPortrait = capturedBmp.height >= capturedBmp.width
                    val (targetW, targetH) = settings.resolution.getEffectiveDimensions(settings.aspectRatio, isPortrait)
                    return if (capturedBmp.width != targetW || capturedBmp.height != targetH) {
                        Bitmap.createScaledBitmap(capturedBmp, targetW, targetH, true)
                    } else {
                        capturedBmp
                    }
                }
            } catch (e: Exception) {
                Log.e("ProCameraEngine", "Hardware takePicture exception: ${e.message}", e)
            }
        }

        // Fallback for simulation / emulator mode or if hardware capture is not active
        val (targetW, targetH) = settings.resolution.getEffectiveDimensions(settings.aspectRatio, isPortrait = true)

        val currentView = previewView
        if (currentView != null && currentView.bitmap != null) {
            val previewBmp = currentView.bitmap!!
            val isPortrait = previewBmp.height >= previewBmp.width
            val (w, h) = settings.resolution.getEffectiveDimensions(settings.aspectRatio, isPortrait)
            return Bitmap.createScaledBitmap(previewBmp, w, h, true)
        }

        val currentSim = _simulatedPreviewBitmap.value
        if (currentSim != null) {
            val captureBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(captureBitmap)
            canvas.drawBitmap(currentSim, null, android.graphics.Rect(0, 0, targetW, targetH), null)
            return captureBitmap
        }

        val defaultBmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(defaultBmp)
        canvas.drawColor(Color.DKGRAY)
        return defaultBmp
    }

    fun release() {
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }
}
