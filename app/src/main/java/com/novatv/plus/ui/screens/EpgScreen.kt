package com.novatv.plus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.novatv.plus.data.Channel
import com.novatv.plus.data.epg.RealEpgProgram
import com.novatv.plus.ui.BackgroundBlack
import com.novatv.plus.ui.BorderFocused
import com.novatv.plus.ui.BorderSubtle
import com.novatv.plus.ui.BorderSubtleHighlight
import com.novatv.plus.ui.LiveIndicatorRed
import com.novatv.plus.ui.SurfaceGraphite0D
import com.novatv.plus.ui.SurfaceGraphite14
import com.novatv.plus.ui.SurfaceGraphite1E
import com.novatv.plus.ui.SurfaceGraphite28
import com.novatv.plus.ui.TextMutedGraphite
import com.novatv.plus.ui.TextPrimaryWhite
import com.novatv.plus.ui.TextSecondarySilver
import com.novatv.plus.viewmodel.NovaTvViewModel
import java.util.Calendar
import java.util.Locale

@Composable
fun EpgScreen(
    viewModel: NovaTvViewModel,
    modifier: Modifier = Modifier
) {
    val allChannels by viewModel.allChannels.collectAsState()
    val timelineSlots by viewModel.epgTimeline.collectAsState()
    val horizontalScrollState = rememberScrollState()

    var selectedProgramDialog by remember { mutableStateOf<Pair<Channel, RealEpgProgram>?>(null) }

    // Live clock string
    val calendar = remember { Calendar.getInstance() }
    val timeString = remember {
        String.format(Locale.US, "%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack)
    ) {
        // EPG Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceGraphite0D)
                .border(0.5.dp, BorderSubtle)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "ELECTRONIC PROGRAM GUIDE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextPrimaryWhite
                    )
                )
                Text(
                    text = "Real-Time Broadcast Grid • ${allChannels.size} Channels Synchronized",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 11.5.sp,
                        color = TextSecondarySilver
                    )
                )
            }

            // Current Time Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(SurfaceGraphite1E, RoundedCornerShape(3.dp))
                    .border(0.75.dp, BorderSubtleHighlight, RoundedCornerShape(3.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(LiveIndicatorRed, RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$timeString LIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryWhite,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // Timeline Ribbon (Fixed left channels label + horizontally scrolling hour markers)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceGraphite14)
                .border(0.5.dp, BorderSubtle)
        ) {
            // Fixed Left Header Spacer
            Box(
                modifier = Modifier
                    .width(170.dp)
                    .background(SurfaceGraphite14)
                    .border(0.5.dp, BorderSubtle)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "NETWORK / CH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMutedGraphite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }

            // Scrolling Timeline Intervals
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
            ) {
                timelineSlots.forEach { slot ->
                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .background(if (slot.isCurrentSlot) SurfaceGraphite28 else Color.Transparent)
                            .border(0.5.dp, if (slot.isCurrentSlot) BorderSubtleHighlight else BorderSubtle)
                            .padding(vertical = 10.dp, horizontal = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (slot.isCurrentSlot) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(LiveIndicatorRed, RoundedCornerShape(2.5.dp))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = slot.timeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (slot.isCurrentSlot) FontWeight.Bold else FontWeight.Medium,
                                    color = if (slot.isCurrentSlot) TextPrimaryWhite else TextSecondarySilver
                                )
                            )
                        }
                    }
                }
            }
        }

        // Channels & Program Grid Rows (High performance LazyColumn)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(
                items = allChannels,
                key = { it.id }
            ) { channel ->
                val programs = remember(channel.id) { viewModel.getEpgPrograms(channel) }
                EpgChannelRow(
                    channel = channel,
                    programs = programs,
                    horizontalScrollState = horizontalScrollState,
                    onSelectChannel = { viewModel.playChannelAndGoToLive(channel) },
                    onProgramClick = { program ->
                        selectedProgramDialog = Pair(channel, program)
                    }
                )
            }
        }
    }

    // Program Details Modal
    selectedProgramDialog?.let { (channel, program) ->
        AlertDialog(
            onDismissRequest = { selectedProgramDialog = null },
            containerColor = SurfaceGraphite14,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondarySilver,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(SurfaceGraphite28, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = program.qualityBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        color = TextPrimaryWhite
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(SurfaceGraphite28, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = program.rating,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        color = TextSecondarySilver
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = program.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryWhite
                            )
                        )
                    }
                }
            },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${program.startTime} - ${program.endTime} (${program.durationMinutes} mins)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimaryWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        if (program.isCurrent) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(LiveIndicatorRed, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ON AIR • ${program.remainingMinutes}m remaining",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }

                    if (program.isCurrent) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { program.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = TextPrimaryWhite,
                            trackColor = SurfaceGraphite28
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = program.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondarySilver,
                            lineHeight = 20.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Audio: ${program.audioBadge} • Stream: ${channel.category}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMutedGraphite
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedProgramDialog = null
                        viewModel.playChannelAndGoToLive(channel)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextPrimaryWhite,
                        contentColor = BackgroundBlack
                    ),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Watch Live Stream", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProgramDialog = null }) {
                    Text(text = "Close", color = TextSecondarySilver)
                }
            }
        )
    }
}

