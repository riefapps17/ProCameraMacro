package com.example.model

data class CapturedPhoto(
    val id: String,
    val title: String,
    val filePath: String,
    val rawFilePath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val width: Int = 4000,
    val height: Int = 3000,
    val fileSizeMb: Double = 6.2,
    val rawSizeMb: Double? = 24.5,
    val captureFormat: CaptureFormat = CaptureFormat.RAW_JPEG,
    val iso: String = "100",
    val shutterSpeed: String = "1/250s",
    val aperture: String = "f/1.8",
    val focalLength: String = "26mm",
    val whiteBalance: String = "5500K",
    val jpegQualityPercent: Int = 95,
    val aspectRatioLabel: String = "4:3",
    val isMacro: Boolean = false,
    val isStacked: Boolean = false,
    val stackedShotCount: Int = 1
)

data class HistogramData(
    val redBins: IntArray = IntArray(256),
    val greenBins: IntArray = IntArray(256),
    val blueBins: IntArray = IntArray(256),
    val luminanceBins: IntArray = IntArray(256),
    val maxBinValue: Int = 1
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistogramData

        if (!redBins.contentEquals(other.redBins)) return false
        if (!greenBins.contentEquals(other.greenBins)) return false
        if (!blueBins.contentEquals(other.blueBins)) return false
        if (!luminanceBins.contentEquals(other.luminanceBins)) return false
        if (maxBinValue != other.maxBinValue) return false

        return true
    }

    override fun hashCode(): Int {
        var result = redBins.contentHashCode()
        result = 31 * result + greenBins.contentHashCode()
        result = 31 * result + blueBins.contentHashCode()
        result = 31 * result + luminanceBins.contentHashCode()
        result = 31 * result + maxBinValue
        return result
    }
}
