import React,{useEffect,useState} from 'react';
import {View,Text,Pressable,StyleSheet,Alert} from 'react-native';
import {startAssistant} from '../voice/AssistantSession';
import {isAccessibilityServiceEnabled,openAccessibilitySettings} from '../services/AccessibilityBridge';

export default function HomeScreen({onSettings}){
 const [enabled,setEnabled]=useState(false);
 useEffect(()=>{isAccessibilityServiceEnabled().then(setEnabled).catch(()=>{});},[]);
 const launch=async(mode)=>{
  if(!enabled){
   Alert.alert('Accessibility required','Fix My Phone needs Accessibility Service to understand the screen and perform actions.',[
    {text:'Open Settings',onPress:openAccessibilitySettings},{text:'Cancel',style:'cancel'}
   ]); return;
  }
  try { await startAssistant(mode); } catch(e) { Alert.alert('Could not start',String(e?.message||e)); }
 };
 return <View style={s.c}>
   <View style={s.top}><View><Text style={s.logo}>🤖 Fix My Phone</Text><Text style={s.sub}>Your AI Phone Assistant</Text></View><Pressable onPress={onSettings} style={s.gear}><Text style={s.gearT}>⚙</Text></Pressable></View>
   <View style={s.center}>
    <Text style={s.head}>How can I help, Sir?</Text>
    <Text style={s.hint}>Choose a mode. I’ll handle the rest by voice.</Text>
    <Pressable style={s.fix} onPress={()=>launch('fix')}>
      <View style={s.icon}><Text style={s.iconT}>🔧</Text></View><View style={s.copy}><Text style={s.title}>Fix My Phone</Text><Text style={s.desc}>Tell me your phone problem</Text></View><Text style={s.arrow}>›</Text>
    </Pressable>
    <Pressable style={s.ai} onPress={()=>launch('24x7')}>
      <View style={s.icon}><Text style={s.iconT}>🤖</Text></View><View style={s.copy}><Text style={s.title}>24×7 AI</Text><Text style={s.desc}>Always ready for your voice</Text></View><Text style={s.arrow}>›</Text>
    </Pressable>
   </View>
   <View style={s.bottom}><View style={[s.dot,{backgroundColor:enabled?'#36e58b':'#ffbd4a'}]}/><Text style={s.bottomT}>{enabled?'Accessibility ready':'Accessibility needs setup'}</Text></View>
 </View>
}
const s=StyleSheet.create({c:{flex:1,padding:22,backgroundColor:'#071018'},top:{flexDirection:'row',justifyContent:'space-between',alignItems:'center',paddingTop:16},logo:{color:'#fff',fontSize:25,fontWeight:'900'},sub:{color:'#7e91a6',marginTop:4},gear:{width:44,height:44,borderRadius:22,backgroundColor:'#111d29',alignItems:'center',justifyContent:'center'},gearT:{fontSize:21,color:'#dce8f4'},center:{flex:1,justifyContent:'center'},head:{fontSize:28,fontWeight:'900',color:'#fff'},hint:{color:'#8194a8',fontSize:14,marginTop:7,marginBottom:25},fix:{minHeight:118,borderRadius:22,backgroundColor:'#087eea',padding:20,flexDirection:'row',alignItems:'center',marginBottom:15},ai:{minHeight:118,borderRadius:22,backgroundColor:'#079b68',padding:20,flexDirection:'row',alignItems:'center'},icon:{width:56,height:56,borderRadius:18,backgroundColor:'rgba(255,255,255,.16)',alignItems:'center',justifyContent:'center'},iconT:{fontSize:27},copy:{flex:1,marginLeft:15},title:{color:'#fff',fontSize:20,fontWeight:'900'},desc:{color:'rgba(255,255,255,.82)',fontSize:13,marginTop:5},arrow:{color:'#fff',fontSize:38,fontWeight:'300'},bottom:{flexDirection:'row',alignItems:'center',justifyContent:'center',paddingBottom:15},dot:{width:8,height:8,borderRadius:4,marginRight:8},bottomT:{color:'#73879a',fontSize:12}});
