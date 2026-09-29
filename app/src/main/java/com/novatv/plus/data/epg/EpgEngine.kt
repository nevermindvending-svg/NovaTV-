package com.novatv.plus.data.epg

import com.novatv.plus.data.Channel
import java.util.Calendar
import java.util.Locale

data class RealEpgProgram(
    val id: String,
    val title: String,
    val category: String,
    val startTime: String,
    val endTime: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val durationMinutes: Int,
    val isCurrent: Boolean,
    val progress: Float, // 0.0f to 1.0f for currently playing program
    val remainingMinutes: Int,
    val description: String,
    val rating: String = "TV-14",
    val qualityBadge: String = "1080p HD",
    val audioBadge: String = "Dolby 5.1"
)

data class EpgTimelineSlot(
    val timeLabel: String,
    val minuteOfDay: Int,
    val isCurrentSlot: Boolean
)

object EpgEngine {

    /**
     * Generates a 6-hour timeline ribbon centered around the current local device time.
     */
    fun getTimelineSlots(): List<EpgTimelineSlot> {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentMinuteOfDay = currentHour * 60 + currentMinute

        // Start 1.5 hours before current rounded half-hour
        val roundedHalfHour = if (currentMinute >= 30) currentHour * 60 + 30 else currentHour * 60
        val startMinute = (roundedHalfHour - 90).coerceAtLeast(0)

        val slots = mutableListOf<EpgTimelineSlot>()
        for (i in 0..11) { // 12 slots of 30 minutes = 6 hours total
            val minuteOfDay = (startMinute + i * 30) % (24 * 60)
            val h = minuteOfDay / 60
            val m = minuteOfDay % 60
            val isCurrent = currentMinuteOfDay in minuteOfDay..(minuteOfDay + 29)
            val label = String.format(Locale.US, "%02d:%02d%s", h, m, if (isCurrent) " (NOW)" else "")
            slots.add(EpgTimelineSlot(label, minuteOfDay, isCurrent))
        }
        return slots
    }

    /**
     * Generates realistic, contextual EPG programs for any channel tailored to its specific genre and name.
     */
    fun getProgramsForChannel(channel: Channel): List<RealEpgProgram> {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentMinuteOfDay = currentHour * 60 + currentMinute

        // Start schedule from 2 hours ago
        var cursorMinute = ((currentHour - 2) * 60).let { if (it < 0) it + 24 * 60 else it }

        val scheduleTemplates = getProgramTemplates(channel)
        val programs = mutableListOf<RealEpgProgram>()

        for ((index, template) in scheduleTemplates.withIndex()) {
            val startMin = cursorMinute % (24 * 60)
            val duration = template.durationMinutes
            val endMin = (cursorMinute + duration) % (24 * 60)

            // Determine if this program is currently on air
            val isCurrent = if (cursorMinute <= currentMinuteOfDay && (cursorMinute + duration) > currentMinuteOfDay) {
                true
            } else if (cursorMinute > (cursorMinute + duration)) { // Wraps past midnight
                currentMinuteOfDay >= cursorMinute || currentMinuteOfDay < endMin
            } else {
                false
            }

            val elapsed = (currentMinuteOfDay - cursorMinute).coerceAtLeast(0)
            val progress = if (isCurrent && duration > 0) {
                (elapsed.toFloat() / duration.toFloat()).coerceIn(0.05f, 0.95f)
            } else 0f

            val remaining = if (isCurrent) (duration - elapsed).coerceAtLeast(1) else 0

            val startH = startMin / 60
            val startM = startMin % 60
            val endH = endMin / 60
            val endM = endMin % 60

            val startTimeStr = String.format(Locale.US, "%02d:%02d", startH, startM)
            val endTimeStr = String.format(Locale.US, "%02d:%02d", endH, endM)

            programs.add(
                RealEpgProgram(
                    id = "${channel.id}_$index",
                    title = if (isCurrent && !channel.programTitle.isNullOrBlank()) channel.programTitle else template.title,
                    category = channel.category,
                    startTime = startTimeStr,
                    endTime = endTimeStr,
                    startMinuteOfDay = startMin,
                    endMinuteOfDay = endMin,
                    durationMinutes = duration,
                    isCurrent = isCurrent,
                    progress = progress,
                    remainingMinutes = remaining,
                    description = template.description,
                    rating = template.rating,
                    qualityBadge = template.quality,
                    audioBadge = template.audio
                )
            )

            cursorMinute += duration
            if (programs.size >= 6) break
        }

        // Guarantee at least one program is marked as current
        if (programs.none { it.isCurrent } && programs.size > 2) {
            val middleIndex = programs.size / 2
            val old = programs[middleIndex]
            programs[middleIndex] = old.copy(isCurrent = true, progress = 0.45f, remainingMinutes = 25)
        }

        return programs
    }

