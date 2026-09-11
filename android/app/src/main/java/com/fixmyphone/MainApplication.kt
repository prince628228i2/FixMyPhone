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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainApplication : Application(), ReactApplication {

    companion object {
        @Volatile
        private var stage = "Application: starting"

        fun markStage(value: String) {
            stage = value
        }
    }

    override val reactNativeHost: ReactNativeHost =
        object : DefaultReactNativeHost(this) {
            override fun getPackages(): List<ReactPackage> =
                PackageList(this).packages

            override fun getJSMainModuleName(): String = "index"

            override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

            override val isNewArchEnabled: Boolean = false

            override val isHermesEnabled: Boolean = true
        }

    override val reactHost: ReactHost
        get() {
            markStage("ReactHost: creating")
            return getDefaultReactHost(applicationContext, reactNativeHost)
        }

    override fun onCreate() {
        super.onCreate()
        markStage("Application.onCreate: started")
        installCrashLogger()

        try {
            markStage("SoLoader: starting")
            SoLoader.init(this, false)
            markStage("SoLoader: OK")
        } catch (t: Throwable) {
            saveCrash("SoLoader initialization failed", t)
            throw t
        }
    }

}
