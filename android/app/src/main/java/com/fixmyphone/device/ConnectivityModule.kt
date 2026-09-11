package com.fixmyphone.device
import android.content.*; import android.net.wifi.WifiManager; import android.provider.Settings; import com.facebook.react.bridge.*
class ConnectivityModule(private val ctx:ReactApplicationContext):ReactContextBaseJavaModule(ctx){
 override fun getName()="Connectivity"
 @ReactMethod fun setWifi(state:String,p:Promise){if(android.os.Build.VERSION.SDK_INT>=29){ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));p.resolve(result("requires_user","android_restricts_wifi_toggle"))}else{val w=ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager;w.isWifiEnabled=state=="on";p.resolve(result("executed",null))}}
 @ReactMethod fun openAirplaneSettings(p:Promise){ctx.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));p.resolve(true)}
 private fun result(s:String,r:String?)=Arguments.createMap().apply{putString("status",s);r?.let{putString("reason",it)}}
}
