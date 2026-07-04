package com.mbush.notifymqtt.data

enum class SubscriptionBehavior(val token: String) {
    DING("ding"),
    SILENT("silent"),
    LOG_ONLY("log"),
    ;

    companion object {
        fun fromToken(raw: String): SubscriptionBehavior? =
            when (raw.trim().lowercase()) {
                "ding", "audible", "notify", "sound" -> DING
                "silent", "quiet" -> SILENT
                "log", "log-only", "log_only" -> LOG_ONLY
                else -> null
            }
    }
}

data class SubscriptionRule(
    val topicFilter: String,
    val behavior: SubscriptionBehavior,
)

object TopicParser {
    fun parse(
        raw: String,
        defaultBehavior: SubscriptionBehavior = SubscriptionBehavior.DING,
    ): List<SubscriptionRule> =
        raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { it.startsWith("#") }
            .mapNotNull { line -> parseLine(line, defaultBehavior) }
            .distinctBy { it.topicFilter }
            .toList()

    fun behaviorFor(topic: String, rules: List<SubscriptionRule>): SubscriptionBehavior? =
        rules.firstOrNull { matches(it.topicFilter, topic) }?.behavior

    fun matches(filter: String, topic: String): Boolean {
        val filterLevels = filter.split('/')
        val topicLevels = topic.split('/')
        var filterIndex = 0
        var topicIndex = 0

        while (filterIndex < filterLevels.size) {
            when (val filterLevel = filterLevels[filterIndex]) {
                "#" -> return filterIndex == filterLevels.lastIndex
                "+" -> if (topicIndex >= topicLevels.size) return false
                else -> {
                    if (topicIndex >= topicLevels.size || filterLevel != topicLevels[topicIndex]) {
                        return false
                    }
                }
            }

            filterIndex += 1
            topicIndex += 1
        }

        return topicIndex == topicLevels.size
    }

    private fun parseLine(
        line: String,
        defaultBehavior: SubscriptionBehavior,
    ): SubscriptionRule? {
        val parts = line.split('|', limit = 2)
        val topicFilter = parts[0].trim()
        if (topicFilter.isBlank()) return null

        val behavior = if (parts.size == 1) {
            defaultBehavior
        } else {
            SubscriptionBehavior.fromToken(parts[1]) ?: return null
        }

        return SubscriptionRule(topicFilter, behavior)
    }
}
