package com.fixmyphone.voice

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule

class SpeechToTextModule(private val ctx: ReactApplicationContext) : ReactContextBaseJavaModule(ctx) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var destroyed = false

    override fun getName() = "SpeechToText"

    @ReactMethod
    fun startListening(locale: String, p: Promise) {
        mainHandler.post {
            if (destroyed) {
                p.reject("DESTROYED", "Speech recognition module is destroyed")
                return@post
            }
            try {
                Log.i(TAG, "startListening locale=$locale")
                com.fixmyphone.ProgressLogger.log("STT: START locale=$locale")
                if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
                    p.reject("UNAVAILABLE", "Speech recognition unavailable")
                    return@post
                }

                recognizer?.cancel()
                recognizer?.destroy()
                recognizer = SpeechRecognizer.createSpeechRecognizer(ctx)

                recognizer!!.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        listening = true
                        Log.i(TAG, "onReadyForSpeech")
                        com.fixmyphone.ProgressLogger.log("STT: READY")
                        emit("start", null)
                    }

                    override fun onBeginningOfSpeech() {
                        Log.i(TAG, "onBeginningOfSpeech")
                    }

                    override fun onRmsChanged(v: Float) {}
                    override fun onBufferReceived(b: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        listening = false
                        Log.i(TAG, "onEndOfSpeech")
                        com.fixmyphone.ProgressLogger.log("STT: END_OF_SPEECH")
                        emit("end", null)
                    }

                    override fun onError(e: Int) {
                        listening = false
                        Log.e(TAG, "onError=$e")
                        com.fixmyphone.ProgressLogger.log("STT: ERROR code=$e")
                        emit("error", e.toString())
                    }

                    override fun onResults(b: Bundle?) {
                        listening = false
                        val x = b?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )?.firstOrNull()
                        Log.i(TAG, "onResults=$x")
                        if (!x.isNullOrBlank()) com.fixmyphone.ProgressLogger.log("STT: RESULT: $x")
                        if (!x.isNullOrBlank()) emit("results", x)
                    }

                    override fun onPartialResults(b: Bundle?) {
                        val x = b?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )?.firstOrNull()
                        if (!x.isNullOrBlank()) {
                            com.fixmyphone.ProgressLogger.log("STT: PARTIAL: $x")
                            emit("partial", x)
                        }
                    }

                    override fun onEvent(a: Int, b: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                recognizer!!.startListening(intent)
                p.resolve(true)
            } catch (e: Exception) {
                Log.e(TAG, "startListening failed", e)
                com.fixmyphone.ProgressLogger.log("STT: START_FAILED: ${e.message}")
                p.reject("START_FAILED", e)
            }
        }
    }

    @ReactMethod
    fun stopListening(p: Promise?) {
        mainHandler.post {
            try {
                listening = false
                recognizer?.cancel()
                p?.resolve(true)
            } catch (e: Exception) {
                p?.reject("STOP_FAILED", e)
            }
        }
    }

    private fun emit(e: String, v: String?) {
        val m = Arguments.createMap()
        m.putString("event", e)
        if (v != null) m.putString("text", v)
        ctx.getJSModule(
            DeviceEventManagerModule.RCTDeviceEventEmitter::class.java
        ).emit("onSpeechEvent", m)
    }

    override fun onCatalystInstanceDestroy() {
        destroyed = true
        mainHandler.post {
            recognizer?.cancel()
            recognizer?.destroy()
            recognizer = null
            listening = false
        }
        super.onCatalystInstanceDestroy()
    }

    companion object {
        private const val TAG = "FixMyPhoneSTT"
    }
}
