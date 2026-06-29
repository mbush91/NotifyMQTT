package com.mbush.notifymqtt.mqtt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.mbush.notifymqtt.MainActivity

class NotificationHelper(private val context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannels() {
        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            "MQTT listener",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows when NotifyMQTT is actively listening for broker messages."
            setSound(null, null)
            enableVibration(false)
        }

        val dingChannel = NotificationChannel(
            MESSAGE_DING_CHANNEL_ID,
            "MQTT messages with ding",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "MQTT message notifications with the default notification sound."
            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }

        val silentChannel = NotificationChannel(
            MESSAGE_SILENT_CHANNEL_ID,
            "MQTT messages silent",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "MQTT message notifications without sound."
            setSound(null, null)
            enableVibration(false)
        }

        notificationManager.createNotificationChannels(
            listOf(serviceChannel, dingChannel, silentChannel),
        )
    }

    fun serviceNotification(title: String, text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val stopIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, MqttForegroundService::class.java).apply {
                action = MqttForegroundService.ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    fun messageNotification(topic: String, payload: String, dingEnabled: Boolean): Notification {
        val openIntent = PendingIntent.getActivity(
            context,
            topic.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val channelId = if (dingEnabled) MESSAGE_DING_CHANNEL_ID else MESSAGE_SILENT_CHANNEL_ID

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(topic)
            .setContentText(payload.take(MAX_SINGLE_LINE_PAYLOAD))
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.take(MAX_BIG_TEXT_PAYLOAD)))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(if (dingEnabled) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(!dingEnabled)
            .build()
    }

    companion object {
        const val SERVICE_CHANNEL_ID = "mqtt_listener"
        const val MESSAGE_DING_CHANNEL_ID = "mqtt_messages_ding"
        const val MESSAGE_SILENT_CHANNEL_ID = "mqtt_messages_silent"
        const val SERVICE_NOTIFICATION_ID = 1001
        private const val MAX_SINGLE_LINE_PAYLOAD = 160
        private const val MAX_BIG_TEXT_PAYLOAD = 1200
    }
}
