import React,{useEffect,useState} from 'react';
import {
  View,
  Text,
  TextInput,
  Pressable,
  ScrollView,
  StyleSheet,
  NativeModules,
  DeviceEventEmitter
} from 'react-native';
import {runAgent} from '../agent/AgentEngine';
import {useAgentStore} from '../agent/AgentState';

export default function AgentTestScreen({onBack}){
  const [goal,setGoal]=useState('');
  const [running,setRunning]=useState(false);
  const [logs,setLogs]=useState([]);
  const state=useAgentStore(s=>s);

  useEffect(()=>{
    const sub=DeviceEventEmitter.addListener(
      'FixMyPhoneProgress',
      message=>{
        setLogs(prev=>[...prev,String(message)].slice(-300));
      }
    );

    return ()=>{
      sub.remove();
      useAgentStore.getState().stop();
    };
  },[]);

  const run=async()=>{
    const g=goal.trim();
    if(!g||running)return;

    setLogs([]);
    useAgentStore.getState().reset();
    setRunning(true);

    try{
      await runAgent(g,{
        conversation:{source:'developer_test'},
        onAssistantReply:reply=>{
          useAgentStore.getState().addTranscript({
            role:'assistant',
            text:String(reply)
          });
        },
        confirmAction:async()=>true
      });
    }finally{
      setRunning(false);
    }
  };

  const clearLogs=()=>{
    setLogs([]);
    useAgentStore.getState().reset();
  };

  return (
    <View style={s.root}>

      <View style={s.header}>
        <Pressable onPress={onBack}>
          <Text style={s.back}>‹ Back</Text>
        </Pressable>
        <Text style={s.head}>AI Agent Test</Text>
      </View>

      <Text style={s.label}>Test command</Text>

      <TextInput
        value={goal}
        onChangeText={setGoal}
        editable={!running}
        placeholder="Example: Wi-Fi on karo"
        placeholderTextColor="#718398"
        style={s.input}
      />

      <View style={s.buttons}>
        <Pressable
          disabled={running}
          onPress={run}
          style={[s.run,{opacity:running?.55:1}]}
        >
          <Text style={s.runText}>
            {running?'RUNNING...':'RUN AI TEST'}
          </Text>
        </Pressable>

        <Pressable onPress={clearLogs} style={s.clear}>
          <Text style={s.clearText}>CLEAR</Text>
        </Pressable>
      </View>

      <View style={s.card}>
        <Text style={s.status}>
          STATUS: {state.status}
        </Text>

        <Text style={s.info}>
          Task: {state.task||'—'}
        </Text>

        <Text style={s.info}>
          Actions: {state.actionsDone}
        </Text>

        {state.lastError ? (
          <Text style={s.error}>
            ERROR: {state.lastError}
          </Text>
        ):null}
      </View>

      <Text style={s.label}>LIVE DIAGNOSTIC</Text>

      <ScrollView
        style={s.log}
        contentContainerStyle={s.logContent}
      >
        {logs.length===0 ? (
          <Text style={s.empty}>
            Waiting for test...
          </Text>
        ):(
          logs.map((line,i)=>(
            <Text key={i} style={s.line}>
              {line}
            </Text>
          ))
        )}
      </ScrollView>

      <Text style={s.label}>ASSISTANT REPLIES</Text>

      <ScrollView style={s.replyLog}>
        {(state.transcript||[]).map((x,i)=>(
          <Text key={i} style={s.line}>
            {x.role}: {x.text}
          </Text>
        ))}
      </ScrollView>

    </View>
  );
}

const s=StyleSheet.create({
  root:{
    flex:1,
    backgroundColor:'#071018',
    padding:20
  },
  header:{
    flexDirection:'row',
    alignItems:'center',
    marginBottom:25
  },
  back:{
    color:'#62b5ff',
    fontSize:16,
    fontWeight:'700',
    marginRight:20
  },
  head:{
    color:'#fff',
    fontSize:22,
    fontWeight:'900'
  },
  label:{
    color:'#9db0c3',
    fontSize:13,
    fontWeight:'700',
    marginBottom:8,
    marginTop:8
  },
  input:{
    backgroundColor:'#111d29',
    borderRadius:14,
    color:'#fff',
    padding:15,
    fontSize:16
  },
  buttons:{
    flexDirection:'row',
    gap:10,
    marginTop:12
  },
  run:{
    flex:1,
    borderRadius:14,
    backgroundColor:'#087eea',
    padding:16,
    alignItems:'center'
  },
  runText:{
    color:'#fff',
    fontWeight:'900'
  },
  clear:{
    borderRadius:14,
    backgroundColor:'#182634',
    paddingHorizontal:18,
    justifyContent:'center'
  },
  clearText:{
    color:'#8fc7ff',
    fontWeight:'900'
  },
  card:{
    backgroundColor:'#111d29',
    borderRadius:14,
    padding:15,
    marginTop:15
  },
  status:{
    color:'#36e58b',
    fontWeight:'900',
    fontSize:15
  },
  info:{
    color:'#c8d5e2',
    marginTop:6
  },
  error:{
    color:'#ff6b6b',
    marginTop:8
  },
  log:{
    backgroundColor:'#050b10',
    borderRadius:14,
    height:210,
    padding:12
  },
  logContent:{
    paddingBottom:10
  },
  replyLog:{
    backgroundColor:'#050b10',
    borderRadius:14,
    height:90,
    padding:12
  },
  line:{
    color:'#b8c8d8',
    fontSize:12,
    marginBottom:7
  },
  empty:{
    color:'#607487',
    fontSize:12
  }
});
