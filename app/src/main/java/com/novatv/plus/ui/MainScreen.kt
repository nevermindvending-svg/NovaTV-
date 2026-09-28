package com.novatv.plus.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.novatv.plus.data.Channel
import com.novatv.plus.viewmodel.NovaTvViewModel

@Composable
fun MainScreen(
    viewModel: NovaTvViewModel,
    modifier: Modifier = Modifier
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val playerError by viewModel.playerError.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val customPlaylistUrl by viewModel.customPlaylistUrl.collectAsState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            configuration.screenWidthDp >= 600

    var showPlaylistDialog by remember { mutableStateOf(false) }

    // Intercept Back button when in Fullscreen mode
    BackHandler(enabled = isFullscreen) {
        viewModel.setFullscreen(false)
    }

    if (isFullscreen) {
        // Fullscreen Player Mode
        Box(modifier = Modifier.fillMaxSize().background(OledBlack)) {
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

    // Normal IPTV Dashboard Layout
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        // Top Header Bar
        TopHeaderBar(
            searchQuery = searchQuery,
            onSearchQueryChanged = { viewModel.setSearchQuery(it) },
            onRefresh = { viewModel.loadChannels() },
            onOpenPlaylistConfig = { showPlaylistDialog = true }
        )

        if (isLoading) {
            // Loading Splash State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = CyanAccent,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "NOVA TV+",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = CyanAccent,
                            letterSpacing = 2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Loading 12 playlists concurrently…",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                }
            }
        } else {
            // Main Content Area
            if (isLandscape) {
                // TV / Landscape Layout: Top section has Player (40%) on left + EPG info on right
                Column(modifier = Modifier.fillMaxSize()) {
                    // TOP SECTION
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Left: Video Player (~42% width)
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
                                .weight(0.42f)
                                .fillMaxHeight()
                        )

                        // Right: Channel & Program Information
                        ChannelInfoPanel(
                            channel = selectedChannel,
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                        )
                    }

                    // CATEGORY BAR
                    CategoryBar(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { viewModel.selectCategory(it) }
                    )

                    // CHANNEL LIST
                    ChannelList(
                        channels = filteredChannels,
                        selectedChannel = selectedChannel,
                        onChannelSelected = { viewModel.selectChannel(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Mobile Portrait Layout: Stacked vertically
                Column(modifier = Modifier.fillMaxSize()) {
                    // Video Player
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .padding(4.dp)
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

                    // Compact Channel Details
                    if (selectedChannel != null) {
                        CompactChannelDetails(channel = selectedChannel!!)
                    }

                    // Category Bar
                    CategoryBar(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { viewModel.selectCategory(it) }
                    )

                    // Channel List
                    ChannelList(
                        channels = filteredChannels,
                        selectedChannel = selectedChannel,
                        onChannelSelected = { viewModel.selectChannel(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Playlist Configuration Dialog (Security Requirement: configurable user URL)
    if (showPlaylistDialog) {
        PlaylistConfigDialog(
            currentUrl = customPlaylistUrl,
            onDismiss = { showPlaylistDialog = false },
            onSave = { newUrl ->
                viewModel.saveCustomPlaylist(newUrl)
                showPlaylistDialog = false
            }
        )
    }
}

@Composable
fun TopHeaderBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenPlaylistConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark08)
            .border(0.5.dp, SurfaceBorderDark)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // App Title & Logo Branding
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF00383D), RoundedCornerShape(2.dp))
                    .border(1.dp, CyanAccent, RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "N+",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = CyanAccent
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "NOVA TV+",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Sub-badge
            Box(
                modifier = Modifier
                    .background(Color(0xFF141414), RoundedCornerShape(2.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "IPTV PLAYER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = CyanAccentDark
                    )
                )
            }
        }

        // Action Buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSearchExpanded) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = {
                        Text(
                            text = "Search channel or number…",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = TextMuted)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SurfaceBorderDark,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = CyanAccent
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            onSearchQueryChanged("")
                            isSearchExpanded = false
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Search",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .width(220.dp)
                        .height(44.dp)
                )
            } else {
                IconButton(onClick = { isSearchExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reload Playlists",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onOpenPlaylistConfig) {
                Icon(
                    imageVector = Icons.Default.PlaylistPlay,
                    contentDescription = "Manage Playlists",
                    tint = CyanAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun ChannelInfoPanel(
    channel: Channel?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "panelPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "panelLiveAlpha"
    )

    Box(
        modifier = modifier
            .background(SurfaceDark08, RoundedCornerShape(2.dp))
            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(2.dp))
            .padding(12.dp)
    ) {
        if (channel != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Channel ID, Name, Logo, LIVE Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo Box
                    Box(
                        modifier = Modifier
                            .size(52.dp, 40.dp)
                            .background(Color(0xFF141414), RoundedCornerShape(2.dp))
                            .border(0.5.dp, SurfaceBorderDark, RoundedCornerShape(2.dp))
                            .clip(RoundedCornerShape(2.dp)),
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
                                modifier = Modifier.fillMaxSize(),
                                error = {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${channel.id} • ${channel.name}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = channel.category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyanAccent,
                                    letterSpacing = 0.5.sp
                                )
                            )

                            if (channel.isLive) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(LiveRed.copy(alpha = pulseAlpha), RoundedCornerShape(2.dp))
                                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = TextWhite
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Middle: NOW PLAYING Program Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark11, RoundedCornerShape(2.dp))
                        .border(0.5.dp, SurfaceBorderDark, RoundedCornerShape(2.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NOW PLAYING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )

                        Text(
                            text = "${channel.programStart ?: "20:00"} — ${channel.programEnd ?: "21:00"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = channel.programTitle ?: "Live Broadcast",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = channel.description ?: "Live high-definition transmission from broadcast source.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom: NEXT Program Preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NEXT: ${channel.nextProgramTitle ?: "Upcoming Feature"}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = channel.programEnd ?: "21:00",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select a channel to view live guide",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            }
        }
    }
}

@Composable
fun CompactChannelDetails(
    channel: Channel,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "compactPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "compactLiveAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark08)
            .border(0.5.dp, SurfaceBorderDark)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${channel.id} • ${channel.name}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (channel.isLive) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(LiveRed.copy(alpha = pulseAlpha), RoundedCornerShape(2.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = TextWhite
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = "${channel.category} • ${channel.programTitle ?: "Live Broadcast"}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.sp,
                    color = TextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PlaylistConfigDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var inputUrl by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark11,
        title = {
            Text(
                text = "Configure Playlists",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Nova TV+ loads 12 playlists concurrently. You can customize slot 12 with your own private M3U / M3U8 URL below.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Security reminder: Do not publish private authentication tokens in shared code.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 11.5.sp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    placeholder = {
                        Text(
                            text = "https://example.com/playlist.m3u",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                        )
                    },
                    label = { Text("Playlist 12 URL", color = CyanAccent) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = SurfaceBorderDark,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = CyanAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(inputUrl) }
            ) {
                Text("SAVE & RELOAD", color = CyanAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}
