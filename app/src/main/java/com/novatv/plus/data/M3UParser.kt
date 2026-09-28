package com.novatv.plus.data

import java.io.BufferedReader
import java.io.StringReader
import java.util.regex.Pattern

/**
 * Robust parser for M3U / M3U8 playlists.
 * Handles standard #EXTINF format variations and ignores corrupt lines.
 */
object M3UParser {

    private val TVG_ID_PATTERN = Pattern.compile("tvg-id=[\"']?([^\"',]*)[\"']?", Pattern.CASE_INSENSITIVE)
    private val TVG_NAME_PATTERN = Pattern.compile("tvg-name=[\"']?([^\"',]*)[\"']?", Pattern.CASE_INSENSITIVE)
    private val TVG_LOGO_PATTERN = Pattern.compile("tvg-logo=[\"']?([^\"',]*)[\"']?", Pattern.CASE_INSENSITIVE)
    private val GROUP_TITLE_PATTERN = Pattern.compile("group-title=[\"']?([^\"',]*)[\"']?", Pattern.CASE_INSENSITIVE)

    /**
     * Parses raw M3U text into a list of channels.
     * Note: IDs are assigned during deduplication in PlaylistRepository.
     */
    fun parse(m3uContent: String, defaultCategory: String = "GENERAL"): List<Channel> {
        val channels = mutableListOf<Channel>()
        val reader = BufferedReader(StringReader(m3uContent))

        var currentTvgId: String? = null
        var currentTvgName: String? = null
        var currentTvgLogo: String? = null
        var currentGroupTitle: String? = null
        var currentChannelName: String? = null

        var line = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()

            if (trimmed.isEmpty()) {
                line = reader.readLine()
                continue
            }

            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                // Parse attributes
                currentTvgId = extractAttribute(TVG_ID_PATTERN, trimmed)
                currentTvgName = extractAttribute(TVG_NAME_PATTERN, trimmed)
                currentTvgLogo = extractAttribute(TVG_LOGO_PATTERN, trimmed)
                currentGroupTitle = extractAttribute(GROUP_TITLE_PATTERN, trimmed)

                // Channel title is after the last comma
                val commaIndex = trimmed.lastIndexOf(',')
                currentChannelName = if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                    trimmed.substring(commaIndex + 1).trim()
                } else {
                    currentTvgName ?: "Live Stream"
                }

                if (currentChannelName.isEmpty()) {
                    currentChannelName = currentTvgName ?: "Live Channel"
                }
            } else if (!trimmed.startsWith("#")) {
                // This is the stream URL
                if (trimmed.startsWith("http://", ignoreCase = true) ||
                    trimmed.startsWith("https://", ignoreCase = true) ||
                    trimmed.startsWith("rtmp://", ignoreCase = true) ||
                    trimmed.startsWith("rtsp://", ignoreCase = true)
                ) {
                    val name = currentChannelName ?: currentTvgName ?: "Channel"
                    val category = normalizeCategory(currentGroupTitle ?: defaultCategory)
                    val program = generateMockProgram(name, category)

                    val channel = Channel(
                        id = 0, // Assigned later
                        name = name,
                        logoUrl = currentTvgLogo?.takeIf { it.isNotBlank() },
                        category = category,
                        streamUrl = trimmed,
                        tvgId = currentTvgId,
                        tvgName = currentTvgName,
                        isLive = true,
                        programTitle = program.currentTitle,
                        programStart = program.startTime,
                        programEnd = program.endTime,
                        nextProgramTitle = program.nextTitle,
                        description = program.description
                    )
                    channels.add(channel)
                }

                // Reset per-channel fields
                currentTvgId = null
                currentTvgName = null
                currentTvgLogo = null
                currentGroupTitle = null
                currentChannelName = null
            }

            line = reader.readLine()
        }

        return channels
    }

    private fun extractAttribute(pattern: Pattern, line: String): String? {
        val matcher = pattern.matcher(line)
        return if (matcher.find()) {
            matcher.group(1)?.trim()?.takeIf { it.isNotEmpty() }
        } else {
            null
        }
    }

    private fun normalizeCategory(raw: String): String {
        val upper = raw.trim().uppercase()
        return when {
            upper.contains("NEWS") -> "NEWS"
            upper.contains("SPORT") -> "SPORTS"
            upper.contains("MOVIE") || upper.contains("CINEMA") || upper.contains("FILM") -> "MOVIES"
            upper.contains("KID") || upper.contains("ANIMATION") || upper.contains("CARTOON") -> "KIDS"
            upper.contains("MUSIC") -> "MUSIC"
            upper.contains("DOC") || upper.contains("SCIENCE") || upper.contains("NATURE") -> "DOCUMENTARY"
            upper.contains("ENTERTAIN") || upper.contains("SERIES") || upper.contains("COMEDY") -> "ENTERTAINMENT"
            upper.isEmpty() -> "GENERAL"
            else -> upper.take(16)
        }
    }

    /**
     * Generates realistic EPG data for IPTV display when real XMLTV EPG is not attached.
     */
    fun generateMockProgram(channelName: String, category: String): ProgramGuide {
        val cat = category.uppercase()
        return when {
            cat.contains("NEWS") -> ProgramGuide(
                currentTitle = "Global Newsroom Live",
                startTime = "20:00",
                endTime = "21:00",
                nextTitle = "World in Perspective & Economy",
                nextTime = "21:00",
                description = "Live 24/7 breaking updates, exclusive foreign correspondent reports and real-time financial market insights."
            )
            cat.contains("SPORT") -> ProgramGuide(
                currentTitle = "Championship Live Arena",
                startTime = "19:30",
                endTime = "21:30",
                nextTitle = "Post-Game Analysis & Press Conference",
                nextTime = "21:30",
                description = "High-definition live coverage of premier sporting events, expert tactical analysis, and player interviews."
            )
            cat.contains("MOVIE") || cat.contains("CINEMA") -> ProgramGuide(
                currentTitle = "Primetime Feature Film",
                startTime = "20:00",
                endTime = "22:15",
                nextTitle = "Late Night Action Thriller",
                nextTime = "22:15",
                description = "Award-winning cinematic presentation in digital multi-channel audio and remastered high definition."
            )
            cat.contains("KID") || cat.contains("ANIMATION") -> ProgramGuide(
                currentTitle = "Animated Galaxy Adventures",
                startTime = "19:00",
                endTime = "20:00",
                nextTitle = "The Secret Toon Squad",
                nextTime = "20:00",
                description = "Entertaining and vibrant animated series packed with humor, positive messages, and excitement for all ages."
            )
            cat.contains("MUSIC") -> ProgramGuide(
                currentTitle = "Global Chart Top 50 Countdown",
                startTime = "20:00",
                endTime = "21:00",
                nextTitle = "Unplugged Live Studio Sessions",
                nextTime = "21:00",
                description = "Non-stop hit music videos, acoustic live stage performances, and backstage interviews with global superstars."
            )
            cat.contains("DOC") -> ProgramGuide(
                currentTitle = "Wonders of the Cosmos & Deep Oceans",
                startTime = "20:00",
                endTime = "21:00",
                nextTitle = "Engineering Extreme Megastructures",
                nextTime = "21:00",
                description = "Breathtaking natural history and scientific discoveries filmed on location across all seven continents."
            )
            else -> ProgramGuide(
                currentTitle = "Evening Showcase: $channelName",
                startTime = "20:00",
                endTime = "21:00",
                nextTitle = "Nightly Spotlight",
                nextTime = "21:00",
                description = "Top-tier entertainment broadcasting live featuring cultural programming, features, and special guest panels."
            )
        }
    }
}
