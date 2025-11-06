package website.xihan.kv

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri


/**
 * 模块独立的ContentProvider，在模块Activity中注册
 */
class KVContentProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY = "website.xihan.kv"
        private const val CODE_GET = 1
        private const val CODE_PUT = 2
        private const val TAG = "KVContentProvider"

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "get/*/*", CODE_GET) // kvId/key
            addURI(AUTHORITY, "put/*/*", CODE_PUT) // kvId/key
        }

        fun buildUri(operation: String, kvId: String, key: String): Uri {
            return "content://$AUTHORITY/$operation/$kvId/$key".toUri()
        }

        fun getAuthority(): String = AUTHORITY
    }

    override fun onCreate(): Boolean {
        // 初始化模块存储
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return try {
            when (uriMatcher.match(uri)) {
                CODE_GET -> {
                    val pathSegments = uri.pathSegments
                    if (pathSegments.size < 3) {
                        Log.e(TAG, "Invalid URI: $uri")
                        return null
                    }
                    val kvId = pathSegments[1]
                    val key = pathSegments[2]
                    val type = uri.getQueryParameter("type") ?: "string"
                    val default = uri.getQueryParameter("default")

                    val cursor = MatrixCursor(arrayOf("value"))
                    val value = when (type) {
                        "string" -> KVStorage.getString(kvId, key, default ?: "")
                        "int" -> KVStorage.getInt(kvId, key, default?.toIntOrNull() ?: 0).toString()
                        "long" -> KVStorage.getLong(kvId, key, default?.toLongOrNull() ?: 0L).toString()
                        "boolean" -> KVStorage.getBoolean(
                            kvId,
                            key,
                            default?.toBooleanStrictOrNull() ?: false
                        ).toString()
                        "float" -> KVStorage.getFloat(kvId, key, default?.toFloatOrNull() ?: 0f)
                            .toString()
                        "double" -> KVStorage.getDouble(kvId, key, default?.toDoubleOrNull() ?: 0.0)
                            .toRawBits().toString()
                        "contains" -> KVStorage.contains(kvId, key).toString()
                        else -> {
                            Log.w(TAG, "Unknown type: $type")
                            ""
                        }
                    }
                    cursor.addRow(arrayOf(value))
                    cursor
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Query failed", e)
            null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return try {
            when (uriMatcher.match(uri)) {
                CODE_PUT -> {
                    val pathSegments = uri.pathSegments
                    if (pathSegments.size < 3) {
                        Log.e(TAG, "Invalid URI: $uri")
                        return null
                    }
                    val kvId = pathSegments[1]
                    val key = pathSegments[2]
                    val type = uri.getQueryParameter("type") ?: "string"
                    val value = uri.getQueryParameter("value")

                    when (type) {
                        "string" -> value?.let { KVStorage.putString(kvId, key, it) }
                        "int" -> value?.toIntOrNull()?.let { KVStorage.putInt(kvId, key, it) }
                        "long" -> value?.toLongOrNull()?.let { KVStorage.putLong(kvId, key, it) }
                        "boolean" -> value?.toBooleanStrictOrNull()?.let { KVStorage.putBoolean(kvId, key, it) }
                        "float" -> value?.toFloatOrNull()?.let { KVStorage.putFloat(kvId, key, it) }
                        "double" -> value?.toLongOrNull()?.let { KVStorage.putDouble(kvId, key, Double.fromBits(it)) }
                        "remove" -> KVStorage.remove(kvId, key)
                        "clear" -> KVStorage.clearAll(kvId)
                        else -> Log.w(TAG, "Unknown type: $type")
                    }
                    uri
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Insert failed", e)
            null
        }
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun getType(uri: Uri): String? = null
}