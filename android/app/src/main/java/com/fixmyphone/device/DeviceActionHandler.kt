package com.fixmyphone.device
import android.content.*; import android.net.Uri; import android.media.AudioManager; import android.provider.Settings; import com.fixmyphone.executor.*
import com.fixmyphone.accessibility.FixMyPhoneAccessibilityService
class DeviceActionHandler(private val ctx:Context):ActionHandler{
 override fun canHandle(a:ActionType)=a in setOf(ActionType.CALL,ActionType.SPEAKER_ON,ActionType.SPEAKER_OFF,ActionType.OPEN_APP,ActionType.SET_VOLUME,ActionType.TOGGLE_BLUETOOTH,ActionType.TOGGLE_AIRPLANE_MODE,ActionType.MEDIA_PLAY,ActionType.MEDIA_PAUSE,ActionType.MEDIA_SEEK,ActionType.RESOLVE_CONTACT)
 override fun execute(a:FixMyPhoneAction):String{try{when(a.action){
 ActionType.CALL->{val ref=a.params["contactRef"]?.toString()?:return "failed";if(ctx.checkSelfPermission("android.permission.CALL_PHONE")!=0)return "requires_user";var number=ref;val cr=ctx.contentResolver.query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),"DISPLAY_NAME LIKE ?",arrayOf("%$ref%"),null);cr?.use{if(it.count==1&&it.moveToFirst()) number=it.getString(0) else if(it.count>1)return "requires_user"};ctx.startActivity(Intent(Intent.ACTION_CALL,Uri.parse("tel:${Uri.encode(number)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
 ActionType.SPEAKER_ON,ActionType.SPEAKER_OFF->{(ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager).isSpeakerphoneOn=a.action==ActionType.SPEAKER_ON}
 ActionType.OPEN_APP->{val q=a.params["appName"].toString();val pm=ctx.packageManager;val x=pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0).firstOrNull{it.loadLabel(pm).toString().contains(q,true)}?:return "failed";ctx.startActivity(pm.getLaunchIntentForPackage(x.activityInfo.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
 ActionType.SET_VOLUME->{val v=a.params["level"].toString().toInt().coerceIn(0,100);val am=ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager;am.setStreamVolume(AudioManager.STREAM_MUSIC,am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*v/100,0)}
 ActionType.TOGGLE_BLUETOOTH->{ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return "requires_user"}
 ActionType.TOGGLE_AIRPLANE_MODE->{ctx.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return "requires_user"}
 ActionType.MEDIA_PLAY->{sendMedia(android.view.KeyEvent.KEYCODE_MEDIA_PLAY)}
 ActionType.MEDIA_PAUSE->{sendMedia(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)}
 ActionType.MEDIA_SEEK->{return "requires_user"}
 ActionType.RESOLVE_CONTACT->{return "requires_user"}
 else->return "failed"};return "executed"}catch(e:Exception){return "failed"}}
 private fun sendMedia(code:Int){val i=Intent(Intent.ACTION_MEDIA_BUTTON).apply{putExtra(Intent.EXTRA_KEY_EVENT,android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,code))};ctx.sendBroadcast(i)}
}
