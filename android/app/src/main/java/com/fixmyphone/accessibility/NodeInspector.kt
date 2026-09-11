package com.fixmyphone.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap

class NodeInspector(private val service: FixMyPhoneAccessibilityService) {

    fun captureSnapshot(maxNodes: Int = 600, maxDepth: Int = 50): WritableMap {
        val result = Arguments.createMap()
        val root = service.rootInActiveWindow

        result.putBoolean("serviceConnected", FixMyPhoneAccessibilityService.isConnected())
        result.putInt("windowCount", service.windows?.size ?: 0)

        if (root == null) {
            result.putBoolean("available", false)
            result.putString("reason", "no_active_window")
            return result
        }

        val nodes = Arguments.createArray()
        val counter = intArrayOf(0)

        walk(root, 0, maxDepth, maxNodes, counter, nodes)

        result.putBoolean("available", true)
        result.putString("packageName", root.packageName?.toString())
        result.putString("className", root.className?.toString())
        result.putInt("nodeCount", counter[0])
        result.putArray("nodes", nodes)

        root.recycle()
        return result
    }

    fun captureWindows(): WritableArray {
        val result = Arguments.createArray()

        service.windows?.forEach { window ->
            val map = Arguments.createMap()
            map.putInt("id", window.id)
            map.putInt("type", window.type)
            map.putBoolean("active", window.isActive)
            map.putBoolean("focused", window.isFocused)
            map.putString("title", window.title?.toString())
            map.putString("packageName", window.root?.packageName?.toString())
            result.pushMap(map)
        }

        return result
    }

    fun findNode(
        target: ReadableMap?,
        root: AccessibilityNodeInfo? = service.rootInActiveWindow
    ): AccessibilityNodeInfo? {
        if (root == null || target == null) {
            root?.recycle()
            return null
        }

        val text = target.stringOrNull("text")
        val desc = target.stringOrNull("contentDescription")
        val viewId = target.stringOrNull("viewId")
        val className = target.stringOrNull("className")

        val result = findRecursive(root, text, desc, viewId, className)

        if (result !== root) root.recycle()
        return result
    }

    private fun findRecursive(
        node: AccessibilityNodeInfo,
        text: String?,
        desc: String?,
        viewId: String?,
        className: String?
    ): AccessibilityNodeInfo? {
        val matchesText = text != null &&
            node.text?.toString()?.contains(text, ignoreCase = true) == true

        val matchesDesc = desc != null &&
            node.contentDescription?.toString()?.contains(desc, ignoreCase = true) == true

        val matchesId = viewId != null &&
            node.viewIdResourceName == viewId

        val matchesClass = className != null &&
            node.className?.toString() == className

        if (matchesText || matchesDesc || matchesId || matchesClass) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val match = findRecursive(child, text, desc, viewId, className)

            if (match != null) return match
            child.recycle()
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

        counter[0]++

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
            putBoolean("focusable", node.isFocusable)
            putBoolean("selected", node.isSelected)
            putBoolean("enabled", node.isEnabled)
            putBoolean("password", node.isPassword)
            putBoolean("visible", node.isVisibleToUser)
            putBoolean("dismissable", node.isDismissable)
            putBoolean("contextClickable", node.isContextClickable)
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
            child.recycle()
        }
    }

    private fun ReadableMap.stringOrNull(key: String): String? =
        if (hasKey(key) && !isNull(key)) getString(key) else null
}
