package com.fixmyphone.voice

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import java.util.*

class TextToSpeechModule(
    private val ctx: ReactApplicationContext
) : ReactContextBaseJavaModule(ctx), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun getName() = "TextToSpeech"

    override fun initialize() {
        super.initialize()
        tts = TextToSpeech(ctx, this)
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            com.fixmyphone.ProgressLogger.log("TTS: INIT_FAILED status=$status")
            return
        }

        val engine = tts ?: return
        val voices = engine.voices ?: emptySet()

        val preferred = voices
            .filter {
                val lang = it.locale.language.lowercase(Locale.US)
                lang == "hi" || lang == "en"
            }
            .sortedWith(
                compareByDescending<android.speech.tts.Voice> {
                    !it.isNetworkConnectionRequired
                }.thenByDescending {
                    it.quality
                }
            )

        val female = preferred.firstOrNull {
            val n = it.name.lowercase(Locale.US)
            n.contains("female") ||
            n.contains("femin") ||
            n.contains("woman") ||
            n.contains("samantha") ||
            n.contains("zira")
        }

        val indian = preferred.firstOrNull {
            it.locale.toLanguageTag().equals("hi-IN", true)
        } ?: preferred.firstOrNull {
            it.locale.toLanguageTag().equals("en-IN", true)
        }

        engine.voice = female ?: indian ?: preferred.firstOrNull() ?: engine.defaultVoice

        engine.setSpeechRate(1.3f)
        engine.setPitch(1.04f)

        com.fixmyphone.ProgressLogger.log("TTS: READY speed=1.3x voice=${engine.voice?.name}")

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                com.fixmyphone.ProgressLogger.log("TTS: START id=$id")
                emit("start", id)
            }

            override fun onDone(id: String?) {
                com.fixmyphone.ProgressLogger.log("TTS: DONE id=$id")
                emit("done", id)
            }

            override fun onError(id: String?) {
                com.fixmyphone.ProgressLogger.log("TTS: ERROR id=$id")
                emit("error", id)
            }
        })
    }

    @ReactMethod
    fun speak(text: String, opts: ReadableMap?, p: Promise) {
        val engine = tts ?: run {
            p.reject("NOT_READY", "Text to speech is not ready")
            return
        }

        val id = "fixmyphone-${System.nanoTime()}"

        try {
            com.fixmyphone.ProgressLogger.log("TTS: SPEAK: $text")
            val result = engine.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                Bundle(),
                id
            )

            if (result == TextToSpeech.ERROR) {
                p.reject("TTS_ERROR", "Text to speech failed")
            } else {
                p.resolve(id)
            }
        } catch (e: Exception) {
            com.fixmyphone.ProgressLogger.log("TTS: SPEAK_FAILED ${e.message}")
            p.reject("TTS_ERROR", e)
        }
    }

    @ReactMethod
    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    private fun emit(event: String, id: String?) {
        val m = Arguments.createMap()
        m.putString("event", event)
        if (id != null) m.putString("id", id)

        ctx.getJSModule(
            DeviceEventManagerModule.RCTDeviceEventEmitter::class.java
        ).emit("onTtsEvent", m)
    }

    override fun onCatalystInstanceDestroy() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}

        tts = null
        super.onCatalystInstanceDestroy()
    }
}
