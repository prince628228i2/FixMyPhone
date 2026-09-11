package com.fixmyphone

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.soloader.SoLoader
import com.fixmyphone.accessibility.AccessibilityActionHandler
import com.fixmyphone.accessibility.AccessibilityPackage
import com.fixmyphone.executor.ActionDispatcher
import com.fixmyphone.executor.ExecutorPackage
import com.fixmyphone.device.DeviceActionHandler
import com.fixmyphone.voice.VoicePackage
import com.fixmyphone.service.AssistantPackage
import com.fixmyphone.device.DevicePackage
import com.fixmyphone.safety.SafetyPackage

class MainApplication : Application(), ReactApplication {
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                openFileOutput("startup_crash.txt", MODE_PRIVATE).use { out ->
                    out.write(("Thread: ${thread.name}\n${throwable.stackTraceToString()}").toByteArray())
                }
            } catch (_: Throwable) {}
            previous?.uncaughtException(thread, throwable)
        }
    }


    override val reactNativeHost: ReactNativeHost =
        object : DefaultReactNativeHost(this) {
            override fun getPackages(): List<ReactPackage> {
                return PackageList(this).packages
            }

            override fun getJSMainModuleName(): String = "index"

            override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

            override val isNewArchEnabled: Boolean = false
            override val isHermesEnabled: Boolean = true
        }

    override val reactHost: ReactHost
        get() = getDefaultReactHost(applicationContext, reactNativeHost)

    override fun onCreate() {
        super.onCreate()

        SoLoader.init(this, false)
        // Register one ActionHandler per domain here. AccessibilityActionHandler
        // is stateless (reads FixMyPhoneAccessibilityService.instance lazily),
        // so registering it before the service connects is safe — it will
        // just return "requires_user" for any action until the user enables
        // the service. TelephonyActionHandler, DeviceActionHandler etc. are
        // added the same way in Parts 7-8.
        ActionDispatcher.registerHandler(AccessibilityActionHandler())
        ActionDispatcher.registerHandler(DeviceActionHandler(this))
    }
}
