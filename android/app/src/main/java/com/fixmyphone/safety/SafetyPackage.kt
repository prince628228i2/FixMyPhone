package com.fixmyphone.safety
import com.facebook.react.*; import com.facebook.react.bridge.*
class SafetyPackage:ReactPackage{override fun createNativeModules(c:ReactApplicationContext)=listOf(SecureKeyStoreModule(c));override fun createViewManagers(c:ReactApplicationContext)=emptyList<ViewManager<*,*>>()}
