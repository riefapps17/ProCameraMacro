package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.PointF
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.camera.FocusStackEngine
import com.example.camera.ProCameraEngine
import com.example.data.PhotoRepository
import com.example.model.Aperture
import com.example.model.AspectRatioSetting
import com.example.model.CameraSettings
import com.example.model.CaptureFormat
import com.example.model.CapturedPhoto
import com.example.model.DriveMode
import com.example.model.FlashMode
import com.example.model.FocusBracketingCount
import com.example.model.FocusDistance
import com.example.model.FocusPeakingColor
import com.example.model.FocusStepSize
import com.example.model.GridType
import com.example.model.HistogramData
import com.example.model.IsoValue
import com.example.model.JpegQuality
import com.example.model.MeteringMode
import com.example.model.ResolutionOption
import com.example.model.ShutterSpeed
import com.example.model.WhiteBalance
import com.example.sensor.OrientationSensorManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.round

enum class ActiveControlTab {
    NONE, SHUTTER, ISO, FOCUS, APERTURE_EV, WHITE_BALANCE
}

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    val cameraEngine = ProCameraEngine(application)
    val photoRepository = PhotoRepository(application)
    val sensorManager = OrientationSensorManager(application)

    private val _settings = MutableStateFlow(CameraSettings())
    val settings: StateFlow<CameraSettings> = _settings.asStateFlow()

    private val _activeTab = MutableStateFlow(ActiveControlTab.NONE)
    val activeTab: StateFlow<ActiveControlTab> = _activeTab.asStateFlow()

    private val _isTopBarExpanded = MutableStateFlow(false)
    val isTopBarExpanded: StateFlow<Boolean> = _isTopBarExpanded.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    val photos = photoRepository.photos
    val histogramData = cameraEngine.histogramState
    val simulatedPreview = cameraEngine.simulatedPreviewBitmap
    val focusPeakingBitmap = cameraEngine.focusPeakingBitmap
    val isLiveCameraActive = cameraEngine.isLiveCameraActive
    val cameraStatusMessage = cameraEngine.cameraStatusMessage
    val focusPoint = cameraEngine.focusPoint
    val macroSharpnessScore = cameraEngine.macroSharpnessScore

    val rollAngle = sensorManager.rollAngle
    val pitchAngle = sensorManager.pitchAngle

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _isBracketingRunning = MutableStateFlow(false)
    val isBracketingRunning: StateFlow<Boolean> = _isBracketingRunning.asStateFlow()

    private val _bracketingProgressText = MutableStateFlow<String?>(null)
    val bracketingProgressText: StateFlow<String?> = _bracketingProgressText.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(0)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    private val _selectedPhotoForDetail = MutableStateFlow<CapturedPhoto?>(null)
    val selectedPhotoForDetail: StateFlow<CapturedPhoto?> = _selectedPhotoForDetail.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showGalleryScreen = MutableStateFlow(false)
    val showGalleryScreen: StateFlow<Boolean> = _showGalleryScreen.asStateFlow()

    init {
        sensorManager.startListening()
        cameraEngine.updateSettings(_settings.value)
    }

    fun bindPreviewView(view: PreviewView, lifecycleOwner: LifecycleOwner) {
        cameraEngine.bindPreviewView(view, lifecycleOwner, _settings.value)
    }

    fun onScreenTapped(normX: Float, normY: Float) {
        cameraEngine.onUserTapToFocus(normX, normY, _settings.value)
    }

    fun toggleAfLock() {
        val nextState = !_settings.value.isAfLocked
        _settings.value = _settings.value.copy(isAfLocked = nextState)
        cameraEngine.updateSettings(_settings.value)
    }

    fun toggleAeLock() {
        val nextState = !_settings.value.isAeLocked
        _settings.value = _settings.value.copy(isAeLocked = nextState)
        cameraEngine.updateSettings(_settings.value)
    }

    fun setMeteringMode(mode: MeteringMode) {
        _settings.value = _settings.value.copy(meteringMode = mode)
        cameraEngine.updateSettings(_settings.value)
    }

    fun setZoom(ratio: Float) {
        val stepped = (round(ratio * 5.0f) / 5.0f).coerceIn(1.0f, 16.0f)
        _zoomRatio.value = stepped
        cameraEngine.setZoomRatio(stepped)
    }

    fun adjustZoomDelta(delta: Float) {
        val newTarget = _zoomRatio.value + delta
        setZoom(newTarget)
    }

    fun toggleTopBarExpanded() {
        _isTopBarExpanded.value = !_isTopBarExpanded.value
    }

    fun setTopBarExpanded(expanded: Boolean) {
        _isTopBarExpanded.value = expanded
    }

    fun toggleControlTab(tab: ActiveControlTab) {
        _activeTab.value = if (_activeTab.value == tab) ActiveControlTab.NONE else tab
    }

    fun setShutterSpeed(speed: ShutterSpeed) {
        _settings.value = _settings.value.copy(shutterSpeed = speed)
        cameraEngine.updateSettings(_settings.value)
    }

    fun setIso(iso: IsoValue) {
        _settings.value = _settings.value.copy(iso = iso)
        cameraEngine.updateSettings(_settings.value)
    }

    fun setFocusDistance(distance: FocusDistance) {
        _settings.value = _settings.value.copy(
            focusDistance = distance,
            isContinuousManualFocus = false
        )
        cameraEngine.updateSettings(_settings.value)
    }

    fun setContinuousManualFocus(enabled: Boolean) {
        _settings.value = _settings.value.copy(isContinuousManualFocus = enabled)
        cameraEngine.updateSettings(_settings.value)
    }

    fun setManualFocusDistanceFactor(factor: Float) {
        val clamped = factor.coerceIn(0.0f, 1.0f)
        _settings.value = _settings.value.copy(
            isContinuousManualFocus = true,
            manualFocusDistanceFactor = clamped
        )
        cameraEngine.updateSettings(_settings.value)
    }

    fun adjustManualFocusDelta(delta: Float) {
        val current = _settings.value.manualFocusDistanceFactor
        setManualFocusDistanceFactor(current + delta)
    }

    fun setDriveMode(mode: DriveMode) {
        _settings.value = _settings.value.copy(
            driveMode = mode,
            isFocusBracketingEnabled = (mode == DriveMode.FOCUS_STACK)
        )
    }

    fun cycleDriveMode() {
        val nextMode = when (_settings.value.driveMode) {
            DriveMode.SINGLE -> DriveMode.BURST_3
            DriveMode.BURST_3 -> DriveMode.BURST_5
            DriveMode.BURST_5 -> DriveMode.BURST_10
            DriveMode.BURST_10 -> DriveMode.FOCUS_STACK
            DriveMode.FOCUS_STACK -> DriveMode.SINGLE
        }
        setDriveMode(nextMode)
    }

    fun setFocusBracketingEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(
            isFocusBracketingEnabled = enabled,
            driveMode = if (enabled) DriveMode.FOCUS_STACK else DriveMode.SINGLE
        )
    }

    fun setFocusBracketingCount(count: FocusBracketingCount) {
        _settings.value = _settings.value.copy(focusBracketingCount = count)
    }

    fun setFocusStepSize(step: FocusStepSize) {
        _settings.value = _settings.value.copy(focusStepSize = step)
    }

    fun setAutoStackingEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(isAutoStackingEnabled = enabled)
    }

    fun setAperture(aperture: Aperture) {
        _settings.value = _settings.value.copy(aperture = aperture)
        cameraEngine.updateSettings(_settings.value)
    }

    fun setExposureCompensation(ev: Float) {
        _settings.value = _settings.value.copy(exposureCompensationEv = ev)
        cameraEngine.updateSettings(_settings.value)
    }

    fun setWhiteBalance(wb: WhiteBalance) {
        _settings.value = _settings.value.copy(whiteBalance = wb)
        cameraEngine.updateSettings(_settings.value)
    }

    fun setCustomKelvin(kelvin: Int) {
        _settings.value = _settings.value.copy(customKelvin = kelvin)
        cameraEngine.updateSettings(_settings.value)
    }

    fun setCaptureFormat(format: CaptureFormat) {
        _settings.value = _settings.value.copy(captureFormat = format)
        cameraEngine.updateSettings(_settings.value)
    }

    fun setAspectRatio(aspectRatio: AspectRatioSetting) {
        _settings.value = _settings.value.copy(aspectRatio = aspectRatio)
        cameraEngine.updateSettings(_settings.value)
    }

    fun setResolution(res: ResolutionOption) {
        _settings.value = _settings.value.copy(resolution = res)
        cameraEngine.updateSettings(_settings.value)
    }

    fun setJpegQuality(quality: JpegQuality) {
        _settings.value = _settings.value.copy(jpegQuality = quality)
        cameraEngine.updateSettings(_settings.value)
    }

    fun toggleMacroMode() {
        val newMacro = !_settings.value.isMacroMode
        _settings.value = _settings.value.copy(
            isMacroMode = newMacro,
            isFocusPeakingEnabled = newMacro || _settings.value.isFocusPeakingEnabled,
            isContinuousManualFocus = false,
            focusDistance = FocusDistance.AUTO
        )
        cameraEngine.updateSettings(_settings.value)
        if (newMacro) {
            cameraEngine.triggerMacroFocus(_settings.value)
        }
    }

    fun triggerAutoFocus() {
        cameraEngine.triggerMacroFocus(_settings.value)
    }

    fun toggleFocusPeaking() {
        _settings.value = _settings.value.copy(
            isFocusPeakingEnabled = !_settings.value.isFocusPeakingEnabled
        )
        cameraEngine.updateSettings(_settings.value)
    }

    fun setFocusPeakingColor(color: FocusPeakingColor) {
        _settings.value = _settings.value.copy(focusPeakingColor = color)
        cameraEngine.updateSettings(_settings.value)
    }

    fun setGridType(gridType: GridType) {
        _settings.value = _settings.value.copy(gridType = gridType)
    }

    fun toggleHistogram() {
        _settings.value = _settings.value.copy(isHistogramEnabled = !_settings.value.isHistogramEnabled)
        cameraEngine.updateSettings(_settings.value)
    }

    fun toggleLevelMeter() {
        _settings.value = _settings.value.copy(isLevelMeterEnabled = !_settings.value.isLevelMeterEnabled)
    }

    fun toggleZebraStripes() {
        _settings.value = _settings.value.copy(isZebraEnabled = !_settings.value.isZebraEnabled)
        cameraEngine.updateSettings(_settings.value)
    }

    fun toggleFlashMode() {
        val nextFlash = when (_settings.value.flashMode) {
            FlashMode.OFF -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.ON
            FlashMode.ON -> FlashMode.TORCH
            FlashMode.TORCH -> FlashMode.OFF
        }
        _settings.value = _settings.value.copy(flashMode = nextFlash)
        cameraEngine.updateSettings(_settings.value)
    }

    fun toggleCameraFacing() {
        _settings.value = _settings.value.copy(isFrontCamera = !_settings.value.isFrontCamera)
        cameraEngine.switchCameraFacing(_settings.value)
    }

    fun setShowSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun setShowGalleryScreen(show: Boolean) {
        _showGalleryScreen.value = show
    }

    fun selectPhotoDetail(photo: CapturedPhoto?) {
        _selectedPhotoForDetail.value = photo
    }

    fun deletePhoto(photoId: String) {
        photoRepository.deletePhoto(photoId)
        if (_selectedPhotoForDetail.value?.id == photoId) {
            _selectedPhotoForDetail.value = null
        }
    }

    fun capturePhoto() {
        if (_isCapturing.value || _isBracketingRunning.value) return

        viewModelScope.launch {
            val timerSec = _settings.value.timerSeconds
            if (timerSec > 0) {
                for (t in timerSec downTo 1) {
                    _countdownSeconds.value = t
                    delay(1000)
                }
                _countdownSeconds.value = 0
            }

            when (_settings.value.driveMode) {
                DriveMode.SINGLE -> {
                    executeSingleCapture()
                }
                DriveMode.BURST_3, DriveMode.BURST_5, DriveMode.BURST_10 -> {
                    executeBurstCapture(_settings.value.driveMode.shotCount)
                }
                DriveMode.FOCUS_STACK -> {
                    executeFocusBracketingSequence()
                }
            }
        }
    }

    private suspend fun executeBurstCapture(count: Int) {
        _isCapturing.value = true
        _isBracketingRunning.value = true
        try {
            var lastPhoto: CapturedPhoto? = null
            for (i in 1..count) {
                _bracketingProgressText.value = "Burst Foto $i/$count"
                val bitmap = cameraEngine.capturePhotoBitmap(_settings.value)
                lastPhoto = photoRepository.saveCapturedPhoto(
                    sourceBitmap = bitmap,
                    settings = _settings.value,
                    isStacked = false,
                    stackCount = 1
                )
                delay(120)
            }
            _selectedPhotoForDetail.value = lastPhoto
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isCapturing.value = false
            _isBracketingRunning.value = false
            _bracketingProgressText.value = null
        }
    }

    private suspend fun executeSingleCapture() {
        _isCapturing.value = true
        delay(150)

        val bitmap = cameraEngine.capturePhotoBitmap(_settings.value)
        val savedPhoto = photoRepository.saveCapturedPhoto(bitmap, _settings.value, isStacked = false, stackCount = 1)

        delay(200)
        _isCapturing.value = false

        _selectedPhotoForDetail.value = savedPhoto
    }

    private suspend fun executeFocusBracketingSequence() {
        _isCapturing.value = true
        _isBracketingRunning.value = true

        val shotCount = _settings.value.focusBracketingCount.count
        val stepSize = _settings.value.focusStepSize.stepValue
        val baseFocus = if (_settings.value.isContinuousManualFocus) {
            _settings.value.manualFocusDistanceFactor
        } else {
            0.85f // Close-up macro focus baseline
        }

        val halfRange = (shotCount - 1) / 2.0f
        val capturedSlices = mutableListOf<Bitmap>()

        try {
            for (i in 0 until shotCount) {
                // Focus moves incrementally from nearest (foreground) to deeper focal plane
                val sliceOffset = (i - halfRange) * stepSize
                val sliceFocus = (baseFocus + sliceOffset).coerceIn(0.05f, 1.0f)

                _bracketingProgressText.value = "Membidik Irisan ${i + 1}/$shotCount (Fokus ${String.format(Locale.US, "%.3f", sliceFocus)})"

                // Apply micro-focus step
                val sliceSettings = _settings.value.copy(
                    isContinuousManualFocus = true,
                    manualFocusDistanceFactor = sliceFocus
                )
                cameraEngine.updateSettings(sliceSettings)

                // Lens stabilization delay for macro vibration damping
                delay(150)

                val sliceBitmap = cameraEngine.capturePhotoBitmap(sliceSettings)
                capturedSlices.add(sliceBitmap)

                delay(80)
            }

            _bracketingProgressText.value = "Menyusun Focus Stacking (Algoritma Ketajaman)..."
            delay(120)

            val finalBitmap = if (_settings.value.isAutoStackingEnabled && capturedSlices.isNotEmpty()) {
                FocusStackEngine.stackImages(capturedSlices)
            } else {
                capturedSlices.lastOrNull() ?: cameraEngine.capturePhotoBitmap(_settings.value)
            }

            val savedPhoto = photoRepository.saveCapturedPhoto(
                sourceBitmap = finalBitmap,
                settings = _settings.value,
                isStacked = true,
                stackCount = shotCount
            )

            _selectedPhotoForDetail.value = savedPhoto
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isCapturing.value = false
            _isBracketingRunning.value = false
            _bracketingProgressText.value = null

            // Restore original settings
            cameraEngine.updateSettings(_settings.value)
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopListening()
        cameraEngine.release()
    }
}
