package com.novatv.plus.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Repository responsible for concurrently fetching, parsing, and deduplicating M3U playlists.
 */
class PlaylistRepository(private val context: Context) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val prefs = context.getSharedPreferences("nova_tv_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "PlaylistRepository"
        private const val PREF_CUSTOM_URL = "custom_playlist_url"
        private const val PREF_FAVORITES = "favorite_channel_ids"

        /**
         * Security Notice:
         * Playlist 12 is a configurable user slot. Do NOT hardcode private tokens.
         * The user can change this URL via the in-app "Playlists" dialog or SharedPreferences.
         */
        const val PLACEHOLDER_PRIVATE_URL = "https://example.com/private-playlist.m3u"

        // 12 Playlist sources loaded concurrently
        val DEFAULT_PLAYLIST_SOURCES = listOf(
            // 1. Global Public News (DW, France24, Sky News, EuroNews, Al Jazeera, etc.)
            "https://iptv-org.github.io/iptv/categories/news.m3u",
            // 2. Global Sports & Motorsports (Red Bull TV, outdoor, racing)
            "https://iptv-org.github.io/iptv/categories/sports.m3u",
            // 3. Movies & Cinema Streams
            "https://iptv-org.github.io/iptv/categories/movies.m3u",
            // 4. Entertainment & Series
            "https://iptv-org.github.io/iptv/categories/entertainment.m3u",
            // 5. Kids & Animation
            "https://iptv-org.github.io/iptv/categories/kids.m3u",
            // 6. Music & Concerts
            "https://iptv-org.github.io/iptv/categories/music.m3u",
            // 7. Documentaries & Science (NASA TV, Nature, Earth)
            "https://iptv-org.github.io/iptv/categories/documentary.m3u",
            // 8. Technology & Gaming
            "https://iptv-org.github.io/iptv/categories/tech.m3u",
            // 9. Classic Cinema & Retro TV
            "https://iptv-org.github.io/iptv/categories/classic.m3u",
            // 10. Weather & Radar 24/7
            "https://iptv-org.github.io/iptv/categories/weather.m3u",
            // 11. Culture & Lifestyle
            "https://iptv-org.github.io/iptv/categories/lifestyle.m3u",
            // 12. Configurable / Private User Playlist (No hardcoded credentials)
            PLACEHOLDER_PRIVATE_URL
        )
    }

    fun getCustomPlaylistUrl(): String {
        return prefs.getString(PREF_CUSTOM_URL, "") ?: ""
    }

    fun setCustomPlaylistUrl(url: String) {
        prefs.edit().putString(PREF_CUSTOM_URL, url.trim()).apply()
    }

    fun getFavoriteIds(): Set<Int> {
        val stringSet = prefs.getStringSet(PREF_FAVORITES, emptySet()) ?: emptySet()
        return stringSet.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun toggleFavorite(channelId: Int): Set<Int> {
        val current = getFavoriteIds().toMutableSet()
        if (current.contains(channelId)) {
            current.remove(channelId)
        } else {
            current.add(channelId)
        }
        prefs.edit().putStringSet(PREF_FAVORITES, current.map { it.toString() }.toSet()).apply()
        return current
    }

    /**
     * Concurrently loads all 12 playlists, deduplicates stream URLs,
     * and assigns sequential channel IDs starting at 101.
     */
    suspend fun loadAllChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val activeSources = DEFAULT_PLAYLIST_SOURCES.toMutableList()

        // Replace slot 12 with user-configured URL if provided
        val userCustomUrl = getCustomPlaylistUrl()
        if (userCustomUrl.isNotBlank() && userCustomUrl != PLACEHOLDER_PRIVATE_URL) {
            activeSources[11] = userCustomUrl
        }

        // Concurrently fetch each playlist using coroutines async/awaitAll
        val parsedChannelLists: List<List<Channel>> = coroutineScope {
            activeSources.map { sourceUrl ->
                async {
                    fetchAndParsePlaylist(sourceUrl)
                }
            }.awaitAll()
        }

        // Flatten all lists
        val allRawChannels = mutableListOf<Channel>()
        for (list in parsedChannelLists) {
            allRawChannels.addAll(list)
        }

        // If network was unavailable or all playlists were empty, provide fallback channels
        if (allRawChannels.isEmpty()) {
            allRawChannels.addAll(getBuiltInFallbackChannels())
        }

        // Deduplication: remove duplicate stream URLs, keeping first valid metadata
        val seenUrls = mutableSetOf<String>()
        val deduplicated = mutableListOf<Channel>()
        val favorites = getFavoriteIds()

        var nextChannelId = 101 // Channel IDs MUST start at 101

        for (channel in allRawChannels) {
            val normalizedUrl = channel.streamUrl.trim()
            if (normalizedUrl.isNotEmpty() && !seenUrls.contains(normalizedUrl)) {
                seenUrls.add(normalizedUrl)
                deduplicated.add(
                    channel.copy(
                        id = nextChannelId++,
                        isFavorite = favorites.contains(nextChannelId - 1)
                    )
                )
            }
        }

        Log.d(TAG, "Loaded and deduplicated ${deduplicated.size} channels starting at ID 101")
        return@withContext deduplicated
    }

    /**
     * Safely fetches a single M3U playlist from HTTP/HTTPS or local assets without crashing.
     */
    private suspend fun fetchAndParsePlaylist(url: String): List<Channel> {
        // Skip placeholder URL without throwing errors
        if (url == PLACEHOLDER_PRIVATE_URL || url.isBlank()) {
            return emptyList()
        }

        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "NovaTV-Plus/1.0 (Android; IPTV Player)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Failed to load playlist: HTTP ${response.code} from $url")
                    return emptyList()
                }

                val bodyString = response.body?.string()
                if (bodyString.isNullOrBlank()) {
                    return emptyList()
                }

                // Parse with M3UParser
                M3UParser.parse(bodyString)
            }
        } catch (e: IOException) {
            Log.w(TAG, "Network error fetching playlist from $url: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing playlist from $url", e)
            emptyList()
        }
    }

    /**
     * Guaranteed reliable public HLS test and public broadcasting streams.
     * Ensures the player is immediately functional even in restricted emulator environments.
     */
    fun getBuiltInFallbackChannels(): List<Channel> {
        return listOf(
            Channel(
                id = 0,
                name = "NASA TV HD (Public)",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e5/NASA_logo.svg",
                category = "DOCUMENTARY",
                streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
                isLive = true,
                programTitle = "ISS Live Views & Earth from Orbit",
                programStart = "20:00",
                programEnd = "21:00",
                nextProgramTitle = "Mars Rover Discoveries",
                description = "Live high-definition views of planet Earth from the International Space Station and NASA deep space briefings."
            ),
            Channel(
                id = 0,
                name = "DW News English",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/7/75/Deutsche_Welle_symbol_2012.svg",
                category = "NEWS",
                streamUrl = "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8",
                isLive = true,
                programTitle = "DW News: Global Perspective",
                programStart = "20:00",
                programEnd = "20:30",
                nextProgramTitle = "Business Africa & Tech News",
                description = "Comprehensive global news broadcasting direct from Berlin, covering geopolitics, business, science and culture."
            ),
            Channel(
                id = 0,
                name = "France 24 English",
                logoUrl = "https://upload.wikimedia.org/wikipedia/en/6/65/France_24_logo.svg",
                category = "NEWS",
                streamUrl = "https://static.france24.com/live/F24_EN_LO_HLS/live_tv.m3u8",
                isLive = true,
                programTitle = "Live World Headlines",
                programStart = "20:00",
                programEnd = "21:00",
                nextProgramTitle = "The Debate with François Picard",
                description = "Breaking international news, analysis and European debates broadcasting 24/7."
            ),
            Channel(
                id = 0,
                name = "Red Bull TV Live",
                logoUrl = "https://upload.wikimedia.org/wikipedia/en/f/f5/Red_Bull_TV_logo.png",
                category = "SPORTS",
                streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
                isLive = true,
                programTitle = "Extreme Action Sports & F1",
                programStart = "19:30",
                programEnd = "21:30",
                nextProgramTitle = "Downhill World Cup",
                description = "High-adrenaline action sports, cliff diving, motocross, Formula 1 racing insights and world-class live competitions."
            ),
            Channel(
                id = 0,
                name = "Nova Cinema: Big Buck Bunny (HLS)",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/c/c5/Big_buck_bunny_poster_big.jpg",
                category = "MOVIES",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                isLive = true,
                programTitle = "Big Buck Bunny Special Presentation",
                programStart = "20:00",
                programEnd = "21:15",
                nextProgramTitle = "Animation Highlights",
                description = "Classic award-winning open animation rendered in multi-bitrate high-definition HLS streaming."
            ),
            Channel(
                id = 0,
                name = "Nova Cinema: Tears of Steel (HLS)",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/ec/Tears_of_Steel_poster.jpg",
                category = "MOVIES",
                streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                isLive = true,
                programTitle = "Tears of Steel: Sci-Fi Chronicle",
                programStart = "21:00",
                programEnd = "22:30",
                nextProgramTitle = "Cosmic Odyssey",
                description = "Sci-fi visual effects showcase filmed in Amsterdam, presented in adaptive multi-track audio."
            ),
            Channel(
                id = 0,
                name = "Nova Entertainment: Tears of Steel 4K",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/ec/Tears_of_Steel_poster.jpg",
                category = "ENTERTAINMENT",
                streamUrl = "https://dash.akamaized.net/dash2pr/features/bigbuckbunny/bigbuckbunny-on-demand.mpd",
                isLive = true,
                programTitle = "Evening Variety Special",
                programStart = "20:00",
                programEnd = "21:00",
                nextProgramTitle = "Late Night Cinema",
                description = "Primetime entertainment showcase featuring music, comedy, and cultural specials."
            ),
            Channel(
                id = 0,
                name = "Nova Kids: Sintel Animation",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/8/8f/Sintel_poster.jpg",
                category = "KIDS",
                streamUrl = "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
                isLive = true,
                programTitle = "Sintel: The Dragon Quest",
                programStart = "19:00",
                programEnd = "20:00",
                nextProgramTitle = "Toon Galaxy",
                description = "A young hero embarks on a grand fantasy journey across perilous mountain peaks and forgotten kingdoms."
            ),
            Channel(
                id = 0,
                name = "Sky News Live UK",
                logoUrl = "https://upload.wikimedia.org/wikipedia/en/b/b3/Sky_News_logo_2020.svg",
                category = "NEWS",
                streamUrl = "https://skynews-live.akamaized.net/hls/live/2011244/skynewshls/master.m3u8",
                isLive = true,
                programTitle = "The News Hour with Mark Austin",
                programStart = "20:00",
                programEnd = "21:00",
                nextProgramTitle = "Press Preview",
                description = "Non-stop breaking news, political investigations, live press conferences and sports analysis."
            ),
            Channel(
                id = 0,
                name = "EuroNews English",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e0/Euronews_2016_logo.svg",
                category = "NEWS",
                streamUrl = "https://euronews-live.akamaized.net/hls/live/2034960/euronews-en/master.m3u8",
                isLive = true,
                programTitle = "Europe Tonight Live",
                programStart = "20:00",
                programEnd = "20:30",
                nextProgramTitle = "Global Economy Watch",
                description = "European perspective on world news, cultural happenings and financial markets."
            ),
            Channel(
                id = 0,
                name = "Bloomberg Live TV",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/5/5e/Bloomberg_Television_logo.svg",
                category = "NEWS",
                streamUrl = "https://liveproduseast.akamaized.net/us/Channel-USTV-AWS-virginia-1/master.m3u8",
                isLive = true,
                programTitle = "Bloomberg Technology & Markets",
                programStart = "20:00",
                programEnd = "21:00",
                nextProgramTitle = "Wall Street Week",
                description = "Global financial markets, technology giants, cryptocurrency movements and CEO interviews."
            ),
            Channel(
                id = 0,
                name = "Nova Music: Live Stage",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/3/35/Musical_notes.svg",
                category = "MUSIC",
                streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                isLive = true,
                programTitle = "Electronic & Pop Symphony",
                programStart = "20:00",
                programEnd = "22:00",
                nextProgramTitle = "Club Nights Live",
                description = "Electrifying live festival performances, audio-visual light shows, and chart-topping concerts."
            )
        )
    }
}
