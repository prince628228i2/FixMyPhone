package com.fixmyphone

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StartupCrashLogger {
    @Volatile private var stage = "Application: starting"

    fun setStage(value: String) {
        stage = value
    }

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            writeCrash(context, thread, throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrash(context: Context, thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val text = buildString {
                appendLine("FIX MY PHONE STARTUP CRASH DIAGNOSTIC")
                appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())}")
                appendLine("Package: ${context.packageName}")
                appendLine("Stage: $stage")
                appendLine("Thread: ${thread.name}")
                appendLine("Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
                appendLine()
                appendLine("=== EXCEPTION ===")
                appendLine(throwable.javaClass.name)
                appendLine(throwable.message ?: "")
                appendLine()
                appendLine("=== FULL STACK TRACE ===")
                appendLine(sw.toString())
            }
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "FixMyPhone_crash2.txt")
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
        } catch (_: Throwable) {
        }
    }
}
