package com.novatv.plus.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novatv.plus.ui.components.NovaHeaderBranding
import com.novatv.plus.ui.screens.EpgScreen
import com.novatv.plus.ui.screens.HomeScreen
import com.novatv.plus.ui.screens.LiveTvScreen
import com.novatv.plus.ui.screens.SearchScreen
import com.novatv.plus.ui.screens.SettingsScreen
import com.novatv.plus.viewmodel.AppDestination
import com.novatv.plus.viewmodel.NovaTvViewModel

@Composable
fun MainScreen(
    viewModel: NovaTvViewModel,
    modifier: Modifier = Modifier
) {
    val currentDestination by viewModel.currentDestination.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()

    val configuration = LocalConfiguration.current
    val isTvOrLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            configuration.screenWidthDp >= 600

    // Back button behavior
    BackHandler(enabled = isFullscreen || currentDestination != AppDestination.HOME) {
        if (isFullscreen) {
            viewModel.setFullscreen(false)
        } else if (currentDestination != AppDestination.HOME) {
            viewModel.navigateTo(AppDestination.HOME)
        }
    }

    if (isLoading) {
        // High-end Splash Loading
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = TextPrimaryWhite,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                NovaHeaderBranding(iconSize = 36.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Loading stream directories…",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedGraphite)
                )
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack)
    ) {
        // If in Fullscreen mode, don't show navigation header
        if (!isFullscreen) {
            // TV / Landscape Top Navigation Header
            if (isTvOrLandscape) {
                TvTopNavigationHeader(
                    currentDestination = currentDestination,
                    onDestinationSelected = { viewModel.navigateTo(it) }
                )
            } else {
                // Mobile Top Minimal Brand Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceGraphite0D)
                        .border(0.5.dp, BorderSubtle)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NovaHeaderBranding(iconSize = 26.dp)
                }
            }
        }

        // Active Screen Content with Smooth Fade Transitions
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { destination ->
                when (destination) {
                    AppDestination.HOME -> HomeScreen(viewModel = viewModel)
                    AppDestination.LIVE_TV -> LiveTvScreen(viewModel = viewModel)
                    AppDestination.EPG -> EpgScreen(viewModel = viewModel)
                    AppDestination.SEARCH -> SearchScreen(viewModel = viewModel)
                    AppDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }

        // Mobile Bottom Navigation Bar (Shown on portrait mobile only, hidden in fullscreen)
        if (!isTvOrLandscape && !isFullscreen) {
            MobileBottomNavBar(
                currentDestination = currentDestination,
                onDestinationSelected = { viewModel.navigateTo(it) }
            )
        }
    }
}

@Composable
fun TvTopNavigationHeader(
    currentDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceGraphite0D)
            .border(0.5.dp, BorderSubtle)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Logo & Branding Left
        NovaHeaderBranding(iconSize = 30.dp)

        // Navigation Tabs Right (Remote D-pad friendly)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvNavTab(
                label = "Home",
                icon = Icons.Default.Home,
                isSelected = currentDestination == AppDestination.HOME,
                onClick = { onDestinationSelected(AppDestination.HOME) }
            )
            TvNavTab(
                label = "Live TV",
                icon = Icons.Default.Tv,
                isSelected = currentDestination == AppDestination.LIVE_TV,
                onClick = { onDestinationSelected(AppDestination.LIVE_TV) }
            )
            TvNavTab(
                label = "EPG",
                icon = Icons.Default.CalendarToday,
                isSelected = currentDestination == AppDestination.EPG,
                onClick = { onDestinationSelected(AppDestination.EPG) }
            )
            TvNavTab(
                label = "Search",
                icon = Icons.Default.Search,
                isSelected = currentDestination == AppDestination.SEARCH,
                onClick = { onDestinationSelected(AppDestination.SEARCH) }
            )
            TvNavTab(
                label = "Settings",
                icon = Icons.Default.Settings,
                isSelected = currentDestination == AppDestination.SETTINGS,
                onClick = { onDestinationSelected(AppDestination.SETTINGS) }
            )
        }
    }
}

@Composable
fun TvNavTab(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .background(
                if (isFocused) SurfaceGraphite28 else if (isSelected) SurfaceGraphite1E else Color.Transparent,
                RoundedCornerShape(3.dp)
            )
            .border(
                1.dp,
                if (isFocused) BorderFocused else if (isSelected) Color(0xFF4A4A4A) else Color.Transparent,
                RoundedCornerShape(3.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && (it.key == Key.DirectionCenter || it.key == Key.Enter)) {
                    onClick()
                    true
                } else false
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected || isFocused) TextPrimaryWhite else TextSecondarySilver,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                color = if (isSelected || isFocused) TextPrimaryWhite else TextSecondarySilver
            )
        )
    }
}

@Composable
fun MobileBottomNavBar(
    currentDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceGraphite0D,
        contentColor = TextPrimaryWhite,
        tonalElevation = 0.dp,
        modifier = Modifier
            .border(0.5.dp, BorderSubtle)
            .height(64.dp)
    ) {
        val items = listOf(
            Triple(AppDestination.HOME, "Home", Icons.Default.Home),
            Triple(AppDestination.LIVE_TV, "Live TV", Icons.Default.Tv),
            Triple(AppDestination.EPG, "EPG", Icons.Default.CalendarToday),
            Triple(AppDestination.SEARCH, "Search", Icons.Default.Search),
            Triple(AppDestination.SETTINGS, "Settings", Icons.Default.Settings)
        )

        items.forEach { (destination, label, icon) ->
            val isSelected = currentDestination == destination
            NavigationBarItem(
                selected = isSelected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TextPrimaryWhite,
                    selectedTextColor = TextPrimaryWhite,
                    unselectedIconColor = TextMutedGraphite,
                    unselectedTextColor = TextMutedGraphite,
                    indicatorColor = SurfaceGraphite28
                )
            )
        }
    }
}
