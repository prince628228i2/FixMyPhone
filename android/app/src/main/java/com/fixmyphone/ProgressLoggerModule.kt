package com.fixmyphone

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class ProgressLoggerModule(private val ctx: ReactApplicationContext) :
    ReactContextBaseJavaModule(ctx) {

    override fun getName() = "ProgressLogger"

    @ReactMethod
    fun log(message: String) {
        ProgressLogger.log(message)
    }
}
