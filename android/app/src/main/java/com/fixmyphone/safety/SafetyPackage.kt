package com.fixmyphone.safety
import com.facebook.react.uimanager.ViewManager

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class SafetyPackage : ReactPackage {
    override fun createNativeModules(c: ReactApplicationContext): List<NativeModule> =
        listOf(SecureKeyStoreModule(c))

    override fun createViewManagers(c: ReactApplicationContext): List<ViewManager<*, *>> =
        emptyList()
}
