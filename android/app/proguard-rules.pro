# Keep React Native and Kotlin reflection-friendly entry points.
-keep class com.fixmyphone.** { *; }
-keepclassmembers class * { @com.facebook.react.bridge.ReactMethod <methods>; }
-dontwarn javax.annotation.**
