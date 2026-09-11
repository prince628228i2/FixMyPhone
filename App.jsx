import React,{useEffect,useState} from 'react';
import {SafeAreaView,StatusBar,StyleSheet,View} from 'react-native';
import HomeScreen from './src/screens/HomeScreen';
import SettingsScreen from './src/screens/SettingsScreen';
import {useAgentStore} from './src/agent/AgentState';
export default function App(){
 const dark=true; const [screen,setScreen]=useState('home'); const reset=useAgentStore(s=>s.reset);
 useEffect(()=>reset(),[reset]);
 return <SafeAreaView style={s.root}><StatusBar barStyle="light-content"/><View style={s.body}>{screen==='settings'?<SettingsScreen onBack={()=>setScreen('home')}/>:<HomeScreen onSettings={()=>setScreen('settings')}/>}</View></SafeAreaView>
}
const s=StyleSheet.create({root:{flex:1,backgroundColor:'#071018'},body:{flex:1}});
