package com.fixmyphone

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.soloader.SoLoader
import com.fixmyphone.safety.SafetyPackage
import com.fixmyphone.device.DevicePackage
import com.fixmyphone.service.AssistantPackage
import com.fixmyphone.voice.VoicePackage
import com.fixmyphone.device.DeviceActionHandler
import com.fixmyphone.executor.ExecutorPackage
import com.fixmyphone.executor.ActionDispatcher
import com.fixmyphone.accessibility.AccessibilityPackage
import com.fixmyphone.accessibility.AccessibilityActionHandler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainApplication : Application(), ReactApplication {

    override val reactNativeHost: ReactNativeHost =
        object : DefaultReactNativeHost(this) {
            override fun getPackages(): List<ReactPackage> =
                PackageList(this).packages.apply {
                    add(AccessibilityPackage())
                    add(ExecutorPackage())
                    add(VoicePackage())
                    add(AssistantPackage())
                    add(DevicePackage())
                    add(SafetyPackage())
                }

            override fun getJSMainModuleName(): String = "index"

            override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

            override val isNewArchEnabled: Boolean = false

            override val isHermesEnabled: Boolean = true
        }

    override val reactHost: ReactHost
        get() {
            return getDefaultReactHost(applicationContext, reactNativeHost)
        }

    override fun onCreate() {
        super.onCreate()
        SoLoader.init(this, false)
        ActionDispatcher.registerHandler(AccessibilityActionHandler())
        ActionDispatcher.registerHandler(DeviceActionHandler(this))
    }

}
