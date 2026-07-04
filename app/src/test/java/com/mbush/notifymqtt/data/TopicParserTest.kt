package com.mbush.notifymqtt.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicParserTest {
    @Test
    fun parseTrimsBlankLinesCommentsAndDuplicates() {
        val raw = """
            home/garage/door

            # comment
            home/hot-tub/alerts | silent
            home/garage/door | log
        """.trimIndent()

        assertEquals(
            listOf(
                SubscriptionRule("home/garage/door", SubscriptionBehavior.DING),
                SubscriptionRule("home/hot-tub/alerts", SubscriptionBehavior.SILENT),
            ),
            TopicParser.parse(raw),
        )
    }

    @Test
    fun bareTopicsUseConfiguredDefaultBehavior() {
        val result = TopicParser.parse(
            "home/status",
            defaultBehavior = SubscriptionBehavior.SILENT,
        )

        assertEquals(
            listOf(SubscriptionRule("home/status", SubscriptionBehavior.SILENT)),
            result,
        )
    }

    @Test
    fun parsesAllExplicitBehaviors() {
        val raw = """
            alerts/critical | ding
            alerts/info | silent
            telemetry/# | log
            sensors/heartbeat | missing:15
        """.trimIndent()

        assertEquals(
            listOf(
                SubscriptionRule("alerts/critical", SubscriptionBehavior.DING),
                SubscriptionRule("alerts/info", SubscriptionBehavior.SILENT),
                SubscriptionRule("telemetry/#", SubscriptionBehavior.LOG_ONLY),
                SubscriptionRule("sensors/heartbeat", SubscriptionBehavior.MISSING, timeoutMinutes = 15),
            ),
            TopicParser.parse(raw),
        )
    }

    @Test
    fun missingBehaviorAliasesAreSupported() {
        assertEquals(
            SubscriptionRule("a", SubscriptionBehavior.MISSING, timeoutMinutes = 5),
            TopicParser.parse("a | stale:5").single(),
        )
        assertEquals(
            SubscriptionRule("b", SubscriptionBehavior.MISSING, timeoutMinutes = 10),
            TopicParser.parse("b | watch:10").single(),
        )
    }

    @Test
    fun invalidMissingTimeoutsAreIgnored() {
        assertTrue(TopicParser.parse("a | missing:0").isEmpty())
        assertTrue(TopicParser.parse("a | missing:nope").isEmpty())
    }

    @Test
    fun behaviorForUsesFirstMatchingRule() {
        val rules = TopicParser.parse(
            """
                sensors/+/temperature | log
                sensors/kitchen/# | ding
            """.trimIndent(),
        )

        assertEquals(
            SubscriptionBehavior.LOG_ONLY,
            TopicParser.behaviorFor("sensors/kitchen/temperature", rules),
        )
    }

    @Test
    fun mqttWildcardMatchingSupportsSingleAndMultiLevelFilters() {
        assertTrue(TopicParser.matches("zigbee2mqtt/+/availability", "zigbee2mqtt/kitchen/availability"))
        assertTrue(TopicParser.matches("telemetry/#", "telemetry/device/temperature"))
        assertTrue(TopicParser.matches("telemetry/#", "telemetry"))
        assertFalse(TopicParser.matches("zigbee2mqtt/+/availability", "zigbee2mqtt/kitchen/state"))
        assertFalse(TopicParser.matches("telemetry/+/state", "telemetry/device/sub/state"))
    }
}
