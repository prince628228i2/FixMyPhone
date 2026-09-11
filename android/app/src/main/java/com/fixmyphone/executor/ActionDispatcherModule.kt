package com.fixmyphone.executor

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import java.util.UUID

/**
 * Single JS entry point for dispatching ANY schema-defined action, across
 * every domain (accessibility today; telephony/device/media once those
 * parts land). JS's ActionDispatcher.js calls this instead of talking to
 * per-domain bridge modules directly, so adding a new domain never requires
 * a JS-side change here — only a new native ActionHandler registration.
 */
class ActionDispatcherModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "ActionDispatcherNative"

    @ReactMethod
    fun dispatch(actionMap: ReadableMap, promise: Promise) {
        val actionName = actionMap.getString("action")
        val type = actionName?.let { ActionType.fromWire(it) }

        if (type == null) {
            val result = Arguments.createMap().apply {
                putString("status", "failed")
                putString("reason", "unknown_action:$actionName")
            }
            promise.resolve(result)
            return
        }

        val params = actionMap.getMap("params")?.toHashMap() ?: emptyMap<String, Any?>()
        val id = if (actionMap.hasKey("id")) actionMap.getString("id") ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()
        val requiresConfirmation = if (actionMap.hasKey("requiresConfirmation")) actionMap.getBoolean("requiresConfirmation") else false

        val action = FixMyPhoneAction(
            id = id,
            action = type,
            params = params,
            requiresConfirmation = requiresConfirmation
        )

        val dispatchResult = ActionDispatcher.dispatch(action)

        val result = Arguments.createMap().apply {
            putString("status", dispatchResult.status)
            dispatchResult.reason?.let { putString("reason", it) }
        }
        promise.resolve(result)
    }
}
