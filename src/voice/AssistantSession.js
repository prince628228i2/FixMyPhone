import {NativeModules} from 'react-native';
import {startListening,stopListening,subscribeVoiceEvents} from './VoiceInputManager';
import {speak,stopSpeaking} from './TextToSpeech';
import {runAgent} from '../agent/AgentEngine';
import {useAgentStore} from '../agent/AgentState';
import {ConversationContext} from './ConversationContext';
import {requestMicrophonePermission} from '../utils/permissions';

const {AssistantBridge}=NativeModules;

let active=false;
let mode='fix';
let processing=false;
let listening=false;
let queue=[];
let unsub=null;
let restartTimer=null;
let partialTimer=null;
let confirmationResolver=null;
let lastSubmitted='';
let lastPartial='';

let wakeArmed=false;
const WAKE_WORD=/\bedith\b/i;

function extractWakeCommand(text){
 const raw=String(text||'').trim();
 if(!WAKE_WORD.test(raw))return null;
 return raw.replace(WAKE_WORD,'').replace(/^[,;:!?.\s]+/,'').trim();
}

function greeting(){
 const h=new Date().getHours();
 if(h>=5&&h<12)return 'Good morning, Sir.';
 if(h>=12&&h<17)return 'Good afternoon, Sir.';
 if(h>=17&&h<21)return 'Good evening, Sir.';
 return 'Good night, Sir.';
}

function isYes(text){
 return /^(yes|yeah|yep|haan|ha|ji|bilkul|kar do|do it|okay|ok|theek hai|thik hai)[.!?\s]*$/i.test(String(text||'').trim());
}

function isNo(text){
 return /^(no|nope|nahi|nahin|mat karo|cancel|cancel it|ruk jao|stop)[.!?\s]*$/i.test(String(text||'').trim());
}

function isSmallTalk(text){
 return /^(ok|okay|haan|ha|yes|no|nahi|theek|thik|hmm|samajh gaya|samajh gayi|ji|achha|accha|right|fine|thank you|thanks)[.!\s]*$/i.test(String(text||'').trim());
}

async function restartListening(delay=80){
 if(!active||processing&&!confirmationResolver)return;
 clearTimeout(restartTimer);
 restartTimer=setTimeout(async()=>{
  if(!active)return;
  try{
   await startListening('hi-IN');
   listening=true;
  }catch(e){
   listening=false;
   console.warn('[FixMyPhone][STT] restart failed:',String(e?.message||e));
  }
 },delay);
}

async function say(text){
 if(!text||!active)return;
 clearTimeout(partialTimer);
 await stopListening();
 listening=false;
 console.log('[FixMyPhone][TTS]',text);
 try{await speak(text);}
 catch(e){console.warn('[FixMyPhone][TTS] failed:',String(e?.message||e));}
 if(active)await restartListening(80);
}

function normalize(text){
 return String(text||'').trim().replace(/\s+/g,' ').toLowerCase();
}

async function processUtterance(text){
 const goal=String(text||'').trim();
 if(!goal||!active)return;

 const key=normalize(goal);
 if(key===lastSubmitted)return;
 lastSubmitted=key;

 if(confirmationResolver){
  if(isYes(goal)){
   const r=confirmationResolver;
   confirmationResolver=null;
   r(true);
   return;
  }
  if(isNo(goal)){
   const r=confirmationResolver;
   confirmationResolver=null;
   r(false);
   return;
  }
 }

 if(isSmallTalk(goal)){
  await say('Ji Sir, main sun rahi hoon.');
  return;
 }

 processing=true;
 ConversationContext.set({lastUserUtterance:goal});
 useAgentStore.getState().addTranscript({
  role:'user',
  text:goal,
  at:Date.now()
 });

 try{
  const result=await runAgent(goal,{
   conversation:ConversationContext.get(),

   confirmAction:async action=>{
    const answer=new Promise(resolve=>{
     confirmationResolver=resolve;
    });
    await say('Sir, is action ki permission hai? Haan ya nahi.');
    return await answer;
   },

   onAssistantReply:async reply=>{
    if(reply){
     useAgentStore.getState().addTranscript({
      role:'assistant',
      text:reply,
      at:Date.now()
     });
     await say(reply);
    }
   }
  });

  if(result.status==='success'&&!result.assistantReply){
   await say(
    mode==='24x7'
     ? 'Ho gaya Sir. Main yahin hoon.'
     : 'Ho gaya Sir.'
   );
   if(mode==='fix'){
    await stopAssistant();
    return;
   }
  }else if(result.status==='needs_input'&&!result.assistantReply){
   await say('Sir, ek chhoti si information chahiye.');
  }else if(result.status==='failed'){
   await say('Sir, main screen dobara check karke try karti hoon.');
  }
 }catch(e){
  console.warn('[FixMyPhone][AGENT] error:',String(e?.message||e));
  await say('Sir, ek problem aa gayi. Main dobara try karti hoon.');
 }finally{
  processing=false;
  if(active&&!confirmationResolver){
   if(queue.length){
    const next=queue.shift();
    await processUtterance(next);
   }else{
    await restartListening(80);
   }
  }
 }
}

