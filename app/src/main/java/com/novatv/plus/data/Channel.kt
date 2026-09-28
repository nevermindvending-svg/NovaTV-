package com.novatv.plus.data

/**
 * Data model representing a television channel.
 */
data class Channel(
    val id: Int,                        // Channel ID starting at 101 (101, 102, 103...)
    val name: String,                   // Display name of the channel
    val logoUrl: String? = null,        // Channel logo URL from tvg-logo
    val category: String = "GENERAL",   // Category from group-title
    val streamUrl: String,              // Live HLS or media stream URL (.m3u8, etc.)
    val tvgId: String? = null,          // EPG channel identifier
    val tvgName: String? = null,        // EPG channel name
    val isLive: Boolean = true,         // LIVE broadcast indicator
    val programTitle: String? = null,   // Current program title (EPG or placeholder)
    val programStart: String? = null,   // Program start time (e.g., "20:00")
    val programEnd: String? = null,     // Program end time (e.g., "21:00")
    val nextProgramTitle: String? = null,// Next program title
    val description: String? = null,    // Program synopsis or channel summary
    val isFavorite: Boolean = false     // User favorite status
)

/**
 * Program guide / EPG information.
 */
data class ProgramGuide(
    val currentTitle: String,
    val startTime: String,
    val endTime: String,
    val nextTitle: String,
    val nextTime: String,
    val description: String
)
