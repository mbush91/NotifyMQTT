package com.mbush.notifymqtt.mqtt

import com.mbush.notifymqtt.data.SubscriptionBehavior
import com.mbush.notifymqtt.data.SubscriptionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshnessTrackerTest {
    @Test
    fun expiresOnceUntilMatchingMessageRearmsRule() {
        val rule = SubscriptionRule(
            topicFilter = "sensors/+/heartbeat",
            behavior = SubscriptionBehavior.MISSING,
            timeoutMinutes = 5,
        )
        val tracker = FreshnessTracker(listOf(rule), startMillis = 1_000L)

        assertTrue(tracker.collectNewlyExpired(nowMillis = 300_999L).isEmpty())
        assertEquals(listOf(rule), tracker.collectNewlyExpired(nowMillis = 301_000L))
        assertTrue(tracker.collectNewlyExpired(nowMillis = 400_000L).isEmpty())

        tracker.recordMessage("sensors/kitchen/heartbeat", nowMillis = 500_000L)

        assertTrue(tracker.collectNewlyExpired(nowMillis = 799_999L).isEmpty())
        assertEquals(listOf(rule), tracker.collectNewlyExpired(nowMillis = 800_000L))
    }

    @Test
    fun nonMatchingMessageDoesNotResetTimer() {
        val rule = SubscriptionRule(
            topicFilter = "device/heartbeat",
            behavior = SubscriptionBehavior.MISSING,
            timeoutMinutes = 1,
        )
        val tracker = FreshnessTracker(listOf(rule), startMillis = 0L)

        tracker.recordMessage("other/heartbeat", nowMillis = 30_000L)

        assertEquals(listOf(rule), tracker.collectNewlyExpired(nowMillis = 60_000L))
    }
}
