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

            override val isHermesEnabled: Boolean = false
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

    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            saveCrash(
                "UNCAUGHT EXCEPTION\nThread: ${thread.name}\nStage: $stage",
                throwable
            )
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrash(header: String, throwable: Throwable) {
        try {
            val time = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS",
                Locale.US
            ).format(Date())

            val report = buildString {
                append("Fix My Phone STARTUP DIAGNOSTIC\n")
                append("Time: $time\n")
                append("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
                append("Stage: $stage\n")
                append("App: ${BuildConfig.APPLICATION_ID}\n")
                append("Version: ${BuildConfig.VERSION_NAME}\n")
                append("\n=== ERROR ===\n")
                append(header)
                append("\n\n")
                append(throwable.stackTraceToString())
                append("\n")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val values = ContentValues().apply {
                    put(
                        MediaStore.Downloads.DISPLAY_NAME,
                        "FixMyPhone_startup_crash.txt"
                    )
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS
                    )
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val uri = resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                )

                uri?.let {
                    resolver.openOutputStream(it)?.use { out ->
                        out.write(report.toByteArray(Charsets.UTF_8))
                    }

                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(it, values, null, null)
                }
            } else {
                openFileOutput(
                    "FixMyPhone_startup_crash.txt",
                    MODE_PRIVATE
                ).use {
                    it.write(report.toByteArray(Charsets.UTF_8))
                }
            }
        } catch (_: Throwable) {
        }
    }
}
