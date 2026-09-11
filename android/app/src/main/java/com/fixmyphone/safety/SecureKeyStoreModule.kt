package com.fixmyphone.safety
import android.content.Context; import androidx.security.crypto.EncryptedSharedPreferences; import androidx.security.crypto.MasterKey; import com.facebook.react.bridge.*
class SecureKeyStoreModule(private val ctx:ReactApplicationContext):ReactContextBaseJavaModule(ctx){
 private val prefs by lazy{val key=MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();EncryptedSharedPreferences.create(ctx,"fixmyphone_secrets",key,EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)}
 override fun getName()="SecureKeyStore"
 @ReactMethod fun setProviderConfig(provider:String,key:String,model:String,p:Promise){prefs.edit().putString("provider",provider.lowercase()).putString("key",key).putString("model",model).apply();p.resolve(true)}
 @ReactMethod fun getProviderConfig(p:Promise){val r=Arguments.createMap();r.putString("provider",prefs.getString("provider",null));r.putString("key",prefs.getString("key",null));r.putString("model",prefs.getString("model","gpt-4o-mini"));p.resolve(r)}
 @ReactMethod fun clear(p:Promise){prefs.edit().clear().apply();p.resolve(true)}
}
