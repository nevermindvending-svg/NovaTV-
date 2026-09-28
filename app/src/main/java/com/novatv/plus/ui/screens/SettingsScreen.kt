package com.novatv.plus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novatv.plus.ui.BackgroundBlack
import com.novatv.plus.ui.BorderFocused
import com.novatv.plus.ui.BorderSubtle
import com.novatv.plus.ui.SurfaceGraphite0D
import com.novatv.plus.ui.SurfaceGraphite14
import com.novatv.plus.ui.SurfaceGraphite1E
import com.novatv.plus.ui.SurfaceGraphite28
import com.novatv.plus.ui.TextMutedGraphite
import com.novatv.plus.ui.TextPrimaryWhite
import com.novatv.plus.ui.TextSecondarySilver
import com.novatv.plus.viewmodel.NovaTvViewModel

@Composable
fun SettingsScreen(
    viewModel: NovaTvViewModel,
    modifier: Modifier = Modifier
) {
    val customPlaylistUrl by viewModel.customPlaylistUrl.collectAsState()
    var showPlaylistDialog by remember { mutableStateOf(false) }

    var hwDecoderEnabled by remember { mutableStateOf(true) }
    var autoPlayNext by remember { mutableStateOf(true) }
    var highBitrateAudio by remember { mutableStateOf(true) }
    var parentalLockEnabled by remember { mutableStateOf(false) }
    var oledDeepBlackTheme by remember { mutableStateOf(true) }
    var rememberPlaybackPosition by remember { mutableStateOf(true) }
    var lowLatencyBuffer by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Page Title
        item {
            Column {
                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = TextPrimaryWhite
                    )
                )
                Text(
                    text = "System configuration, playlists & playback preferences",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondarySilver)
                )
            }
        }

        // Group 1: Account
        item {
            SettingsGroup(title = "ACCOUNT") {
                SettingsNavigationItem(
                    icon = Icons.Default.Person,
                    title = "Profile & Device",
                    subtitle = "Nova TV+ Registered Client (ID: 009-PRO-TV)",
                    onClick = {}
                )
            }
        }

        // Group 2: Playlist
        item {
            SettingsGroup(title = "PLAYLIST") {
                SettingsNavigationItem(
                    icon = Icons.Default.PlaylistPlay,
                    title = "Manage Playlists & Custom M3U",
                    subtitle = if (customPlaylistUrl.isNotBlank()) "12 Active Sources (Custom Slot Configured)" else "12 Active Concurrent M3U Sources",
                    onClick = { showPlaylistDialog = true }
                )
            }
        }

        // Group 3: EPG
        item {
            SettingsGroup(title = "EPG (ELECTRONIC PROGRAM GUIDE)") {
                SettingsNavigationItem(
                    icon = Icons.Default.Schedule,
                    title = "EPG Provider & Time Shift",
                    subtitle = "Auto-sync every 12 hours • Local UTC offset 00:00",
                    onClick = {}
                )
            }
        }

        // Group 4: Player
        item {
            SettingsGroup(title = "PLAYER") {
                SettingsToggleItem(
                    icon = Icons.Default.Tv,
                    title = "Hardware Accelerated Decoding",
                    subtitle = "Direct GPU decoding for 4K / 60FPS streams",
                    checked = hwDecoderEnabled,
                    onCheckedChange = { hwDecoderEnabled = it }
                )
                SettingsToggleItem(
                    icon = Icons.Default.PlayCircle,
                    title = "Auto-Play Next Stream",
                    subtitle = "Seamlessly switch to the next channel on connection end",
                    checked = autoPlayNext,
                    onCheckedChange = { autoPlayNext = it }
                )
                SettingsToggleItem(
                    icon = Icons.Default.PlayCircle,
                    title = "Dolby Digital & Lossless Audio Passthrough",
                    subtitle = "Send multi-channel audio directly to HDMI AV receiver",
                    checked = highBitrateAudio,
                    onCheckedChange = { highBitrateAudio = it }
                )
            }
        }

        // Group 5: Appearance
        item {
            SettingsGroup(title = "APPEARANCE") {
                SettingsToggleItem(
                    icon = Icons.Default.Palette,
                    title = "Pure OLED Deep Black (#050505)",
                    subtitle = "Maximum contrast and energy savings on OLED panels",
                    checked = oledDeepBlackTheme,
                    onCheckedChange = { oledDeepBlackTheme = it }
                )
            }
        }

        // Group 6: Playback
        item {
            SettingsGroup(title = "PLAYBACK") {
                SettingsToggleItem(
                    icon = Icons.Default.PlayCircle,
                    title = "Remember Playback Position",
                    subtitle = "Resume VOD and catch-up streams from the last saved stamp",
                    checked = rememberPlaybackPosition,
                    onCheckedChange = { rememberPlaybackPosition = it }
                )
                SettingsToggleItem(
                    icon = Icons.Default.Tv,
                    title = "Low-Latency Stream Buffer",
                    subtitle = "Minimize broadcast delay (recommended for live sports)",
                    checked = lowLatencyBuffer,
                    onCheckedChange = { lowLatencyBuffer = it }
                )
            }
        }

        // Group 7: Parental Controls
        item {
            SettingsGroup(title = "PARENTAL CONTROLS") {
                SettingsToggleItem(
                    icon = Icons.Default.Lock,
                    title = "Require PIN for Adult & Restricted Streams",
                    subtitle = "Protect channels with 4-digit security PIN",
                    checked = parentalLockEnabled,
                    onCheckedChange = { parentalLockEnabled = it }
                )
            }
        }

        // Group 8: About
        item {
            SettingsGroup(title = "ABOUT") {
                SettingsNavigationItem(
                    icon = Icons.Default.Info,
                    title = "NOVA TV+ Production Build",
                    subtitle = "Version 1.0.0 • Media3 ExoPlayer Engine • Architecture: MVVM",
                    onClick = {}
                )
            }
        }
    }

    // Playlist Dialog
    if (showPlaylistDialog) {
        var inputUrl by remember { mutableStateOf(customPlaylistUrl) }
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            containerColor = SurfaceGraphite14,
            title = {
                Text(
                    text = "Configure Custom Playlist",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimaryWhite,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = "Nova TV+ loads 12 playlists concurrently. You can customize slot 12 with your own private M3U / M3U8 URL below.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondarySilver)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        placeholder = {
                            Text(
                                text = "https://example.com/playlist.m3u",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedGraphite)
                            )
                        },
                        label = { Text("M3U URL", color = TextSecondarySilver) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BorderFocused,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimaryWhite,
                            unfocusedTextColor = TextPrimaryWhite,
                            cursorColor = TextPrimaryWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveCustomPlaylist(inputUrl)
                        showPlaylistDialog = false
                    }
                ) {
                    Text("SAVE & RELOAD", color = TextPrimaryWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlaylistDialog = false }) {
                    Text("CANCEL", color = TextSecondarySilver)
                }
            }
        )
    }
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextMutedGraphite,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceGraphite0D, RoundedCornerShape(4.dp))
                .border(0.75.dp, BorderSubtle, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
        ) {
            content()
        }
    }
}

@Composable
fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isFocused) SurfaceGraphite1E else Color.Transparent)
            .border(1.dp, if (isFocused) BorderFocused else Color.Transparent)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) TextPrimaryWhite else TextSecondarySilver,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryWhite
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    color = TextSecondarySilver
                )
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextMutedGraphite,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isFocused) SurfaceGraphite1E else Color.Transparent)
            .border(1.dp, if (isFocused) BorderFocused else Color.Transparent)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) TextPrimaryWhite else TextSecondarySilver,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryWhite
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    color = TextSecondarySilver
                )
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BackgroundBlack,
                checkedTrackColor = TextPrimaryWhite,
                uncheckedThumbColor = TextSecondarySilver,
                uncheckedTrackColor = SurfaceGraphite28
            )
        )
    }
}
