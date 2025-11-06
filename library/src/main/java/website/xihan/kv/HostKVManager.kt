package website.xihan.kv

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * 宿主端KV管理器，通过systemContext访问模块的ContentProvider
 */
object HostKVManager : KoinComponent {

    private val systemContext: Context by inject()
    private val cache = ConcurrentHashMap<String, Any>()
    private val listeners =
        ConcurrentHashMap<String, MutableList<WeakReference<(String, Any?) -> Unit>>>()
    private var broadcastReceiver: BroadcastReceiver? = null
    private var spCache: SharedPreferences? = null
    private var enableSpCache = false
    private var targetPackageName: String? = null

    private const val MODULE_AUTHORITY = "website.xihan.kv"
    private const val ACTION_KV_CHANGED = "${MODULE_AUTHORITY}.KV_CHANGED"
    private const val EXTRA_KV_ID = "kvId"
    private const val EXTRA_KEY = "key"
    private const val TAG = "HostKVManager"

    /**
     * 初始化宿主端KV管理器
     * @param enableSharedPreferencesCache 是否启用SharedPreferences缓存
     * @param modulePackageName 模块包名，用于广播目标包名
     */
    fun init(enableSharedPreferencesCache: Boolean = false, modulePackageName: String? = null) {
        enableSpCache = enableSharedPreferencesCache
        targetPackageName = modulePackageName
        if (enableSpCache) {
            spCache = systemContext.getSharedPreferences("kv_host_cache", Context.MODE_PRIVATE)
        }
        initBroadcastReceiver()
    }

    /**
     * 初始化广播接收器
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun initBroadcastReceiver() {
        if (broadcastReceiver == null) {
            broadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == ACTION_KV_CHANGED) {
                        val changedKvId = intent.getStringExtra(EXTRA_KV_ID) ?: ""
                        val changedKey = intent.getStringExtra(EXTRA_KEY) ?: ""

                        // 处理清空所有数据
                        if (changedKey == "__CLEAR_ALL__") {
                            clearCacheForKvId(changedKvId)
                            notifyListenersForKvId(changedKvId)
                            return
                        }

                        // 处理批量更新
                        if (changedKey == "__BATCH_UPDATE__") {
                            clearCacheForKvId(changedKvId)
                            notifyListenersForKvId(changedKvId)
                            return
                        }

                        val cacheKey = "${changedKvId}_$changedKey"

                        // 清除缓存
                        cache.remove(cacheKey)
                        if (enableSpCache) {
                            spCache?.edit(true) { remove(cacheKey) }
                        }

                        // 通知监听器
                        val listenerKey = "${changedKvId}_$changedKey"
                        listeners[listenerKey]?.removeAll { ref ->
                            val listener = ref.get()
                            if (listener == null) {
                                true // 移除已被回收的引用
                            } else {
                                try {
                                    listener(changedKey, null)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Listener error", e)
                                }
                                false
                            }
                        }
                    }
                }
            }

            try {
                val filter = IntentFilter(ACTION_KV_CHANGED)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    systemContext.registerReceiver(
                        broadcastReceiver, filter, Context.RECEIVER_EXPORTED
                    )
                } else {
                    systemContext.registerReceiver(broadcastReceiver, filter)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register broadcast receiver", e)
            }
        }
    }

    /**
     * 清除指定kvId的所有缓存
     */
    private fun clearCacheForKvId(kvId: String) {
        val keysToRemove = cache.keys.filter { it.startsWith("${kvId}_") }
        keysToRemove.forEach { cache.remove(it) }
        if (enableSpCache) {
            spCache?.edit(true) {
                keysToRemove.forEach { remove(it) }
            }
        }
    }

