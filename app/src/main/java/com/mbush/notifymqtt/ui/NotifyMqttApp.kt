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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mbush.notifymqtt.data.AppSettings
import com.mbush.notifymqtt.data.SettingsRepository
import com.mbush.notifymqtt.mqtt.MqttForegroundService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun NotifyMqttApp(settingsRepository: SettingsRepository) {
    val settings by settingsRepository.settingsFlow
        .map { it as AppSettings? }
        .collectAsStateWithLifecycle(initialValue = null)

    MaterialTheme(colorScheme = lightColorScheme()) {
        val loadedSettings = settings
        if (loadedSettings == null) {
            Text("Loading settings...")
        } else {
            NotifyMqttScreen(settingsRepository, loadedSettings)
        }
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

    var host by rememberSaveable { mutableStateOf(settings.host) }
    var portText by rememberSaveable { mutableStateOf(settings.port.toString()) }
    var username by rememberSaveable { mutableStateOf(settings.username) }
    var password by rememberSaveable { mutableStateOf(settings.password) }
    var clientId by rememberSaveable { mutableStateOf(settings.clientId) }
    var topics by rememberSaveable { mutableStateOf(settings.topics) }

    val editedSettings = settings.copy(
        host = host,
        port = portText.toIntOrNull() ?: 0,
        username = username,
        password = password,
        clientId = clientId,
        topics = topics,
    )

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
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Broker host") },
                        placeholder = { Text("192.168.1.10 or broker.example.com") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    scope.launch { settingsRepository.updateHost(host) }
                                }
                            },
                    )
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { value ->
                            if (value.all { it.isDigit() }) {
                                portText = value
                            }
                        },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    portText.toIntOrNull()?.let { port ->
                                        scope.launch { settingsRepository.updatePort(port) }
                                    }
                                }
                            },
                    )
                    ToggleRow("Use TLS", settings.useTls) { scope.launch { settingsRepository.updateUseTls(it) } }
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    scope.launch { settingsRepository.updateUsername(username) }
                                }
                            },
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    scope.launch { settingsRepository.updatePassword(password) }
                                }
                            },
                    )
                    OutlinedTextField(
                        value = clientId,
                        onValueChange = { clientId = it },
                        label = { Text("Client ID") },
                        placeholder = { Text("Leave blank to auto-generate") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    scope.launch { settingsRepository.updateClientId(clientId) }
                                }
                            },
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Subscriptions", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = topics,
                        onValueChange = { topics = it },
                        label = { Text("Topics, one per line") },
                        placeholder = { Text("home/garage/door\nhome/hot-tub/alerts") },
                        minLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    scope.launch { settingsRepository.updateTopics(topics) }
                                }
                            },
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
                    Text(if (editedSettings.isReadyToConnect) "Ready: ${editedSettings.brokerUri}" else "Add a broker and topic.")

                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        Button(
                            onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Allow notifications") }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                scope.launch {
                                    saveEditableSettings(settingsRepository, host, portText, username, password, clientId, topics)
                                    settingsRepository.updateServiceEnabled(true)
                                    ContextCompat.startForegroundService(
                                        context,
                                        Intent(context, MqttForegroundService::class.java).apply {
                                            action = MqttForegroundService.ACTION_START
                                        },
                                    )
                                }
                            },
                            enabled = editedSettings.isReadyToConnect,
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

private suspend fun saveEditableSettings(
    settingsRepository: SettingsRepository,
    host: String,
    portText: String,
    username: String,
    password: String,
    clientId: String,
    topics: String,
) {
    settingsRepository.updateHost(host)
    portText.toIntOrNull()?.let { settingsRepository.updatePort(it) }
    settingsRepository.updateUsername(username)
    settingsRepository.updatePassword(password)
    settingsRepository.updateClientId(clientId)
    settingsRepository.updateTopics(topics)
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
