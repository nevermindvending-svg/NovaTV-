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

class NovaTvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlaylistRepository(application.applicationContext)

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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _playerError = MutableStateFlow<String?>(null)
    val playerError: StateFlow<String?> = _playerError.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _customPlaylistUrl = MutableStateFlow(repository.getCustomPlaylistUrl())
    val customPlaylistUrl: StateFlow<String> = _customPlaylistUrl.asStateFlow()

    // Filtered channels based on category and search query
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
                channel.category.contains(query, ignoreCase = true)
            }

            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadChannels()
    }

    fun loadChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            _playerError.value = null
            try {
                val loaded = repository.loadAllChannels()
                _allChannels.value = loaded

                // Build unique dynamic categories list
                val categorySet = linkedSetOf("ALL", "FAVORITES")
                loaded.forEach { ch ->
                    if (ch.category.isNotBlank() && ch.category != "ALL" && ch.category != "FAVORITES") {
                        categorySet.add(ch.category)
                    }
                }
                _categories.value = categorySet.toList()

                // Auto-select first channel if none is currently selected
                if (_selectedChannel.value == null && loaded.isNotEmpty()) {
                    _selectedChannel.value = loaded.first()
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
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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

        // Also update selectedChannel reference if it's the one toggled
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

    fun retryCurrentChannel() {
        val current = _selectedChannel.value
        if (current != null) {
            _playerError.value = null
            _isBuffering.value = true
            // Trigger state change
            _selectedChannel.value = null
            _selectedChannel.value = current
        }
    }

    fun saveCustomPlaylist(url: String) {
        repository.setCustomPlaylistUrl(url)
        _customPlaylistUrl.value = url
        loadChannels()
    }
}
