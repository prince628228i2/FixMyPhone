package com.fixmyphone.voice
import com.facebook.react.*; import com.facebook.react.bridge.*
class VoicePackage:ReactPackage{override fun createNativeModules(c:ReactApplicationContext)=listOf(SpeechToTextModule(c),TextToSpeechModule(c));override fun createViewManagers(c:ReactApplicationContext)=emptyList<ViewManager<*,*>>()}
