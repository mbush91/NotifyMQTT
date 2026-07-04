package com.mbush.notifymqtt.data

data class AppSettings(
    val host: String = "",
    val port: Int = 1883,
    val useTls: Boolean = false,
    val username: String = "",
    val password: String = "",
    val clientId: String = "",
    val topics: String = "",
    val dingEnabled: Boolean = true,
    val autoStartOnBoot: Boolean = false,
    val serviceEnabled: Boolean = false,
) {
    val brokerUri: String
        get() {
            val scheme = if (useTls) "ssl" else "tcp"
            return "$scheme://${host.trim()}:$port"
        }

    val defaultSubscriptionBehavior: SubscriptionBehavior
        get() = if (dingEnabled) SubscriptionBehavior.DING else SubscriptionBehavior.SILENT

    val subscriptions: List<SubscriptionRule>
        get() = TopicParser.parse(topics, defaultSubscriptionBehavior)

    val parsedTopics: List<String>
        get() = subscriptions.map { it.topicFilter }

    fun behaviorForTopic(topic: String): SubscriptionBehavior? =
        TopicParser.behaviorFor(topic, subscriptions)

    val isReadyToConnect: Boolean
        get() = host.isNotBlank() && port in 1..65535 && subscriptions.isNotEmpty()
}
