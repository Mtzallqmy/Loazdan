package com.lughzdan.preferences
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
private val Context.settingsDataStore by preferencesDataStore("settings")
data class Settings(val dark:Boolean=false,val sound:Boolean=false,val wifiOnly:Boolean=true,val automaticDownloads:Boolean=false)
class SettingsStore(context:Context) {
 private val store=context.settingsDataStore
 private val dark=booleanPreferencesKey("dark")
 private val sound=booleanPreferencesKey("sound")
 private val wifiOnly=booleanPreferencesKey("wifi_only")
 private val automaticDownloads=booleanPreferencesKey("automatic_downloads")
 val settings=store.data.map { Settings(it[dark]?:false,it[sound]?:false,it[wifiOnly]?:true,it[automaticDownloads]?:false) }
 suspend fun dark(value:Boolean) { store.edit { it[dark]=value } }
 suspend fun sound(value:Boolean) { store.edit { it[sound]=value } }
 suspend fun wifiOnly(value:Boolean) { store.edit { it[wifiOnly]=value } }
 suspend fun automaticDownloads(value:Boolean) { store.edit { it[automaticDownloads]=value } }
}
