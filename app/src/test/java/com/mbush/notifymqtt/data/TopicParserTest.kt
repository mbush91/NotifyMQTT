package com.mbush.notifymqtt.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TopicParserTest {
    @Test
    fun parseTrimsBlankLinesCommentsAndDuplicates() {
        val raw = """
            home/garage/door

            # comment
            home/hot-tub/alerts
            home/garage/door
        """.trimIndent()

        assertEquals(
            listOf("home/garage/door", "home/hot-tub/alerts"),
            TopicParser.parse(raw),
        )
    }
}
