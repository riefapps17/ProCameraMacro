package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.LevelHorizonGreen
import kotlin.math.abs

@Composable
fun LevelMeterView(
    rollAngle: Float,
    pitchAngle: Float,
    modifier: Modifier = Modifier
) {
    val isPerfectLevel = abs(rollAngle) <= 1.5f && abs(pitchAngle) <= 2.0f
    val lineColor = if (isPerfectLevel) LevelHorizonGreen else Color(0xCCFFFFFF)

    Canvas(modifier = modifier.fillMaxSize().testTag("level_meter_canvas")) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val lineLen = size.width * 0.35f

        // Center target pitch dot & tick marks
        drawCircle(
            color = lineColor,
            radius = 5f,
            center = Offset(cx, cy)
        )

        drawCircle(
            color = Color(0x66FFFFFF),
            radius = 35f,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        // Rotate Horizon line according to Roll sensor angle
        rotate(degrees = -rollAngle, pivot = Offset(cx, cy)) {
            // Left horizon line segment
            drawLine(
                color = lineColor,
                start = Offset(cx - lineLen, cy),
                end = Offset(cx - 50f, cy),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )

            // Right horizon line segment
            drawLine(
                color = lineColor,
                start = Offset(cx + 50f, cy),
                end = Offset(cx + lineLen, cy),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
    }
}
