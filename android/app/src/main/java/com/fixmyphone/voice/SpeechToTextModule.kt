package com.fixmyphone.voice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule

class SpeechToTextModule(private val ctx: ReactApplicationContext) : ReactContextBaseJavaModule(ctx) {
    private var recognizer: SpeechRecognizer? = null

    override fun getName() = "SpeechToText"

    @ReactMethod
    fun startListening(locale: String, p: Promise) {
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            p.reject("UNAVAILABLE", "Speech recognition unavailable")
            return
        }

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(ctx)

        recognizer!!.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = emit("start", null)
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() = emit("end", null)
            override fun onError(e: Int) = emit("error", e.toString())

            override fun onResults(b: Bundle?) {
                val x = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                emit("results", x)
            }

            override fun onPartialResults(b: Bundle?) {
                emit("partial", b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull())
            }

            override fun onEvent(a: Int, b: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        recognizer!!.startListening(intent)
        p.resolve(true)
    }

    @ReactMethod
    fun stopListening() {
        recognizer?.stopListening()
    }

    private fun emit(e: String, v: String?) {
        val m = Arguments.createMap()
        m.putString("event", e)
        if (v != null) m.putString("text", v)
        ctx.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("onSpeechEvent", m)
    }

    override fun onCatalystInstanceDestroy() {
        recognizer?.destroy()
        super.onCatalystInstanceDestroy()
    }
}
