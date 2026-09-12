package com.fixmyphone

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.modules.core.DeviceEventManagerModule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ProgressLogger {
    @Volatile private var initialized = false
    private var context: Context? = null
    private var reactContext: ReactApplicationContext? = null
    private val lock = Any()

    fun install(ctx: Context) {
        synchronized(lock) {
            context = ctx.applicationContext
            initialized = true
            write("========== FIX MY PHONE SESSION START ==========")
            write("Package: ${ctx.packageName}")
            write("Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
            write("APP STARTED")
        }
    }

    fun attachReactContext(ctx: ReactApplicationContext) {
        synchronized(lock) {
            reactContext = ctx
        }
    }

    fun detachReactContext() {
        synchronized(lock) {
            reactContext = null
        }
    }

    fun log(message: String) {
        if (!initialized) return
        write(message)
        try {
            reactContext
                ?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                ?.emit("FixMyPhoneProgress", message)
        } catch (_: Throwable) {
            // Live diagnostics must never crash the app.
        }
    }

    private fun write(message: String) {
        val ctx = context ?: return

        synchronized(lock) {
            try {
                val time = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss.SSS",
                    Locale.US
                ).format(Date())

                val line = "[$time] $message\n"

                val resolver = ctx.contentResolver

                val projection = arrayOf(
                    MediaStore.Downloads._ID,
                    MediaStore.Downloads.IS_PENDING
                )

                val selection =
                    "${MediaStore.Downloads.DISPLAY_NAME}=? AND " +
                    "${MediaStore.Downloads.RELATIVE_PATH}=?"

                val args = arrayOf(
                    "FixMyPhone_diagnostic.txt",
                    "${Environment.DIRECTORY_DOWNLOADS}/"
                )

                var uri = resolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    args,
                    "${MediaStore.Downloads._ID} ASC"
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Downloads._ID
                            )
                        )
                        android.content.ContentUris.withAppendedId(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            id
                        )
                    } else null
                }

                if (uri == null) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, "FixMyPhone_diagnostic.txt")
                        put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }

                    uri = resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                    )

                    if (uri != null) {
                        resolver.openOutputStream(uri, "wa")?.use {
                            it.write(line.toByteArray(Charsets.UTF_8))
                        }

                        values.clear()
                        values.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                    }

                    return
                }

                resolver.openOutputStream(uri, "wa")?.use {
                    it.write(line.toByteArray(Charsets.UTF_8))
                }

            } catch (_: Throwable) {
                // Logging must never crash the app.
            }
        }
    }
}
