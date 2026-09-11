package com.fixmyphone.device
import com.facebook.react.uimanager.ViewManager
import com.facebook.react.*; import com.facebook.react.bridge.*
class DevicePackage:ReactPackage{override fun createNativeModules(c:ReactApplicationContext)=listOf(TelephonyModule(c),ContactsModule(c),ConnectivityModule(c),VolumeModule(c),AppLauncherModule(c),MediaControlModule(c));override fun createViewManagers(c:ReactApplicationContext)=emptyList<ViewManager<*,*>>()}