@Composable
fun EpgChannelRow(
    channel: Channel,
    programs: List<RealEpgProgram>,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onSelectChannel: () -> Unit,
    onProgramClick: (RealEpgProgram) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .border(0.5.dp, BorderSubtle),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sticky Channel Header
        var isChannelFocused by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .width(170.dp)
                .fillMaxSize()
                .background(if (isChannelFocused) SurfaceGraphite1E else SurfaceGraphite0D)
                .border(1.dp, if (isChannelFocused) BorderFocused else BorderSubtle)
                .onFocusChanged { isChannelFocused = it.isFocused }
                .focusable()
                .clickable(onClick = onSelectChannel)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Number
            Text(
                text = "${channel.id}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isChannelFocused) TextPrimaryWhite else TextMutedGraphite,
                    fontSize = 11.sp
                ),
                modifier = Modifier.width(26.dp)
            )

            // Logo
            Box(
                modifier = Modifier
                    .size(34.dp, 26.dp)
                    .background(SurfaceGraphite14, RoundedCornerShape(2.dp))
                    .border(0.5.dp, BorderSubtle, RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(channel.logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(2.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = TextMutedGraphite,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isChannelFocused) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isChannelFocused) TextPrimaryWhite else Color(0xFFD4D4D8),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = channel.category,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMutedGraphite,
                        fontSize = 9.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Horizontal Row of Contextual Program Blocks
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(horizontalScrollState)
        ) {
            programs.forEach { program ->
                // Calculate display width based on duration (1 minute ~ 3.2 dp, min 140dp)
                val blockWidth = (program.durationMinutes * 3.4f).coerceIn(140f, 380f).dp
                EpgProgramBlock(
                    program = program,
                    width = blockWidth,
                    onClick = { onProgramClick(program) }
                )
            }
        }
    }
}

@Composable
fun EpgProgramBlock(
    program: RealEpgProgram,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val bg = when {
        isFocused -> SurfaceGraphite28
        program.isCurrent -> SurfaceGraphite1E
        else -> SurfaceGraphite0D
    }

    val border = when {
        isFocused -> BorderFocused
        program.isCurrent -> BorderSubtleHighlight
        else -> BorderSubtle
    }

    Box(
        modifier = Modifier
            .width(width)
            .fillMaxSize()
            .background(bg)
            .border(if (isFocused) 1.5.dp else 0.5.dp, border)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && (it.key == Key.DirectionCenter || it.key == Key.Enter)) {
                    onClick()
                    true
                } else false
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${program.startTime} - ${program.endTime}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = if (program.isCurrent) TextPrimaryWhite else TextMutedGraphite,
                            fontWeight = if (program.isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                    )

                    if (program.isCurrent) {
                        Box(
                            modifier = Modifier
                                .background(LiveIndicatorRed, RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "NOW",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = program.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (program.isCurrent || isFocused) FontWeight.Bold else FontWeight.Medium,
                        color = if (program.isCurrent || isFocused) TextPrimaryWhite else TextSecondarySilver,
                        fontSize = 12.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom Progress Bar if On Air
            if (program.isCurrent) {
                LinearProgressIndicator(
                    progress = { program.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color = TextPrimaryWhite,
                    trackColor = SurfaceGraphite28
                )
            } else {
                Text(
                    text = program.qualityBadge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        color = TextMutedGraphite
                    )
                )
            }
        }
    }
}
