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
import java.util.Locale

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

    private var restartAttempts = 0

    override fun getName() = "SpeechToText"

    @ReactMethod
    fun startListening(requestedLocale: String, promise: Promise) {
        mainHandler.post {
            if (destroyed) {
                promise.reject(
                    "DESTROYED",
                    "Speech recognition module is destroyed"
                )
                return@post
            }

            val requested = requestedLocale.ifBlank { "hi-IN" }

            if (desiredListening) {
                com.fixmyphone.ProgressLogger.log(
                    "STT: START_IGNORED_ALREADY_DESIRED actual=$actualListening scheduled=$restartScheduled"
                )
                promise.resolve(true)
                return@post
            }

            locale = requested
            desiredListening = true
            restartAttempts = 0

            com.fixmyphone.ProgressLogger.log(
                "STT: DESIRED_LISTENING locale=$locale"
            )

            startRecognizerNow()

            promise.resolve(true)
        }
    }

    private fun startRecognizerNow() {
        if (destroyed || !desiredListening) return
        if (actualListening) return

        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            com.fixmyphone.ProgressLogger.log(
                "STT: UNAVAILABLE"
            )
            emit("error", "unavailable")
            scheduleRestart(2000)
            return
        }

        try {
            restartScheduled = false

            recognizer?.cancel()
            recognizer?.destroy()
            recognizer = null

            recognizer = SpeechRecognizer.createSpeechRecognizer(ctx)

            recognizer?.setRecognitionListener(
                object : RecognitionListener {

                    override fun onReadyForSpeech(params: Bundle?) {
                        actualListening = true
                        restartAttempts = 0

                        Log.i(TAG, "onReadyForSpeech")
                        com.fixmyphone.ProgressLogger.log(
                            "STT: READY"
                        )

                        emit("start", null)
                    }

                    override fun onBeginningOfSpeech() {
                        Log.i(TAG, "onBeginningOfSpeech")
                        com.fixmyphone.ProgressLogger.log(
                            "STT: BEGIN"
                        )
                    }

                    override fun onRmsChanged(v: Float) {}

                    override fun onBufferReceived(b: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        actualListening = false

                        Log.i(TAG, "onEndOfSpeech")
                        com.fixmyphone.ProgressLogger.log(
                            "STT: END_OF_SPEECH"
                        )

                        emit("end", null)

                        /*
                         * Android normally ends a recognition session
                         * after the user stops talking. Immediately
                         * create another session while the assistant
                         * remains active.
                         */
                        if (desiredListening) {
                            scheduleRestart(120)
                        }
                    }

                    override fun onError(error: Int) {
                        actualListening = false

                        Log.e(TAG, "onError=$error")

                        com.fixmyphone.ProgressLogger.log(
                            "STT: ERROR code=$error"
                        )

                        emit("error", error.toString())

                        if (desiredListening) {
                            val delay = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> 150L
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 150L
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 500L
                                SpeechRecognizer.ERROR_CLIENT -> 500L
                                SpeechRecognizer.ERROR_NETWORK -> 1200L
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> 1500L
                                else -> 1000L
                            }

                            scheduleRestart(delay)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        actualListening = false

                        val text =
                            results
                                ?.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION
                                )
                                ?.firstOrNull()

                        Log.i(TAG, "onResults=$text")

                        if (!text.isNullOrBlank()) {
                            com.fixmyphone.ProgressLogger.log(
                                "STT: RESULT: $text"
                            )

                            emit("results", text)
                        }

                        if (desiredListening) {
                            scheduleRestart(100)
                        }
                    }

                    override fun onPartialResults(results: Bundle?) {
                        val text =
                            results
                                ?.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION
                                )
                                ?.firstOrNull()

                        if (!text.isNullOrBlank()) {
                            emit("partial", text)
                        }
                    }

                    override fun onEvent(
                        eventType: Int,
                        params: Bundle?
                    ) {}
                }
            )

            val intent =
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
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
                "STT: START_NATIVE locale=$locale attempt=$restartAttempts"
            )

            recognizer?.startListening(intent)

        } catch (e: Exception) {
            actualListening = false

            Log.e(TAG, "startRecognizerNow failed", e)

            com.fixmyphone.ProgressLogger.log(
                "STT: START_FAILED ${e.message}"
            )

            emit(
                "error",
                "start_failed:${e.message}"
            )

            if (desiredListening) {
                scheduleRestart(1000)
            }
        }
    }

    private fun scheduleRestart(delay: Long) {
        if (destroyed || !desiredListening) return
        if (restartScheduled) return

        restartScheduled = true
        restartAttempts++

        /*
         * Prevent an endless rapid restart loop if Android's
         * recognizer is temporarily busy.
         */
        val safeDelay =
            if (restartAttempts >= 5) {
                2000L
            } else {
                delay
            }

        mainHandler.postDelayed({
            restartScheduled = false

            if (destroyed || !desiredListening) return@postDelayed

            if (!actualListening) {
                startRecognizerNow()
            }
        }, safeDelay)
    }

    @ReactMethod
    fun stopListening(promise: Promise?) {
        mainHandler.post {
            desiredListening = false
            actualListening = false
            restartScheduled = false
            restartAttempts = 0

            mainHandler.removeCallbacksAndMessages(null)

            try {
                recognizer?.cancel()
                recognizer?.destroy()
                recognizer = null

                com.fixmyphone.ProgressLogger.log(
                    "STT: STOPPED"
                )

                promise?.resolve(true)
            } catch (e: Exception) {
                promise?.reject(
                    "STOP_FAILED",
                    e
                )
            }
        }
    }

    private fun emit(
        event: String,
        value: String?
    ) {
        val map = Arguments.createMap()

        map.putString(
            "event",
            event
        )

        if (value != null) {
            map.putString(
                "text",
                value
            )
        }

        ctx.getJSModule(
            DeviceEventManagerModule
                .RCTDeviceEventEmitter::class.java
        ).emit(
            "onSpeechEvent",
            map
        )
    }

    override fun onCatalystInstanceDestroy() {
        destroyed = true
        desiredListening = false
        actualListening = false

        mainHandler.post {
            try {
                recognizer?.cancel()
                recognizer?.destroy()
            } catch (_: Exception) {}

            recognizer = null
        }

        super.onCatalystInstanceDestroy()
    }

    companion object {
        private const val TAG = "FixMyPhoneSTT"
    }
}
