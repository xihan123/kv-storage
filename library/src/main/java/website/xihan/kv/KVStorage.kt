package website.xihan.kv


import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 模块端KV存储实现
 */
object KVStorage : KoinComponent {

    private val context: Context by inject()
    private val prefCache = ConcurrentHashMap<String, SharedPreferences>()
    private val locks = ConcurrentHashMap<String, ReentrantReadWriteLock>()
    private const val TAG = "KVStorage"
    private const val MODULE_AUTHORITY = "website.xihan.kv"


    /**
     * 获取SharedPreferences实例
     */
    private fun getPrefs(kvId: String): SharedPreferences {
        return prefCache.getOrPut(kvId) {
            val prefName = if (kvId.isEmpty()) "kv_default" else "kv_$kvId"
            context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        }
    }

    /**
     * 获取读写锁
     */
    private fun getLock(kvId: String): ReentrantReadWriteLock {
        return locks.getOrPut(kvId) { ReentrantReadWriteLock() }
    }

    /**
     * 检查键是否存在
     */
    fun contains(kvId: String, key: String): Boolean {
        return getLock(kvId).read {
            getPrefs(kvId).contains(key)
        }
    }

    /**
     * 删除指定键
     */
    fun remove(kvId: String, key: String): Boolean {
        return getLock(kvId).write {
            try {
                getPrefs(kvId).edit(true) { remove(key) }
                KVSyncManager.notifyChange(kvId, key)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove key: $key", e)
                false
            }
        }
    }

    // String操作
    fun putString(kvId: String, key: String, value: String): Boolean {
        return getLock(kvId).write {
            try {
                getPrefs(kvId).edit(true) { putString(key, value) }
                KVSyncManager.notifyChange(kvId, key)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to put string: $key", e)
                false
            }
        }
    }

    fun getString(kvId: String, key: String, default: String = ""): String {
        return getLock(kvId).read {
            try {
                getPrefs(kvId).getString(key, default) ?: default
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get string: $key", e)
                default
            }
        }
    }

    // Int操作
    fun putInt(kvId: String, key: String, value: Int): Boolean {
        return getLock(kvId).write {
            try {
                getPrefs(kvId).edit(true) { putInt(key, value) }
                KVSyncManager.notifyChange(kvId, key)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to put int: $key", e)
                false
            }
        }
    }

    fun getInt(kvId: String, key: String, default: Int = 0): Int {
        return getLock(kvId).read {
            try {
                getPrefs(kvId).getInt(key, default)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get int: $key", e)
                default
            }
        }
    }

    // Long操作
    fun putLong(kvId: String, key: String, value: Long): Boolean {
        return getLock(kvId).write {
            try {
                getPrefs(kvId).edit(true) { putLong(key, value) }
                KVSyncManager.notifyChange(kvId, key)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to put long: $key", e)
                false
            }
        }
    }

    fun getLong(kvId: String, key: String, default: Long = 0L): Long {
        return getLock(kvId).read {
            try {
                getPrefs(kvId).getLong(key, default)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get long: $key", e)
                default
            }
        }
    }

    // Boolean操作
    fun putBoolean(kvId: String, key: String, value: Boolean): Boolean {
        return getLock(kvId).write {
            try {
                getPrefs(kvId).edit(true) { putBoolean(key, value) }
                KVSyncManager.notifyChange(kvId, key)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to put boolean: $key", e)
                false
            }
        }
    }

    fun getBoolean(kvId: String, key: String, default: Boolean = false): Boolean {
        return getLock(kvId).read {
            try {
                getPrefs(kvId).getBoolean(key, default)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get boolean: $key", e)
                default
            }
        }
    }

    // Float操作
    fun putFloat(kvId: String, key: String, value: Float): Boolean {
        return getLock(kvId).write {
            try {
                getPrefs(kvId).edit(true) { putFloat(key, value) }
                KVSyncManager.notifyChange(kvId, key)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to put float: $key", e)
                false
            }
        }
    }

