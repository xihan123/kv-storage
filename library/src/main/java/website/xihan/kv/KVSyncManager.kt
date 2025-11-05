package website.xihan.kv

import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * 同步管理器，通过广播实现跨进程配置变化通知
 */
object KVSyncManager : KoinComponent {

    private const val ACTION_KV_CHANGED = "website.xihan.kv.KV_CHANGED"
    private const val EXTRA_KV_ID = "kvId"
    private const val EXTRA_KEY = "key"

    private val context: Context by inject()

    /**
     * 通知配置变化
     */
    fun notifyChange(kvId: String, key: String) {
        val intent = Intent(ACTION_KV_CHANGED).apply {
            putExtra(EXTRA_KV_ID, kvId)
            putExtra(EXTRA_KEY, key)
//            setPackage(BuildConfig.APPLICATION_ID) // 指定目标包名
        }
        context.sendBroadcast(intent)
    }

    fun getChangeAction(): String = ACTION_KV_CHANGED
    fun getKvIdExtra(): String = EXTRA_KV_ID
    fun getKeyExtra(): String = EXTRA_KEY
}