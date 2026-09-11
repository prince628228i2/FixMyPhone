package com.fixmyphone.executor

/**
 * Contract every domain module (accessibility, telephony, device, media...)
 * implements to plug into the central ActionDispatcher. This is what keeps
 * new domains loosely coupled: adding TelephonyModule in Part 7 means
 * writing one TelephonyActionHandler and registering it — nothing else in
 * the executor package changes.
 */
interface ActionHandler {
    fun canHandle(action: ActionType): Boolean

    /**
     * Executes the action. Must return one of: "executed", "failed",
     * "requires_user". Implementations must NOT throw for expected failure
     * cases (missing permission, no match found, service not connected) —
     * those are "failed" or "requires_user", not exceptions. Only truly
     * unexpected errors should propagate.
     */
    fun execute(action: FixMyPhoneAction): String
}
