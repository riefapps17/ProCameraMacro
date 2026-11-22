package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AspectRatioSetting
import com.example.model.CameraSettings
import com.example.model.FlashMode
import com.example.model.GridType
import com.example.model.MeteringMode
import com.example.ui.theme.FocusPeakingGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RawBadgeColor

@Composable
fun TopBarControls(
    settings: CameraSettings,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onFormatClick: () -> Unit,
    onRatioClick: () -> Unit,
    onMacroClick: () -> Unit,
    onFocusPeakingClick: () -> Unit,
    onMeteringClick: () -> Unit,
    onGridClick: () -> Unit,
    onFlashClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xEE0D0E12))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 20 && !isExpanded) {
                        onToggleExpanded()
                    } else if (dragAmount < -20 && isExpanded) {
                        onToggleExpanded()
                    }
                }
            }
            .testTag("top_bar_controls")
    ) {
        // Minimalist collapsed header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // RAW/JPEG Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (settings.captureFormat.isRaw) RawBadgeColor else Color(0xFF2E3244))
                    .clickable { onFormatClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("badge_raw_format")
            ) {
                Text(
                    text = settings.captureFormat.label,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Pull-down tab indicator handle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFFFFF))
                    .clickable { onToggleExpanded() }
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Tutup Menu" else "Tarik Menu",
                    color = Color(0xCCFFFFFF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Tarik Panel Ikon Kamera",
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Settings Cog button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("button_settings")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Pengaturan Kamera",
                    tint = Color.White
                )
            }
        }

        // Hidden Expandable Drawer (Muncul saat ditarik kebawah)
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161820))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash Toggle
                IconButton(
                    onClick = onFlashClick,
                    modifier = Modifier.testTag("button_flash_toggle")
                ) {
                    val icon = when (settings.flashMode) {
                        FlashMode.OFF -> Icons.Default.FlashOff
                        FlashMode.ON, FlashMode.TORCH -> Icons.Default.FlashOn
                        FlashMode.AUTO -> Icons.Default.FlashAuto
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Flash Mode",
                        tint = if (settings.flashMode != FlashMode.OFF) GoldAccent else Color(0xAAFFFFFF)
                    )
                }

                // Metering Mode Selector (Matrix, Center, Spot)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2E3244))
                        .clickable { onMeteringClick() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("button_metering_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MTR",
                            color = Color(0x88FFFFFF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = settings.meteringMode.label,
                            color = GoldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Macro Mode Toggle
                IconButton(
                    onClick = onMacroClick,
                    modifier = Modifier.testTag("button_macro_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterVintage,
                        contentDescription = "Macro Mode",
                        tint = if (settings.isMacroMode) GoldAccent else Color(0xAAFFFFFF)
                    )
                }

                // Focus Peaking Toggle
                IconButton(
                    onClick = onFocusPeakingClick,
                    modifier = Modifier.testTag("button_focus_peaking_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusWeak,
                        contentDescription = "Focus Peaking",
                        tint = if (settings.isFocusPeakingEnabled) FocusPeakingGreen else Color(0xAAFFFFFF)
                    )
                }

                // Aspect Ratio Selector
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2E3244))
                        .clickable { onRatioClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("badge_aspect_ratio")
                ) {
                    Text(
                        text = settings.aspectRatio.label,
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Grid Overlay Toggle
                IconButton(
                    onClick = onGridClick,
                    modifier = Modifier.testTag("button_grid_toggle")
                ) {
                    Icon(
                        imageVector = if (settings.gridType != GridType.NONE) Icons.Default.Grid3x3 else Icons.Default.GridOn,
                        contentDescription = "Composition Grid",
                        tint = if (settings.gridType != GridType.NONE) GoldAccent else Color(0xAAFFFFFF)
                    )
                }
            }
        }
    }
}
