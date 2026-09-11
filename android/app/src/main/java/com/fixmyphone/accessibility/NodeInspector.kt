package com.fixmyphone.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap

/**
 * Reads the current UI hierarchy into a plain, JSON-serializable snapshot
 * that the JS-side Observer can reason about. This is a READ-ONLY component
 * — it never mutates node state. Nothing here is persisted to disk or sent
 * anywhere outside the app process; the snapshot lives only in the RN
 * bridge call's return value for that single Observer step.
 *
 * Privacy note: text content of on-screen fields (including ones a user
 * hasn't chosen to share) is inherently visible to an AccessibilityService.
 * We do not filter it here because the Agent Engine needs it to do its job,
 * but the app must disclose this plainly to the user (see Part 10 —
 * Play Store readiness / accessibility disclosure copy) and must never log
 * or transmit snapshots for any purpose other than the current, user
 * initiated task.
 */
class NodeInspector(private val service: FixMyPhoneAccessibilityService) {

    /**
     * Returns a snapshot of the active window's node tree, capped in depth
     * and node count so a huge/broken hierarchy can't hang the bridge call.
     */
    fun captureSnapshot(maxNodes: Int = 400, maxDepth: Int = 40): WritableMap {
        val root = service.rootInActiveWindow
        val result = Arguments.createMap()

        if (root == null) {
            result.putBoolean("available", false)
            result.putString("reason", "no_active_window")
            return result
        }

        val nodesArray = Arguments.createArray()
        val counter = intArrayOf(0)
        walk(root, depth = 0, maxDepth = maxDepth, maxNodes = maxNodes, counter = counter, out = nodesArray)

        result.putBoolean("available", true)
        result.putString("packageName", root.packageName?.toString())
        result.putInt("nodeCount", counter[0])
        result.putArray("nodes", nodesArray)
        root.recycle()
        return result
    }

    /**
     * Finds the first node matching a simple target descriptor — used by
     * NodeActionExecutor to resolve { text } / { contentDescription } /
     * { viewId } targets coming from an AI action before acting on it.
     */
    fun findNode(target: ReadableMap?, root: AccessibilityNodeInfo? = service.rootInActiveWindow): AccessibilityNodeInfo? {
        if (root == null || target == null) return null

        val text = if (target.hasKey("text")) target.getString("text") else null
        val desc = if (target.hasKey("contentDescription")) target.getString("contentDescription") else null
        val viewId = if (target.hasKey("viewId")) target.getString("viewId") else null

        return findRecursive(root, text, desc, viewId)
    }

    private fun findRecursive(
        node: AccessibilityNodeInfo,
        text: String?,
        desc: String?,
        viewId: String?
    ): AccessibilityNodeInfo? {
        val matchesText = text != null && node.text?.toString()?.contains(text, ignoreCase = true) == true
        val matchesDesc = desc != null && node.contentDescription?.toString()?.contains(desc, ignoreCase = true) == true
        val matchesId = viewId != null && node.viewIdResourceName == viewId

        if (matchesText || matchesDesc || matchesId) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val match = findRecursive(child, text, desc, viewId)
            if (match != null) return match
            if (match != child) child.recycle()
        }
        return null
    }

    private fun walk(
        node: AccessibilityNodeInfo,
        depth: Int,
        maxDepth: Int,
        maxNodes: Int,
        counter: IntArray,
        out: WritableArray
    ) {
        if (depth > maxDepth || counter[0] >= maxNodes) return
        counter[0] += 1

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val map = Arguments.createMap().apply {
            putInt("depth", depth)
            putString("className", node.className?.toString())
            putString("text", node.text?.toString())
            putString("contentDescription", node.contentDescription?.toString())
            putString("viewId", node.viewIdResourceName)
            putBoolean("clickable", node.isClickable)
            putBoolean("longClickable", node.isLongClickable)
            putBoolean("editable", node.isEditable)
            putBoolean("scrollable", node.isScrollable)
            putBoolean("checkable", node.isCheckable)
            putBoolean("checked", node.isChecked)
            putBoolean("focused", node.isFocused)
            putBoolean("enabled", node.isEnabled)
            putMap("bounds", Arguments.createMap().apply {
                putInt("left", bounds.left)
                putInt("top", bounds.top)
                putInt("right", bounds.right)
                putInt("bottom", bounds.bottom)
            })
        }
        out.pushMap(map)

        for (i in 0 until node.childCount) {
            if (counter[0] >= maxNodes) break
            val child = node.getChild(i) ?: continue
            walk(child, depth + 1, maxDepth, maxNodes, counter, out)
        }
    }
}
