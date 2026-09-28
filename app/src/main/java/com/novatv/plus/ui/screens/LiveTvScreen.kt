package com.novatv.plus.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.novatv.plus.ui.LiveIndicatorRed
import com.novatv.plus.ui.PlayerSection
import com.novatv.plus.ui.SurfaceGraphite0D
import com.novatv.plus.ui.SurfaceGraphite14
import com.novatv.plus.ui.SurfaceGraphite1E
import com.novatv.plus.ui.SurfaceGraphite28
import com.novatv.plus.ui.TextMutedGraphite
import com.novatv.plus.ui.TextPrimaryWhite
import com.novatv.plus.ui.TextSecondarySilver
import com.novatv.plus.viewmodel.AppDestination
import com.novatv.plus.viewmodel.NovaTvViewModel

@Composable
fun LiveTvScreen(
    viewModel: NovaTvViewModel,
    modifier: Modifier = Modifier
) {
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val playerError by viewModel.playerError.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val selectedAudioTrack by viewModel.selectedAudioTrack.collectAsState()
    val selectedSubtitleTrack by viewModel.selectedSubtitleTrack.collectAsState()

    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            configuration.screenWidthDp >= 600

    BackHandler(enabled = isFullscreen) {
        viewModel.setFullscreen(false)
    }

    if (isFullscreen) {
        Box(modifier = Modifier.fillMaxSize().background(BackgroundBlack)) {
            PlayerSection(
                channel = selectedChannel,
                isBuffering = isBuffering,
                playerError = playerError,
                isFullscreen = true,
                isMuted = isMuted,
                onBufferingChanged = { viewModel.setBuffering(it) },
                onErrorOccurred = { viewModel.setPlayerError(it) },
                onToggleFullscreen = { viewModel.toggleFullscreen() },
                onToggleMute = { viewModel.toggleMute() },
                onRetry = { viewModel.retryCurrentChannel() },
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack)
    ) {
        if (isLandscape) {
            // TV / Landscape Layout: Player top-left, channel metadata top-right; bottom: category + list
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Video Player (~45%)
                PlayerSection(
                    channel = selectedChannel,
                    isBuffering = isBuffering,
                    playerError = playerError,
                    isFullscreen = false,
                    isMuted = isMuted,
                    onBufferingChanged = { viewModel.setBuffering(it) },
                    onErrorOccurred = { viewModel.setPlayerError(it) },
                    onToggleFullscreen = { viewModel.toggleFullscreen() },
                    onToggleMute = { viewModel.toggleMute() },
                    onRetry = { viewModel.retryCurrentChannel() },
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight()
                )

                // Channel Info & Control Strip (~55%)
                LiveChannelInfoPanel(
                    channel = selectedChannel,
                    selectedAudioTrack = selectedAudioTrack,
                    selectedSubtitleTrack = selectedSubtitleTrack,
                    onToggleFavorite = { selectedChannel?.let { viewModel.toggleFavorite(it.id) } },
                    onGoToEpg = { viewModel.navigateTo(AppDestination.EPG) },
                    onToggleFullscreen = { viewModel.toggleFullscreen() },
                    onOpenAudio = { showAudioDialog = true },
                    onOpenSubtitles = { showSubtitleDialog = true },
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight()
                )
            }
        } else {
            // Mobile Portrait: Stacked Video Player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(BackgroundBlack)
            ) {
                PlayerSection(
                    channel = selectedChannel,
                    isBuffering = isBuffering,
                    playerError = playerError,
                    isFullscreen = false,
                    isMuted = isMuted,
                    onBufferingChanged = { viewModel.setBuffering(it) },
                    onErrorOccurred = { viewModel.setPlayerError(it) },
                    onToggleFullscreen = { viewModel.toggleFullscreen() },
                    onToggleMute = { viewModel.toggleMute() },
                    onRetry = { viewModel.retryCurrentChannel() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Compact Info Strip
            if (selectedChannel != null) {
                LiveMobileInfoStrip(
                    channel = selectedChannel!!,
                    onToggleFavorite = { viewModel.toggleFavorite(selectedChannel!!.id) },
                    onGoToEpg = { viewModel.navigateTo(AppDestination.EPG) },
                    onToggleFullscreen = { viewModel.toggleFullscreen() }
                )
            }
        }

        // Category Horizontal Selector
        CategorySelectorBar(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { viewModel.selectCategory(it) }
        )

        // Vertical Channels List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(BackgroundBlack)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filteredChannels, key = { it.id }) { channel ->
                val isSelected = channel.id == selectedChannel?.id
                LiveChannelRowItem(
                    channel = channel,
                    isSelected = isSelected,
                    onSelect = { viewModel.selectChannel(channel) },
                    onToggleFavorite = { viewModel.toggleFavorite(channel.id) }
                )
            }
        }
    }

    // Audio Track Dialog
    if (showAudioDialog) {
        val tracks = listOf("Original Stereo (AAC)", "English 5.1 (Dolby)", "Descriptive Audio")
        TrackSelectionDialog(
            title = "Audio Track",
            options = tracks,
            selectedOption = selectedAudioTrack,
            onSelect = {
                viewModel.setAudioTrack(it)
                showAudioDialog = false
            },
            onDismiss = { showAudioDialog = false }
        )
    }

    // Subtitles Dialog
    if (showSubtitleDialog) {
        val subs = listOf("Off", "English (CC)", "English [SDH]", "Spanish")
        TrackSelectionDialog(
            title = "Subtitles",
            options = subs,
            selectedOption = selectedSubtitleTrack,
            onSelect = {
                viewModel.setSubtitleTrack(it)
                showSubtitleDialog = false
            },
            onDismiss = { showSubtitleDialog = false }
        )
    }
}