    fun getFloat(kvId: String, key: String, default: Float = 0f): Float {
        return getLock(kvId).read {
            try {
                getPrefs(kvId).getFloat(key, default)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get float: $key", e)
                default
            }
        }
    }

    // Double操作 (使用Long存储bits)
    fun putDouble(kvId: String, key: String, value: Double): Boolean {
        return getLock(kvId).write {
            try {
                getPrefs(kvId).edit(true) { putLong(key, value.toRawBits()) }
                KVSyncManager.notifyChange(kvId, key)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to put double: $key", e)
                false
            }
        }
    }

    fun getDouble(kvId: String, key: String, default: Double = 0.0): Double {
        return getLock(kvId).read {
            try {
                if (!getPrefs(kvId).contains(key)) return@read default
                Double.fromBits(getPrefs(kvId).getLong(key, default.toRawBits()))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get double: $key", e)
                default
            }
        }
    }

    // StringSet操作
    fun putStringSet(kvId: String, key: String, value: Set<String>): Boolean {
        return getLock(kvId).write {
            try {
                getPrefs(kvId).edit(true) { putStringSet(key, value) }
                KVSyncManager.notifyChange(kvId, key)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to put string set: $key", e)
                false
            }
        }
    }

    fun getStringSet(kvId: String, key: String, default: Set<String>): Set<String> {
        return getLock(kvId).read {
            try {
                getPrefs(kvId).getStringSet(key, default)?.toSet() ?: default
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get string set: $key", e)
                default
            }
        }
    }


    /**
     * 获取所有KV数据
     */
    fun getAllKV(kvId: String): Map<String, Any?> {
        return getLock(kvId).read {
            try {
                val prefs = getPrefs(kvId)
                val all = prefs.all
                val result = mutableMapOf<String, Any?>()

                all.forEach { (key, value) ->
                    when {
                        key.endsWith("_bytes") -> {
                            val originalKey = key.removeSuffix("_bytes")
                            try {
                                val encoded = value as? String
                                if (encoded != null) {
                                    result[originalKey] = Base64.decode(encoded, Base64.DEFAULT)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to decode bytes for key: $originalKey", e)
                            }
                        }

                        key.endsWith("_parcelable") -> {
                            val originalKey = key.removeSuffix("_parcelable")
                            result[originalKey] = value as? String
                        }

                        else -> {
                            result[key] = value
                        }
                    }
                }
                result
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get all KV", e)
                emptyMap()
            }
        }
    }

    /**
     * 清除所有数据
     */
    fun clearAll(kvId: String): Boolean {
        return getLock(kvId).write {
            try {
                getPrefs(kvId).edit(true) { clear() }
                KVSyncManager.notifyChange(kvId, "__CLEAR_ALL__")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear all", e)
                false
            }
        }
    }

    /**
     * 批量操作
     */
    fun putAll(kvId: String, values: Map<String, Any>): Boolean {
        return getLock(kvId).write {
            try {
                getPrefs(kvId).edit(true) {
                    values.forEach { (key, value) ->
                        when (value) {
                            is String -> putString(key, value)
                            is Int -> putInt(key, value)
                            is Long -> putLong(key, value)
                            is Boolean -> putBoolean(key, value)
                            is Float -> putFloat(key, value)
                            is Double -> putLong(key, value.toRawBits())
                            is Set<*> -> putStringSet(key, value as Set<String>)
                            is ByteArray -> {
                                val encoded = Base64.encodeToString(value, Base64.DEFAULT)
                                putString("${key}_bytes", encoded)
                            }
                            else -> Log.w(TAG, "Unsupported type for key: $key")
                        }
                    }
                }
                KVSyncManager.notifyChange(kvId, "__BATCH_UPDATE__")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to put all", e)
                false
            }
        }
    }
}