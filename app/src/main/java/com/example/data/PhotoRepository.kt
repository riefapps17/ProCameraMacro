package com.example.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.CameraSettings
import com.example.model.CaptureFormat
import com.example.model.CapturedPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PhotoRepository(private val context: Context) {

    private val _photos = MutableStateFlow<List<CapturedPhoto>>(emptyList())
    val photos: StateFlow<List<CapturedPhoto>> = _photos

    init {
        loadSamplePhotos()
    }

    private fun loadSamplePhotos() {
        val sampleMacro = CapturedPhoto(
            id = "sample_macro_1",
            title = "IMG_MACRO_001.RAW+JPG",
            filePath = "android.resource://" + context.packageName + "/drawable/sample_macro_flower_1793939945086",
            rawFilePath = "sample_macro_001.dng",
            timestamp = System.currentTimeMillis() - 3600_000 * 2,
            width = 4000,
            height = 3000,
            fileSizeMb = 5.8,
            rawSizeMb = 23.4,
            captureFormat = CaptureFormat.RAW_JPEG,
            iso = "100",
            shutterSpeed = "1/500s",
            aperture = "f/1.8",
            focalLength = "Macro 26mm",
            whiteBalance = "5500K Daylight",
            jpegQualityPercent = 95,
            aspectRatioLabel = "4:3",
            isMacro = true,
            isStacked = true,
            stackedShotCount = 5
        )

        val sampleNight = CapturedPhoto(
            id = "sample_night_1",
            title = "IMG_NIGHT_002.RAW+JPG",
            filePath = "android.resource://" + context.packageName + "/drawable/sample_night_city_1793939960010",
            rawFilePath = "sample_night_002.dng",
            timestamp = System.currentTimeMillis() - 3600_000 * 24,
            width = 3840,
            height = 2160,
            fileSizeMb = 8.2,
            rawSizeMb = 31.0,
            captureFormat = CaptureFormat.RAW_JPEG,
            iso = "50",
            shutterSpeed = "8s",
            aperture = "f/4.0",
            focalLength = "24mm",
            whiteBalance = "4000K Fluorescent",
            jpegQualityPercent = 100,
            aspectRatioLabel = "16:9",
            isMacro = false,
            isStacked = false,
            stackedShotCount = 1
        )

        _photos.value = listOf(sampleMacro, sampleNight)
    }

    suspend fun saveCapturedPhoto(
        sourceBitmap: Bitmap,
        settings: CameraSettings,
        isStacked: Boolean = false,
        stackCount: Int = 1
    ): CapturedPhoto = withContext(Dispatchers.IO) {
        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val prefix = if (isStacked) "STACK_" else "PRO_"
        val fileName = "$prefix$timestampStr.jpg"
        val dngFileName = "$prefix$timestampStr.dng"

        // 1. Resolve storage directory - prefer Public DCIM/ProCamera so photos show in system phone Gallery
        val publicDcim = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "ProCamera")
        if (!publicDcim.exists()) {
            publicDcim.mkdirs()
        }
        val outputDir = if (publicDcim.exists() && publicDcim.canWrite()) {
            publicDcim
        } else {
            val appPictures = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
            val proCamSub = File(appPictures, "ProCamera")
            if (!proCamSub.exists()) proCamSub.mkdirs()
            proCamSub
        }

        // Ensure saved image matches chosen resolution (48MP, 12MP, etc.) while strictly preserving portrait/landscape proportions
        val isPortrait = sourceBitmap.height >= sourceBitmap.width
        val (targetW, targetH) = settings.resolution.getEffectiveDimensions(settings.aspectRatio, isPortrait)
        val finalBitmap = if (sourceBitmap.width != targetW || sourceBitmap.height != targetH) {
            Bitmap.createScaledBitmap(sourceBitmap, targetW, targetH, true)
        } else {
            sourceBitmap
        }

        val jpegQuality = settings.jpegQuality.qualityPercent
        val jpegFile = File(outputDir, fileName)
        val fos = FileOutputStream(jpegFile)

        finalBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, fos)
        fos.flush()
        fos.close()

        val fileSizeMb = jpegFile.length() / (1024.0 * 1024.0)

        var rawFilePath: String? = null
        var rawSizeMb: Double? = null
        if (settings.captureFormat.isRaw) {
            val rawFile = File(outputDir, dngFileName)
            createSimulatedDngHeader(rawFile, finalBitmap, settings)
            rawFilePath = rawFile.absolutePath
            rawSizeMb = rawFile.length() / (1024.0 * 1024.0)
        }

        // 2. Register directly with Android MediaStore on Android 10+ (Q+) for native Gallery app indexing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/ProCamera")
                    put(MediaStore.Images.Media.WIDTH, finalBitmap.width)
                    put(MediaStore.Images.Media.HEIGHT, finalBitmap.height)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outStream ->
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, outStream)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Trigger MediaScanner broadcast so phone's Gallery (Google Photos, Tecno AI Gallery) immediately displays the new photo
        val scanPaths = listOfNotNull(jpegFile.absolutePath, rawFilePath).toTypedArray()
        val mimeTypes = listOfNotNull("image/jpeg", if (rawFilePath != null) "image/x-adobe-dng" else null).toTypedArray()
        MediaScannerConnection.scanFile(context, scanPaths, mimeTypes, null)

        val suffix = if (isStacked) ".STACK.RAW+JPG" else if (settings.captureFormat.isRaw) ".RAW+JPG" else ".JPG"
        val photo = CapturedPhoto(
            id = UUID.randomUUID().toString(),
            title = "$prefix$timestampStr$suffix",
            filePath = jpegFile.absolutePath,
            rawFilePath = rawFilePath,
            timestamp = System.currentTimeMillis(),
            width = finalBitmap.width,
            height = finalBitmap.height,
            fileSizeMb = String.format(Locale.US, "%.2f", fileSizeMb).toDouble(),
            rawSizeMb = rawSizeMb?.let { String.format(Locale.US, "%.1f", it).toDouble() },
            captureFormat = settings.captureFormat,
            iso = settings.iso.label,
            shutterSpeed = settings.shutterSpeed.label,
            aperture = settings.aperture.label,
            focalLength = if (settings.isMacroMode) "Macro 26mm" else "26mm Prime",
            whiteBalance = settings.whiteBalance.label,
            jpegQualityPercent = jpegQuality,
            aspectRatioLabel = settings.aspectRatio.label,
            isMacro = settings.isMacroMode,
            isStacked = isStacked,
            stackedShotCount = stackCount
        )

        _photos.value = listOf(photo) + _photos.value
        photo
    }

    private fun createSimulatedDngHeader(file: File, bitmap: Bitmap, settings: CameraSettings) {
        val fos = FileOutputStream(file)
        val dngMagicHeader = byteArrayOf(0x49, 0x49, 0x2A, 0x00, 0x08, 0x00, 0x00, 0x00)
        fos.write(dngMagicHeader)

        val metaString = "DNG RAW v1.4 | ISO ${settings.iso.label} | Shutter ${settings.shutterSpeed.label} | Aperture ${settings.aperture.label} | Quality ${settings.jpegQuality.qualityPercent}%\n"
        fos.write(metaString.toByteArray(Charsets.UTF_8))

        val bayerDataSize = 1024 * 1024 * 12
        val buffer = ByteArray(4096)
        var remaining = bayerDataSize
        while (remaining > 0) {
            val toWrite = minOf(remaining, buffer.size)
            fos.write(buffer, 0, toWrite)
            remaining -= toWrite
        }
        fos.flush()
        fos.close()
    }

    fun deletePhoto(photoId: String) {
        val current = _photos.value.toMutableList()
        val item = current.find { it.id == photoId }
        if (item != null) {
            if (!item.filePath.startsWith("android.resource")) {
                try {
                    File(item.filePath).delete()
                    item.rawFilePath?.let { File(it).delete() }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            current.remove(item)
            _photos.value = current
        }
    }
}
