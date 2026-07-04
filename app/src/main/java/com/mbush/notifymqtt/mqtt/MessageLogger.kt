package com.mbush.notifymqtt.mqtt

import android.content.Context
import android.util.Log
import java.io.File
import java.time.Instant

class MessageLogger(context: Context) {
    private val logFile = File(context.applicationContext.filesDir, LOG_FILE_NAME)
    private val backupFile = File(context.applicationContext.filesDir, BACKUP_LOG_FILE_NAME)

    @Synchronized
    fun log(topic: String, payload: String) {
        val timestamp = Instant.now().toString()
        Log.i(LOG_TAG, "topic=$topic payload=$payload")

        rotateIfNeeded()
        logFile.appendText(
            "$timestamp\t${escape(topic)}\t${escape(payload)}\n",
            Charsets.UTF_8,
        )
    }

    private fun rotateIfNeeded() {
        if (!logFile.exists() || logFile.length() < MAX_LOG_BYTES) return

        if (backupFile.exists()) {
            backupFile.delete()
        }
        logFile.renameTo(backupFile)
    }

    private fun escape(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\t", "\\t")
            .replace("\r", "\\r")
            .replace("\n", "\\n")

    companion object {
        const val LOG_TAG = "NotifyMQTT"
        const val LOG_FILE_NAME = "mqtt-messages.log"
        const val BACKUP_LOG_FILE_NAME = "mqtt-messages.log.1"
        private const val MAX_LOG_BYTES = 1_048_576L
    }
}
