package com.lughzdan.preferences
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
private val Context.settingsDataStore by preferencesDataStore("settings")
data class Settings(val dark:Boolean=false,val sound:Boolean=false)
class SettingsStore(context:Context) {
 private val store=context.settingsDataStore
 private val dark=booleanPreferencesKey("dark")
 private val sound=booleanPreferencesKey("sound")
 val settings=store.data.map { Settings(it[dark]?:false,it[sound]?:false) }
 suspend fun dark(value:Boolean) { store.edit { it[dark]=value } }
 suspend fun sound(value:Boolean) { store.edit { it[sound]=value } }
}
