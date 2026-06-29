package com.mbush.notifymqtt.data

object TopicParser {
    fun parse(raw: String): List<String> =
        raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { it.startsWith("#") }
            .distinct()
            .toList()
}
