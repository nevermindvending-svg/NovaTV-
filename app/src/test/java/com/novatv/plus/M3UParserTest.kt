package com.novatv.plus

import com.novatv.plus.data.M3UParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3UParserTest {

    @Test
    fun testParseStandardM3U() {
        val sampleM3U = """
            #EXTM3U
            #EXTINF:-1 tvg-id="cnn.us" tvg-name="CNN Live" tvg-logo="https://example.com/cnn.png" group-title="NEWS",CNN International
            https://example.com/cnn/live.m3u8
            #EXTINF:-1 tvg-id="espn.us" tvg-name="ESPN" tvg-logo="https://example.com/espn.png" group-title="SPORTS",ESPN HD
            https://example.com/espn/live.m3u8
        """.trimIndent()

        val channels = M3UParser.parse(sampleM3U)

        assertEquals(2, channels.size)

        val ch1 = channels[0]
        assertEquals("CNN International", ch1.name)
        assertEquals("cnn.us", ch1.tvgId)
        assertEquals("https://example.com/cnn.png", ch1.logoUrl)
        assertEquals("NEWS", ch1.category)
        assertEquals("https://example.com/cnn/live.m3u8", ch1.streamUrl)
        assertNotNull(ch1.programTitle)

        val ch2 = channels[1]
        assertEquals("ESPN HD", ch2.name)
        assertEquals("SPORTS", ch2.category)
    }

    @Test
    fun testMalformedLinesDoNotCrash() {
        val corruptM3U = """
            Random garbage text
            #EXTINF: invalid, incomplete
            #EXTINF:-1
            not a url
            #EXTINF:-1 group-title="MUSIC",Valid Music Channel
            https://stream.example.com/music.m3u8
        """.trimIndent()

        val channels = M3UParser.parse(corruptM3U)
        assertEquals(1, channels.size)
        assertEquals("Valid Music Channel", channels[0].name)
        assertEquals("MUSIC", channels[0].category)
    }
}
