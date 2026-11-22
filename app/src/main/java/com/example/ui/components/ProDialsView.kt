package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.model.Aperture
import com.example.model.CameraSettings
import com.example.model.FocusBracketingCount
import com.example.model.FocusDistance
import com.example.model.FocusStepSize
import com.example.model.IsoValue
import com.example.model.ShutterSpeed
import com.example.model.WhiteBalance
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentLight

@Composable
fun ProDialsView(
    settings: CameraSettings,
    activeTab: com.example.viewmodel.ActiveControlTab,
    onTabSelected: (com.example.viewmodel.ActiveControlTab) -> Unit,
    onShutterSelected: (ShutterSpeed) -> Unit,
    onIsoSelected: (IsoValue) -> Unit,
    onFocusSelected: (FocusDistance) -> Unit,
    onManualFocusChanged: (Float) -> Unit,
    onManualFocusDelta: (Float) -> Unit,
    onToggleContinuousFocus: (Boolean) -> Unit,
    onToggleFocusBracketing: (Boolean) -> Unit,
    onFocusBracketingCountSelected: (FocusBracketingCount) -> Unit,
    onFocusStepSizeSelected: (FocusStepSize) -> Unit,
    onApertureSelected: (Aperture) -> Unit,
    onEvChanged: (Float) -> Unit,
    onWbSelected: (WhiteBalance) -> Unit,
    onKelvinChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xEE0D0E12))
            .padding(vertical = 8.dp)
            .testTag("pro_dials_panel")
    ) {
        // Active Dial Value Selector Carousel
        AnimatedVisibility(
            visible = activeTab != com.example.viewmodel.ActiveControlTab.NONE,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161820))
                    .padding(vertical = 10.dp, horizontal = 12.dp)
            ) {
                when (activeTab) {
                    com.example.viewmodel.ActiveControlTab.SHUTTER -> {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ShutterSpeed.entries.forEach { speed ->
                                DialPillItem(
                                    label = speed.label,
                                    isSelected = settings.shutterSpeed == speed,
                                    onClick = { onShutterSelected(speed) },
                                    tag = "shutter_${speed.name}"
                                )
                            }
                        }
                    }
                    com.example.viewmodel.ActiveControlTab.ISO -> {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IsoValue.entries.forEach { iso ->
                                DialPillItem(
                                    label = iso.label,
                                    isSelected = settings.iso == iso,
                                    onClick = { onIsoSelected(iso) },
                                    tag = "iso_${iso.name}"
                                )
                            }
                        }
                    }
                    com.example.viewmodel.ActiveControlTab.FOCUS -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Sub-mode selectors: [Presets] [Micro MF Slider] [Stack BKT]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DialPillItem(
                                    label = "Presets AF/MF",
                                    isSelected = !settings.isContinuousManualFocus && !settings.isFocusBracketingEnabled,
                                    onClick = {
                                        onToggleContinuousFocus(false)
                                        onToggleFocusBracketing(false)
                                    },
                                    tag = "focus_sub_presets"
                                )
                                DialPillItem(
                                    label = "Micro Slider MF",
                                    isSelected = settings.isContinuousManualFocus && !settings.isFocusBracketingEnabled,
                                    onClick = {
                                        onToggleContinuousFocus(true)
                                        onToggleFocusBracketing(false)
                                    },
                                    tag = "focus_sub_micro"
                                )
                                DialPillItem(
                                    label = if (settings.isFocusBracketingEnabled) "Stack BKT (ON)" else "Stack BKT (Off)",
                                    isSelected = settings.isFocusBracketingEnabled,
                                    onClick = {
                                        onToggleFocusBracketing(!settings.isFocusBracketingEnabled)
                                    },
                                    tag = "focus_sub_bracketing"
                                )
                            }

                            // Preset distance pills
                            if (!settings.isContinuousManualFocus && !settings.isFocusBracketingEnabled) {
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(rememberScrollState())
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FocusDistance.entries.forEach { dist ->
                                        DialPillItem(
                                            label = dist.label,
                                            isSelected = settings.focusDistance == dist,
                                            onClick = { onFocusSelected(dist) },
                                            tag = "focus_${dist.name}"
                                        )
                                    }
                                }
                            }

                            // Continuous Micro-Focus Slider with fine-tuning [-] and [+] buttons
                            if (settings.isContinuousManualFocus || settings.isFocusBracketingEnabled) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Micro Focus Plane: ${(settings.manualFocusDistanceFactor * 100).toInt()}% " +
                                                    if (settings.manualFocusDistanceFactor > 0.75f) "(Super Makro)"
                                                    else if (settings.manualFocusDistanceFactor > 0.4f) "(Jarak Dekat)"
                                                    else "(Fokus Jauh)",
                                            color = GoldAccentLight,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = String.format(java.util.Locale.US, "%.3f", settings.manualFocusDistanceFactor),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { onManualFocusDelta(-0.02f) },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(Color(0xFF262936), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Mundur Fokus 0.02",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Slider(
                                            value = settings.manualFocusDistanceFactor,
                                            onValueChange = onManualFocusChanged,
                                            valueRange = 0.0f..1.0f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = GoldAccent,
                                                activeTrackColor = GoldAccent
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 8.dp)
                                                .testTag("micro_focus_slider")
                                        )

                                        IconButton(
                                            onClick = { onManualFocusDelta(+0.02f) },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(Color(0xFF262936), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Maju Fokus 0.02",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Focus Bracketing Configuration panel
                            if (settings.isFocusBracketingEnabled) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF1E212D), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Layers,
                                                contentDescription = null,
                                                tint = GoldAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Focus Bracketing (Depth Stacking)",
                                                color = GoldAccent,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "${settings.focusBracketingCount.count} Irisan",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // Shot counts
                                    Row(
                                        modifier = Modifier
                                            .horizontalScroll(rememberScrollState())
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FocusBracketingCount.entries.filter { it != FocusBracketingCount.OFF }.forEach { count ->
                                            DialPillItem(
                                                label = "${count.count} Shots",
                                                isSelected = settings.focusBracketingCount == count,
                                                onClick = { onFocusBracketingCountSelected(count) },
                                                tag = "bkt_count_${count.count}"
                                            )
                                        }
                                    }

                                    // Step sizes
                                    Row(
                                        modifier = Modifier
                                            .horizontalScroll(rememberScrollState())
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FocusStepSize.entries.forEach { step ->
                                            DialPillItem(
                                                label = step.label,
                                                isSelected = settings.focusStepSize == step,
                                                onClick = { onFocusStepSizeSelected(step) },
                                                tag = "bkt_step_${step.name}"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    com.example.viewmodel.ActiveControlTab.APERTURE_EV -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Aperture & EV Compensation: ${String.format("%.1f", settings.exposureCompensationEv)} EV",
                                color = GoldAccentLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Aperture.entries.forEach { ap ->
                                    DialPillItem(
                                        label = ap.label,
                                        isSelected = settings.aperture == ap,
                                        onClick = { onApertureSelected(ap) },
                                        tag = "aperture_${ap.name}"
                                    )
                                }
                            }
                            Slider(
                                value = settings.exposureCompensationEv,
                                onValueChange = onEvChanged,
                                valueRange = -3.0f..3.0f,
                                steps = 11,
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldAccent,
                                    activeTrackColor = GoldAccent
                                ),
                                modifier = Modifier.testTag("ev_slider")
                            )
                        }
                    }
                    com.example.viewmodel.ActiveControlTab.WHITE_BALANCE -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WhiteBalance.entries.forEach { wb ->
                                    DialPillItem(
                                        label = wb.label,
                                        isSelected = settings.whiteBalance == wb,
                                        onClick = { onWbSelected(wb) },
                                        tag = "wb_${wb.name}"
                                    )
                                }
                            }
                            if (settings.whiteBalance == WhiteBalance.CUSTOM) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Color Temp Kelvin: ${settings.customKelvin} K",
                                    color = GoldAccentLight,
                                    fontSize = 12.sp
                                )
                                Slider(
                                    value = settings.customKelvin.toFloat(),
                                    onValueChange = { onKelvinChanged(it.toInt()) },
                                    valueRange = 2000f..10000f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = GoldAccent,
                                        activeTrackColor = GoldAccent
                                    ),
                                    modifier = Modifier.testTag("kelvin_slider")
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        // Bottom Mode Tabs Bar: [S], [ISO], [F], [EV], [WB]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlTabButton(
                title = "S",
                valueText = settings.shutterSpeed.label,
                isSelected = activeTab == com.example.viewmodel.ActiveControlTab.SHUTTER,
                isModified = settings.shutterSpeed != ShutterSpeed.AUTO,
                onClick = { onTabSelected(com.example.viewmodel.ActiveControlTab.SHUTTER) },
                tag = "tab_shutter"
            )

            ControlTabButton(
                title = "ISO",
                valueText = settings.iso.label,
                isSelected = activeTab == com.example.viewmodel.ActiveControlTab.ISO,
                isModified = settings.iso != IsoValue.AUTO,
                onClick = { onTabSelected(com.example.viewmodel.ActiveControlTab.ISO) },
                tag = "tab_iso"
            )

            val focusDisplay = if (settings.isFocusBracketingEnabled) {
                "BKT ${settings.focusBracketingCount.count}x"
            } else if (settings.isContinuousManualFocus) {
                "MF ${(settings.manualFocusDistanceFactor * 100).toInt()}%"
            } else {
                settings.focusDistance.label
            }

            ControlTabButton(
                title = "FOCUS",
                valueText = focusDisplay,
                isSelected = activeTab == com.example.viewmodel.ActiveControlTab.FOCUS,
                isModified = settings.focusDistance != FocusDistance.AUTO || settings.isContinuousManualFocus || settings.isFocusBracketingEnabled,
                onClick = { onTabSelected(com.example.viewmodel.ActiveControlTab.FOCUS) },
                tag = "tab_focus"
            )

            ControlTabButton(
                title = "EV",
                valueText = "${settings.aperture.label} ${if (settings.exposureCompensationEv >= 0) "+" else ""}${String.format("%.1f", settings.exposureCompensationEv)}",
                isSelected = activeTab == com.example.viewmodel.ActiveControlTab.APERTURE_EV,
                isModified = settings.exposureCompensationEv != 0.0f,
                onClick = { onTabSelected(com.example.viewmodel.ActiveControlTab.APERTURE_EV) },
                tag = "tab_ev"
            )

            ControlTabButton(
                title = "WB",
                valueText = settings.whiteBalance.label.take(8),
                isSelected = activeTab == com.example.viewmodel.ActiveControlTab.WHITE_BALANCE,
                isModified = settings.whiteBalance != WhiteBalance.AUTO,
                onClick = { onTabSelected(com.example.viewmodel.ActiveControlTab.WHITE_BALANCE) },
                tag = "tab_wb"
            )
        }
    }
}

@Composable
private fun ControlTabButton(
    title: String,
    valueText: String,
    isSelected: Boolean,
    isModified: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(tag)
    ) {
        Text(
            text = title,
            color = if (isSelected) GoldAccent else if (isModified) GoldAccentLight else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        Text(
            text = valueText,
            color = if (isSelected) GoldAccent else Color(0xAAFFFFFF),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(2.dp)
                .background(if (isSelected) GoldAccent else Color.Transparent)
        )
    }
}

@Composable
private fun DialPillItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) GoldAccent else Color(0xFF262936))
            .border(
                1.dp,
                if (isSelected) GoldAccentLight else Color(0x33FFFFFF),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}