@Composable
fun LiveChannelInfoPanel(
    channel: Channel?,
    selectedAudioTrack: String,
    selectedSubtitleTrack: String,
    onToggleFavorite: () -> Unit,
    onGoToEpg: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenSubtitles: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(SurfaceGraphite0D, RoundedCornerShape(4.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
            .padding(16.dp)
    ) {
        if (channel != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Channel Name & Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Channel Logo Box
                        Box(
                            modifier = Modifier
                                .size(48.dp, 36.dp)
                                .background(SurfaceGraphite14, RoundedCornerShape(3.dp))
                                .border(0.5.dp, BorderSubtle, RoundedCornerShape(3.dp)),
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
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = TextMutedGraphite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${channel.id}  ${channel.name}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryWhite
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (channel.isLive) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(LiveIndicatorRed, RoundedCornerShape(2.dp))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
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
                            Text(
                                text = channel.category,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 11.5.sp,
                                    color = TextSecondarySilver
                                )
                            )
                        }
                    }

                    // Favorite Button
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (channel.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (channel.isFavorite) TextPrimaryWhite else TextMutedGraphite
                        )
                    }
                }

                // Middle: NOW PLAYING Program with Progress Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceGraphite14, RoundedCornerShape(3.dp))
                        .border(0.5.dp, BorderSubtle, RoundedCornerShape(3.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "NOW PLAYING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = TextSecondarySilver
                            )
                        )
                        Text(
                            text = "${channel.programStart ?: "20:00"} — ${channel.programEnd ?: "21:00"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = TextMutedGraphite
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = channel.programTitle ?: "Live Transmission",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = TextPrimaryWhite
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { 0.45f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = TextPrimaryWhite,
                        trackColor = SurfaceGraphite28
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = channel.description ?: "Live broadcast feed directly from provider.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 11.sp,
                            color = TextSecondarySilver,
                            lineHeight = 15.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bottom: Action Buttons (EPG, Audio, Subtitles, Fullscreen)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MinimalPillButton(
                        icon = Icons.Default.CalendarToday,
                        label = "EPG Guide",
                        onClick = onGoToEpg,
                        modifier = Modifier.weight(1f)
                    )
                    MinimalPillButton(
                        icon = Icons.Default.Audiotrack,
                        label = "Audio",
                        onClick = onOpenAudio,
                        modifier = Modifier.weight(1f)
                    )
                    MinimalPillButton(
                        icon = Icons.Default.Subtitles,
                        label = "Subtitles",
                        onClick = onOpenSubtitles,
                        modifier = Modifier.weight(1f)
                    )
                    MinimalPillButton(
                        icon = Icons.Default.Fullscreen,
                        label = "Full",
                        onClick = onToggleFullscreen,
                        modifier = Modifier.width(72.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MinimalPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .background(
                if (isFocused) SurfaceGraphite28 else SurfaceGraphite14,
                RoundedCornerShape(3.dp)
            )
            .border(
                1.dp,
                if (isFocused) BorderFocused else BorderSubtle,
                RoundedCornerShape(3.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isFocused) TextPrimaryWhite else TextSecondarySilver,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isFocused) TextPrimaryWhite else TextSecondarySilver,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LiveMobileInfoStrip(
    channel: Channel,
    onToggleFavorite: () -> Unit,
    onGoToEpg: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceGraphite0D)
            .border(0.5.dp, BorderSubtle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${channel.id}  ${channel.name}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryWhite
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel.isLive) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(LiveIndicatorRed, RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "LIVE",
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
                text = "${channel.category} • ${channel.programTitle ?: "Live Broadcast"}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.sp,
                    color = TextSecondarySilver
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (channel.isFavorite) TextPrimaryWhite else TextMutedGraphite
                )
            }
            IconButton(onClick = onGoToEpg) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "EPG",
                    tint = TextSecondarySilver,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onToggleFullscreen) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = TextSecondarySilver,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun CategorySelectorBar(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundBlack)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { it }) { cat ->
            val isSelected = cat.equals(selectedCategory, ignoreCase = true)
            var isFocused by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .background(
                        if (isSelected) SurfaceGraphite28 else SurfaceGraphite0D,
                        RoundedCornerShape(3.dp)
                    )
                    .border(
                        1.dp,
                        if (isFocused) BorderFocused else if (isSelected) Color(0xFF555555) else BorderSubtle,
                        RoundedCornerShape(3.dp)
                    )
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .clickable { onCategorySelected(cat) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = cat,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.8.sp,
                        color = if (isSelected || isFocused) TextPrimaryWhite else TextSecondarySilver
                    )
                )
            }
        }
    }
}

