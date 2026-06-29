package com.mbush.notifymqtt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mbush.notifymqtt.data.SettingsRepository
import com.mbush.notifymqtt.ui.NotifyMqttApp

class MainActivity : ComponentActivity() {
    private val settingsRepository by lazy { SettingsRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotifyMqttApp(settingsRepository = settingsRepository)
        }
    }
}
