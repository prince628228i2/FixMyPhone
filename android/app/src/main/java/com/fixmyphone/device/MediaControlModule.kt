package com.fixmyphone.device

import android.content.Intent
import android.view.KeyEvent
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class MediaControlModule(private val ctx: ReactApplicationContext) : ReactContextBaseJavaModule(ctx) {
    override fun getName() = "MediaControl"

    @ReactMethod
    fun control(action: String, p: Promise) {
        val key = when (action) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> 0
        }

        if (key == 0) {
            p.reject("BAD_ACTION", "Unsupported media action")
            return
        }

        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, key))
        }

        ctx.sendBroadcast(intent)
        p.resolve(true)
    }
}