    private data class Template(
        val title: String,
        val durationMinutes: Int,
        val description: String,
        val rating: String = "TV-14",
        val quality: String = "1080p HD",
        val audio: String = "Dolby 5.1"
    )

    private fun getProgramTemplates(channel: Channel): List<Template> {
        val cat = channel.category.uppercase()
        val name = channel.name.uppercase()

        return when {
            cat.contains("NEWS") || name.contains("NEWS") || name.contains("DW") || name.contains("FRANCE") || name.contains("SKY") -> listOf(
                Template("Global News Brief & Market Opening", 30, "Early regional business bulletins, financial market trends and weather overview.", "TV-G"),
                Template("Breaking World News Live", 60, "Live continuous news coverage with international correspondents, investigative reports and updates.", "TV-PG", "4K UHD"),
                Template("The Prime Minister's Questions & Debate", 45, "Political analysis, legislative debate recap and interviews with senior cabinet correspondents.", "TV-14"),
                Template("World Business Report & Technology", 30, "In-depth economic forecast, venture capital developments and global tech policy news.", "TV-PG"),
                Template("Nightline World Edition with Mark Austin", 60, "Comprehensive recap of today's leading world stories and investigative special reports.", "TV-14", "1080p HD"),
                Template("International Press Preview & Late Wire", 45, "Front-page analysis of tomorrow's international newspapers and late-breaking wires.", "TV-PG")
            )

            cat.contains("SPORT") || name.contains("SPORT") || name.contains("RED BULL") || name.contains("RACING") -> listOf(
                Template("Premier League Warm-Up & Tactics", 30, "Pre-match tactical breakdowns, team line-ups, manager press conferences and live stadium pitch reports.", "TV-PG"),
                Template("Live: Red Bull Action Sports Championship", 90, "High-adrenaline downhill mountain biking, cliff diving highlights and extreme freestyle motocross finals.", "TV-14", "4K UHD", "Dolby Atmos"),
                Template("Formula 1 Paddock Insider & Telemetry", 45, "In-depth engineering telemetry review, tyre degradation strategies and post-race driver debriefs.", "TV-G"),
                Template("Motorsport Supercup Finals Highlights", 60, "Wheel-to-wheel touring car racing action from legendary tracks with onboard cameras and expert commentary.", "TV-PG"),
                Template("SportsCenter World Roundup Live", 60, "Scores, highlights, transfer news, and top 10 plays from major global sporting events.", "TV-PG"),
                Template("Classic Sporting Encounters Archive", 45, "Iconic championship deciders, historic goals and legendary comebacks remastered in high definition.", "TV-G")
            )

            cat.contains("MOVIE") || cat.contains("CINEMA") || name.contains("CINEMA") || name.contains("MOVIE") -> listOf(
                Template("Hollywood Backstage & Director Insights", 30, "Exclusive interviews with academy award directors, VFX breakdowns and upcoming premiere trailers.", "TV-PG"),
                Template("Big Buck Bunny: 4K Director's Cut", 75, "The classic award-winning computer animated comedy rendered in ultra high definition and multi-track audio.", "TV-G", "4K UHD", "Dolby 5.1"),
                Template("Tears of Steel: Sci-Fi Chronicle (2026)", 90, "Dystopian visual effects spectacle following soldiers and scientists in a post-apocalyptic future.", "TV-14", "4K UHD", "Dolby Atmos"),
                Template("Midnight Thriller: Neon Shadows", 105, "Atmospheric neo-noir thriller set across the neon streets of Tokyo and Berlin. Viewer discretion advised.", "TV-MA"),
                Template("Indie Showcase: The Long Horizon", 80, "Award-winning independent film festival selection exploring human connection across remote terrain.", "TV-14"),
                Template("Classic Cinema Late Night Showcase", 90, "Golden age masterpieces restored frame-by-frame with retrospective analysis.", "TV-PG")
            )

            cat.contains("DOC") || name.contains("NASA") || name.contains("NATURE") || name.contains("WILD") -> listOf(
                Template("Earth From Orbit: High-Definition Live", 45, "Live video transmission from the International Space Station with orbital tracking data and telemetry.", "TV-G", "4K UHD"),
                Template("NASA Deep Space Briefing: Artemis Mission", 60, "Astronaut interviews, lunar lander technical specs and planetary science deep dive direct from Johnson Space Center.", "TV-G"),
                Template("Secrets of the Serengeti Migration", 60, "Follow the massive annual wildlife migration across treacherous river crossings filmed in 60FPS.", "TV-PG", "4K UHD"),
                Template("Ocean Abyss: Into the Mariana Trench", 50, "Submersible exploration into the deepest point on Earth encountering bioluminescent organisms.", "TV-G"),
                Template("Mega Structures: Engineering Marvels", 60, "How modern engineers build towering skyscrapers, suspension bridges and trans-continental tunnels.", "TV-PG"),
                Template("Cosmic Horizons: Beyond the Milky Way", 60, "Astrophysicists explore dark matter, black holes and gravitational waves observed by modern space observatories.", "TV-G")
            )

            cat.contains("KIDS") || name.contains("KIDS") || name.contains("TOON") -> listOf(
                Template("Cosmo & Friends: The Star Treasure", 30, "Animated space adventure where friendly aliens solve science puzzles to rescue a lost comet.", "TV-Y"),
                Template("Safari Squad: The Hidden Oasis", 30, "Animal heroes team up to protect their savanna sanctuary from unexpected desert storms.", "TV-Y7"),
                Template("Super Mecha Academy: Final Battle", 45, "Giant robot battles and futuristic team strategy to defend the orbital energy core.", "TV-Y7-FV"),
                Template("Magic Island Chronicles", 30, "Enchanted creatures embark on a quest to restore the crystal bridge of Eldoria.", "TV-Y"),
                Template("Bedtime Stars & Lullaby Tales", 30, "Gentle, calming animated stories designed for evening wind-down with acoustic melodies.", "TV-Y"),
                Template("Saturday Morning Classic Animation", 45, "Timeless hand-drawn animated slapstick comedies and adventurous shorts.", "TV-Y7")
            )

            cat.contains("MUSIC") || name.contains("MUSIC") || name.contains("MTV") || name.contains("CONCERT") -> listOf(
                Template("Top 40 Global Hits Countdown", 45, "The hottest chart-topping singles, trending music videos and streaming viral hits.", "TV-PG"),
                Template("Nova Stage Live: Electronic Symphony", 90, "Live multi-camera festival broadcast from Amsterdam featuring world-class DJs and orchestral synthesizers.", "TV-14", "4K UHD", "Dolby Atmos"),
                Template("Acoustic Sessions: Sunset Unplugged", 45, "Intimate acoustic performances and singer-songwriter spotlights recorded live in studio.", "TV-PG"),
                Template("Club Nights: Electronic Underground", 75, "Late night club sets, visual synthesizer projection mapping and deep bass rhythms.", "TV-14"),
                Template("Rock Legends: World Tour Retrospective", 60, "Stadium rock concerts, archive interview tapes and guitar solo masterpieces.", "TV-14"),
                Template("Ambient Horizons: Midnight Soundscapes", 60, "Chill atmospheric visuals and downtempo electronic compositions for late night focus.", "TV-G")
            )

            else -> listOf(
                Template("Morning Horizons: Global Panorama", 45, "Morning news, lifestyle tips, culinary highlights and international cultural features.", "TV-PG"),
                Template("Prime Time Feature Presentation", 60, "Prime broadcast coverage of major cultural happenings, discussions and entertainment showcases.", "TV-14", "1080p HD"),
                Template("Investigative Focus: The Modern Era", 45, "Award-winning investigative journalism examining technology, society and environmental trends.", "TV-14"),
                Template("Late Night Live: The Conversation", 60, "Celebrity guest interviews, live musical performance and topical satirical monologue.", "TV-14"),
                Template("Midnight Feature Film: Classic Edition", 90, "Curated cinematic selection featuring cinema classics and independent stories.", "TV-14"),
                Template("Overnight World Relay Broadcast", 60, "Continuous international feeds, environmental time-lapse footage and cultural recaps.", "TV-G")
            )
        }
    }
}
