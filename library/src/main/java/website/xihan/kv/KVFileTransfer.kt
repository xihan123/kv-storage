package website.xihan.kv

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * 文件传输工具类 - 模块端使用
 */
object KVFileTransfer : KoinComponent {
    private val context: Context by inject()

    /**
     * 保存文件URI并授予目标应用权限
     * @param targetPackage 目标应用包名
     * @param uri 文件URI
     * @param kvId KV存储ID
     * @param key 存储键名
     */
    fun saveFileUri(
        targetPackage: String,
        uri: Uri,
        kvId: String = "SHARED_SETTINGS",
        key: String = "fileUri"
    ) {
        context.grantUriPermission(
            targetPackage,
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        KVStorage.putString(kvId, key, uri.toString())
    }
}

/**
 * 文件接收工具类 - 宿主端使用
 */
class KVFileReceiver(private val context: Context) {

    /**
     * 读取文件URI并复制到指定目录
     * @param kvId KV存储ID
     * @param key 存储键名
     * @param targetDir 目标目录，默认为应用私有目录
     * @return 复制后的文件，如果失败返回null
     */
    fun receiveFile(
        kvId: String = "SHARED_SETTINGS",
        key: String = "fileUri",
        targetDir: File = context.filesDir
    ): File? {
        return try {
            val uriStr = HostKVManager.createKVHelper(kvId).getString(key)
            if (uriStr.isEmpty()) return null

            val uri = uriStr.toUri()
            context.contentResolver.openInputStream(uri)?.use { input ->
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "downloaded_file"
                val outputFile = File(targetDir, fileName)
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
                outputFile
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 监听文件URI变化并自动复制
     * @param kvId KV存储ID
     * @param key 存储键名
     * @param targetDir 目标目录
     * @param onReceived 接收成功回调
     */
    fun observeFile(
        kvId: String = "SHARED_SETTINGS",
        key: String = "fileUri",
        targetDir: File = context.filesDir,
        onReceived: (File) -> Unit
    ) {
        HostKVManager.createKVHelper(kvId).addChangeListener(key) { _, any ->
            var uriStr = any as? String
            if (uriStr.isNullOrEmpty()) {
                val helper = HostKVManager.createKVHelper(kvId)
                uriStr = helper.getString(key)
                if (uriStr.isEmpty()) return@addChangeListener
            }

            try {
                val uri = uriStr.toUri()
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val fileName =
                        uri.lastPathSegment?.substringAfterLast('/') ?: "downloaded_file"
                    val outputFile = File(targetDir, fileName)
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    onReceived(outputFile)
                }
            } catch (_: Exception) {
                // Ignore
            }
        }
    }
}
