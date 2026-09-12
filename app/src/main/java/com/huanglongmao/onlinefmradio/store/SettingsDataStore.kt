package com.huanglongmao.onlinefmradio.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * 共享 DataStore 实例（对应 Flutter 版 SharedPreferences 的角色）。
 * 全部小体量配置（收藏/历史/主题/音量/断点等）统一存放在此。
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** DataStore 读写辅助（一次性读取 + 写入，与原版 SharedPreferences 用法对齐） */
class SettingsDataStore(private val store: DataStore<Preferences>) {

    suspend fun getString(key: String): String? = store.data.first()[stringPreferencesKey(key)]

    suspend fun putString(key: String, value: String?) {
        store.edit { prefs ->
            if (value == null) prefs.remove(stringPreferencesKey(key))
            else prefs[stringPreferencesKey(key)] = value
        }
    }

    suspend fun getInt(key: String): Int? = store.data.first()[intPreferencesKey(key)]

    suspend fun putInt(key: String, value: Int?) {
        store.edit { prefs ->
            if (value == null) prefs.remove(intPreferencesKey(key))
            else prefs[intPreferencesKey(key)] = value
        }
    }

    suspend fun getBool(key: String): Boolean? = store.data.first()[booleanPreferencesKey(key)]

    suspend fun putBool(key: String, value: Boolean) {
        store.edit { it[booleanPreferencesKey(key)] = value }
    }

    suspend fun getFloat(key: String): Float? = store.data.first()[floatPreferencesKey(key)]

    suspend fun putFloat(key: String, value: Float) {
        store.edit { it[floatPreferencesKey(key)] = value }
    }

    companion object {
        fun from(context: Context) = SettingsDataStore(context.applicationContext.dataStore)
    }
}
