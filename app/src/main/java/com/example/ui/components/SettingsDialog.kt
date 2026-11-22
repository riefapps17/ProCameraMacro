package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AspectRatioSetting
import com.example.model.CameraSettings
import com.example.model.DriveMode
import com.example.model.FocusPeakingColor
import com.example.model.JpegQuality
import com.example.model.MeteringMode
import com.example.model.ResolutionOption
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentLight

@Composable
fun SettingsDialog(
    settings: CameraSettings,
    onDismiss: () -> Unit,
    onDriveModeSelected: (DriveMode) -> Unit,
    onJpegQualitySelected: (JpegQuality) -> Unit,
    onResolutionSelected: (ResolutionOption) -> Unit,
    onAspectRatioSelected: (AspectRatioSetting) -> Unit,
    onMeteringModeSelected: (MeteringMode) -> Unit,
    onFocusPeakingColorSelected: (FocusPeakingColor) -> Unit,
    onHistogramToggle: () -> Unit,
    onLevelMeterToggle: () -> Unit,
    onZebraToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categories = listOf("Mode Jepretan", "Tampilan Layar", "Resolusi & Rasio", "Metering", "Kualitas JPEG", "Peaking")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161822)),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header with Title and Close Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pengaturan Kamera",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                        Text(
                            text = "Konfigurasi instan dalam format grid",
                            fontSize = 11.sp,
                            color = Color(0x99FFFFFF)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF232634))
                            .testTag("button_close_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Compact Category Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedCategoryIndex,
                    containerColor = Color(0xFF10121A),
                    contentColor = GoldAccent,
                    edgePadding = 4.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                            color = GoldAccent,
                            height = 2.5.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    categories.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedCategoryIndex == index,
                            onClick = { selectedCategoryIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedCategoryIndex == index) GoldAccent else Color(0xAAFFFFFF)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Body Content (Compact Grid Layouts)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                ) {
                    when (selectedCategoryIndex) {
                        // 0: Mode Jepretan (Tunggal 1x / Burst / Focus Stack)
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "JUMLAH FOTO / MODE BURST",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccentLight
                                )
                                DriveMode.entries.forEach { mode ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (settings.driveMode == mode) Color(0xFF332A15) else Color(0xFF202330))
                                            .border(
                                                1.5.dp,
                                                if (settings.driveMode == mode) GoldAccent else Color(0x22FFFFFF),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { onDriveModeSelected(mode) }
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = mode.label,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (settings.driveMode == mode) GoldAccent else Color.White
                                                )
                                                Text(
                                                    text = mode.desc,
                                                    fontSize = 10.sp,
                                                    color = if (settings.driveMode == mode) GoldAccentLight else Color(0x88FFFFFF)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (settings.driveMode == mode) GoldAccent else Color(0x33FFFFFF))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (mode == DriveMode.SINGLE) "1x" else "${mode.shotCount}x",
                                                    color = if (settings.driveMode == mode) Color.Black else Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 1: Tampilan Layar (Grid 1x3 Switch Cards)
                        1 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingSwitchCard(
                                    title = "RGB Histogram",
                                    subtitle = "Grafik eksposur real-time",
                                    isChecked = settings.isHistogramEnabled,
                                    onCheckedChange = { onHistogramToggle() }
                                )
                                SettingSwitchCard(
                                    title = "Level Meter",
                                    subtitle = "Garis panduan horison sejajar",
                                    isChecked = settings.isLevelMeterEnabled,
                                    onCheckedChange = { onLevelMeterToggle() }
                                )
                                SettingSwitchCard(
                                    title = "Zebra Stripes",
                                    subtitle = "Peringatan overexposure",
                                    isChecked = settings.isZebraEnabled,
                                    onCheckedChange = { onZebraToggle() }
                                )
                            }
                        }

                        // 2: Resolusi & Rasio
                        2 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "UKURAN RESOLUSI SENSOR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccentLight
                                )
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.height(110.dp)
                                ) {
                                    items(ResolutionOption.entries) { res ->
                                        GridSelectCard(
                                            title = res.label,
                                            subtitle = "${res.width}x${res.height}",
                                            isSelected = settings.resolution == res,
                                            onClick = { onResolutionSelected(res) }
                                        )
                                    }
                                }

                                Text(
                                    text = "ASPEK RASIO FOTO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccentLight
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AspectRatioSetting.entries.forEach { ratio ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (settings.aspectRatio == ratio) GoldAccent else Color(0xFF222534))
                                                .border(
                                                    1.dp,
                                                    if (settings.aspectRatio == ratio) GoldAccentLight else Color(0x22FFFFFF),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { onAspectRatioSelected(ratio) }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = ratio.label,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (settings.aspectRatio == ratio) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3: Pemilihan Metering (Matrix, Center-weighted, Spot)
                        3 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "METODE PERHITUNGAN CAHAYA (METERING)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccentLight
                                )
                                MeteringMode.entries.forEach { mode ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (settings.meteringMode == mode) Color(0xFF332A15) else Color(0xFF202330))
                                            .border(
                                                1.5.dp,
                                                if (settings.meteringMode == mode) GoldAccent else Color(0x22FFFFFF),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { onMeteringModeSelected(mode) }
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = mode.label,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (settings.meteringMode == mode) GoldAccent else Color.White
                                            )
                                            Text(
                                                text = mode.desc,
                                                fontSize = 10.sp,
                                                color = if (settings.meteringMode == mode) GoldAccentLight else Color(0x88FFFFFF)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4: Kualitas JPEG (2x2 Grid)
                        4 -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "TINGKAT KOMPRESI JPEG",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccentLight
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.height(190.dp)
                                ) {
                                    items(JpegQuality.entries) { quality ->
                                        GridSelectCard(
                                            title = quality.label,
                                            subtitle = quality.desc,
                                            isSelected = settings.jpegQuality == quality,
                                            onClick = { onJpegQualitySelected(quality) }
                                        )
                                    }
                                }
                            }
                        }

                        // 5: Peaking (2x2 Grid Warna Highlight)
                        5 -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "WARNA FOCUS PEAKING",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccentLight
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.height(190.dp)
                                ) {
                                    items(FocusPeakingColor.entries) { colorOption ->
                                        val c = Color(colorOption.hexColor)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF222534))
                                                .border(
                                                    2.dp,
                                                    if (settings.focusPeakingColor == colorOption) c else Color(0x22FFFFFF),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable { onFocusPeakingColorSelected(colorOption) }
                                                .padding(14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(c)
                                                )
                                                Text(
                                                    text = colorOption.label,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (settings.focusPeakingColor == colorOption) FontWeight.Bold else FontWeight.Medium,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridSelectCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFF332A15) else Color(0xFF202330))
            .border(
                1.5.dp,
                if (isSelected) GoldAccent else Color(0x22FFFFFF),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) GoldAccent else Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = if (isSelected) GoldAccentLight else Color(0x88FFFFFF)
            )
        }
    }
}

@Composable
private fun SettingSwitchCard(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF202330))
            .clickable { onCheckedChange() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color(0x88FFFFFF)
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = { onCheckedChange() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = GoldAccent,
                checkedTrackColor = Color(0xFF4D3B00)
            )
        )
    }
}
