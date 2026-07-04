package com.mbush.notifymqtt.mqtt

import com.mbush.notifymqtt.data.SubscriptionBehavior
import com.mbush.notifymqtt.data.SubscriptionRule
import com.mbush.notifymqtt.data.TopicParser

class FreshnessTracker(
    rules: List<SubscriptionRule>,
    startMillis: Long = monotonicMillis(),
) {
    private val watchedRules = rules.filter {
        it.behavior == SubscriptionBehavior.MISSING && it.timeoutMinutes != null
    }
    private val deadlines = watchedRules.associate { rule ->
        rule.topicFilter to startMillis + timeoutMillis(rule)
    }.toMutableMap()
    private val notifiedFilters = mutableSetOf<String>()

    @Synchronized
    fun recordMessage(topic: String, nowMillis: Long = monotonicMillis()) {
        watchedRules
            .filter { TopicParser.matches(it.topicFilter, topic) }
            .forEach { rule ->
                deadlines[rule.topicFilter] = nowMillis + timeoutMillis(rule)
                notifiedFilters.remove(rule.topicFilter)
            }
    }

    @Synchronized
    fun collectNewlyExpired(nowMillis: Long = monotonicMillis()): List<SubscriptionRule> =
        watchedRules.filter { rule ->
            val expired = nowMillis >= (deadlines[rule.topicFilter] ?: Long.MAX_VALUE)
            expired && notifiedFilters.add(rule.topicFilter)
        }

    val hasWatchedRules: Boolean
        get() = watchedRules.isNotEmpty()

    private fun timeoutMillis(rule: SubscriptionRule): Long =
        requireNotNull(rule.timeoutMinutes).toLong() * MILLIS_PER_MINUTE

    companion object {
        private const val MILLIS_PER_MINUTE = 60_000L

        private fun monotonicMillis(): Long = System.nanoTime() / 1_000_000L
    }
}
