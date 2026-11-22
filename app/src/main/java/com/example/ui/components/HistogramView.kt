package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HistogramData

@Composable
fun HistogramView(
    data: HistogramData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(160.dp)
            .height(90.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xCC0D0E12))
            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(8.dp))
            .padding(4.dp)
            .testTag("histogram_container")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val maxBin = data.maxBinValue.coerceAtLeast(1)

            val rPath = Path()
            val gPath = Path()
            val bPath = Path()
            val lumPath = Path()

            rPath.moveTo(0f, height)
            gPath.moveTo(0f, height)
            bPath.moveTo(0f, height)
            lumPath.moveTo(0f, height)

            for (i in 0 until 256) {
                val x = (i / 255f) * width
                val rY = height - ((data.redBins[i].toFloat() / maxBin) * height).coerceIn(0f, height)
                val gY = height - ((data.greenBins[i].toFloat() / maxBin) * height).coerceIn(0f, height)
                val bY = height - ((data.blueBins[i].toFloat() / maxBin) * height).coerceIn(0f, height)
                val lY = height - ((data.luminanceBins[i].toFloat() / maxBin) * height).coerceIn(0f, height)

                rPath.lineTo(x, rY)
                gPath.lineTo(x, gY)
                bPath.lineTo(x, bY)
                lumPath.lineTo(x, lY)
            }

            // Draw Red, Green, Blue & White Luminance curves
            drawPath(path = rPath, color = Color(0x99FF3D00), style = Stroke(width = 1.5f))
            drawPath(path = gPath, color = Color(0x9900E676), style = Stroke(width = 1.5f))
            drawPath(path = bPath, color = Color(0x992979FF), style = Stroke(width = 1.5f))
            drawPath(path = lumPath, color = Color(0xDDFFFFFF), style = Stroke(width = 2.0f))
        }

        // Label tag
        Text(
            text = "RGB HISTOGRAM",
            color = Color(0xAAFFFFFF),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}
