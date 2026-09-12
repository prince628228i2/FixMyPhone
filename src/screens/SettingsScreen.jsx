import React,{useEffect,useState} from 'react';
import {View,Text,TextInput,Pressable,StyleSheet,Alert,NativeModules} from 'react-native';

export default function SettingsScreen({onBack}){
 const [provider,setProvider]=useState('gemini');
 const [key,setKey]=useState('');
 const [model,setModel]=useState('gemini-2.0-flash');

 useEffect(()=>{
  (async()=>{
   try{
    const c=await NativeModules.SecureKeyStore?.getProviderConfig?.();
    if(c?.provider)setProvider(String(c.provider).toLowerCase());
    if(c?.key)setKey(String(c.key));
    if(c?.model)setModel(String(c.model));
   }catch(e){}
  })();
 },[]);

 const save=async()=>{
  const p=provider.trim().toLowerCase();
  const k=key.trim();
  const m=model.trim();

  if(p!=='gemini'&&p!=='openai'){
   Alert.alert('Invalid provider','Provider must be gemini or openai.');
   return;
  }

  if(!k){
   Alert.alert('API key required','Please enter your API key first.');
   return;
  }

  const finalModel=m||(p==='openai'?'gpt-4o-mini':'gemini-2.0-flash');

  try{
   await NativeModules.SecureKeyStore.setProviderConfig(p,k,finalModel);
   setProvider(p);
   setModel(finalModel);
   Alert.alert('Saved','AI provider configuration stored securely on device.');
  }catch(e){
   Alert.alert('Error',String(e?.message||e));
  }
 };

 return (
  <View style={s.c}>
   <Pressable onPress={onBack} style={s.back}>
    <Text style={s.backT}>‹  Back</Text>
   </Pressable>

   <Text style={s.h}>AI Settings</Text>

   <Text style={s.l}>Provider (gemini/openai)</Text>
   <TextInput
    value={provider}
    onChangeText={setProvider}
    autoCapitalize="none"
    autoCorrect={false}
    style={s.i}
   />

   <Text style={s.l}>API key</Text>
   <TextInput
    value={key}
    onChangeText={setKey}
    secureTextEntry
    autoCapitalize="none"
    autoCorrect={false}
    style={s.i}
   />

   <Text style={s.l}>Model</Text>
   <TextInput
    value={model}
    onChangeText={setModel}
    autoCapitalize="none"
    autoCorrect={false}
    style={s.i}
   />

   <Pressable onPress={save} style={s.b}>
    <Text style={s.bt}>Save securely</Text>
   </Pressable>
  </View>
 );
}

const s=StyleSheet.create({
 c:{flex:1,backgroundColor:'#0b0e14',padding:22},
 back:{marginBottom:18},
 backT:{color:'#8ecbff',fontSize:16,fontWeight:'700'},
 h:{color:'#fff',fontSize:28,fontWeight:'900',marginBottom:20},
 l:{color:'#9aa7b8',marginTop:12,marginBottom:6},
 i:{backgroundColor:'#151b25',color:'#fff',padding:13,borderRadius:10},
 b:{backgroundColor:'#fff',padding:15,borderRadius:10,marginTop:20,alignItems:'center'},
 bt:{color:'#000',fontWeight:'700'}
});
