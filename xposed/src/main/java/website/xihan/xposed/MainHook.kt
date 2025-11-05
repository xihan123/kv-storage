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
                    HostKVManager.init()

                    runCatching {
                        val textViewText = HostKVManager.createKVHelper().getString("textViewText")

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
                                Log.d("LSP-XPOSED", "swithchEnable changed: $s,$any")
                            }
                    }

                    runCatching {
                        HostKVManager.createKVHelper().addChangeListener("textViewText") { s, any ->
                            Log.d("LSP-XPOSED", "textViewText changed: $s,$any")
                        }
                    }
                }
            }

        )
    }

}