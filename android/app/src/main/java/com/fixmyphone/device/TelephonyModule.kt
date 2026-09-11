package com.fixmyphone.device
import android.Manifest; import android.content.*; import android.content.pm.PackageManager; import android.media.AudioManager; import android.net.Uri; import android.provider.Settings; import android.telecom.TelecomManager; import androidx.core.content.ContextCompat; import com.facebook.react.bridge.*
class TelephonyModule(private val ctx:ReactApplicationContext):ReactContextBaseJavaModule(ctx){
 override fun getName()="Telephony"
 @ReactMethod fun call(number:String,p:Promise){if(ContextCompat.checkSelfPermission(ctx,Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){p.resolve(result("requires_user","call_phone_permission"));return};try{ctx.startActivity(Intent(Intent.ACTION_CALL,Uri.parse("tel:${Uri.encode(number)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));p.resolve(result("executed",null))}catch(e:Exception){p.resolve(result("failed",e.message))}}
 @ReactMethod fun speaker(on:Boolean,p:Promise){val am=ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager;am.isSpeakerphoneOn=on;p.resolve(result("executed",null))}
 @ReactMethod fun openPhoneSettings(p:Promise){ctx.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));p.resolve(true)}
 private fun result(s:String,r:String?)=Arguments.createMap().apply{putString("status",s);r?.let{putString("reason",it)}}
}
