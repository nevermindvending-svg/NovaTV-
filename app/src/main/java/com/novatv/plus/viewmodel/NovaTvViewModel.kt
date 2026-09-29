package com.novatv.plus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novatv.plus.data.Channel
import com.novatv.plus.data.PlaylistRepository
import com.novatv.plus.data.epg.EpgEngine
import com.novatv.plus.data.epg.EpgTimelineSlot
import com.novatv.plus.data.epg.RealEpgProgram
import kotlinx.coroutines.Dispatchers
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
    private val _isLoading = MutableStateFlow(false)
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

    // Real EPG Timeline Slots
    private val _epgTimeline = MutableStateFlow<List<EpgTimelineSlot>>(EpgEngine.getTimelineSlots())
    val epgTimeline: StateFlow<List<EpgTimelineSlot>> = _epgTimeline.asStateFlow()

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
        observeFavoritesFromRoom()
    }

    private fun observeFavoritesFromRoom() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getFavoritesFlow().collect { favList ->
                val favUrls = favList.map { it.streamUrl }.toSet()
                _allChannels.value = _allChannels.value.map { ch ->
                    ch.copy(isFavorite = favUrls.contains(ch.streamUrl))
                }
                _selectedChannel.value?.let { current ->
                    _selectedChannel.value = current.copy(isFavorite = favUrls.contains(current.streamUrl))
                }
            }
        }
    }

    fun navigateTo(destination: AppDestination) {
        _currentDestination.value = destination
        if (destination == AppDestination.EPG) {
            refreshEpgTimeline()
        }
    }

    fun refreshEpgTimeline() {
        _epgTimeline.value = EpgEngine.getTimelineSlots()
    }

    fun loadChannels() {
        viewModelScope.launch {
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
                            remainingMinutes = 24 - (idx * 5)
                        )
                    }
                }
            } catch (e: Exception) {
                _playerError.value = "Failed to load playlists: ${e.localizedMessage ?: "Unknown error"}"
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
            updatedRecent.removeAll { it.streamUrl == channel.streamUrl }
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
        val target = _allChannels.value.find { it.id == channelId } ?: return
        toggleFavorite(target)
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch(Dispatchers.IO) {
            val isNowFavorite = repository.toggleFavorite(channel)
            // Immediately update memory state for instant UI response
            _allChannels.value = _allChannels.value.map { ch ->
                if (ch.streamUrl == channel.streamUrl || ch.id == channel.id) {
                    ch.copy(isFavorite = isNowFavorite)
                } else {
                    ch
                }
            }
            if (_selectedChannel.value?.streamUrl == channel.streamUrl) {
                _selectedChannel.value = _selectedChannel.value?.copy(isFavorite = isNowFavorite)
            }
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
     * Delegates to Real EPG Engine to calculate dynamic programs tailored to channel genre.
     */
    fun getEpgPrograms(channel: Channel): List<RealEpgProgram> {
        return EpgEngine.getProgramsForChannel(channel)
    }
}
