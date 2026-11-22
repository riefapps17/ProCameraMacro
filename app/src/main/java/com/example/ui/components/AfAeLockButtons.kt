package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraSettings
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentLight
import kotlinx.coroutines.delay

@Composable
fun AfAeLockButtons(
    isAfLocked: Boolean,
    isAeLocked: Boolean,
    onToggleAfLock: () -> Unit,
    onToggleAeLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(8.dp)
            .testTag("af_ae_lock_bar"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // AF Lock Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isAfLocked) GoldAccent else Color(0xCC161822))
                .border(1.dp, if (isAfLocked) GoldAccentLight else Color(0x44FFFFFF), RoundedCornerShape(8.dp))
                .clickable { onToggleAfLock() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isAfLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "AF Lock",
                    tint = if (isAfLocked) Color.Black else Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "AF-L",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isAfLocked) Color.Black else Color.White
                )
            }
        }

        // AE Lock Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isAeLocked) Color(0xFFFF9800) else Color(0xCC161822))
                .border(1.dp, if (isAeLocked) Color(0xFFFFB74D) else Color(0x44FFFFFF), RoundedCornerShape(8.dp))
                .clickable { onToggleAeLock() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isAeLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "AE Lock",
                    tint = if (isAeLocked) Color.Black else Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "AE-L",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isAeLocked) Color.Black else Color.White
                )
            }
        }
    }
}

@Composable
fun TapFocusTargetRing(
    point: android.graphics.PointF?,
    isAfLocked: Boolean,
    isAeLocked: Boolean,
    modifier: Modifier = Modifier
) {
    if (point == null) return

    var visible by remember(point) { mutableStateOf(true) }

    LaunchedEffect(point) {
        visible = true
        delay(2500)
        if (!isAfLocked && !isAeLocked) {
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(2.dp)) {
            Canvas(modifier = Modifier.size(64.dp)) {
                val ringColor = if (isAfLocked) Color(0xFFFFB800) else Color(0xEEFFFFFF)
                drawCircle(
                    color = ringColor,
                    radius = size.width / 2f,
                    style = Stroke(width = 2.5f)
                )
                // Center metering dot
                drawCircle(
                    color = ringColor,
                    radius = 3.5f
                )
            }
        }
    }
}
