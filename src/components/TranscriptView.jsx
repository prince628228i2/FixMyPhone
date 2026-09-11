import React from 'react'; import {ScrollView,Text,StyleSheet} from 'react-native';
export default function TranscriptView({items=[]}){return <ScrollView style={s.box}>{items.map((x,i)=><Text key={i} style={s.t}>{typeof x==='string'?x:x.text||JSON.stringify(x)}</Text>)}</ScrollView>}
const s=StyleSheet.create({box:{maxHeight:150,marginVertical:10},t:{color:'#aab7c8',paddingVertical:3}});
