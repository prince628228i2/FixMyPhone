import React from 'react'; import {Pressable,Text,StyleSheet} from 'react-native';
export default function StopButton({onPress}){return <Pressable onPress={onPress} style={s.b}><Text style={s.t}>STOP</Text></Pressable>}
const s=StyleSheet.create({b:{padding:14,borderRadius:12,borderWidth:1,borderColor:'#ef5350'},t:{color:'#ff8a80',fontWeight:'800'}});
