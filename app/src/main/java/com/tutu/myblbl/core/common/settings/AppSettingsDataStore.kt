package com.tutu.myblbl.core.common.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.os.SystemClock
import com.tutu.myblbl.core.common.log.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private val Context.appDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsDataStore(private val context: Context) {

    private val dataStore: DataStore<Preferences> get() = context.appDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = ConcurrentHashMap<String, Any?>()
    private val cacheInitialized = AtomicBoolean(false)
    private var initJob: kotlinx.coroutines.Job? = null

    companion object {
        private const val TAG = "AppSettingsDataStore"
    }

    /**
     * 非阻塞：启动后台协程从 DataStore 读取全部设置到 [cache]。
     * DataStore 首次 `data.first()` 在电视上约 30~80ms（磁盘 IO + XML 解析），
     * 放到后台不阻塞 app.onCreate 主线程。
     * [getCachedXxx] 在 cache miss 时会同步 fallback，保证正确性。
     */
    fun initCache() {
        if (cacheInitialized.get()) return
        if (initJob != null) return
        synchronized(this) {
            if (cacheInitialized.get()) return
            if (initJob != null) return
            initJob = scope.launch {
                val startMs = SystemClock.elapsedRealtime()
                val prefs = dataStore.data.first()
                prefs.asMap().forEach { (key, value) ->
                    cache[key.name] = value
                }
                cacheInitialized.set(true)
                AppLog.i(TAG, "initCache async loaded keys=${cache.size} elapsed=${SystemClock.elapsedRealtime() - startMs}ms")
            }
        }
    }

    /**
     * Application 启动阶段的显式同步初始化。这样所有 getCachedXxx 都只读内存，
     * 不再在任意页面/任意主线程 getter 中随机等待 DataStore 首读。
     */
    fun initCacheBlocking(reason: String = "startup") {
        if (cacheInitialized.get()) return
        val startMs = SystemClock.elapsedRealtime()
        synchronized(this) {
            if (cacheInitialized.get()) return
            initJob?.cancel()
            initJob = null
            runBlocking(Dispatchers.IO) {
                val prefs = dataStore.data.first()
                prefs.asMap().forEach { (key, value) ->
                    cache[key.name] = value
                }
            }
            cacheInitialized.set(true)
        }
        AppLog.i(TAG, "initCacheBlocking reason=$reason keys=${cache.size} elapsed=${SystemClock.elapsedRealtime() - startMs}ms")
    }

    private fun ensureCacheReady(key: String) {
        if (cacheInitialized.get()) return
        AppLog.w(TAG, "getCached($key) before cache ready on thread=${Thread.currentThread().name}; starting async cache load and returning fallback if cache miss")
        initCache()
    }

    fun getCachedString(key: String, defaultValue: String? = null): String? {
        cache[key]?.let { return it as? String ?: defaultValue }
        ensureCacheReady(key)
        return cache[key] as? String ?: defaultValue
    }

    fun getCachedInt(key: String, defaultValue: Int = 0): Int {
        val cached = cache[key]
        if (cached != null) return cached as? Int ?: defaultValue
        ensureCacheReady(key)
        return cache[key] as? Int ?: defaultValue
    }

    fun getCachedBoolean(key: String, defaultValue: Boolean = false): Boolean {
        val cached = cache[key]
        if (cached != null) return cached as? Boolean ?: defaultValue
        ensureCacheReady(key)
        return cache[key] as? Boolean ?: defaultValue
    }

    fun getCachedStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> {
        @Suppress("UNCHECKED_CAST")
        cache[key]?.let { return it as? Set<String> ?: defaultValue }
        ensureCacheReady(key)
        @Suppress("UNCHECKED_CAST")
        return cache[key] as? Set<String> ?: defaultValue
    }

    suspend fun getString(key: String, defaultValue: String? = null): String? {
        cache[key]?.let { return it as? String ?: defaultValue }
        return dataStore.data.first()[stringPreferencesKey(key)]?.also { cache[key] = it } ?: defaultValue
    }

    suspend fun getInt(key: String, defaultValue: Int = 0): Int {
        val cached = cache[key]
        if (cached != null) return cached as? Int ?: defaultValue
        return (dataStore.data.first()[intPreferencesKey(key)] ?: defaultValue).also { cache[key] = it }
    }

    suspend fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> {
        cache[key]?.let {
            @Suppress("UNCHECKED_CAST")
            return it as? Set<String> ?: defaultValue
        }
        return (dataStore.data.first()[stringSetPreferencesKey(key)] ?: defaultValue).also { cache[key] = it }
    }

    fun getStringFlow(key: String, defaultValue: String? = null): Flow<String?> {
        return dataStore.data.map { it[stringPreferencesKey(key)] ?: defaultValue }
    }

    fun getIntFlow(key: String, defaultValue: Int = 0): Flow<Int> {
        return dataStore.data.map { it[intPreferencesKey(key)] ?: defaultValue }
    }

    fun getStringSetFlow(key: String, defaultValue: Set<String> = emptySet()): Flow<Set<String>> {
        return dataStore.data.map { it[stringSetPreferencesKey(key)] ?: defaultValue }
    }

    suspend fun putString(key: String, value: String?) {
        if (value != null) cache[key] = value else cache.remove(key)
        dataStore.edit { prefs ->
            if (value != null) prefs[stringPreferencesKey(key)] = value
            else prefs.remove(stringPreferencesKey(key))
        }
    }

    suspend fun putInt(key: String, value: Int) {
        cache[key] = value
        dataStore.edit { prefs ->
            prefs[intPreferencesKey(key)] = value
        }
    }

    suspend fun putBoolean(key: String, value: Boolean) {
        cache[key] = value
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(key)] = value
        }
    }

    suspend fun putStringSet(key: String, value: Set<String>) {
        cache[key] = value
        dataStore.edit { prefs ->
            prefs[stringSetPreferencesKey(key)] = value
        }
    }

    fun putStringAsync(key: String, value: String?) {
        if (value != null) cache[key] = value else cache.remove(key)
        scope.launch {
            dataStore.edit { prefs ->
                if (value != null) prefs[stringPreferencesKey(key)] = value
                else prefs.remove(stringPreferencesKey(key))
            }
        }
    }

    fun putIntAsync(key: String, value: Int) {
        cache[key] = value
        scope.launch {
            dataStore.edit { prefs ->
                prefs[intPreferencesKey(key)] = value
            }
        }
    }

    fun putBooleanAsync(key: String, value: Boolean) {
        cache[key] = value
        scope.launch {
            dataStore.edit { prefs ->
                prefs[booleanPreferencesKey(key)] = value
            }
        }
    }

    fun putStringSetAsync(key: String, value: Set<String>) {
        cache[key] = value
        scope.launch {
            dataStore.edit { prefs ->
                prefs[stringSetPreferencesKey(key)] = value
            }
        }
    }

    suspend fun remove(key: String) {
        cache.remove(key)
        dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey(key))
            prefs.remove(intPreferencesKey(key))
            prefs.remove(booleanPreferencesKey(key))
            prefs.remove(stringSetPreferencesKey(key))
        }
    }

    fun clearAll() {
        cache.clear()
        cacheInitialized.set(false)
        scope.launch {
            dataStore.edit { it.clear() }
        }
    }
}
