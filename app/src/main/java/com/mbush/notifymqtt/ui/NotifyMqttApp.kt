package com.mbush.notifymqtt.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mbush.notifymqtt.data.AppSettings
import com.mbush.notifymqtt.data.SettingsRepository
import com.mbush.notifymqtt.mqtt.MqttForegroundService
import kotlinx.coroutines.launch

@Composable
fun NotifyMqttApp(settingsRepository: SettingsRepository) {
    val settings by settingsRepository.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())
    MaterialTheme(colorScheme = lightColorScheme()) {
        NotifyMqttScreen(settingsRepository, settings)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotifyMqttScreen(
    settingsRepository: SettingsRepository,
    settings: AppSettings,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    Scaffold(
        topBar = { TopAppBar(title = { Text("NotifyMQTT") }) },
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("MQTT connection", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = settings.host,
                        onValueChange = { scope.launch { settingsRepository.updateHost(it) } },
                        label = { Text("Broker host") },
                        placeholder = { Text("192.168.1.10 or broker.example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = settings.port.toString(),
                        onValueChange = { value -> value.toIntOrNull()?.let { scope.launch { settingsRepository.updatePort(it) } } },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ToggleRow("Use TLS", settings.useTls) { scope.launch { settingsRepository.updateUseTls(it) } }
                    OutlinedTextField(
                        value = settings.username,
                        onValueChange = { scope.launch { settingsRepository.updateUsername(it) } },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = settings.password,
                        onValueChange = { scope.launch { settingsRepository.updatePassword(it) } },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = settings.clientId,
                        onValueChange = { scope.launch { settingsRepository.updateClientId(it) } },
                        label = { Text("Client ID") },
                        placeholder = { Text("Leave blank to auto-generate") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Subscriptions", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = settings.topics,
                        onValueChange = { scope.launch { settingsRepository.updateTopics(it) } },
                        label = { Text("Topics, one per line") },
                        placeholder = { Text("home/garage/door\nhome/hot-tub/alerts") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ToggleRow("Ding on message", settings.dingEnabled) {
                        scope.launch { settingsRepository.updateDingEnabled(it) }
                    }
                    ToggleRow("Start after reboot", settings.autoStartOnBoot) {
                        scope.launch { settingsRepository.updateAutoStartOnBoot(it) }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Listener", style = MaterialTheme.typography.titleMedium)
                    Text(if (settings.isReadyToConnect) "Ready: ${settings.brokerUri}" else "Add a broker and topic.")

                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        Button(
                            onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Allow notifications") }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                scope.launch { settingsRepository.updateServiceEnabled(true) }
                                ContextCompat.startForegroundService(
                                    context,
                                    Intent(context, MqttForegroundService::class.java).apply {
                                        action = MqttForegroundService.ACTION_START
                                    },
                                )
                            },
                            enabled = settings.isReadyToConnect,
                            modifier = Modifier.weight(1f),
                        ) { Text(if (settings.serviceEnabled) "Restart" else "Start") }

                        Button(
                            onClick = {
                                scope.launch { settingsRepository.updateServiceEnabled(false) }
                                context.startService(
                                    Intent(context, MqttForegroundService::class.java).apply {
                                        action = MqttForegroundService.ACTION_STOP
                                    },
                                )
                            },
                            enabled = settings.serviceEnabled,
                            modifier = Modifier.weight(1f),
                        ) { Text("Stop") }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
