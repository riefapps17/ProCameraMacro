package com.example.model

enum class ShutterSpeed(val label: String, val exposureTimeNanos: Long?) {
    AUTO("AUTO", null),
    S_1_8000("1/8000s", 125_000L),
    S_1_4000("1/4000s", 250_000L),
    S_1_2000("1/2000s", 500_000L),
    S_1_1000("1/1000s", 1_000_000L),
    S_1_500("1/500s", 2_000_000L),
    S_1_250("1/250s", 4_000_000L),
    S_1_125("1/125s", 8_000_000L),
    S_1_60("1/60s", 16_666_666L),
    S_1_30("1/30s", 33_333_333L),
    S_1_15("1/15s", 66_666_666L),
    S_1_8("1/8s", 125_000_000L),
    S_1_4("1/4s", 250_000_000L),
    S_1_2("1/2s", 500_000_000L),
    S_1s("1s", 1_000_000_000L),
    S_2s("2s", 2_000_000_000L),
    S_4s("4s", 4_000_000_000L),
    S_8s("8s", 8_000_000_000L),
    S_15s("15s", 15_000_000_000L),
    S_30s("30s", 30_000_000_000L)
}

enum class IsoValue(val label: String, val value: Int?) {
    AUTO("AUTO", null),
    ISO_50("50", 50),
    ISO_100("100", 100),
    ISO_200("200", 200),
    ISO_400("400", 400),
    ISO_800("800", 800),
    ISO_1600("1600", 1600),
    ISO_3200("3200", 3200),
    ISO_6400("6400", 6400),
    ISO_12800("12800", 12800)
}

enum class FocusDistance(val label: String, val diopters: Float?) {
    AUTO("AUTO AF", null),
    MACRO("MACRO (0.05m)", 20.0f),
    M_0_1("0.10m", 10.0f),
    M_0_2("0.20m", 5.0f),
    M_0_5("0.50m", 2.0f),
    M_1_0("1.00m", 1.0f),
    M_2_0("2.00m", 0.5f),
    INFINITY("∞ (Inf)", 0.0f)
}

enum class Aperture(val label: String, val fNumber: Float) {
    F1_4("f/1.4", 1.4f),
    F1_8("f/1.8", 1.8f),
    F2_2("f/2.2", 2.2f),
    F2_8("f/2.8", 2.8f),
    F4_0("f/4.0", 4.0f),
    F5_6("f/5.6", 5.6f),
    F8_0("f/8.0", 8.0f),
    F11("f/11", 11.0f),
    F16("f/16", 16.0f)
}

enum class MeteringMode(val label: String, val desc: String) {
    MATRIX("Matrix", "Rata-rata seluruh bingkai"),
    CENTER_WEIGHTED("Center", "Utamakan area tengah"),
    SPOT("Spot", "Titik fokus presisi")
}

enum class FocusBracketingCount(val count: Int, val label: String) {
    OFF(1, "Off"),
    SHOTS_3(3, "3 Shots (Cepat)"),
    SHOTS_5(5, "5 Shots (Standar)"),
    SHOTS_7(7, "7 Shots (Mendalam)"),
    SHOTS_10(10, "10 Shots (Ultra Makro)")
}

enum class FocusStepSize(val stepValue: Float, val label: String) {
    FINE(0.015f, "Halus (Mikron)"),
    MEDIUM(0.035f, "Sedang (Detail)"),
    WIDE(0.070f, "Lebar (Objek Besar)")
}

enum class WhiteBalance(val label: String, val kelvin: Int?) {
    AUTO("AUTO WB", null),
    DAYLIGHT("5500K Daylight", 5500),
    CLOUDY("6500K Cloudy", 6500),
    SHADE("7500K Shade", 7500),
    INCANDESCENT("3000K Tungsten", 3000),
    FLUORESCENT("4000K Fluorescent", 4000),
    CUSTOM("Custom K", 5000)
}

enum class CaptureFormat(val label: String, val isRaw: Boolean, val isJpeg: Boolean) {
    RAW_JPEG("RAW + JPEG", true, true),
    RAW_ONLY("RAW (DNG)", true, false),
    JPEG_ONLY("JPEG Only", false, true)
}

enum class AspectRatioSetting(val label: String, val ratioFloat: Float) {
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_1_1("1:1", 1f),
    RATIO_3_2("3:2", 3f / 2f)
}

enum class ResolutionOption(val label: String, val width: Int, val height: Int, val megaPixels: String) {
    ULTRA_48MP("48 MP Ultra", 8000, 6000, "48.0 MP"),
    HIGH_12MP("12 MP Full", 4000, 3000, "12.0 MP"),
    MED_8MP("8 MP High", 3264, 2448, "8.0 MP"),
    STD_4MP("4 MP Std", 2304, 1728, "4.0 MP");

