package com.mbush.notifymqtt.mqtt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mbush.notifymqtt.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_LOCKED_BOOT_COMPLETED)) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repository = SettingsRepository(context)
                val settings = repository.settingsFlow.first()
                if (settings.autoStartOnBoot && settings.isReadyToConnect) {
                    repository.updateServiceEnabled(true)
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, MqttForegroundService::class.java).apply {
                            action = MqttForegroundService.ACTION_START
                        },
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