@Composable
fun LiveChannelRowItem(
    channel: Channel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) SurfaceGraphite1E else SurfaceGraphite0D,
                RoundedCornerShape(3.dp)
            )
            .border(
                1.dp,
                if (isFocused) BorderFocused else if (isSelected) Color(0xFF444444) else BorderSubtle,
                RoundedCornerShape(3.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = channel.id.toString(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) TextPrimaryWhite else TextSecondarySilver
            ),
            modifier = Modifier.width(36.dp)
        )

        // Logo
        Box(
            modifier = Modifier
                .size(36.dp, 28.dp)
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
                text = channel.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = TextPrimaryWhite
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = channel.programTitle ?: "Live Feed",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.sp,
                    color = TextSecondarySilver
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (channel.isLive) {
            Box(
                modifier = Modifier
                    .background(LiveIndicatorRed, RoundedCornerShape(2.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "LIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                )
            }
        }

        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (channel.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = "Favorite",
                tint = if (channel.isFavorite) TextPrimaryWhite else TextMutedGraphite,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun TrackSelectionDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceGraphite14,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = TextPrimaryWhite,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column {
                options.forEach { option ->
                    val isSelected = option == selectedOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = if (isSelected) TextPrimaryWhite else TextSecondarySilver,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                        if (isSelected) {
                            Text(
                                text = "✓",
                                color = TextPrimaryWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("DONE", color = TextPrimaryWhite, fontWeight = FontWeight.Bold)
            }
        }
    )
}
