package com.fixmyphone.executor

import android.util.Log
import com.fixmyphone.safety.ActionGuardRails

/**
 * Central registry + router. Domain packages (accessibility now; telephony,
 * device, media in later parts) register one ActionHandler each here at app
 * startup. Dispatch always re-validates (ActionValidator) before looking for
 * a handler — so even a direct call into this object from anywhere in the
 * codebase gets the same guarantees the JS entry point provides.
 */
object ActionDispatcher {
    private const val TAG = "ActionDispatcher"
    private val handlers = mutableListOf<ActionHandler>()

    fun registerHandler(handler: ActionHandler) {
        handlers.add(handler)
    }

    fun dispatch(action: FixMyPhoneAction): DispatchResult {
        if (!ActionGuardRails.allow(action)) return DispatchResult("failed", "blocked_by_safety_guard")
        val validation = ActionValidator.validate(action)
        if (!validation.valid) {
            Log.w(TAG, "Rejected action ${action.action}: ${validation.reason}")
            return DispatchResult("failed", validation.reason)
        }

        val handler = handlers.firstOrNull { it.canHandle(action.action) }
        if (handler == null) {
            // Action is schema-valid but no domain module implements it yet
            // (e.g. CALL before Part 7's TelephonyModule lands). This is
            // "requires_user" rather than "failed" because it's not that the
            // action is wrong — the capability just isn't wired up yet.
            return DispatchResult("requires_user", "no_handler_registered:${action.action}")
        }

        val status = try {
            handler.execute(action)
        } catch (e: Exception) {
            Log.e(TAG, "Handler threw for ${action.action}", e)
            "failed"
        }
        return DispatchResult(status, null)
    }
}

data class DispatchResult(
    val status: String,
    val reason: String?
)
