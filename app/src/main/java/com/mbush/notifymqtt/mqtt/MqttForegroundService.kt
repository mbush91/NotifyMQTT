package com.mbush.notifymqtt.mqtt

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import com.mbush.notifymqtt.data.AppSettings
import com.mbush.notifymqtt.data.SettingsRepository
import com.mbush.notifymqtt.data.SubscriptionBehavior
import com.mbush.notifymqtt.data.TopicParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import javax.net.ssl.SSLSocketFactory

class MqttForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: SettingsRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var publisher: MessagePublisher
    private lateinit var messageLogger: MessageLogger

    private var settingsCollectorJob: Job? = null
    private var freshnessWatchJob: Job? = null
    private var mqttClient: MqttAsyncClient? = null

    override fun onCreate() {
        super.onCreate()
        repository = SettingsRepository(applicationContext)
        notificationHelper = NotificationHelper(applicationContext)
        publisher = MessagePublisher(applicationContext)
        messageLogger = MessageLogger(applicationContext)
        notificationHelper.createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch {
                    repository.updateServiceEnabled(false)
                    disconnectClient()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                return START_NOT_STICKY
            }

            ACTION_START, null -> {
                startAsForeground("NotifyMQTT", "Starting MQTT listener")
                scope.launch { repository.updateServiceEnabled(true) }
                ensureSettingsCollector()
                return START_STICKY
            }

            else -> return START_STICKY
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        settingsCollectorJob?.cancel()
        freshnessWatchJob?.cancel()
        scope.launch { disconnectClient() }
        scope.cancel()
        super.onDestroy()
    }

    private fun ensureSettingsCollector() {
        if (settingsCollectorJob?.isActive == true) return

        settingsCollectorJob = scope.launch {
            repository.settingsFlow.collectLatest { settings ->
                if (!settings.serviceEnabled) {
                    disconnectClient()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@collectLatest
                }

                runMqttLoop(settings)
            }
        }
    }

    private suspend fun runMqttLoop(settings: AppSettings) {
        if (!settings.isReadyToConnect) {
            startAsForeground("NotifyMQTT", "Add a broker and at least one topic")
            awaitCancellation()
        }

        while (true) {
            try {
                connectAndSubscribe(settings)
                startAsForeground(
                    "NotifyMQTT connected",
                    "Listening to ${settings.subscriptions.size} subscription(s) on ${settings.host}:${settings.port}",
                )
                awaitCancellation()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                startAsForeground(
                    "NotifyMQTT disconnected",
                    exception.message ?: "Retrying MQTT connection",
                )
                delay(RETRY_DELAY_MS)
            } finally {
                disconnectClient()
            }
        }
    }

    private suspend fun connectAndSubscribe(settings: AppSettings) = withContext(Dispatchers.IO) {
        disconnectClient()

        val subscriptions = settings.subscriptions
        val topics = subscriptions.map { it.topicFilter }
        val freshnessTracker = FreshnessTracker(subscriptions)
        val clientId = settings.clientId.ifBlank { "NotifyMQTT-${System.currentTimeMillis()}" }
        val client = MqttAsyncClient(settings.brokerUri, clientId, MemoryPersistence())
        mqttClient = client

        client.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                scope.launch {
                    subscribeToTopics(client, topics)
                    val status = if (reconnect) "reconnected" else "connected"
                    startAsForeground(
                        "NotifyMQTT $status",
                        "Listening on ${settings.host}:${settings.port}",
                    )
                }
            }

            override fun connectionLost(cause: Throwable?) {
                startAsForeground(
                    "NotifyMQTT reconnecting",
                    cause?.message ?: "Connection lost",
                )
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                val payload = String(message.payload, Charsets.UTF_8)
                freshnessTracker.recordMessage(topic)

                when (TopicParser.behaviorFor(topic, subscriptions)) {
                    SubscriptionBehavior.DING -> publisher.show(topic, payload, sound = true)
                    SubscriptionBehavior.SILENT -> publisher.show(topic, payload, sound = false)
                    SubscriptionBehavior.LOG_ONLY -> messageLogger.log(topic, payload)
                    SubscriptionBehavior.MISSING -> messageLogger.log(topic, payload)
                    null -> messageLogger.log(topic, payload)
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
        })

        val options = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = false
            connectionTimeout = CONNECTION_TIMEOUT_SECONDS
            keepAliveInterval = KEEP_ALIVE_SECONDS

            if (settings.username.isNotBlank()) {
                setUserName(settings.username)
            }
            if (settings.password.isNotBlank()) {
                setPassword(settings.password.toCharArray())
            }
            if (settings.useTls) {
                setSocketFactory(SSLSocketFactory.getDefault())
            }
        }

        client.connect(options).waitForCompletion(CONNECT_WAIT_MS)
        subscribeToTopics(client, topics)
        startFreshnessWatcher(freshnessTracker)
    }

    private fun startFreshnessWatcher(tracker: FreshnessTracker) {
        freshnessWatchJob?.cancel()
        if (!tracker.hasWatchedRules) {
            freshnessWatchJob = null
            return
        }

        freshnessWatchJob = scope.launch {
            while (true) {
                delay(FRESHNESS_CHECK_INTERVAL_MS)
                tracker.collectNewlyExpired().forEach { rule ->
                    val minutes = requireNotNull(rule.timeoutMinutes)
                    val timeoutMessage = "No matching MQTT message received for $minutes minute(s)."
                    messageLogger.log(rule.topicFilter, "TIMEOUT: $timeoutMessage")
                    publisher.show(
                        topic = rule.topicFilter,
                        payload = timeoutMessage,
                        sound = true,
                    )
                }
            }
        }
    }

    private suspend fun subscribeToTopics(client: MqttAsyncClient, topics: List<String>) = withContext(Dispatchers.IO) {
        topics.forEach { topic ->
            try {
                client.subscribe(topic, MQTT_QOS).waitForCompletion(SUBSCRIBE_WAIT_MS)
            } catch (_: MqttException) {
                // Automatic reconnect may briefly race subscription. The outer loop/callback retries.
            }
        }
    }

    private suspend fun disconnectClient() = withContext(Dispatchers.IO) {
        freshnessWatchJob?.cancel()
        freshnessWatchJob = null

        val client = mqttClient ?: return@withContext
        mqttClient = null
        runCatching {
            if (client.isConnected) {
                client.disconnect().waitForCompletion(DISCONNECT_WAIT_MS)
            }
        }
        runCatching { client.close() }
    }

    private fun startAsForeground(title: String, text: String) {
        val notification = notificationHelper.serviceNotification(title, text)
        startForeground(
            NotificationHelper.SERVICE_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING,
        )

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NotificationHelper.SERVICE_NOTIFICATION_ID, notification)
    }

    companion object {
        const val ACTION_START = "com.mbush.notifymqtt.action.START"
        const val ACTION_STOP = "com.mbush.notifymqtt.action.STOP"

        private const val MQTT_QOS = 1
        private const val CONNECTION_TIMEOUT_SECONDS = 10
        private const val KEEP_ALIVE_SECONDS = 30
        private const val CONNECT_WAIT_MS = 30_000L
        private const val SUBSCRIBE_WAIT_MS = 10_000L
        private const val DISCONNECT_WAIT_MS = 5_000L
        private const val RETRY_DELAY_MS = 5_000L
        private const val FRESHNESS_CHECK_INTERVAL_MS = 15_000L
    }
}
