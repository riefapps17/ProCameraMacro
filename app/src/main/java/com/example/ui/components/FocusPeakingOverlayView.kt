package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag

@Composable
fun FocusPeakingOverlayView(
    peakingBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    if (peakingBitmap == null) return

    Image(
        bitmap = peakingBitmap.asImageBitmap(),
        contentDescription = "Focus Peaking Edge Highlights",
        modifier = modifier
            .fillMaxSize()
            .testTag("focus_peaking_overlay"),
        contentScale = ContentScale.FillBounds
    )
}
