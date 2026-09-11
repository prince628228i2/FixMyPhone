package com.fixmyphone.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class AssistantBridgeModule(private val ctx: ReactApplicationContext) : ReactContextBaseJavaModule(ctx) {
    override fun getName() = "AssistantBridge"

    @ReactMethod
    fun startAssistant(mode: String, promise: Promise) {
        try {
            com.fixmyphone.ProgressLogger.log("ASSISTANT: START mode=$mode")
            val i = Intent(ctx, AgentForegroundService::class.java).apply {
                action = AgentForegroundService.ACTION_START
                putExtra(AgentForegroundService.EXTRA_MODE, mode)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i) else ctx.startService(i)
            promise.resolve(true)
        } catch (e: Exception) { promise.reject("START_FAILED", e) }
    }

    @ReactMethod
    fun stopAssistant(promise: Promise) {
        try {
            ctx.startService(Intent(ctx, AgentForegroundService::class.java).setAction(AgentForegroundService.ACTION_STOP))
            promise.resolve(true)
        } catch (e: Exception) { promise.reject("STOP_FAILED", e) }
    }

    @ReactMethod
    fun minimizeApp(promise: Promise) {
        try {
            com.fixmyphone.ProgressLogger.log("ASSISTANT: MINIMIZE_APP")
            currentActivity?.moveTaskToBack(true)
            promise.resolve(true)
        } catch (e: Exception) { promise.reject("MINIMIZE_FAILED", e) }
    }
}
