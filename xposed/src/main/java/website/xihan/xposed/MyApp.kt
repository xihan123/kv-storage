package website.xihan.xposed

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import website.xihan.kv.KVSyncManager

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApp)
            androidLogger()
        }
        KVSyncManager.setTargetPackages("website.xihan.kv.storage")
    }
}