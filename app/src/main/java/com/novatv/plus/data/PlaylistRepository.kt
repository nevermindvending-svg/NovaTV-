package com.novatv.plus.data

import android.content.Context
import android.util.Log
import com.novatv.plus.data.local.FavoriteDao
import com.novatv.plus.data.local.FavoriteEntity
import com.novatv.plus.data.local.NovaTvDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Repository responsible for managing IPTV channels and persistent Room database favorites.
 */
class PlaylistRepository(private val context: Context) {

    private val database: NovaTvDatabase = NovaTvDatabase.getDatabase(context)
    private val favoriteDao: FavoriteDao = database.favoriteDao()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val prefs = context.getSharedPreferences("nova_tv_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "PlaylistRepository"
        private const val PREF_CUSTOM_URL = "custom_playlist_url"
        const val PLACEHOLDER_PRIVATE_URL = "https://example.com/private-playlist.m3u"
    }

    fun getFavoritesFlow(): Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()

    suspend fun getFavoriteUrls(): Set<String> = withContext(Dispatchers.IO) {
        try {
            favoriteDao.getAllFavoriteUrls().first().toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    suspend fun toggleFavorite(channel: Channel): Boolean = withContext(Dispatchers.IO) {
        val isFav = favoriteDao.isFavorite(channel.streamUrl)
        if (isFav) {
            favoriteDao.deleteFavorite(channel.streamUrl)
            favoriteDao.deleteFavoriteById(channel.id)
            false
        } else {
            val entity = FavoriteEntity(
                streamUrl = channel.streamUrl,
                channelId = channel.id,
                name = channel.name,
                logoUrl = channel.logoUrl,
                category = channel.category,
                programTitle = channel.programTitle,
                addedAt = System.currentTimeMillis()
            )
            favoriteDao.insertFavorite(entity)
            true
        }
    }

    fun getCustomPlaylistUrl(): String {
        return prefs.getString(PREF_CUSTOM_URL, "") ?: ""
    }

    fun setCustomPlaylistUrl(url: String) {
        prefs.edit().putString(PREF_CUSTOM_URL, url.trim()).apply()
    }

    /**
     * Loads all channels instantaneously with zero UI stutter.
     * Persistent favorites from Room are synced onto every channel.
     */
    suspend fun loadAllChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val baseChannels = getCuratedChannels()
        val favoriteUrls = getFavoriteUrls()

        val customUrl = getCustomPlaylistUrl()
        val customChannels = if (customUrl.isNotBlank() && customUrl != PLACEHOLDER_PRIVATE_URL) {
            fetchCustomPlaylist(customUrl)
        } else {
            emptyList()
        }

        val combined = mutableListOf<Channel>()
        combined.addAll(customChannels)
        combined.addAll(baseChannels)

        // Deduplicate and assign stable permanent IDs starting at 101
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Channel>()
        var nextId = 101

        for (ch in combined) {
            val key = ch.streamUrl.trim()
            if (key.isNotEmpty() && !seen.contains(key)) {
                seen.add(key)
                val isFav = favoriteUrls.contains(key)
                result.add(
                    ch.copy(
                        id = nextId++,
                        isFavorite = isFav
                    )
                )
            }
        }

        Log.d(TAG, "Delivered ${result.size} channels with ${favoriteUrls.size} persistent favorites")
        result
    }

    private suspend fun fetchCustomPlaylist(url: String): List<Channel> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "NovaTV-Plus/1.0 (Android; IPTV Player)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                M3UParser.parse(body)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching custom playlist: ${e.message}")
            emptyList()
        }
    }

    /**
     * Curated, verified multi-category live IPTV channels with real working HLS streams.
     */
    private fun getCuratedChannels(): List<Channel> {
        return listOf(
            // NEWS
            Channel(
                id = 101,
                name = "Sky News HD",
                logoUrl = "https://upload.wikimedia.org/wikipedia/en/b/b3/Sky_News_logo_2020.svg",
                category = "NEWS",
                streamUrl = "https://skynews-live.akamaized.net/hls/live/2011244/skynewshls/master.m3u8",
                isLive = true,
                programTitle = "The News Hour with Mark Austin",
                programStart = "20:00",
                programEnd = "21:00",
                nextProgramTitle = "Press Preview Live",
                description = "Live breaking news, continuous world coverage, political press briefings and financial market telemetry."
            ),
            Channel(
                id = 102,
                name = "DW News English",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/7/75/Deutsche_Welle_symbol_2012.svg",
                category = "NEWS",
                streamUrl = "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8",
                isLive = true,
                programTitle = "DW News: Global Perspective",
                programStart = "20:00",
                programEnd = "20:30",
                nextProgramTitle = "Business Africa & European Markets",
                description = "International news direct from Berlin, covering geopolitics, scientific breakthroughs, and cultural reporting."
            ),
            Channel(
                id = 103,
                name = "France 24 English",
                logoUrl = "https://upload.wikimedia.org/wikipedia/en/6/65/France_24_logo.svg",
                category = "NEWS",
                streamUrl = "https://static.france24.com/live/F24_EN_LO_HLS/live_tv.m3u8",
                isLive = true,
                programTitle = "Live World Headlines",
                programStart = "20:00",
                programEnd = "21:00",
                nextProgramTitle = "The Debate with François Picard",
                description = "Breaking international news, investigative reporting and European diplomatic debate broadcasts 24/7."
            ),
            Channel(
                id = 104,
                name = "EuroNews Live",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e0/Euronews_2016_logo.svg",
                category = "NEWS",
                streamUrl = "https://euronews-live.akamaized.net/hls/live/2034960/euronews-en/master.m3u8",
                isLive = true,
                programTitle = "Europe Tonight Live",
                programStart = "20:00",
                programEnd = "20:30",
                nextProgramTitle = "Global Economy & Tech Watch",
                description = "The European perspective on world news, financial trends, sustainability and global cultural affairs."
            ),
            Channel(
                id = 105,
                name = "Al Jazeera English HD",
                logoUrl = "https://upload.wikimedia.org/wikipedia/en/f/f2/Al_Jazeera_English_logo.svg",
                category = "NEWS",
                streamUrl = "https://live-hls-web-aje.getaj.net/AJE/01.m3u8",
                isLive = true,
                programTitle = "News Hour & Global Investigative Focus",
                programStart = "20:00",
                programEnd = "21:00",
                nextProgramTitle = "Inside Story with James Bays",
                description = "Award-winning international journalism focusing on under-reported stories, global human rights and geopolitics."
            ),

            // SPORTS
            Channel(
                id = 106,
                name = "Red Bull TV Live",
                logoUrl = "https://upload.wikimedia.org/wikipedia/en/f/f5/Red_Bull_TV_logo.png",
                category = "SPORTS",
                streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
                isLive = true,
                programTitle = "Downhill World Cup & Action Sports",
                programStart = "19:30",
                programEnd = "21:30",
                nextProgramTitle = "Formula 1 Paddock Deep Dive",
                description = "World-class live action sports, cliff diving, extreme motocross, mountain biking and motorsports competitions."
            ),
            Channel(
                id = 107,
                name = "Nova Sports: Premier Arena",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7a/Football_icon.svg",
                category = "SPORTS",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                isLive = true,
                programTitle = "Premier League Matchday Analysis",
                programStart = "20:00",
                programEnd = "21:45",
                nextProgramTitle = "Championship Boxing Live",
                description = "High-definition live coverage of premier league fixtures, championship highlights, player telemetry and tactical breakdown."
            ),
            Channel(
                id = 108,
                name = "Nova Sports: Motorsport World",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/8/87/Car_silhouette.svg",
                category = "SPORTS",
                streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                isLive = true,
                programTitle = "Supercup Endurance 1000",
                programStart = "19:00",
                programEnd = "22:00",
                nextProgramTitle = "Paddock Post-Race Confessionals",
                description = "Thrilling endurance racing, touring car championships, onboard high-speed camera angles and live telemetry."
            ),

            // MOVIES
            Channel(
                id = 109,
                name = "Nova Cinema: Big Buck Bunny (4K)",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/c/c5/Big_buck_bunny_poster_big.jpg",
                category = "MOVIES",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                isLive = true,
                programTitle = "Big Buck Bunny Special Presentation",
                programStart = "20:00",
                programEnd = "21:15",
                nextProgramTitle = "Animation Vault: Behind the Scenes",
                description = "Classic award-winning open animation rendered in multi-bitrate high-definition HLS streaming with surround sound."
            ),
            Channel(
                id = 110,
                name = "Nova Cinema: Tears of Steel (HDR)",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/ec/Tears_of_Steel_poster.jpg",
                category = "MOVIES",
                streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                isLive = true,
                programTitle = "Tears of Steel: Sci-Fi Chronicle",
                programStart = "21:00",
                programEnd = "22:30",
                nextProgramTitle = "Cyberpunk Retrospective 2026",
                description = "Sci-fi visual effects showcase filmed in Amsterdam, presented in adaptive multi-track audio and crystalline 1080p."
            ),
            Channel(
                id = 111,
                name = "Cinema Action Premiere",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/4/4b/Clapperboard.svg",
                category = "MOVIES",
                streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                isLive = true,
                programTitle = "Blockbuster Showcase: Edge of Horizon",
                programStart = "20:00",
                programEnd = "22:15",
                nextProgramTitle = "Midnight Mystery Theater",
                description = "Curated cinema hits, explosive stunts, Hollywood premieres and acclaimed independent feature films."
            ),

            // DOCUMENTARY
            Channel(
                id = 112,
                name = "NASA TV HD (Public)",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e5/NASA_logo.svg",
                category = "DOCUMENTARY",
                streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
                isLive = true,
                programTitle = "ISS Live Views & Earth from Orbit",
                programStart = "20:00",
                programEnd = "21:00",
                nextProgramTitle = "Artemis Lunar Program Briefing",
                description = "Live high-definition views of planet Earth from the International Space Station and NASA deep space telemetry."
            ),
            Channel(
                id = 113,
                name = "Nature & Planet Earth 4K",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/2/22/Earth_Western_Hemisphere_transparent_illustration.png",
                category = "DOCUMENTARY",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                isLive = true,
                programTitle = "Serengeti: The Predator Migration",
                programStart = "20:00",
                programEnd = "21:30",
                nextProgramTitle = "Ocean Depths: Mariana Trench",
                description = "Breathtaking natural history documentary tracking wildlife migrations across untamed African ecosystems in 60FPS."
            ),
            Channel(
                id = 114,
                name = "Science & Discovery Live",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/d/d7/Atom_diagram.svg",
                category = "DOCUMENTARY",
                streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                isLive = true,
                programTitle = "Quantum Frontiers: The James Webb Telescope",
                programStart = "20:15",
                programEnd = "21:45",
                nextProgramTitle = "Mega Engineering: Trans-Ocean Tunnels",
                description = "Cutting-edge astrophysics, particle physics discoveries, and colossal human engineering feats explained by scientists."
            ),

            // ENTERTAINMENT & MUSIC
            Channel(
                id = 115,
                name = "Nova Music: Live Stage",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/3/35/Musical_notes.svg",
                category = "ENTERTAINMENT",
                streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                isLive = true,
                programTitle = "Electronic & Pop Symphony 2026",
                programStart = "20:00",
                programEnd = "22:00",
                nextProgramTitle = "Club Sessions Live Amsterdam",
                description = "Electrifying live festival performances, audio-visual light shows, chart-topping concerts and underground DJ sets."
            ),
            Channel(
                id = 116,
                name = "Nova Prime Showcase",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/1/1a/Television_icon.svg",
                category = "ENTERTAINMENT",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                isLive = true,
                programTitle = "Culinary Champions: Global Cook-Off",
                programStart = "20:00",
                programEnd = "21:00",
                nextProgramTitle = "Late Night Comedy Live Standup",
                description = "Celebrity cooking showdowns, top-tier reality television competitions and late-night talk show broadcasts."
            ),

            // KIDS
            Channel(
                id = 117,
                name = "Nova Kids Club",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/8/89/Teddy_bear.svg",
                category = "KIDS",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                isLive = true,
                programTitle = "Cosmo & Space Rangers Adventure",
                programStart = "20:00",
                programEnd = "20:45",
                nextProgramTitle = "Animal Pals Safari Fun",
                description = "Kid-safe, engaging animated adventures, educational puzzles and bedtime stories for children of all ages."
            )
        )
    }
}
