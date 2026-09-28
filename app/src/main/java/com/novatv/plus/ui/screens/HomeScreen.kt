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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.scale
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
import com.novatv.plus.ui.LiveIndicatorRed
import com.novatv.plus.ui.SurfaceGraphite0D
import com.novatv.plus.ui.SurfaceGraphite14
import com.novatv.plus.ui.SurfaceGraphite1E
import com.novatv.plus.ui.SurfaceGraphite28
import com.novatv.plus.ui.TextMutedGraphite
import com.novatv.plus.ui.TextPrimaryWhite
import com.novatv.plus.ui.TextSecondarySilver
import com.novatv.plus.ui.components.NovaHeroBranding
import com.novatv.plus.viewmodel.ContinueWatchingItem
import com.novatv.plus.viewmodel.NovaTvViewModel

@Composable
fun HomeScreen(
    viewModel: NovaTvViewModel,
    modifier: Modifier = Modifier
) {
    val allChannels by viewModel.allChannels.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val recentlyWatched by viewModel.recentlyWatched.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val favorites = allChannels.filter { it.isFavorite }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Top Hero Header Branding
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                NovaHeroBranding()
            }
        }

        // 1. Continue Watching Section
        if (continueWatching.isNotEmpty()) {
            item {
                SectionHeader(title = "CONTINUE WATCHING")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(continueWatching, key = { it.channel.id }) { item ->
                        ContinueWatchingCard(
                            item = item,
                            onClick = { viewModel.playChannelAndGoToLive(item.channel) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 2. Live TV Section (Horizontal row of live stream cards)
        if (allChannels.isNotEmpty()) {
            item {
                SectionHeader(title = "LIVE TV CHANNELS")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(allChannels.take(12), key = { it.id }) { channel ->
                        ChannelCard(
                            channel = channel,
                            onClick = { viewModel.playChannelAndGoToLive(channel) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 3. Recently Watched Section
        if (recentlyWatched.isNotEmpty()) {
            item {
                SectionHeader(title = "RECENTLY WATCHED")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(recentlyWatched, key = { "recent_${it.id}" }) { channel ->
                        ChannelCard(
                            channel = channel,
                            onClick = { viewModel.playChannelAndGoToLive(channel) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 4. Favorites Section
        if (favorites.isNotEmpty()) {
            item {
                SectionHeader(title = "FAVORITES")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(favorites, key = { "fav_${it.id}" }) { channel ->
                        ChannelCard(
                            channel = channel,
                            onClick = { viewModel.playChannelAndGoToLive(channel) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 5. Category Rows: Entertainment, Movies, Sports, Kids, News, Documentary
        val standardCategories = listOf("NEWS", "SPORTS", "MOVIES", "ENTERTAINMENT", "DOCUMENTARY", "KIDS")
        for (cat in standardCategories) {
            val catChannels = allChannels.filter { it.category.equals(cat, ignoreCase = true) }
            if (catChannels.isNotEmpty()) {
                item {
                    SectionHeader(title = cat)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(catChannels.take(10), key = { "cat_${cat}_${it.id}" }) { channel ->
                            ChannelCard(
                                channel = channel,
                                onClick = { viewModel.playChannelAndGoToLive(channel) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.8.sp,
            fontSize = 13.sp,
            color = TextSecondarySilver
        ),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
    )
}

@Composable
fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(220.dp)
            .scale(if (isFocused) 1.03f else 1.0f)
            .background(SurfaceGraphite0D, RoundedCornerShape(4.dp))
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) BorderFocused else BorderSubtle,
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && (it.key == Key.DirectionCenter || it.key == Key.Enter)) {
                    onClick()
                    true
                } else false
            }
    ) {
        // 16:9 Thumbnail Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(SurfaceGraphite14),
            contentAlignment = Alignment.Center
        ) {
            if (!item.channel.logoUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.channel.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = TextMutedGraphite,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Play Icon Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
                    .background(Color(0x99000000), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume",
                    tint = TextPrimaryWhite,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Progress bar at bottom of thumbnail
            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
                color = TextPrimaryWhite,
                trackColor = SurfaceGraphite28
            )
        }

        // Details
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = item.programTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryWhite
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.channel.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 11.sp,
                        color = TextSecondarySilver
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${item.remainingMinutes}m left",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 10.sp,
                        color = TextMutedGraphite
                    )
                )
            }
        }
    }
}

@Composable
fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(200.dp)
            .scale(if (isFocused) 1.03f else 1.0f)
            .background(SurfaceGraphite0D, RoundedCornerShape(4.dp))
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) BorderFocused else BorderSubtle,
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && (it.key == Key.DirectionCenter || it.key == Key.Enter)) {
                    onClick()
                    true
                } else false
            }
    ) {
        // 16:9 Thumbnail Area with Channel Logo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(SurfaceGraphite14),
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = TextMutedGraphite,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Top Left: Channel Number Pill
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(SurfaceGraphite28, RoundedCornerShape(2.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = channel.id.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryWhite
                    )
                )
            }

            // Top Right: LIVE Indicator Pill
            if (channel.isLive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(LiveIndicatorRed, RoundedCornerShape(2.dp))
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = "LIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    )
                }
            }
        }

        // Channel Info Area
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryWhite
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = channel.programTitle ?: "Live Transmission",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.sp,
                    color = TextSecondarySilver
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Next: ${channel.nextProgramTitle ?: "Upcoming Program"}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 10.sp,
                    color = TextMutedGraphite
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
