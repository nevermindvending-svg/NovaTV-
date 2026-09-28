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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.novatv.plus.ui.SurfaceGraphite0D
import com.novatv.plus.ui.SurfaceGraphite14
import com.novatv.plus.ui.SurfaceGraphite1E
import com.novatv.plus.ui.SurfaceGraphite28
import com.novatv.plus.ui.TextMutedGraphite
import com.novatv.plus.ui.TextPrimaryWhite
import com.novatv.plus.ui.TextSecondarySilver
import com.novatv.plus.viewmodel.NovaTvViewModel

@Composable
fun SearchScreen(
    viewModel: NovaTvViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val filteredChannels by viewModel.filteredChannels.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Large Clean Search Input Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                viewModel.setSearchQuery(it)
                if (it.length > 2) {
                    viewModel.addRecentSearch(it)
                }
            },
            placeholder = {
                Text(
                    text = "Search channels, live programs, genres or ID…",
                    style = MaterialTheme.typography.bodyLarge.copy(color = TextMutedGraphite)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondarySilver,
                    modifier = Modifier.size(22.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = TextSecondarySilver,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceGraphite0D,
                unfocusedContainerColor = SurfaceGraphite0D,
                focusedBorderColor = BorderFocused,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimaryWhite,
                unfocusedTextColor = TextPrimaryWhite,
                cursorColor = TextPrimaryWhite
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Recent Searches Tags
        if (searchQuery.isEmpty() && recentSearches.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = TextMutedGraphite,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RECENT SEARCHES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMutedGraphite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recentSearches, key = { it }) { query ->
                    Box(
                        modifier = Modifier
                            .background(SurfaceGraphite14, RoundedCornerShape(3.dp))
                            .border(0.75.dp, BorderSubtle, RoundedCornerShape(3.dp))
                            .clickable { viewModel.setSearchQuery(query) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = query,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                color = TextSecondarySilver
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Search Results List or Empty State
        if (searchQuery.isBlank()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = SurfaceGraphite28,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "EXPLORE TELEVISION",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = TextSecondarySilver
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Type any channel name, category, or current show title.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedGraphite)
                    )
                }
            }
        } else if (filteredChannels.isEmpty()) {
            // No Results Found
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NO MATCHING CHANNELS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = TextSecondarySilver
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No channels match \"$searchQuery\". Try checking the spelling.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedGraphite)
                    )
                }
            }
        } else {
            // Instant Results
            Text(
                text = "${filteredChannels.size} RESULTS FOUND",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextMutedGraphite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredChannels, key = { it.id }) { channel ->
                    SearchResultRow(
                        channel = channel,
                        onClick = {
                            viewModel.addRecentSearch(channel.name)
                            viewModel.playChannelAndGoToLive(channel)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultRow(
    channel: Channel,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isFocused) SurfaceGraphite1E else SurfaceGraphite0D, RoundedCornerShape(3.dp))
            .border(
                1.dp,
                if (isFocused) BorderFocused else BorderSubtle,
                RoundedCornerShape(3.dp)
            )
            .clip(RoundedCornerShape(3.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Channel ID
        Text(
            text = channel.id.toString(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextSecondarySilver
            ),
            modifier = Modifier.width(38.dp)
        )

        // Logo Box
        Box(
            modifier = Modifier
                .size(42.dp, 32.dp)
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
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimaryWhite
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${channel.category} • Now: ${channel.programTitle ?: "Live Feed"}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.5.sp,
                    color = TextSecondarySilver
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
