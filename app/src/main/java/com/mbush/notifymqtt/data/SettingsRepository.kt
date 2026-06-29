package com.mbush.notifymqtt.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "notify_mqtt_settings")

class SettingsRepository(context: Context) {
    private val appContext = context.applicationContext

    val settingsFlow: Flow<AppSettings> = appContext.settingsDataStore.data.map { p ->
        AppSettings(
            host = p[Keys.HOST].orEmpty(),
            port = p[Keys.PORT] ?: 1883,
            useTls = p[Keys.USE_TLS] ?: false,
            username = p[Keys.USERNAME].orEmpty(),
            password = p[Keys.SECRET].orEmpty(),
            clientId = p[Keys.CLIENT_ID].orEmpty(),
            topics = p[Keys.TOPICS].orEmpty(),
            dingEnabled = p[Keys.DING_ENABLED] ?: true,
            autoStartOnBoot = p[Keys.AUTO_START_ON_BOOT] ?: false,
            serviceEnabled = p[Keys.SERVICE_ENABLED] ?: false,
        )
    }

    suspend fun updateHost(value: String) = appContext.settingsDataStore.edit { it[Keys.HOST] = value }
    suspend fun updatePort(value: Int) = appContext.settingsDataStore.edit { it[Keys.PORT] = value.coerceIn(1, 65535) }
    suspend fun updateUseTls(value: Boolean) = appContext.settingsDataStore.edit { it[Keys.USE_TLS] = value }
    suspend fun updateUsername(value: String) = appContext.settingsDataStore.edit { it[Keys.USERNAME] = value }
    suspend fun updatePassword(value: String) = appContext.settingsDataStore.edit { it[Keys.SECRET] = value }
    suspend fun updateClientId(value: String) = appContext.settingsDataStore.edit { it[Keys.CLIENT_ID] = value }
    suspend fun updateTopics(value: String) = appContext.settingsDataStore.edit { it[Keys.TOPICS] = value }
    suspend fun updateDingEnabled(value: Boolean) = appContext.settingsDataStore.edit { it[Keys.DING_ENABLED] = value }
    suspend fun updateAutoStartOnBoot(value: Boolean) = appContext.settingsDataStore.edit { it[Keys.AUTO_START_ON_BOOT] = value }
    suspend fun updateServiceEnabled(value: Boolean) = appContext.settingsDataStore.edit { it[Keys.SERVICE_ENABLED] = value }

    private object Keys {
        val HOST = stringPreferencesKey("host")
        val PORT = intPreferencesKey("port")
        val USE_TLS = booleanPreferencesKey("use_tls")
        val USERNAME = stringPreferencesKey("username")
        val SECRET = stringPreferencesKey("auth_secret")
        val CLIENT_ID = stringPreferencesKey("client_id")
        val TOPICS = stringPreferencesKey("topics")
        val DING_ENABLED = booleanPreferencesKey("ding_enabled")
        val AUTO_START_ON_BOOT = booleanPreferencesKey("auto_start_on_boot")
        val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
    }
}
