package com.fixmyphone.device
import android.Manifest; import android.content.pm.PackageManager; import android.provider.ContactsContract; import androidx.core.content.ContextCompat; import com.facebook.react.bridge.*
class ContactsModule(private val ctx:ReactApplicationContext):ReactContextBaseJavaModule(ctx){
 override fun getName()="Contacts"
 @ReactMethod fun resolve(name:String,p:Promise){if(ContextCompat.checkSelfPermission(ctx,Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){p.resolve(result("requires_user","read_contacts_permission"));return};val c=ctx.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER),"DISPLAY_NAME LIKE ?",arrayOf("%$name%"),"${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC");val out=Arguments.createArray();c?.use{while(it.moveToNext()){val m=Arguments.createMap();m.putString("name",it.getString(0));m.putString("number",it.getString(1));out.pushMap(m)}};val r=Arguments.createMap();r.putString("status","executed");r.putArray("contacts",out);p.resolve(r)}
 private fun result(s:String,r:String)=Arguments.createMap().apply{putString("status",s);putString("reason",r)}
}