export async function startAssistant(nextMode='fix'){
 if(active)return;

 const micGranted=await requestMicrophonePermission();
 if(!micGranted){
  throw new Error('Microphone permission is required for voice assistant.');
 }

 active=true;
 mode=nextMode;
 wakeArmed=(nextMode==='24x7');
 processing=false;
 listening=false;
 queue=[];
 lastSubmitted='';
 lastPartial='';
 confirmationResolver=null;

 clearTimeout(restartTimer);
 clearTimeout(partialTimer);

 ConversationContext.clear();
 useAgentStore.getState().reset();

 try{
  await AssistantBridge?.startAssistant?.(mode);
 }catch(e){
  active=false;
  wakeArmed=false;
  throw e;
 }

 unsub=subscribeVoiceEvents(async e=>{
  if(!active)return;

  console.log('[FixMyPhone][STT EVENT]',JSON.stringify(e));

  if(e.event==='start'){
   listening=true;
   return;
  }

  if(e.event==='end'){
   listening=false;
   if(active&&!processing)await restartListening(60);
   return;
  }

  if(e.event==='error'){
   listening=false;
   console.warn('[FixMyPhone][STT ERROR]',e.text||'unknown');
   if(active)await restartListening(120);
   return;
  }

  if(e.event==='partial'&&e.text){
   const raw=String(e.text).trim();
   if(!raw||normalize(raw)===lastPartial)return;
   lastPartial=normalize(raw);

   if(mode==='24x7'&&wakeArmed){
    const command=extractWakeCommand(raw);
    if(command){
     clearTimeout(partialTimer);
     partialTimer=setTimeout(()=>{
      if(active&&!processing)processUtterance(command);
     },180);
    }
   }
   return;
  }

  if(e.event==='results'&&e.text){
   const raw=String(e.text).trim();
   if(!raw)return;

   clearTimeout(partialTimer);

   if(confirmationResolver){
    await processUtterance(raw);
    return;
   }

   if(mode==='24x7'&&wakeArmed){
    const command=extractWakeCommand(raw);
    if(command===null)return;

    if(!command){
     await say('Ji Sir, boliye.');
     return;
    }

    if(processing){
     queue.push(command);
    }else{
     await processUtterance(command);
    }
    return;
   }

   if(processing){
    queue.push(raw);
   }else{
    await processUtterance(raw);
   }
  }
 });

 await AssistantBridge?.minimizeApp?.();

 await say(greeting());

 await say(
  mode==='24x7'
   ? 'Hello Sir, mera naam Edith hai. Jab bhi aapko mujhse baat karni ho, sabse pehle Edith bolna hoga. Uske baad main aapki baat sunungi aur aapki madad karungi.'
   : 'Aapke phone mein kya problem hai? Aap mujhe bataiye, main use fix karne ki koshish karti hoon.'
 );

 await restartListening(100);
}

export async function stopAssistant(){
 active=false;
 wakeArmed=false;
 processing=false;
 listening=false;
 queue=[];
 confirmationResolver=null;

 clearTimeout(restartTimer);
 clearTimeout(partialTimer);

 await stopListening();
 stopSpeaking();

 unsub?.();
 unsub=null;

 await AssistantBridge?.stopAssistant?.();
}

export const isAssistantActive=()=>active;
