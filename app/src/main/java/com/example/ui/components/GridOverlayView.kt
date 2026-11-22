package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import com.example.model.GridType

@Composable
fun GridOverlayView(
    gridType: GridType,
    modifier: Modifier = Modifier
) {
    if (gridType == GridType.NONE) return

    Canvas(modifier = modifier.fillMaxSize().testTag("grid_overlay")) {
        val w = size.width
        val h = size.height
        val gridColor = Color(0x55FFFFFF)
        val strokeW = 1.5f

        when (gridType) {
            GridType.RULE_OF_THIRDS -> {
                // Vertical lines at 1/3 and 2/3
                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = strokeW)
                drawLine(gridColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), strokeWidth = strokeW)

                // Horizontal lines at 1/3 and 2/3
                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = strokeW)
                drawLine(gridColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), strokeWidth = strokeW)
            }
            GridType.GOLDEN_RATIO -> {
                val gV = 0.382f
                drawLine(gridColor, Offset(w * gV, 0f), Offset(w * gV, h), strokeWidth = strokeW)
                drawLine(gridColor, Offset(w * (1 - gV), 0f), Offset(w * (1 - gV), h), strokeWidth = strokeW)

                drawLine(gridColor, Offset(0f, h * gV), Offset(w, h * gV), strokeWidth = strokeW)
                drawLine(gridColor, Offset(0f, h * (1 - gV)), Offset(w, h * (1 - gV)), strokeWidth = strokeW)
            }
            GridType.SQUARE -> {
                val squareDim = minOf(w, h)
                val left = (w - squareDim) / 2f
                val top = (h - squareDim) / 2f
                drawRect(
                    color = Color(0x88000000),
                    topLeft = Offset(0f, 0f),
                    size = Size(left, h)
                )
                drawRect(
                    color = Color(0x88000000),
                    topLeft = Offset(left + squareDim, 0f),
                    size = Size(w - (left + squareDim), h)
                )
                drawRect(
                    color = gridColor,
                    topLeft = Offset(left, top),
                    size = Size(squareDim, squareDim),
                    style = Stroke(width = 2f)
                )
            }
            GridType.NONE -> {}
        }
    }
}
