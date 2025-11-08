package website.xihan.xposed

import android.app.Application
import android.app.Instrumentation
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import website.xihan.kv.HostKVManager
import website.xihan.kv.KVFileReceiver
import kotlin.system.measureTimeMillis

class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "website.xihan.kv.storage") return
        XposedHelpers.findAndHookMethod(
            Instrumentation::class.java,
            "callApplicationOnCreate",
            Application::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val application = param.args[0] as? Application ?: return
                    startKoin {
                        androidContext(application)
                        androidLogger()
                    }
                    HostKVManager.init(
                        enableSharedPreferencesCache = true,
                        modulePackageName = BuildConfig.APPLICATION_ID
                    )

                    runCatching {
                        val textViewText = HostKVManager.createKVHelper().getString("textViewText")
                        Log.d(TAG, "textViewText: $textViewText")

                        XposedHelpers.findAndHookMethod(
                            "website.xihan.kv.storage.MainActivity",
                            lpparam.classLoader,
                            "getText",
                            XC_MethodReplacement.returnConstant(textViewText)
                        )
                    }

                    runCatching {
                        val swithchEnable =
                            HostKVManager.createKVHelper().getBoolean("swithchEnable")
                        Log.d(TAG, "swithchEnable: $swithchEnable")

                        XposedHelpers.findAndHookMethod(
                            "website.xihan.kv.storage.MainActivity",
                            lpparam.classLoader,
                            "getSwitch",
                            XC_MethodReplacement.returnConstant(swithchEnable)
                        )
                    }

                    runCatching {
                        HostKVManager.createKVHelper()
                            .addChangeListener("swithchEnable") { s, any ->
                                Log.d(TAG, "swithchEnable changed: $s,$any")
                            }
                    }

                    runCatching {
                        HostKVManager.createKVHelper().addChangeListener("textViewText") { s, any ->
                            Log.d(TAG, "textViewText changed: $s,$any")
                        }
                    }

                    runCatching {
                        measureTimeMillis {
                            val keys = setOf("swithchEnable", "textViewText")
                            val map = HostKVManager.createKVHelper().getBatch(keys)
                            Log.d(TAG, "getBatch: $map")
                        }.let {
                            Log.d(TAG, "getBatch time: ${it}")
                        }
                    }

                    runCatching {
                        val receiveFile = KVFileReceiver(application)
                        receiveFile.receiveFile()?.let { file ->
                            Log.d(TAG, "File saved to: ${file.absolutePath}")
                        }
                        receiveFile.observeFile { file ->
                            Log.d(TAG, "File updated and saved to: ${file.absolutePath}")
                        }
                    }.onFailure {
                        Log.e(TAG, "Failed to setup file receiver", it)
                    }
                }
            }

        )
    }

    companion object {
        const val TAG = "KV-XPOSED"
    }

}