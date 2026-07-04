package com.mbush.notifymqtt.data

enum class SubscriptionBehavior(val token: String) {
    DING("ding"),
    SILENT("silent"),
    LOG_ONLY("log"),
    MISSING("missing"),
    ;
}

data class SubscriptionRule(
    val topicFilter: String,
    val behavior: SubscriptionBehavior,
    val timeoutMinutes: Int? = null,
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

        if (parts.size == 1) {
            return SubscriptionRule(topicFilter, defaultBehavior)
        }

        val action = parts[1].trim().lowercase()
        return when {
            action in setOf("ding", "audible", "notify", "sound") ->
                SubscriptionRule(topicFilter, SubscriptionBehavior.DING)

            action in setOf("silent", "quiet") ->
                SubscriptionRule(topicFilter, SubscriptionBehavior.SILENT)

            action in setOf("log", "log-only", "log_only") ->
                SubscriptionRule(topicFilter, SubscriptionBehavior.LOG_ONLY)

            action.startsWith("missing:") || action.startsWith("stale:") || action.startsWith("watch:") -> {
                val minutes = action.substringAfter(':').trim().toIntOrNull()
                if (minutes == null || minutes <= 0) {
                    null
                } else {
                    SubscriptionRule(
                        topicFilter = topicFilter,
                        behavior = SubscriptionBehavior.MISSING,
                        timeoutMinutes = minutes,
                    )
                }
            }

            else -> null
        }
    }
}
