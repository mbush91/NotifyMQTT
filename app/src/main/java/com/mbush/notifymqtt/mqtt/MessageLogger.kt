package com.mbush.notifymqtt.mqtt

import android.content.Context
import android.util.Log
import java.io.File
import java.time.Instant

data class LoggedMessage(
    val timestamp: String,
    val topic: String,
    val payload: String,
)

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

    @Synchronized
    fun readRecent(limit: Int = DEFAULT_READ_LIMIT): List<LoggedMessage> {
        if (limit <= 0) return emptyList()

        return sequenceOf(backupFile, logFile)
            .filter { it.exists() }
            .flatMap { file -> file.useLines { lines -> lines.toList().asSequence() } }
            .mapNotNull(::parseLine)
            .toList()
            .takeLast(limit)
            .asReversed()
    }

    @Synchronized
    fun clear() {
        logFile.delete()
        backupFile.delete()
    }

    private fun parseLine(line: String): LoggedMessage? {
        val parts = line.split('\t', limit = 3)
        if (parts.size != 3) return null

        return LoggedMessage(
            timestamp = parts[0],
            topic = unescape(parts[1]),
            payload = unescape(parts[2]),
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

    private fun unescape(value: String): String {
        val result = StringBuilder(value.length)
        var index = 0

        while (index < value.length) {
            if (value[index] == '\\' && index + 1 < value.length) {
                when (value[index + 1]) {
                    '\\' -> result.append('\\')
                    't' -> result.append('\t')
                    'r' -> result.append('\r')
                    'n' -> result.append('\n')
                    else -> {
                        result.append('\\')
                        result.append(value[index + 1])
                    }
                }
                index += 2
            } else {
                result.append(value[index])
                index += 1
            }
        }

        return result.toString()
    }

    companion object {
        const val LOG_TAG = "NotifyMQTT"
        const val LOG_FILE_NAME = "mqtt-messages.log"
        const val BACKUP_LOG_FILE_NAME = "mqtt-messages.log.1"
        private const val MAX_LOG_BYTES = 1_048_576L
        private const val DEFAULT_READ_LIMIT = 100
    }
}