    fun getEffectiveDimensions(aspectRatio: AspectRatioSetting, isPortrait: Boolean = true): Pair<Int, Int> {
        val (w, h) = when (aspectRatio) {
            AspectRatioSetting.RATIO_16_9 -> Pair(width, (width * 9) / 16)
            AspectRatioSetting.RATIO_1_1 -> Pair(height, height)
            AspectRatioSetting.RATIO_3_2 -> Pair(width, (width * 2) / 3)
            else -> Pair(width, height)
        }
        return if (isPortrait) {
            Pair(minOf(w, h), maxOf(w, h))
        } else {
            Pair(maxOf(w, h), minOf(w, h))
        }
    }
}

enum class DriveMode(val label: String, val shotCount: Int, val desc: String) {
    SINGLE("1x Tunggal", 1, "1 foto per jepretan (Burst mati)"),
    BURST_3("3x Burst", 3, "3 jepretan foto beruntun cepat"),
    BURST_5("5x Burst", 5, "5 jepretan foto beruntun"),
    BURST_10("10x Burst", 10, "10 jepretan foto beruntun"),
    FOCUS_STACK("Focus Stack", 5, "Multi-fokus makro bertumpuk")
}

enum class JpegQuality(val label: String, val qualityPercent: Int, val desc: String) {
    ULTRA_100("100% Ultra (Uncompressed)", 100, "~12 MB / photo"),
    HIGH_95("95% High (Pro Detail)", 95, "~6 MB / photo"),
    MED_85("85% Med (Balanced)", 85, "~3.5 MB / photo"),
    LOW_75("75% Low (Storage Saver)", 75, "~2 MB / photo")
}

enum class FocusPeakingColor(val label: String, val hexColor: Long) {
    GREEN("Green", 0xFF00FF66),
    RED("Red", 0xFFFF1744),
    YELLOW("Yellow", 0xFFFFEA00),
    CYAN("Cyan", 0xFF00E5FF)
}

enum class GridType(val label: String) {
    NONE("Off"),
    RULE_OF_THIRDS("3x3 Grid"),
    GOLDEN_RATIO("Golden Ratio"),
    SQUARE("1:1 Frame")
}

enum class FlashMode(val label: String) {
    OFF("Off"),
    ON("On"),
    AUTO("Auto"),
    TORCH("Torch Light")
}

data class CameraSettings(
    val driveMode: DriveMode = DriveMode.SINGLE,
    val shutterSpeed: ShutterSpeed = ShutterSpeed.AUTO,
    val iso: IsoValue = IsoValue.AUTO,
    val focusDistance: FocusDistance = FocusDistance.AUTO,
    val manualFocusDistanceFactor: Float = 0.8f, // 0.0 (Inf) to 1.0 (Macro closest)
    val isContinuousManualFocus: Boolean = false,
    val isFocusBracketingEnabled: Boolean = false,
    val focusBracketingCount: FocusBracketingCount = FocusBracketingCount.SHOTS_5,
    val focusStepSize: FocusStepSize = FocusStepSize.FINE,
    val isAutoStackingEnabled: Boolean = true, // Merge stacked sharp preview
    val aperture: Aperture = Aperture.F1_8,
    val exposureCompensationEv: Float = 0.0f,
    val meteringMode: MeteringMode = MeteringMode.MATRIX,
    val isAfLocked: Boolean = false,
    val isAeLocked: Boolean = false,
    val whiteBalance: WhiteBalance = WhiteBalance.AUTO,
    val customKelvin: Int = 5000,
    val captureFormat: CaptureFormat = CaptureFormat.RAW_JPEG,
    val aspectRatio: AspectRatioSetting = AspectRatioSetting.RATIO_4_3,
    val resolution: ResolutionOption = ResolutionOption.HIGH_12MP,
    val jpegQuality: JpegQuality = JpegQuality.HIGH_95,
    val isMacroMode: Boolean = false,
    val isFocusPeakingEnabled: Boolean = false,
    val focusPeakingColor: FocusPeakingColor = FocusPeakingColor.GREEN,
    val focusPeakingThreshold: Float = 0.5f,
    val gridType: GridType = GridType.RULE_OF_THIRDS,
    val isHistogramEnabled: Boolean = false,
    val isLevelMeterEnabled: Boolean = false,
    val isZebraEnabled: Boolean = false,
    val isFrontCamera: Boolean = false,
    val flashMode: FlashMode = FlashMode.OFF,
    val timerSeconds: Int = 0
)
