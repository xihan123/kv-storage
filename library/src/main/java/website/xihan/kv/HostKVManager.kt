package website.xihan.kv

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap

/**
 * 宿主端KV管理器，通过systemContext访问模块的ContentProvider
 */
object HostKVManager : KoinComponent {


//    private lateinit var systemContext: Context

    private val systemContext: Context by inject()
    private val cache = ConcurrentHashMap<String, Any>()
    private val listeners = ConcurrentHashMap<String, MutableList<(String, Any?) -> Unit>>()
    private var broadcastReceiver: BroadcastReceiver? = null

    private const val MODULE_AUTHORITY = "website.xihan.kv"
    private const val ACTION_KV_CHANGED = "${MODULE_AUTHORITY}.KV_CHANGED"
    private const val EXTRA_KV_ID = "kvId"
    private const val EXTRA_KEY = "key"

    /**
     * 初始化宿主端KV管理器
     */
    fun init() {
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
                        val cacheKey = "${changedKvId}_$changedKey"
                        //Log.d("KV changed: $changedKvId, key: $changedKey")

                        // 清除缓存
                        cache.remove(cacheKey)

                        // 通知监听器
                        val listenerKey = "${changedKvId}_$changedKey"
                        listeners[listenerKey]?.forEach { listener ->
                            listener(changedKey, null) // 传递null表示需要重新获取值
                        }
                    }
                }
            }

            val filter = IntentFilter(ACTION_KV_CHANGED)
            // android 12 以上
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                systemContext.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                systemContext.registerReceiver(broadcastReceiver, filter)
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
     */
    fun createKVHelper(kvId: String = "SHARED_SETTINGS"): HostKVHelper {
        return HostKVHelper(kvId)
    }

    /**
     * 宿主端KV工具类
     */
    class HostKVHelper(private val kvId: String) {

        // String操作
        fun putString(key: String, value: String) {
            try {
                val uri = buildPutUri(kvId, key, "string", value)
                systemContext.contentResolver.insert(uri, null)
//                cache[getCacheKey(kvId, key)] = value
                cache.put(getCacheKey(kvId, key), value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getString(key: String, default: String = ""): String {
            val cacheKey = getCacheKey(kvId, key)
            return cache[cacheKey] as? String ?: try {
                val uri = buildGetUri(kvId, key, "string", default)
                val cursor = systemContext.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val value = it.getString(0)
//                        cache[cacheKey] = value
                        cache.put(cacheKey, value)
                        value
                    } else default
                } ?: default
            } catch (e: Exception) {
                e.printStackTrace()
                default
            }
        }

        // Int操作
        fun putInt(key: String, value: Int) {
            try {
                val uri = buildPutUri(kvId, key, "int", value.toString())
                systemContext.contentResolver.insert(uri, null)
//                cache[getCacheKey(kvId, key)] = value
                cache.put(getCacheKey(kvId, key), value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getInt(key: String, default: Int = 0): Int {
            val cacheKey = getCacheKey(kvId, key)
            return cache[cacheKey] as? Int ?: try {
                val uri = buildGetUri(kvId, key, "int", default.toString())
                val cursor = systemContext.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val value = it.getString(0).toInt()
                        //cache[cacheKey] = value
                        cache.put(cacheKey, value)
                        value
                    } else default
                } ?: default
            } catch (e: Exception) {
                e.printStackTrace()
                default
            }
        }

        // Long操作
        fun putLong(key: String, value: Long) {
            try {
                val uri = buildPutUri(kvId, key, "long", value.toString())
                systemContext.contentResolver.insert(uri, null)
                //cache[getCacheKey(kvId, key)] = value
                cache.put(getCacheKey(kvId, key), value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getLong(key: String, default: Long = 0L): Long {
            val cacheKey = getCacheKey(kvId, key)
            return cache[cacheKey] as? Long ?: try {
                val uri = buildGetUri(kvId, key, "long", default.toString())
                val cursor = systemContext.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val value = it.getString(0).toLong()
                        //cache[cacheKey] = value
                        cache.put(cacheKey, value)
                        value
                    } else default
                } ?: default
            } catch (e: Exception) {
                e.printStackTrace()
                default
            }
        }

        // Boolean操作
        fun putBoolean(key: String, value: Boolean) {
            try {
                val uri = buildPutUri(kvId, key, "boolean", value.toString())
                systemContext.contentResolver.insert(uri, null)
//                cache[getCacheKey(kvId, key)] = value
                cache.put(getCacheKey(kvId, key), value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getBoolean(key: String, default: Boolean = false): Boolean {
            val cacheKey = getCacheKey(kvId, key)
            return cache[cacheKey] as? Boolean ?: try {
                val uri = buildGetUri(kvId, key, "boolean", default.toString())
                val cursor = systemContext.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val value = it.getString(0).toBoolean()
                        //cache[cacheKey] = value
                        cache.put(cacheKey, value)
                        value
                    } else default
                } ?: default
            } catch (e: Exception) {
                e.printStackTrace()
                default
            }
        }

        // Float操作
        fun putFloat(key: String, value: Float) {
            try {
                val uri = buildPutUri(kvId, key, "float", value.toString())
                systemContext.contentResolver.insert(uri, null)
                //cache[getCacheKey(kvId, key)] = value
                cache.put(getCacheKey(kvId, key), value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getFloat(key: String, default: Float = 0f): Float {
            val cacheKey = getCacheKey(kvId, key)
            return cache[cacheKey] as? Float ?: try {
                val uri = buildGetUri(kvId, key, "float", default.toString())
                val cursor = systemContext.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val value = it.getString(0).toFloat()
                        //cache[cacheKey] = value
                        cache.put(cacheKey, value)
                        value
                    } else default
                } ?: default
            } catch (e: Exception) {
                e.printStackTrace()
                default
            }
        }

        // Double操作
        fun putDouble(key: String, value: Double) {
            try {
                val uri = buildPutUri(kvId, key, "double", value.toString())
                systemContext.contentResolver.insert(uri, null)
                //cache[getCacheKey(kvId, key)] = value
                cache.put(getCacheKey(kvId, key), value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getDouble(key: String, default: Double = 0.0): Double {
            val cacheKey = getCacheKey(kvId, key)
            return cache[cacheKey] as? Double ?: try {
                val uri = buildGetUri(kvId, key, "double", default.toString())
                val cursor = systemContext.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val value = it.getString(0).toDouble()
                        //cache[cacheKey] = value
                        cache.put(cacheKey, value)
                        value
                    } else default
                } ?: default
            } catch (e: Exception) {
                e.printStackTrace()
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
         * 添加变化监听器
         */
        fun addChangeListener(key: String, listener: (String, Any?) -> Unit) {
            val listenerKey = "${kvId}_$key"
            listeners.getOrPut(listenerKey) { mutableListOf() }.add(listener)
        }

        /**
         * 移除变化监听器
         */
        fun removeChangeListener(key: String, listener: (String, Any?) -> Unit) {
            val listenerKey = "${kvId}_$key"
            listeners[listenerKey]?.remove(listener)
        }

        /**
         * 清除缓存
         */
        fun clearCache() {
            cache.clear()
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
                e.printStackTrace()
            }
        }
        cache.clear()
        listeners.clear()
    }
}