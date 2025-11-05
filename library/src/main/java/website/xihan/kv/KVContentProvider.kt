package website.xihan.kv

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.core.net.toUri


/**
 * 模块独立的ContentProvider，在模块Activity中注册
 */
class KVContentProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY = "website.xihan.kv"
        private const val CODE_GET = 1
        private const val CODE_PUT = 2

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
        return when (uriMatcher.match(uri)) {
            CODE_GET -> {
                val pathSegments = uri.pathSegments
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
                        .toString()

                    else -> ""
                }
                cursor.addRow(arrayOf(value))
                cursor
            }

            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        when (uriMatcher.match(uri)) {
            CODE_PUT -> {
                val pathSegments = uri.pathSegments
                val kvId = pathSegments[1]
                val key = pathSegments[2]
                val type = uri.getQueryParameter("type") ?: "string"
                val value = uri.getQueryParameter("value") ?: return null

                when (type) {
                    "string" -> KVStorage.putString(kvId, key, value)
                    "int" -> KVStorage.putInt(kvId, key, value.toInt())
                    "long" -> KVStorage.putLong(kvId, key, value.toLong())
                    "boolean" -> KVStorage.putBoolean(kvId, key, value.toBoolean())
                    "float" -> KVStorage.putFloat(kvId, key, value.toFloat())
                    "double" -> KVStorage.putDouble(kvId, key, value.toDouble())
                }
                return uri
            }
        }
        return null
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