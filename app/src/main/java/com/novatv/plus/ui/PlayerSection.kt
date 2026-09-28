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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.novatv.plus.data.Channel
import kotlinx.coroutines.delay

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

    var showControls by remember { mutableStateOf(true) }

    // Auto-hide controls after 4 seconds of inactivity
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    val exoPlayer = remember {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(2000, 8000, 1000, 1500)
            .build()

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> onBufferingChanged(true)
                    Player.STATE_READY -> {
                        onBufferingChanged(false)
                        onErrorOccurred(null)
                    }
                    Player.STATE_ENDED, Player.STATE_IDLE -> onBufferingChanged(false)
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
                    cause is HttpDataSource.HttpDataSourceException -> "No connection to stream server"
                    error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> {
                        exoPlayer.seekToDefaultPosition()
                        exoPlayer.prepare()
                        null
                    }
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "No internet connection"
                    error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "Invalid playlist format / unsupported codec"
                    else -> "Unable to play this channel"
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

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

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

    Box(
        modifier = modifier
            .background(BackgroundBlack)
            .border(1.dp, if (isFullscreen) Color.Transparent else BorderSubtle, RoundedCornerShape(3.dp))
            .clip(RoundedCornerShape(3.dp))
            .clickable { showControls = !showControls }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
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
                    .background(Color(0xCC0D0D0D), RoundedCornerShape(4.dp))
                    .padding(16.dp)
            ) {
                CircularProgressIndicator(
                    color = TextPrimaryWhite,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Buffering stream…",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondarySilver, fontSize = 12.sp)
                )
            }
        }

        // Error State with Retry
        if (playerError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF00A0A0A))
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
                        tint = TextSecondarySilver,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playerError,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimaryWhite,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "The broadcast server is temporarily unreachable.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMutedGraphite,
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
                            containerColor = if (buttonFocused) TextPrimaryWhite else SurfaceGraphite28,
                            contentColor = if (buttonFocused) BackgroundBlack else TextPrimaryWhite
                        ),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier
                            .onFocusChanged { buttonFocused = it.isFocused }
                            .focusable()
                            .border(
                                width = 1.dp,
                                color = if (buttonFocused) BorderFocused else BorderSubtle,
                                shape = RoundedCornerShape(3.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Retry Stream",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }

        // Auto-hiding Minimal Controls Overlay
        AnimatedVisibility(
            visible = showControls || playerError != null || !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xD9050505), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (channel != null) {
                    Box(
                        modifier = Modifier
                            .background(SurfaceGraphite28, RoundedCornerShape(2.dp))
                            .border(0.75.dp, BorderSubtle, RoundedCornerShape(2.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CH ${channel.id}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryWhite
                            )
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayerIconButton(
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        onClick = {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        }
                    )

                    PlayerIconButton(
                        icon = Icons.Default.AspectRatio,
                        contentDescription = "Aspect: ${resizeModeNames[resizeModeIndex]}",
                        onClick = {
                            resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                        }
                    )

                    PlayerIconButton(
                        icon = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        onClick = onToggleMute,
                        activeColor = if (isMuted) LiveIndicatorRed else TextPrimaryWhite
                    )

                    PlayerIconButton(
                        icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                        onClick = onToggleFullscreen,
                        activeColor = TextPrimaryWhite
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    activeColor: Color = TextPrimaryWhite,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    IconButton(
        onClick = onClick,
        modifier = modifier
            .padding(2.dp)
            .size(34.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                if (isFocused) SurfaceGraphite28 else Color(0x66000000),
                RoundedCornerShape(3.dp)
            )
            .border(
                1.dp,
                if (isFocused) BorderFocused else Color.Transparent,
                RoundedCornerShape(3.dp)
            )
            .focusable()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) TextPrimaryWhite else activeColor,
            modifier = Modifier.size(18.dp)
        )
    }
}
