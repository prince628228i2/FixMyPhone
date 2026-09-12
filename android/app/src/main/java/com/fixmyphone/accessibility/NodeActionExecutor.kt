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

class NodeActionExecutor(private val service: FixMyPhoneAccessibilityService) {

    private val inspector get() = service.nodeInspector

    fun tap(target: ReadableMap?): String =
        performNode(target) { node ->
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            } else {
                tapAtNodeCenter(node)
            }
        }

    fun longPress(target: ReadableMap?): String =
        performNode(target) {
            it.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
        }

    fun typeText(target: ReadableMap?, text: String?): String {
        if (text == null) return "failed"
        return performNode(target) {
            if (!it.isEditable) return@performNode false
            Bundle().also { args ->
                args.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
            }.let { args ->
                it.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
        }
    }

    fun clearText(target: ReadableMap?): String =
        typeText(target, "")

    fun focus(target: ReadableMap?): String =
        performNode(target) {
            it.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        }

    fun select(target: ReadableMap?): String =
        performNode(target) {
            it.performAction(AccessibilityNodeInfo.ACTION_SELECT)
        }

    fun copy(target: ReadableMap?): String =
        performNode(target) {
            it.performAction(AccessibilityNodeInfo.ACTION_COPY)
        }

    fun paste(target: ReadableMap?): String =
        performNode(target) {
            it.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }

    fun expand(target: ReadableMap?): String =
        performNode(target) {
            it.performAction(AccessibilityNodeInfo.ACTION_EXPAND)
        }

    fun collapse(target: ReadableMap?): String =
        performNode(target) {
            it.performAction(AccessibilityNodeInfo.ACTION_COLLAPSE)
        }

    fun dismiss(target: ReadableMap?): String =
        performNode(target) {
            it.performAction(AccessibilityNodeInfo.ACTION_DISMISS)
        }

    fun toggle(target: ReadableMap?): String =
        performNode(target) {
            when {
                it.isCheckable ->
                    it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                else ->
                    it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }

    fun scroll(direction: String?): String {
        val root = service.rootInActiveWindow ?: return "failed"
        val scrollable = findFirstScrollable(root)

        if (scrollable == null) {
            root.recycle()
            return "failed"
        }

        val action = when (direction?.lowercase()) {
            "up", "backward", "previous" ->
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            else ->
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }

        val result = scrollable.performAction(action)

        if (scrollable !== root) scrollable.recycle()
        root.recycle()

        return if (result) "executed" else "failed"
    }

    fun swipe(direction: String?): String {
        val root = service.rootInActiveWindow ?: return "failed"
        val bounds = Rect()
        root.getBoundsInScreen(bounds)
        root.recycle()

        val centerX = (bounds.left + bounds.right) / 2f
        val centerY = (bounds.top + bounds.bottom) / 2f
        val quarterH = (bounds.bottom - bounds.top) / 4f

        val startY: Float
        val endY: Float

        when (direction?.lowercase()) {
            "up" -> {
                startY = centerY + quarterH
                endY = centerY - quarterH
            }
            "down" -> {
                startY = centerY - quarterH
                endY = centerY + quarterH
            }
            else -> return "failed"
        }

        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()

        return dispatchGestureSync(gesture)
    }

    fun toggleWifi(desiredOn: Boolean): String {
        val root = service.rootInActiveWindow ?: return "failed"
        return try {
            findAndToggleWifi(root, desiredOn)
        } finally {
            root.recycle()
        }
    }

    private fun findAndToggleWifi(
        node: AccessibilityNodeInfo,
        desiredOn: Boolean
    ): String {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val id = node.viewIdResourceName?.lowercase() ?: ""

        val relevant =
            text.contains("wi-fi") || text.contains("wifi") ||
            text.contains("internet") || desc.contains("wi-fi") ||
            desc.contains("wifi") || desc.contains("internet") ||
            id.contains("wifi") || id.contains("internet")

        if (relevant) {
            if (node.isCheckable && node.isChecked == desiredOn) return "executed"

            val clicked = when {
                node.isClickable ->
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                node.parent?.isClickable == true ->
                    node.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                else ->
                    tapAtNodeCenter(node)
            }

            if (clicked) return "executed"
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = try {
                findAndToggleWifi(child, desiredOn)
            } finally {
                child.recycle()
            }
            if (result == "executed") return result
        }

        return "requires_user"
    }

    fun globalAction(action: String): String {
        val global = when (action.uppercase()) {
            "BACK" -> AccessibilityService.GLOBAL_ACTION_BACK
            "HOME" -> AccessibilityService.GLOBAL_ACTION_HOME
            "RECENTS" -> AccessibilityService.GLOBAL_ACTION_RECENTS
            "NOTIFICATIONS" -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
            "QUICK_SETTINGS" -> AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
            "POWER_DIALOG" -> AccessibilityService.GLOBAL_ACTION_POWER_DIALOG
            "LOCK_SCREEN" -> AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
            else -> return "failed"
        }

        return if (service.performGlobalAction(global)) "executed" else "failed"
    }

    private fun performNode(
        target: ReadableMap?,
        action: (AccessibilityNodeInfo) -> Boolean
    ): String {
        val node = inspector.findNode(target) ?: return "failed"

        return try {
            if (action(node)) "executed" else "failed"
        } finally {
            node.recycle()
        }
    }

    private fun tapAtNodeCenter(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val path = Path().apply {
            moveTo(bounds.exactCenterX(), bounds.exactCenterY())
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
            .build()

        return dispatchGestureSync(gesture) == "executed"
    }

    private fun dispatchGestureSync(gesture: GestureDescription): String {
        val latch = CountDownLatch(1)
        var succeeded = false

        service.dispatchGesture(
            gesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    succeeded = true
                    latch.countDown()
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    succeeded = false
                    latch.countDown()
                }
            },
            null
        )

        val completed = latch.await(2, TimeUnit.SECONDS)
        return if (completed && succeeded) "executed" else "failed"
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
