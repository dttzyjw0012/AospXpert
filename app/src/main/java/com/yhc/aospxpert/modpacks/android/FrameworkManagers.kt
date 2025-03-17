package com.yhc.xp.three.finger.screenshoter.hook

import android.content.Context
import android.os.Build
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object FrameworkManagers {
    private const val TAG = "Screenshoter:FrameworkManagers"
    private const val DEBUG = false
    private const val CLASS_SYSTEM_SERVER = "com.android.server.SystemServer"
    private const val CLASS_TIMINGS_TRACE_AND_SLOG = "com.android.server.utils.TimingsTraceAndSlog"
    private fun log(message: String) {
        XposedBridge.log(TAG + ": " + message)
    }

    @JvmField
    var BroadcastMediator: BroadcastMediator? = null
    @JvmStatic
    fun initAndroid(classLoader: ClassLoader) {
        BroadcastMediator = BroadcastMediator()
        hookStartCoreServices(classLoader)
    }

    private fun hookStartCoreServices(classLoader: ClassLoader) {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                XposedHelpers.findAndHookMethod(
                    CLASS_SYSTEM_SERVER, classLoader, "startCoreServices",
                    CLASS_TIMINGS_TRACE_AND_SLOG, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (DEBUG) log("Core services started")
                            onCoreServicesStarted(
                                XposedHelpers.getObjectField(
                                    param.thisObject, "mSystemContext"
                                ) as Context
                            )
                        }
                    })
            } else {
                XposedHelpers.findAndHookMethod(
                    CLASS_SYSTEM_SERVER, classLoader, "startCoreServices",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (DEBUG) log("Core services started")
                            onCoreServicesStarted(
                                XposedHelpers.getObjectField(
                                    param.thisObject, "mSystemContext"
                                ) as Context
                            )
                        }
                    })
            }
        } catch (t: Throwable) {
            log(t.toString())
        }
    }

    private fun onCoreServicesStarted(systemContext: Context) {
        BroadcastMediator!!.context = systemContext
    }
}