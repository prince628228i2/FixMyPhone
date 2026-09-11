package com.fixmyphone.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

/**
 * Core AccessibilityService. Kept intentionally thin: it owns the Android
 * lifecycle (onServiceConnected / onAccessibilityEvent / onInterrupt) and
 * delegates all real work to NodeInspector (reading) and NodeActionExecutor
 * (acting). AccessibilityBridgeModule talks to this via the companion
 * instance reference so JS never needs its own binder/connection logic.
 *
 * IMPORTANT: this service does nothing by itself. It only inspects/acts when
 * explicitly asked to by the JS-side Agent Engine through the bridge. It does
 * not log, upload, or persist screen content anywhere — see NodeInspector.
 */
class FixMyPhoneAccessibilityService : AccessibilityService() {

    lateinit var nodeInspector: NodeInspector
        private set
    lateinit var nodeActionExecutor: NodeActionExecutor
        private set

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Config is also declared in accessibility_service_config.xml, but we
        // reinforce the flags here so behavior is explicit and inspectable
        // from Kotlin without needing to cross-reference the XML.
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 50
        }
        serviceInfo = info

        nodeInspector = NodeInspector(this)
        nodeActionExecutor = NodeActionExecutor(this)

        instance = this
        Log.i(TAG, "FixMyPhoneAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        // The Observer step of the agent loop pulls state on demand via
        // NodeInspector rather than us pushing every event to JS — pushing
        // every event would flood the bridge. We only forward window-change
        // events, which the Observer uses to know a screen transition
        // finished before it re-inspects.
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            emitWindowChangedEvent(event)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "FixMyPhoneAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    private fun emitWindowChangedEvent(event: AccessibilityEvent) {
        val params: WritableMap = Arguments.createMap().apply {
            putString("packageName", event.packageName?.toString())
            putString("className", event.className?.toString())
            putInt("eventType", event.eventType)
            putDouble("eventTimeMs", event.eventTime.toDouble())
        }
        AccessibilityBridgeModule.emitEvent("onWindowChanged", params)
    }

    companion object {
        private const val TAG = "FixMyPhoneA11yService"

        // Set/cleared in onServiceConnected/onDestroy. AccessibilityBridgeModule
        // reads this to know whether the service is currently bound — if null,
        // the bridge returns a "requires_user" style error asking the user to
        // enable the Accessibility permission in Settings.
        @Volatile
        var instance: FixMyPhoneAccessibilityService? = null
            private set

        fun isConnected(): Boolean = instance != null
    }
}
