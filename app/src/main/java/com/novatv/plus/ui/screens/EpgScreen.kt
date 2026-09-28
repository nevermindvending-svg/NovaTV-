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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.novatv.plus.data.Channel
import com.novatv.plus.ui.BackgroundBlack
import com.novatv.plus.ui.BorderFocused
import com.novatv.plus.ui.BorderSubtle
import com.novatv.plus.ui.BorderSubtleHighlight
import com.novatv.plus.ui.SurfaceGraphite0D
import com.novatv.plus.ui.SurfaceGraphite14
import com.novatv.plus.ui.SurfaceGraphite1E
import com.novatv.plus.ui.SurfaceGraphite28
import com.novatv.plus.ui.TextMutedGraphite
import com.novatv.plus.ui.TextPrimaryWhite
import com.novatv.plus.ui.TextSecondarySilver
import com.novatv.plus.viewmodel.EpgBlock
import com.novatv.plus.viewmodel.NovaTvViewModel

@Composable
fun EpgScreen(
    viewModel: NovaTvViewModel,
    modifier: Modifier = Modifier
) {
    val allChannels by viewModel.allChannels.collectAsState()
    val horizontalScrollState = rememberScrollState()

    val timeSlots = listOf(
        "19:00", "19:30", "20:00 (NOW)", "20:30", "21:00", "21:30", "22:00", "22:30", "23:00"
    )

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
                    text = "Today • Live Grid Schedule",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 11.sp,
                        color = TextSecondarySilver
                    )
                )
            }

            // Current Time Pill
            Box(
                modifier = Modifier
                    .background(SurfaceGraphite28, RoundedCornerShape(3.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "20:25 LIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryWhite
                    )
                )
            }
        }

        // Timeline Ribbon (Horizontal scroll)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceGraphite14)
                .border(0.5.dp, BorderSubtle)
        ) {
            // Fixed Left Header Spacer (Matches channel column width)
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "CHANNELS",
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
                timeSlots.forEach { slot ->
                    val isCurrent = slot.contains("NOW")
                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .background(if (isCurrent) SurfaceGraphite28 else Color.Transparent)
                            .padding(vertical = 8.dp, horizontal = 10.dp)
                    ) {
                        Text(
                            text = slot,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) TextPrimaryWhite else TextSecondarySilver
                            )
                        )
                    }
                }
            }
        }

        // Channels & Program Grid Rows
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(allChannels, key = { it.id }) { channel ->
                val blocks = viewModel.getEpgBlocksForChannel(channel)
                EpgChannelRow(
                    channel = channel,
                    blocks = blocks,
                    horizontalScrollState = horizontalScrollState,
                    onSelectChannel = { viewModel.playChannelAndGoToLive(channel) }
                )
            }
        }
    }
}

@Composable
fun EpgChannelRow(
    channel: Channel,
    blocks: List<EpgBlock>,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onSelectChannel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, BorderSubtle)
            .height(72.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Fixed Left Channel Card (Clickable to switch)
        var isChannelFocused by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .width(180.dp)
                .fillMaxSize()
                .background(if (isChannelFocused) SurfaceGraphite1E else SurfaceGraphite0D)
                .border(1.dp, if (isChannelFocused) BorderFocused else Color.Transparent)
                .onFocusChanged { isChannelFocused = it.isFocused }
                .focusable()
                .clickable(onClick = onSelectChannel)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(38.dp, 28.dp)
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
                        modifier = Modifier.fillMaxSize().padding(3.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = TextMutedGraphite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${channel.id}  ${channel.name}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryWhite
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = channel.category,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 10.sp,
                        color = TextSecondarySilver
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Program Blocks Grid (Synced to horizontal scroll)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            blocks.forEach { block ->
                val blockWidth = (block.durationMinutes * 4).dp.coerceAtLeast(120.dp)
                EpgProgramBlockCard(
                    block = block,
                    width = blockWidth,
                    onClick = onSelectChannel
                )
            }
        }
    }
}

@Composable
fun EpgProgramBlockCard(
    block: EpgBlock,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isFocused -> SurfaceGraphite28
        block.isCurrent -> SurfaceGraphite1E
        else -> SurfaceGraphite14
    }

    val borderColor = when {
        isFocused -> BorderFocused
        block.isCurrent -> BorderSubtleHighlight
        else -> BorderSubtle
    }

    Column(
        modifier = Modifier
            .width(width)
            .fillMaxSize()
            .background(backgroundColor)
            .border(0.75.dp, borderColor)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && (it.key == Key.DirectionCenter || it.key == Key.Enter)) {
                    onClick()
                    true
                } else false
            }
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = if (block.isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                        color = TextPrimaryWhite
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (block.isCurrent) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF333333), RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "NOW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimaryWhite
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${block.startTime} — ${block.endTime}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 10.sp,
                    color = TextSecondarySilver
                )
            )
        }

        Text(
            text = block.description,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 9.5.sp,
                color = TextMutedGraphite,
                lineHeight = 12.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
