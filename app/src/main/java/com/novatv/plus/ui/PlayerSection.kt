package com.novatv.plus.ui

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.novatv.plus.data.Channel

@OptIn(UnstableApi::class)
@Composable
fun PlayerSection(
    channel: Channel?,
    isBuffering: Boolean,
    playerError: String?,
    isFullscreen: Boolean,
    isMuted: Boolean,
    onBufferingChanged: (Boolean) -> Unit,
    onErrorOccurred: (String?) -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleMute: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        AspectRatioFrameLayout.RESIZE_MODE_FILL
    )
    val resizeModeNames = listOf("FIT", "ZOOM", "FILL")

    // Configure ExoPlayer with fast startup buffering
    val exoPlayer = remember {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2000,  // min buffer 2s
                8000,  // max buffer 8s
                1000,  // buffer for playback 1s
                1500   // buffer for playback after rebuffer 1.5s
            )
            .build()

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // Attach listener for playback state and errors
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        onBufferingChanged(true)
                    }
                    Player.STATE_READY -> {
                        onBufferingChanged(false)
                        onErrorOccurred(null)
                    }
                    Player.STATE_ENDED -> {
                        onBufferingChanged(false)
                    }
                    Player.STATE_IDLE -> {
                        onBufferingChanged(false)
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                onBufferingChanged(false)
                val cause = error.cause
                val errorMessage = when {
                    cause is HttpDataSource.InvalidResponseCodeException -> {
                        when (cause.responseCode) {
                            403 -> "Access denied by stream server (HTTP 403)"
                            404 -> "Channel stream not found (HTTP 404)"
                            500, 502, 503 -> "Stream server error (${cause.responseCode})"
                            else -> "Stream server rejected connection (${cause.responseCode})"
                        }
                    }
                    cause is HttpDataSource.HttpDataSourceException -> {
                        "No connection to stream server"
                    }
                    error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> {
                        // Live window slid, retry seek to default
                        exoPlayer.seekToDefaultPosition()
                        exoPlayer.prepare()
                        null
                    }
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> {
                        "No internet connection"
                    }
                    error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> {
                        "Invalid playlist format / unsupported codec"
                    }
                    else -> {
                        "Unable to play this channel"
                    }
                }
                if (errorMessage != null) {
                    onErrorOccurred(errorMessage)
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Update mute state
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Load channel stream URL whenever selected channel changes
    LaunchedEffect(channel?.id, channel?.streamUrl) {
        val streamUrl = channel?.streamUrl
        if (!streamUrl.isNullOrBlank()) {
            onErrorOccurred(null)
            onBufferingChanged(true)

            try {
                val uri = Uri.parse(streamUrl)
                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId(channel.id.toString())
                    .build()

                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            } catch (e: Exception) {
                onBufferingChanged(false)
                onErrorOccurred("Unable to play this channel: ${e.message}")
            }
        } else {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    var showControls by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(OledBlack)
            .border(1.dp, if (isFullscreen) Color.Transparent else SurfaceBorderDark, RoundedCornerShape(2.dp))
            .clip(RoundedCornerShape(2.dp))
    ) {
        // Video Viewport
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Use custom premium OLED controls
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)
                    resizeMode = resizeModes[resizeModeIndex]
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.resizeMode = resizeModes[resizeModeIndex]
            }
        )

        // Buffering Indicator
        AnimatedVisibility(
            visible = isBuffering && playerError == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                    .padding(16.dp)
            ) {
                CircularProgressIndicator(
                    color = CyanAccent,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Connecting to stream…",
                    style = MaterialTheme.typography.bodyMedium.copy(color = CyanAccent, fontWeight = FontWeight.SemiBold)
                )
            }
        }

        // Error State with Retry
        if (playerError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE080808))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = LiveRed,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playerError,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextWhite,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "The stream may be temporarily unavailable.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    var buttonFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            onErrorOccurred(null)
                            onRetry()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (buttonFocused) CyanAccent else Color(0xFF1E293B),
                            contentColor = if (buttonFocused) OledBlack else TextWhite
                        ),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .onFocusChanged { buttonFocused = it.isFocused }
                            .focusable()
                            .border(
                                width = 1.5.dp,
                                color = if (buttonFocused) CyanAccent else SurfaceBorderDark,
                                shape = RoundedCornerShape(2.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Retry Stream",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Top Overlay Bar (Controls: Mute, Aspect, Fullscreen)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xD9000000), Color.Transparent)
                    )
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Number Badge
            if (channel != null) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF00383D), RoundedCornerShape(2.dp))
                        .border(1.dp, CyanAccent, RoundedCornerShape(2.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "CH ${channel.id}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            // Quick Player Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Play / Pause
                PlayerIconButton(
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    onClick = {
                        if (isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    }
                )

                // Aspect Ratio Toggle
                PlayerIconButton(
                    icon = Icons.Default.AspectRatio,
                    contentDescription = "Aspect Ratio: ${resizeModeNames[resizeModeIndex]}",
                    onClick = {
                        resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                    }
                )

                // Mute Toggle
                PlayerIconButton(
                    icon = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    onClick = onToggleMute,
                    activeColor = if (isMuted) LiveRed else TextWhite
                )

                // Fullscreen Toggle
                PlayerIconButton(
                    icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                    onClick = onToggleFullscreen,
                    activeColor = CyanAccent
                )
            }
        }
    }
}

@Composable
fun PlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    activeColor: Color = TextWhite,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    IconButton(
        onClick = onClick,
        modifier = modifier
            .padding(2.dp)
            .size(36.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                if (isFocused) CyanAccentGlow else Color(0x66000000),
                RoundedCornerShape(2.dp)
            )
            .border(
                1.dp,
                if (isFocused) CyanAccent else Color.Transparent,
                RoundedCornerShape(2.dp)
            )
            .focusable()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) CyanAccent else activeColor,
            modifier = Modifier.size(20.dp)
        )
    }
}
