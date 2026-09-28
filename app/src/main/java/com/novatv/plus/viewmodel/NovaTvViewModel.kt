package com.novatv.plus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novatv.plus.data.Channel
import com.novatv.plus.data.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppDestination {
    HOME,
    LIVE_TV,
    EPG,
    SEARCH,
    SETTINGS
}

data class EpgBlock(
    val title: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int,
    val isCurrent: Boolean,
    val description: String
)

data class ContinueWatchingItem(
    val channel: Channel,
    val programTitle: String,
    val progress: Float, // 0.0f to 1.0f
    val remainingMinutes: Int
)

class NovaTvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlaylistRepository(application.applicationContext)

    // Current Navigation Screen
    private val _currentDestination = MutableStateFlow(AppDestination.HOME)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    // Loading & Data States
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _allChannels = MutableStateFlow<List<Channel>>(emptyList())
    val allChannels: StateFlow<List<Channel>> = _allChannels.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(listOf("ALL", "FAVORITES"))
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow("ALL")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedChannel = MutableStateFlow<Channel?>(null)
    val selectedChannel: StateFlow<Channel?> = _selectedChannel.asStateFlow()

    // Recently Watched Channels
    private val _recentlyWatched = MutableStateFlow<List<Channel>>(emptyList())
    val recentlyWatched: StateFlow<List<Channel>> = _recentlyWatched.asStateFlow()

    // Continue Watching Items
    private val _continueWatching = MutableStateFlow<List<ContinueWatchingItem>>(emptyList())
    val continueWatching: StateFlow<List<ContinueWatchingItem>> = _continueWatching.asStateFlow()

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(
        listOf("Sports", "Cinema", "News", "BBC", "NASA", "Documentary")
    )
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    // Player Playback States
    private val _playerError = MutableStateFlow<String?>(null)
    val playerError: StateFlow<String?> = _playerError.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _selectedAudioTrack = MutableStateFlow("Original Stereo (AAC)")
    val selectedAudioTrack: StateFlow<String> = _selectedAudioTrack.asStateFlow()

    private val _selectedSubtitleTrack = MutableStateFlow("Off")
    val selectedSubtitleTrack: StateFlow<String> = _selectedSubtitleTrack.asStateFlow()

    // Playlist Config
    private val _customPlaylistUrl = MutableStateFlow(repository.getCustomPlaylistUrl())
    val customPlaylistUrl: StateFlow<String> = _customPlaylistUrl.asStateFlow()

    // Filtered Channels for Live TV list and search
    val filteredChannels: StateFlow<List<Channel>> = combine(
        _allChannels,
        _selectedCategory,
        _searchQuery
    ) { channels, category, query ->
        channels.filter { channel ->
            val matchesCategory = when (category) {
                "ALL" -> true
                "FAVORITES" -> channel.isFavorite
                else -> channel.category.equals(category, ignoreCase = true)
            }

            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                channel.name.contains(query, ignoreCase = true) ||
                channel.id.toString().contains(query.trim()) ||
                channel.category.contains(query, ignoreCase = true) ||
                (channel.programTitle?.contains(query, ignoreCase = true) == true)
            }

            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadChannels()
    }

    fun navigateTo(destination: AppDestination) {
        _currentDestination.value = destination
    }

    fun loadChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            _playerError.value = null
            try {
                val loaded = repository.loadAllChannels()
                _allChannels.value = loaded

                val categorySet = linkedSetOf("ALL", "FAVORITES")
                loaded.forEach { ch ->
                    if (ch.category.isNotBlank() && ch.category != "ALL" && ch.category != "FAVORITES") {
                        categorySet.add(ch.category)
                    }
                }
                _categories.value = categorySet.toList()

                if (_selectedChannel.value == null && loaded.isNotEmpty()) {
                    selectChannel(loaded.first())
                }

                // Populate Continue Watching and Recently Watched lists
                if (loaded.isNotEmpty()) {
                    _recentlyWatched.value = loaded.take(8)
                    _continueWatching.value = loaded.take(4).mapIndexed { idx, ch ->
                        ContinueWatchingItem(
                            channel = ch,
                            programTitle = ch.programTitle ?: "Evening Feature",
                            progress = 0.35f + (idx * 0.15f),
                            remainingMinutes = 20 - (idx * 4)
                        )
                    }
                }
            } catch (e: Exception) {
                _playerError.value = "Failed to load playlists: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectChannel(channel: Channel) {
        if (_selectedChannel.value?.id != channel.id) {
            _playerError.value = null
            _isBuffering.value = true
            _selectedChannel.value = channel

            // Add to recently watched (front of queue, deduplicated)
            val updatedRecent = _recentlyWatched.value.toMutableList()
            updatedRecent.removeAll { it.id == channel.id }
            updatedRecent.add(0, channel)
            _recentlyWatched.value = updatedRecent.take(12)
        }
    }

    fun playChannelAndGoToLive(channel: Channel) {
        selectChannel(channel)
        navigateTo(AppDestination.LIVE_TV)
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            val list = _recentSearches.value.toMutableList()
            list.removeAll { it.equals(trimmed, ignoreCase = true) }
            list.add(0, trimmed)
            _recentSearches.value = list.take(8)
        }
    }

    fun removeRecentSearch(query: String) {
        val list = _recentSearches.value.toMutableList()
        list.removeAll { it.equals(query, ignoreCase = true) }
        _recentSearches.value = list
    }

    fun toggleFavorite(channelId: Int) {
        val updatedFavs = repository.toggleFavorite(channelId)
        val currentChannels = _allChannels.value.map { ch ->
            if (ch.id == channelId) {
                ch.copy(isFavorite = updatedFavs.contains(channelId))
            } else {
                ch
            }
        }
        _allChannels.value = currentChannels

        if (_selectedChannel.value?.id == channelId) {
            _selectedChannel.value = _selectedChannel.value?.copy(
                isFavorite = updatedFavs.contains(channelId)
            )
        }
    }

    fun setBuffering(buffering: Boolean) {
        _isBuffering.value = buffering
    }

    fun setPlayerError(error: String?) {
        _playerError.value = error
        if (error != null) {
            _isBuffering.value = false
        }
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun setAudioTrack(track: String) {
        _selectedAudioTrack.value = track
    }

    fun setSubtitleTrack(track: String) {
        _selectedSubtitleTrack.value = track
    }

    fun retryCurrentChannel() {
        val current = _selectedChannel.value
        if (current != null) {
            _playerError.value = null
            _isBuffering.value = true
            _selectedChannel.value = null
            _selectedChannel.value = current
        }
    }

    fun saveCustomPlaylist(url: String) {
        repository.setCustomPlaylistUrl(url)
        _customPlaylistUrl.value = url
        loadChannels()
    }

    /**
     * Generates EPG timeline program blocks for each channel
     */
    fun getEpgBlocksForChannel(channel: Channel): List<EpgBlock> {
        val title1 = channel.programTitle ?: "Prime Broadcast"
        val title2 = channel.nextProgramTitle ?: "Nightline Report"
        return listOf(
            EpgBlock(
                title = "News Headlines Early",
                startTime = "19:00",
                endTime = "19:30",
                durationMinutes = 30,
                isCurrent = false,
                description = "Pre-primetime regional updates and weather overview."
            ),
            EpgBlock(
                title = "Market & Analysis Brief",
                startTime = "19:30",
                endTime = "20:00",
                durationMinutes = 30,
                isCurrent = false,
                description = "Daily closing recap and international trade indicators."
            ),
            EpgBlock(
                title = title1,
                startTime = channel.programStart ?: "20:00",
                endTime = channel.programEnd ?: "21:00",
                durationMinutes = 60,
                isCurrent = true,
                description = channel.description ?: "Main broadcast feature presentation."
            ),
            EpgBlock(
                title = title2,
                startTime = channel.programEnd ?: "21:00",
                endTime = "22:00",
                durationMinutes = 60,
                isCurrent = false,
                description = "Late evening coverage, debate analysis and interviews."
            ),
            EpgBlock(
                title = "Midnight Feature & Archive",
                startTime = "22:00",
                endTime = "23:30",
                durationMinutes = 90,
                isCurrent = false,
                description = "Overnight cultural broadcast and classic retrospectives."
            )
        )
    }
}
