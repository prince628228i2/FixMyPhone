import {NativeModules} from 'react-native';
import {startListening, stopListening, subscribeVoiceEvents} from './VoiceInputManager';
import {speak, stopSpeaking} from './TextToSpeech';
import {runAgent} from '../agent/AgentEngine';
import {useAgentStore} from '../agent/AgentState';
import {ConversationContext} from './ConversationContext';
import {requestMicrophonePermission} from '../utils/permissions';

const {AssistantBridge}=NativeModules;
let active=false,mode='fix',processing=false,queue=[],unsub=null,restartTimer=null,confirmationResolver=null;
let wakeArmed=false;
const WAKE_WORD=/\bedith\b/i;

function extractWakeCommand(text){
 const raw=String(text||'').trim();
 if(!WAKE_WORD.test(raw)) return null;
 return raw.replace(WAKE_WORD,'').replace(/^[,;:!?.\s]+/,'').trim();
}

function greeting(){const h=new Date().getHours(); if(h>=5&&h<12)return 'Good morning, Sir.'; if(h>=12&&h<17)return 'Good afternoon, Sir.'; if(h>=17&&h<21)return 'Good evening, Sir.'; return 'Good night, Sir.';}
function isSmallTalk(text){return /^(ok|okay|haan|ha|yes|no|nahi|theek|thik|hmm|samajh gaya|samajh gayi|ji|achha|accha|right|fine|thank you|thanks)[.!\s]*$/i.test(String(text||'').trim());}
async function listenAgain(delay=120){if(!active)return;clearTimeout(restartTimer);restartTimer=setTimeout(()=>startListening('hi-IN').catch(()=>{}),delay);}
async function say(text){stopListening(); await speak(text); if(active) await listenAgain(100);}

async function processUtterance(text){
 const goal=String(text||'').trim(); if(!goal||!active)return;
 if(isSmallTalk(goal)){await say('Ji Sir, main sun rahi hoon.');return;}
 processing=true; ConversationContext.set({lastUserUtterance:goal}); useAgentStore.getState().addTranscript({role:'user',text:goal,at:Date.now()});
 try{
   const result=await runAgent(goal,{conversation:ConversationContext.get(),confirmAction:async action=>{
     const answer=new Promise(resolve=>{confirmationResolver=resolve;});
     await say('Sir, is action ko karne ki permission hai? Haan ya nahi boliye.');
     return await answer;
   },onAssistantReply:async reply=>{if(reply){useAgentStore.getState().addTranscript({role:'assistant',text:reply,at:Date.now()});await say(reply);}}});
   if(result.status==='success'&&!result.assistantReply){ await say(mode==='24x7'?'Ho gaya Sir. Main sleep mode mein hoon, jab chahein boliye.':'Ho gaya Sir. Main sleep mode mein ja rahi hoon.'); if(mode==='fix'){ await stopAssistant(); return; } }
   else if(result.status==='needs_input'&&!result.assistantReply) await say('Sir, ek zaroori information chahiye.');
   else if(result.status==='failed') await say('Sir, main abhi isse solve nahi kar paayi. Main screen dobara check karti hoon.');
 }catch(e){await say('Sir, ek problem aa gayi. Main dobara try karti hoon.');}
 finally{processing=false; if(queue.length) processUtterance(queue.shift()); else await listenAgain();}
}

export async function startAssistant(nextMode='fix'){
 if(active)return;
 const micGranted=await requestMicrophonePermission();
 if(!micGranted) throw new Error('Microphone permission is required for voice assistant.');
 active=true;
 mode=nextMode;
 wakeArmed=(nextMode==='24x7');
 queue=[];
 processing=false;
 ConversationContext.clear();
 useAgentStore.getState().reset();
 try{
  await AssistantBridge?.startAssistant?.(mode);
 }catch(e){
  active=false;
  wakeArmed=false;
  throw e;
 }
 await AssistantBridge?.minimizeApp?.();
 await say(greeting());
 await say(mode==='24x7'
  ? 'Hello Sir, mera naam Edith hai. Jab bhi aapko mujhse baat karni ho, sabse pehle Edith bolna hoga. Uske baad main aapki baat sunungi aur aapki madad karungi.'
  : 'Aapke phone mein kya problem hai? Aap mujhe bataiye, main use fix karne ki koshish karti hoon.');
 unsub=subscribeVoiceEvents(e=>{
  if(!active)return;
  if(e.event==='results'&&e.text){
   const raw=String(e.text).trim();
   listenAgain(40);
   if(mode==='24x7'&&wakeArmed){
    const command=extractWakeCommand(raw);
    if(command===null)return;
    if(!command){say('Ji Sir, boliye.');return;}
    if(processing)queue.push(command);else processUtterance(command);
    return;
   }
   if(processing)queue.push(raw);else processUtterance(raw);
  }
 });
 await listenAgain(120);
}
export async function stopAssistant(){active=false;wakeArmed=false;processing=false;queue=[];confirmationResolver=null;clearTimeout(restartTimer);stopListening();stopSpeaking();unsub?.();unsub=null;await AssistantBridge?.stopAssistant?.();}
export const isAssistantActive=()=>active;
