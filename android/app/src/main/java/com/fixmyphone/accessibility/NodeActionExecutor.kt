package com.fixmyphone.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.facebook.react.bridge.ReadableMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Executes node-level and gesture-level actions. This is the ONLY class in
 * the app that is allowed to call AccessibilityNodeInfo.performAction(...)
 * or dispatchGesture(...). Every call here corresponds to a single entry in
 * the shared ActionSchema (TAP, LONG_PRESS, TYPE_TEXT, CLEAR_TEXT, SWIPE,
 * SCROLL, BACK, HOME, RECENTS) — ActionDispatcher (Part 4) is responsible
 * for making sure only schema-valid actions ever reach these methods.
 *
 * Every method returns a plain result string matching the app-wide action
 * lifecycle: "executed" | "failed" | "requires_user". Verification that the
 * action had the intended *effect* (not just that it ran) is done by
 * Verifier.js on the JS side, using a follow-up NodeInspector snapshot.
 */
class NodeActionExecutor(private val service: FixMyPhoneAccessibilityService) {

    private val inspector get() = service.nodeInspector

    fun tap(target: ReadableMap?): String {
        val node = inspector.findNode(target) ?: return "failed"
        val result = try {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            } else {
                tapAtNodeCenter(node)
            }
        } finally {
            node.recycle()
        }
        return if (result) "executed" else "failed"
    }

    fun longPress(target: ReadableMap?): String {
        val node = inspector.findNode(target) ?: return "failed"
        val result = try {
            node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
        } finally {
            node.recycle()
        }
        return if (result) "executed" else "failed"
    }

    fun typeText(target: ReadableMap?, text: String?): String {
        if (text == null) return "failed"
        val node = inspector.findNode(target) ?: return "failed"
        val result = try {
            if (!node.isEditable) return "failed"
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } finally {
            node.recycle()
        }
        return if (result) "executed" else "failed"
    }

    fun clearText(target: ReadableMap?): String {
        val node = inspector.findNode(target) ?: return "failed"
        val result = try {
            if (!node.isEditable) return "failed"
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } finally {
            node.recycle()
        }
        return if (result) "executed" else "failed"
    }

    fun scroll(direction: String?): String {
        val root = service.rootInActiveWindow ?: return "failed"
        val scrollable = findFirstScrollable(root) ?: run { root.recycle(); return "failed" }
        val action = when (direction?.lowercase()) {
            "up", "backward" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            else -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }
        val result = scrollable.performAction(action)
        if (scrollable != root) scrollable.recycle()
        root.recycle()
        return if (result) "executed" else "failed"
    }

    /**
     * Swipe is implemented as a gesture (not a node action) since it needs to
     * work even when no scrollable node is reachable — e.g. custom-drawn UI.
     * Coordinates are derived from the current screen's active window bounds.
     */
    fun swipe(direction: String?): String {
        val root = service.rootInActiveWindow ?: return "failed"
        val bounds = Rect()
        root.getBoundsInScreen(bounds)
        root.recycle()

        val centerX = (bounds.left + bounds.right) / 2f
        val centerY = (bounds.top + bounds.bottom) / 2f
        val quarterH = (bounds.bottom - bounds.top) / 4f

        val (startY, endY) = when (direction?.lowercase()) {
            "up" -> (centerY + quarterH) to (centerY - quarterH)
            "down" -> (centerY - quarterH) to (centerY + quarterH)
            else -> centerY to centerY
        }

        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 250))
            .build()

        return dispatchGestureSync(gesture)
    }

    fun globalAction(action: String): String {
        val globalAction = when (action.uppercase()) {
            "BACK" -> AccessibilityService.GLOBAL_ACTION_BACK
            "HOME" -> AccessibilityService.GLOBAL_ACTION_HOME
            "RECENTS" -> AccessibilityService.GLOBAL_ACTION_RECENTS
            else -> return "failed"
        }
        return if (service.performGlobalAction(globalAction)) "executed" else "failed"
    }

    private fun tapAtNodeCenter(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val path = Path().apply {
            moveTo(bounds.exactCenterX(), bounds.exactCenterY())
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        return dispatchGestureSync(gesture) == "executed"
    }

    private fun dispatchGestureSync(gesture: GestureDescription): String {
        val latch = CountDownLatch(1)
        var succeeded = false
        service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                succeeded = true
                latch.countDown()
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                succeeded = false
                latch.countDown()
            }
        }, null)

        val completedInTime = latch.await(2, TimeUnit.SECONDS)
        return if (completedInTime && succeeded) "executed" else "failed"
    }

    private fun findFirstScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val match = findFirstScrollable(child)
            if (match != null) return match
            child.recycle()
        }
        return null
    }
}
