package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentLight
import java.util.Locale

/**
 * Vertical Zoom Control Bar oriented along the phone's vertical (portrait) axis.
 * Always active and visible without auto-hide.
 * Uses Plus (+) and Minus (-) buttons with precise 0.2x increments, plus vertical drag track.
 */
@Composable
fun ZoomControlBar(
    zoomRatio: Float,
    onZoomChange: (Float) -> Unit,
    onZoomDelta: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val zoomFraction = ((zoomRatio - 1.0f) / 15.0f).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xE60D0E14))
            .border(1.dp, Color(0x55FFB800), RoundedCornerShape(28.dp))
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { deltaY ->
                    // Dragging UP (negative deltaY) increases zoom by 0.2 steps
                    val zoomDelta = (-deltaY / 14f) * 0.2f
                    onZoomDelta(zoomDelta)
                }
            )
            .padding(vertical = 10.dp, horizontal = 8.dp)
            .testTag("vertical_zoom_bar"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Plus (+) Button: Zoom In +0.2x
        ZoomStepIconButton(
            icon = Icons.Default.Add,
            contentDescription = "Zoom In +0.2x",
            enabled = zoomRatio < 16.0f,
            onClick = { onZoomDelta(0.2f) },
            testTag = "zoom_plus_button"
        )

        // Vertical Progress Track (Top = 16.0x, Bottom = 1.0x)
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(84.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0x33FFFFFF)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = zoomFraction.coerceAtLeast(0.06f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(GoldAccent)
            )
        }

        // Active Zoom Badge displaying current exact ratio (e.g., "1.2x")
        // Tapping badge resets to 1.0x
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(GoldAccent)
                .clickable { onZoomChange(1.0f) }
                .padding(horizontal = 7.dp, vertical = 4.dp)
                .testTag("zoom_value_badge"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format(Locale.US, "%.1fx", zoomRatio),
                color = Color.Black,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Minus (-) Button: Zoom Out -0.2x
        ZoomStepIconButton(
            icon = Icons.Default.Remove,
            contentDescription = "Zoom Out -0.2x",
            enabled = zoomRatio > 1.0f,
            onClick = { onZoomDelta(-0.2f) },
            testTag = "zoom_minus_button"
        )
    }
}

@Composable
private fun ZoomStepIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (enabled) Color(0x33FFB800) else Color(0x15FFFFFF))
            .border(
                width = 1.5.dp,
                color = if (enabled) GoldAccent else Color(0x33FFFFFF),
                shape = CircleShape
            )
            .clickable(enabled = enabled) { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) GoldAccentLight else Color(0x55FFFFFF),
            modifier = Modifier.size(20.dp)
        )
    }
}
