package com.fixmyphone.accessibility

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

/**
 * Registers AccessibilityBridgeModule with React Native. Added to
 * MainApplication.getPackages() alongside the autolinked packages.
 * Each later part (voice, device, agent, safety) gets its own small
 * ReactPackage the same way, rather than one giant package class —
 * keeps native modules as loosely coupled as the JS-side folders are.
 */
class AccessibilityPackage : ReactPackage {
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return listOf(AccessibilityBridgeModule(reactContext))
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }
}
