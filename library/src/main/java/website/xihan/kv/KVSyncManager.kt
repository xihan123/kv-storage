package website.xihan.kv

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * 同步管理器，通过广播实现跨进程配置变化通知
 */
object KVSyncManager : KoinComponent {

    private const val ACTION_KV_CHANGED = "website.xihan.kv.KV_CHANGED"
    private const val EXTRA_KV_ID = "kvId"
    private const val EXTRA_KEY = "key"
    private const val TAG = "KVSyncManager"

    private val context: Context by inject()
    private var targetPackages: MutableSet<String> = mutableSetOf()

    /**
     * 设置目标包名
     */
    fun setTargetPackages(vararg packages: String) {
        targetPackages.clear()
        targetPackages.addAll(packages)
    }

    /**
     * 通知配置变化
     */
    fun notifyChange(kvId: String, key: String) {
        try {
            val intent = Intent(ACTION_KV_CHANGED).apply {
                putExtra(EXTRA_KV_ID, kvId)
                putExtra(EXTRA_KEY, key)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }
            }
            
            if (targetPackages.isEmpty()) {
                context.sendBroadcast(intent)
            } else {
                targetPackages.forEach { packageName ->
                    intent.setPackage(packageName)
                    context.sendBroadcast(intent)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send broadcast", e)
        }
    }

    fun getChangeAction(): String = ACTION_KV_CHANGED
    fun getKvIdExtra(): String = EXTRA_KV_ID
    fun getKeyExtra(): String = EXTRA_KEY
}