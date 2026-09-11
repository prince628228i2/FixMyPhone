package com.fixmyphone.accessibility

import com.fixmyphone.executor.ActionHandler
import com.fixmyphone.executor.ActionType
import com.fixmyphone.executor.FixMyPhoneAction

/**
 * Bridges the generic ActionDispatcher to the accessibility package's
 * NodeActionExecutor. Stateless by design — it always reads
 * FixMyPhoneAccessibilityService.instance at call time rather than holding
 * a reference, so it can be registered once at app startup regardless of
 * whether the service is connected yet.
 */
class AccessibilityActionHandler : ActionHandler {

    private val handledTypes = setOf(
        ActionType.TAP,
        ActionType.LONG_PRESS,
        ActionType.TYPE_TEXT,
        ActionType.CLEAR_TEXT,
        ActionType.SCROLL,
        ActionType.SWIPE,
        ActionType.BACK,
        ActionType.HOME,
        ActionType.RECENTS
    )

    override fun canHandle(action: ActionType): Boolean = action in handledTypes

    override fun execute(action: FixMyPhoneAction): String {
        val service = FixMyPhoneAccessibilityService.instance ?: return "requires_user"
        val executor = service.nodeActionExecutor
        val target = action.params["target"] as? Map<*, *>

        return when (action.action) {
            ActionType.TAP -> executor.tap(target.toWritableMapOrNull())
            ActionType.LONG_PRESS -> executor.longPress(target.toWritableMapOrNull())
            ActionType.TYPE_TEXT -> executor.typeText(target.toWritableMapOrNull(), action.params["text"] as? String)
            ActionType.CLEAR_TEXT -> executor.clearText(target.toWritableMapOrNull())
            ActionType.SCROLL -> executor.scroll(action.params["direction"] as? String)
            ActionType.SWIPE -> executor.swipe(action.params["direction"] as? String)
            ActionType.BACK, ActionType.HOME, ActionType.RECENTS -> executor.globalAction(action.action.name)
            else -> "failed"
        }
    }
}

/**
 * NodeActionExecutor's public methods take a WritableMap (the RN bridge
 * type) because it's also called directly from AccessibilityBridgeModule.
 * When invoked through the generic ActionDispatcher instead, "target"
 * arrives as a plain Map from FixMyPhoneAction.params, so we adapt it here
 * rather than duplicating executor logic for two map types.
 */
private fun Map<*, *>?.toWritableMapOrNull(): com.facebook.react.bridge.WritableMap? {
    if (this == null) return null
    val map = com.facebook.react.bridge.Arguments.createMap()
    for ((key, value) in this) {
        if (key !is String) continue
        when (value) {
            is String -> map.putString(key, value)
            is Boolean -> map.putBoolean(key, value)
            is Int -> map.putInt(key, value)
            is Double -> map.putDouble(key, value)
            null -> map.putNull(key)
        }
    }
    return map
}
