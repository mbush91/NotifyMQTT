package com.mbush.notifymqtt.mqtt

import android.app.NotificationManager
import android.content.Context
import kotlin.math.absoluteValue

class MessagePublisher(context: Context) {
    private val appContext = context.applicationContext
    private val helper = NotificationHelper(appContext)
    private val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun show(topic: String, payload: String, sound: Boolean) {
        val id = (topic + System.nanoTime().toString()).hashCode().absoluteValue
        manager.notify(id, helper.messageNotification(topic, payload, sound))
    }
}
