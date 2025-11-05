package website.xihan.kv


import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap

/**
 * 模块端KV存储实现
 */
object KVStorage : KoinComponent {

    private val context: Context by inject()
    private val prefCache = ConcurrentHashMap<String, SharedPreferences>()


    /**
     * 获取SharedPreferences实例
     */
    private fun getPrefs(kvId: String): SharedPreferences {
        return prefCache.getOrPut(kvId) {
            val prefName = if (kvId.isEmpty()) "kv_default" else "kv_$kvId"
            context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        }
    }

    // String操作
    fun putString(kvId: String, key: String, value: String): Boolean {
        return try {
            getPrefs(kvId).edit(true) { putString(key, value) }
            KVSyncManager.notifyChange(kvId, key)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getString(kvId: String, key: String, default: String = ""): String {
        return try {
            getPrefs(kvId).getString(key, default) ?: default
        } catch (e: Exception) {
            e.printStackTrace()
            default
        }
    }

    // Int操作
    fun putInt(kvId: String, key: String, value: Int): Boolean {
        return try {
            getPrefs(kvId).edit(true) { putInt(key, value) }
            KVSyncManager.notifyChange(kvId, key)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getInt(kvId: String, key: String, default: Int = 0): Int {
        return try {
            getPrefs(kvId).getInt(key, default)
        } catch (e: Exception) {
            e.printStackTrace()
            default
        }
    }

    // Long操作
    fun putLong(kvId: String, key: String, value: Long): Boolean {
        return try {
            getPrefs(kvId).edit(true) { putLong(key, value) }
            KVSyncManager.notifyChange(kvId, key)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getLong(kvId: String, key: String, default: Long = 0L): Long {
        return try {
            getPrefs(kvId).getLong(key, default)
        } catch (e: Exception) {
            e.printStackTrace()
            default
        }
    }

    // Boolean操作
    fun putBoolean(kvId: String, key: String, value: Boolean): Boolean {
        return try {
            getPrefs(kvId).edit(true) { putBoolean(key, value) }
            KVSyncManager.notifyChange(kvId, key)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getBoolean(kvId: String, key: String, default: Boolean = false): Boolean {
        return try {
            getPrefs(kvId).getBoolean(key, default)
        } catch (e: Exception) {
            e.printStackTrace()
            default
        }
    }

    // Float操作
    fun putFloat(kvId: String, key: String, value: Float): Boolean {
        return try {
            getPrefs(kvId).edit(true) { putFloat(key, value) }
            KVSyncManager.notifyChange(kvId, key)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getFloat(kvId: String, key: String, default: Float = 0f): Float {
        return try {
            getPrefs(kvId).getFloat(key, default)
        } catch (e: Exception) {
            e.printStackTrace()
            default
        }
    }

    // Double操作 (使用String存储)
    fun putDouble(kvId: String, key: String, value: Double): Boolean {
        return try {
            getPrefs(kvId).edit(true) { putString("${key}_double", value.toString()) }
            KVSyncManager.notifyChange(kvId, key)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getDouble(kvId: String, key: String, default: Double = 0.0): Double {
        return try {
            val stringValue = getPrefs(kvId).getString("${key}_double", null)
            stringValue?.toDoubleOrNull() ?: default
        } catch (e: Exception) {
            e.printStackTrace()
            default
        }
    }

    // StringSet操作
    fun putStringSet(kvId: String, key: String, value: Set<String>): Boolean {
        return try {
            getPrefs(kvId).edit(true) { putStringSet(key, value) }
            KVSyncManager.notifyChange(kvId, key)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getStringSet(kvId: String, key: String, default: Set<String>): Set<String> {
        return try {
            getPrefs(kvId).getStringSet(key, default) ?: default
        } catch (e: Exception) {
            e.printStackTrace()
            default
        }
    }


    /**
     * 获取所有KV数据
     */
    fun getAllKV(kvId: String): Map<String, Any?> {
        return try {
            val prefs = getPrefs(kvId)
            val all = prefs.all
            val result = mutableMapOf<String, Any?>()

            all.forEach { (key, value) ->
                when {
                    key.endsWith("_double") -> {
                        val originalKey = key.removeSuffix("_double")
                        result[originalKey] = (value as? String)?.toDoubleOrNull()
                    }

                    key.endsWith("_bytes") -> {
                        val originalKey = key.removeSuffix("_bytes")
                        try {
                            val encoded = value as? String
                            if (encoded != null) {
                                result[originalKey] = Base64.decode(encoded, Base64.DEFAULT)
                            }
                        } catch (e: Exception) {
                            // 忽略解码错误
                        }
                    }

                    key.endsWith("_parcelable") -> {
                        val originalKey = key.removeSuffix("_parcelable")
                        result[originalKey] = value as? String // 返回JSON字符串
                    }

                    else -> {
                        result[key] = value
                    }
                }
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    /**
     * 清除所有数据
     */
    fun clearAll(kvId: String) {
        try {
            getPrefs(kvId).edit(true) { clear() }
            KVSyncManager.notifyChange(kvId, "")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 批量操作
     */
    fun putAll(kvId: String, values: Map<String, Any>) {
        try {
            getPrefs(kvId).edit(true) {
                values.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Boolean -> putBoolean(key, value)
                        is Float -> putFloat(key, value)
                        is Double -> putString("${key}_double", value.toString())
                        is Set<*> -> putStringSet(key, value as Set<String>)
                        is ByteArray -> {
                            val encoded = Base64.encodeToString(value, Base64.DEFAULT)
                            putString("${key}_bytes", encoded)
                        }
                    }
                }
            }
            values.keys.forEach { key ->
                KVSyncManager.notifyChange(kvId, key)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}