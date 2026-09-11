package com.fixmyphone.accessibility

import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule

/**
 * The only bridge between JS and the accessibility package. Every method
 * here does one of two things: (1) read state via NodeInspector, or
 * (2) perform ONE schema-defined action via NodeActionExecutor. It never
 * accepts or evaluates arbitrary code from JS — action names are fixed
 * strings matched against a switch, mirroring ActionType in ActionSchema.kt.
 *
 * If the AccessibilityService isn't currently connected (user hasn't granted
 * the permission, or turned it off), every method resolves with
 * { status: "requires_user", reason: "accessibility_service_disabled" }
 * rather than throwing — the Agent Engine treats that the same as any other
 * requires_user result and surfaces it to the person instead of failing
 * silently.
 */
class AccessibilityBridgeModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "AccessibilityBridge"

    init {
        appContext = reactContext
    }

    @ReactMethod
    fun isServiceEnabled(promise: com.facebook.react.bridge.Promise) {
        promise.resolve(FixMyPhoneAccessibilityService.isConnected())
    }

    @ReactMethod
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext?.startActivity(intent)
    }

    @ReactMethod
    fun getSnapshot(promise: com.facebook.react.bridge.Promise) {
        withService(promise) { service ->
            promise.resolve(service.nodeInspector.captureSnapshot())
        }
    }

    @ReactMethod
    fun performAction(actionName: String, params: WritableMap?, promise: com.facebook.react.bridge.Promise) {
        withService(promise) { service ->
            val executor = service.nodeActionExecutor
            val target = params?.let { if (it.hasKey("target")) it.getMap("target") else null }

            val status = when (actionName.uppercase()) {
                "TAP" -> executor.tap(target)
                "LONG_PRESS" -> executor.longPress(target)
                "TYPE_TEXT" -> executor.typeText(target, params?.getString("text"))
                "CLEAR_TEXT" -> executor.clearText(target)
                "SCROLL" -> executor.scroll(params?.getString("direction"))
                "SWIPE" -> executor.swipe(params?.getString("direction"))
                "BACK", "HOME", "RECENTS" -> executor.globalAction(actionName.uppercase())
                else -> null
            }

            if (status == null) {
                val result = Arguments.createMap().apply {
                    putString("status", "failed")
                    putString("reason", "unknown_action:$actionName")
                }
                promise.resolve(result)
                return@withService
            }

            val result = Arguments.createMap().apply {
                putString("status", status)
            }
            promise.resolve(result)
        }
    }

    private inline fun withService(
        promise: com.facebook.react.bridge.Promise,
        block: (FixMyPhoneAccessibilityService) -> Unit
    ) {
        val service = FixMyPhoneAccessibilityService.instance
        if (service == null) {
            val result = Arguments.createMap().apply {
                putString("status", "requires_user")
                putString("reason", "accessibility_service_disabled")
            }
            promise.resolve(result)
            return
        }
        block(service)
    }

    companion object {
        private var appContext: ReactApplicationContext? = null
            set(value) {
                field = value
            }

        /**
         * Called by FixMyPhoneAccessibilityService to forward window-change
         * events to JS as a DeviceEventEmitter event. No-ops if the RN
         * context isn't attached yet (e.g. service connected before RN
         * finished bootstrapping) — the Observer re-queries state directly
         * via getSnapshot() in that case instead of relying on the event.
         */
        fun emitEvent(eventName: String, params: WritableMap) {
            val context = appContext ?: return
            if (!context.hasActiveReactInstance()) return
            context
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(eventName, params)
        }
    }
}
