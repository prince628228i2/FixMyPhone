import React from 'react'; import {Text,View,StyleSheet} from 'react-native';
export default function StatusIndicator({status}){return <View style={s.row}><View style={s.dot}/><Text style={s.text}>{status||'IDLE'}</Text></View>}
const s=StyleSheet.create({row:{flexDirection:'row',alignItems:'center'},dot:{width:9,height:9,borderRadius:5,backgroundColor:'#42d392',marginRight:8},text:{color:'#dbe4f0',fontWeight:'700'}});
