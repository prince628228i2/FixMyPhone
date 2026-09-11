package com.fixmyphone.voice

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import java.util.*

class TextToSpeechModule(private val ctx: ReactApplicationContext) : ReactContextBaseJavaModule(ctx), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    override fun getName() = "TextToSpeech"
    override fun initialize() { super.initialize(); tts = TextToSpeech(ctx, this) }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            com.fixmyphone.ProgressLogger.log("TTS: INIT_FAILED status=$status")
            return
        }
        val engine = tts ?: return
        val voices = engine.voices ?: emptySet()
        val female = voices.firstOrNull {
            val n = it.name.lowercase(Locale.US)
            (n.contains("female") || n.contains("femin") || n.contains("woman") || n.contains("samantha")) &&
            (it.locale.language == "hi" || it.locale.language == "en")
        }
        val indian = voices.firstOrNull { it.locale.toLanguageTag().equals("hi-IN", true) }
            ?: voices.firstOrNull { it.locale.toLanguageTag().equals("en-IN", true) }
        engine.voice = female ?: indian ?: engine.defaultVoice
        engine.setSpeechRate(1.5f)
        com.fixmyphone.ProgressLogger.log("TTS: READY speed=1.5x")
        engine.setPitch(1.06f)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                com.fixmyphone.ProgressLogger.log("TTS: START")
                emit("start", id)
            }
            override fun onDone(id: String?) {
                com.fixmyphone.ProgressLogger.log("TTS: DONE")
                emit("done", id)
            }
            override fun onError(id: String?) {
                com.fixmyphone.ProgressLogger.log("TTS: ERROR")
                emit("error", id)
            }
        })
    }

    @ReactMethod
    fun speak(text: String, opts: ReadableMap?, p: Promise) {
        val engine = tts ?: run { p.reject("NOT_READY", "Text to speech is not ready"); return }
        val id = "fixmyphone-${System.nanoTime()}"
        com.fixmyphone.ProgressLogger.log("TTS: SPEAK: $text")
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), id)
        p.resolve(id)
    }
    @ReactMethod fun stop() { tts?.stop() }
    private fun emit(event: String, id: String?) {
        val m = Arguments.createMap(); m.putString("event", event); if(id != null) m.putString("id", id)
        ctx.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java).emit("onTtsEvent", m)
    }
    override fun onCatalystInstanceDestroy() { tts?.shutdown(); super.onCatalystInstanceDestroy() }
}
