package com.lughzdan.preferences
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
private val Context.settingsDataStore by preferencesDataStore("settings")
data class Settings(val dark:Boolean=false,val sound:Boolean=true,val wifiOnly:Boolean=true,val automaticDownloads:Boolean=false,val haptic:Boolean=false)
class SettingsStore(context:Context) {
 private val store=context.settingsDataStore
 private val guestId=stringPreferencesKey("local_guest_id")
 val localGuestId=store.data.map { it[guestId].orEmpty() }
 suspend fun ensureGuestId() {store.edit {if(it[guestId]==null)it[guestId]="guest-"+java.util.UUID.randomUUID().toString()}}
 private val haptic=booleanPreferencesKey("haptic")
 private val dark=booleanPreferencesKey("dark")
 private val sound=booleanPreferencesKey("sound")
 private val wifiOnly=booleanPreferencesKey("wifi_only")
 private val automaticDownloads=booleanPreferencesKey("automatic_downloads")
 val settings=store.data.map { Settings(it[dark]?:false,it[sound]?:true,it[wifiOnly]?:true,it[automaticDownloads]?:false,it[haptic]?:false) }
 suspend fun haptic(value:Boolean) {store.edit {it[haptic]=value}}
 suspend fun dark(value:Boolean) { store.edit { it[dark]=value } }
 suspend fun sound(value:Boolean) { store.edit { it[sound]=value } }
 suspend fun wifiOnly(value:Boolean) { store.edit { it[wifiOnly]=value } }
 suspend fun automaticDownloads(value:Boolean) { store.edit { it[automaticDownloads]=value } }
}
