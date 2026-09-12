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

class SpeechToTextModule(
    private val ctx: ReactApplicationContext
) : ReactContextBaseJavaModule(ctx) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null
    private var desiredListening = false
    private var actualListening = false
    private var destroyed = false
    private var restartScheduled = false
    private var locale = "hi-IN"
    private var sessionId = 0L

    override fun getName() = "SpeechToText"

    @ReactMethod
    fun startListening(requestedLocale: String, promise: Promise) {
        mainHandler.post {
            if (destroyed) {
                promise.reject("DESTROYED", "Speech recognition module is destroyed")
                return@post
            }

            if (desiredListening) {
                if (actualListening || recognizer != null || restartScheduled) {
                    com.fixmyphone.ProgressLogger.log(
                        "STT: START_IGNORED already_active actual=$actualListening recognizer=${recognizer != null} restart=$restartScheduled"
                    )
                    promise.resolve(true)
                    return@post
                }

                com.fixmyphone.ProgressLogger.log(
                    "STT: START_RECOVERY desired=true actual=false recognizer=false"
                )
                startRecognizer()
                promise.resolve(true)
                return@post
            }

            locale = requestedLocale.ifBlank { "hi-IN" }
            desiredListening = true
            restartScheduled = false

            com.fixmyphone.ProgressLogger.log(
                "STT: LISTENING_REQUEST locale=$locale"
            )

            startRecognizer()
            promise.resolve(true)
        }
    }

    private fun startRecognizer() {
        if (destroyed || !desiredListening || actualListening || restartScheduled) return

        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            com.fixmyphone.ProgressLogger.log("STT: UNAVAILABLE")
            emit("error", "unavailable")
            scheduleRestart(2000L)
            return
        }

        restartScheduled = false

        val mySession = ++sessionId

        try {
            // Only the previous TERMINATED recognizer is destroyed here.
            // Never cancel/destroy a recognizer from inside its own callback.
            recognizer?.let {
                try {
                    it.destroy()
                } catch (_: Throwable) {}
            }
            recognizer = null

            val sr = SpeechRecognizer.createSpeechRecognizer(ctx)
            recognizer = sr

            sr.setRecognitionListener(object : RecognitionListener {

                private fun isCurrent(): Boolean =
                    !destroyed &&
                    desiredListening &&
                    mySession == sessionId

                override fun onReadyForSpeech(params: Bundle?) {
                    if (!isCurrent()) return

                    actualListening = true

                    Log.i(TAG, "onReadyForSpeech session=$mySession")
                    com.fixmyphone.ProgressLogger.log(
                        "STT: READY session=$mySession"
                    )
                    emit("start", null)
                }

                override fun onBeginningOfSpeech() {
                    if (!isCurrent()) return

                    com.fixmyphone.ProgressLogger.log(
                        "STT: BEGIN session=$mySession"
                    )
                }

                override fun onRmsChanged(v: Float) {}

                override fun onBufferReceived(b: ByteArray?) {}

                override fun onEndOfSpeech() {
                    if (!isCurrent()) return

                    actualListening = false

                    Log.i(TAG, "onEndOfSpeech session=$mySession")
                    com.fixmyphone.ProgressLogger.log(
                        "STT: END_OF_SPEECH session=$mySession"
                    )
                    emit("end", null)

                    /*
                     * IMPORTANT:
                     * Do NOT restart here.
                     *
                     * Android normally sends onResults() or onError()
                     * after onEndOfSpeech(). Restarting here was causing
                     * overlapping recognizer sessions and ERROR 11.
                     */
                }

                override fun onError(error: Int) {
                    if (mySession != sessionId || destroyed) return

                    actualListening = false

                    Log.e(TAG, "onError=$error session=$mySession")
                    com.fixmyphone.ProgressLogger.log(
                        "STT: ERROR code=$error session=$mySession"
                    )
                    emit("error", error.toString())

                    if (desiredListening) {
                        val delay = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> 250L
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 250L
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 1200L
                            SpeechRecognizer.ERROR_CLIENT -> 800L
                            SpeechRecognizer.ERROR_NETWORK -> 1500L
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> 1800L
                            else -> 1000L
                        }

                        terminateCurrentSession(mySession)
                        scheduleRestart(delay)
                    }
                }

                override fun onResults(results: Bundle?) {
                    if (mySession != sessionId || destroyed) return

                    actualListening = false

                    val text = results
                        ?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        ?.firstOrNull()
                        ?.trim()

                    Log.i(TAG, "onResults=$text session=$mySession")

                    if (!text.isNullOrBlank()) {
                        com.fixmyphone.ProgressLogger.log(
                            "STT: RESULT: $text"
                        )
                        emit("results", text)
                    }

                    if (desiredListening) {
                        terminateCurrentSession(mySession)
                        scheduleRestart(180L)
                    }
                }

                override fun onPartialResults(results: Bundle?) {
                    if (!isCurrent()) return

                    val text = results
                        ?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        ?.firstOrNull()
                        ?.trim()

                    if (!text.isNullOrBlank()) {
                        emit("partial", text)
                    }
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {}
            })

            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    locale
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                    locale
                )
                putExtra(
                    RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE,
                    false
                )
                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    true
                )
                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    3
                )
                putExtra(
                    RecognizerIntent.EXTRA_CALLING_PACKAGE,
                    ctx.packageName
                )
            }

            com.fixmyphone.ProgressLogger.log(
                "STT: START_NATIVE session=$mySession locale=$locale"
            )

            sr.startListening(intent)

        } catch (e: Exception) {
            actualListening = false

            Log.e(TAG, "startRecognizer failed", e)

            com.fixmyphone.ProgressLogger.log(
                "STT: START_FAILED ${e.message}"
            )

            emit(
                "error",
                "start_failed:${e.message}"
            )

            terminateCurrentSession(mySession)

            if (desiredListening) {
                scheduleRestart(1200L)
            }
        }
    }

    private fun terminateCurrentSession(mySession: Long) {
        if (mySession != sessionId) return

        sessionId++

        actualListening = false

        val old = recognizer
        recognizer = null

        if (old != null) {
            mainHandler.post {
                try {
                    old.cancel()
                } catch (_: Throwable) {}

                try {
                    old.destroy()
                } catch (_: Throwable) {}
            }
        }
    }

    private fun scheduleRestart(delay: Long) {
        if (destroyed || !desiredListening) return
        if (restartScheduled) return

        restartScheduled = true

        mainHandler.postDelayed({
            restartScheduled = false

            if (destroyed || !desiredListening) return@postDelayed
            if (actualListening) return@postDelayed
            if (recognizer != null) return@postDelayed

            startRecognizer()
        }, delay)
    }

    @ReactMethod
    fun stopListening(promise: Promise?) {
        mainHandler.post {
            desiredListening = false
            actualListening = false
            restartScheduled = false
            sessionId++

            mainHandler.removeCallbacksAndMessages(null)

            val old = recognizer
            recognizer = null

            try {
                old?.cancel()
            } catch (_: Throwable) {}

            try {
                old?.destroy()
            } catch (_: Throwable) {}

            com.fixmyphone.ProgressLogger.log("STT: STOPPED")

            promise?.resolve(true)
        }
    }

    private fun emit(event: String, value: String?) {
        val map = Arguments.createMap()
        map.putString("event", event)

        if (value != null) {
            map.putString("text", value)
        }

        ctx.getJSModule(
            DeviceEventManagerModule.RCTDeviceEventEmitter::class.java
        ).emit(
            "onSpeechEvent",
            map
        )
    }

    override fun onCatalystInstanceDestroy() {
        destroyed = true
        desiredListening = false
        actualListening = false
        restartScheduled = false
        sessionId++

        mainHandler.removeCallbacksAndMessages(null)

        val old = recognizer
        recognizer = null

        mainHandler.post {
            try {
                old?.cancel()
            } catch (_: Throwable) {}

            try {
                old?.destroy()
            } catch (_: Throwable) {}
        }

        super.onCatalystInstanceDestroy()
    }

    companion object {
        private const val TAG = "FixMyPhoneSTT"
    }
}