    /**
     * 通知指定kvId的所有监听器
     */
    private fun notifyListenersForKvId(kvId: String) {
        listeners.keys.filter { it.startsWith("${kvId}_") }.forEach { listenerKey ->
            listeners[listenerKey]?.removeAll { ref ->
                val listener = ref.get()
                if (listener == null) {
                    true
                } else {
                    try {
                        listener("", null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Listener error", e)
                    }
                    false
                }
            }
        }
    }

    /**
     * 构建ContentProvider URI
     */
    private fun buildGetUri(kvId: String, key: String, type: String, default: String? = null): Uri {
        val builder = "content://$MODULE_AUTHORITY/get/$kvId/$key".toUri().buildUpon()
            .appendQueryParameter("type", type)
        default?.let { builder.appendQueryParameter("default", it) }
        return builder.build()
    }

    private fun buildPutUri(kvId: String, key: String, type: String, value: String): Uri {
        return "content://$MODULE_AUTHORITY/put/$kvId/$key".toUri().buildUpon()
            .appendQueryParameter("type", type).appendQueryParameter("value", value).build()
    }

    /**
     * 获取缓存键
     */
    private fun getCacheKey(kvId: String, key: String): String = "${kvId}_$key"

    /**
     * 创建KV工具类实例
     * @param kvId KV标识
     * @param enableSharedPreferencesCache 是否为此实例启用SharedPreferences缓存，默认使用全局设置
     */
    fun createKVHelper(
        kvId: String = "SHARED_SETTINGS", enableSharedPreferencesCache: Boolean = enableSpCache
    ): HostKVHelper {
        return HostKVHelper(kvId, enableSharedPreferencesCache)
    }

    /**
     * 宿主端KV工具类
     */
    class HostKVHelper(private val kvId: String, private val enableSpCache: Boolean) {

        // String操作
        fun putString(key: String, value: String) {
            try {
                val uri = buildPutUri(kvId, key, "string", value)
                systemContext.contentResolver.insert(uri, null)
                val cacheKey = getCacheKey(kvId, key)
                cache.put(cacheKey, value)
                if (enableSpCache) {
                    spCache?.edit(true) { putString(cacheKey, value) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getString(key: String, default: String = ""): String {
            val cacheKey = getCacheKey(kvId, key)
            cache[cacheKey]?.let { return it as String }
            if (enableSpCache) {
                spCache?.getString(cacheKey, null)?.let {
                    cache.put(cacheKey, it)
                    return it
                }
            }
            return try {
                val uri = buildGetUri(kvId, key, "string", default)
                val cursor = systemContext.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val value = it.getString(0)
                        cache.put(cacheKey, value)
                        if (enableSpCache) {
                            spCache?.edit(true) { putString(cacheKey, value) }
                        }
                        value
                    } else default
                } ?: default
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get string: $key", e)
                default
            }
        }

        // Int操作
        fun putInt(key: String, value: Int) {
            try {
                val uri = buildPutUri(kvId, key, "int", value.toString())
                systemContext.contentResolver.insert(uri, null)
                val cacheKey = getCacheKey(kvId, key)
                cache.put(cacheKey, value)
                if (enableSpCache) {
                    spCache?.edit(true) { putInt(cacheKey, value) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getInt(key: String, default: Int = 0): Int {
            val cacheKey = getCacheKey(kvId, key)
            cache[cacheKey]?.let { return it as Int }
            if (enableSpCache && spCache?.contains(cacheKey) == true) {
                val value = spCache?.getInt(cacheKey, default) ?: default
                cache.put(cacheKey, value)
                return value
            }
            return try {
                val uri = buildGetUri(kvId, key, "int", default.toString())
                val cursor = systemContext.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val value = it.getString(0).toInt()
                        cache.put(cacheKey, value)
                        if (enableSpCache) {
                            spCache?.edit(true) { putInt(cacheKey, value) }
                        }
                        value
                    } else default
                } ?: default
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get int: $key", e)
                default
            }
        }

        // Long操作
        fun putLong(key: String, value: Long) {
            try {
                val uri = buildPutUri(kvId, key, "long", value.toString())
                systemContext.contentResolver.insert(uri, null)
                val cacheKey = getCacheKey(kvId, key)
                cache.put(cacheKey, value)
                if (enableSpCache) {
                    spCache?.edit(true) { putLong(cacheKey, value) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getLong(key: String, default: Long = 0L): Long {
            val cacheKey = getCacheKey(kvId, key)
            cache[cacheKey]?.let { return it as Long }
            if (enableSpCache && spCache?.contains(cacheKey) == true) {
                val value = spCache?.getLong(cacheKey, default) ?: default
                cache.put(cacheKey, value)
                return value
            }
            return try {
                val uri = buildGetUri(kvId, key, "long", default.toString())
                val cursor = systemContext.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val value = it.getString(0).toLong()
                        cache.put(cacheKey, value)
                        if (enableSpCache) {
                            spCache?.edit(true) { putLong(cacheKey, value) }
                        }
                        value
                    } else default
                } ?: default
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get long: $key", e)
                default
            }
        }

        // Boolean操作
        fun putBoolean(key: String, value: Boolean) {
            try {
                val uri = buildPutUri(kvId, key, "boolean", value.toString())
                systemContext.contentResolver.insert(uri, null)
                val cacheKey = getCacheKey(kvId, key)
                cache.put(cacheKey, value)
                if (enableSpCache) {
                    spCache?.edit(true) { putBoolean(cacheKey, value) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getBoolean(key: String, default: Boolean = false): Boolean {
            val cacheKey = getCacheKey(kvId, key)
            cache[cacheKey]?.let { return it as Boolean }
            if (enableSpCache && spCache?.contains(cacheKey) == true) {
                val value = spCache?.getBoolean(cacheKey, default) ?: default
                cache.put(cacheKey, value)
                return value
            }
            return try {
                val uri = buildGetUri(kvId, key, "boolean", default.toString())
                val cursor = systemContext.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val value = it.getString(0).toBoolean()
                        cache.put(cacheKey, value)
                        if (enableSpCache) {
                            spCache?.edit(true) { putBoolean(cacheKey, value) }
                        }
                        value
                    } else default
                } ?: default
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get boolean: $key", e)
                default
            }
        }

        // Float操作
        fun putFloat(key: String, value: Float) {
            try {
                val uri = buildPutUri(kvId, key, "float", value.toString())
                systemContext.contentResolver.insert(uri, null)
                val cacheKey = getCacheKey(kvId, key)
                cache.put(cacheKey, value)
                if (enableSpCache) {
                    spCache?.edit(true) { putFloat(cacheKey, value) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getFloat(key: String, default: Float = 0f): Float {
            val cacheKey = getCacheKey(kvId, key)
            cache[cacheKey]?.let { return it as Float }
            if (enableSpCache && spCache?.contains(cacheKey) == true) {
                val value = spCache?.getFloat(cacheKey, default) ?: default
                cache.put(cacheKey, value)
                return value
            }
            return try {
                val uri = buildGetUri(kvId, key, "float", default.toString())
                val cursor = systemContext.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val value = it.getString(0).toFloat()
                        cache.put(cacheKey, value)
                        if (enableSpCache) {
                            spCache?.edit(true) { putFloat(cacheKey, value) }
                        }
                        value
                    } else default
                } ?: default
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get float: $key", e)
                default
            }
        }

        // Double操作
        fun putDouble(key: String, value: Double) {
            try {
                val uri = buildPutUri(kvId, key, "double", value.toString())
                systemContext.contentResolver.insert(uri, null)
                val cacheKey = getCacheKey(kvId, key)
                cache.put(cacheKey, value)
                if (enableSpCache) {
                    spCache?.edit(true) { putString(cacheKey, value.toString()) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getDouble(key: String, default: Double = 0.0): Double {
            val cacheKey = getCacheKey(kvId, key)
            cache[cacheKey]?.let { return it as Double }
            if (enableSpCache && spCache?.contains(cacheKey) == true) {
                val value = Double.fromBits(
                    spCache?.getLong(cacheKey, default.toRawBits()) ?: default.toRawBits()
                )
                cache.put(cacheKey, value)
                return value
            }
            return try {
                val uri = buildGetUri(kvId, key, "double", default.toRawBits().toString())
                val cursor = systemContext.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val value = Double.fromBits(it.getString(0).toLong())
                        cache.put(cacheKey, value)
                        if (enableSpCache) {
                            spCache?.edit(true) { putLong(cacheKey, value.toRawBits()) }
                        }
                        value
                    } else default
                } ?: default
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get double: $key", e)
                default
            }
        }

        /**
         * 批量操作
         */
        fun putAll(values: Map<String, Any>) {
            values.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Float -> putFloat(key, value)
                    is Double -> putDouble(key, value)
                    else -> Log.w("", "Unsupported type for key: $key, value: $value")
                }
            }
        }

        /**
         * 检查键是否存在
         */
        fun contains(key: String): Boolean {
            return try {
                val uri = buildGetUri(kvId, key, "contains", "")
                val cursor = systemContext.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    it.moveToFirst() && it.getString(0) == "true"
                } ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check contains: $key", e)
                false
            }
        }

        /**
         * 删除指定键
         */
        fun remove(key: String): Boolean {
            return try {
                val uri = buildPutUri(kvId, key, "remove", "")
                systemContext.contentResolver.insert(uri, null)
                val cacheKey = getCacheKey(kvId, key)
                cache.remove(cacheKey)
                if (enableSpCache) {
                    spCache?.edit(true) { remove(cacheKey) }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove: $key", e)
                false
            }
        }

        /**
         * 添加变化监听器
         */
        fun addChangeListener(key: String, listener: (String, Any?) -> Unit) {
            val listenerKey = "${kvId}_$key"
            listeners.getOrPut(listenerKey) { mutableListOf() }.add(WeakReference(listener))
        }

        /**
         * 移除变化监听器
         */
        fun removeChangeListener(key: String, listener: (String, Any?) -> Unit) {
            val listenerKey = "${kvId}_$key"
            listeners[listenerKey]?.removeAll { it.get() == listener || it.get() == null }
        }

        /**
         * 清除缓存
         */
        fun clearCache() {
            clearCacheForKvId(kvId)
        }

        /**
         * 清除所有数据
         */
        fun clearAll(): Boolean {
            return try {
                val uri = buildPutUri(kvId, "__CLEAR_ALL__", "clear", "")
                systemContext.contentResolver.insert(uri, null)
                clearCache()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear all", e)
                false
            }
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        broadcastReceiver?.let { receiver ->
            try {
                systemContext.unregisterReceiver(receiver)
                broadcastReceiver = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister receiver", e)
            }
        }
        cache.clear()
        listeners.clear()
        spCache = null
    }
}