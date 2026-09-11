import {NativeModules} from 'react-native';
import {startListening,stopListening,subscribeVoiceEvents} from './VoiceInputManager';
import {speak,stopSpeaking} from './TextToSpeech';
import {runAgent} from '../agent/AgentEngine';
import {useAgentStore} from '../agent/AgentState';
import {ConversationContext} from './ConversationContext';
import {requestMicrophonePermission} from '../utils/permissions';

const {AssistantBridge,ProgressLogger}=NativeModules;

const progress=(message)=>{
 try{ProgressLogger?.log?.(String(message));}catch(e){}
};

let active=false;
let mode='fix';
let processing=false;
let listening=false;
let unsub=null;
let restartTimer=null;
let acknowledgementTimer=null;
let confirmationResolver=null;
let lastSubmitted='';
let lastPartial='';
let wakeArmed=false;
let speaking=false;
let restarting=false;

const WAKE_WORD=/\bedith\b/i;

function normalize(text){
 return String(text||'').trim().replace(/\s+/g,' ').toLowerCase();
}

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

async function ensureListening(delay=100){
 if(!active||speaking||listening||restarting)return;

 clearTimeout(restartTimer);
 restartTimer=setTimeout(async()=>{
  if(!active||speaking||listening||restarting)return;

  restarting=true;
  try{
   await startListening('hi-IN');
  }catch(e){
   console.warn('[FixMyPhone][STT] restart failed:',String(e?.message||e));
  }finally{
   restarting=false;
  }
 },delay);
}

async function say(text){
 if(!text||!active)return;

 clearTimeout(acknowledgementTimer);
 speaking=true;

 console.log('[FixMyPhone][TTS]',text);
 progress('TTS: '+text);

 try{
  await speak(text);
 }catch(e){
  console.warn('[FixMyPhone][TTS] failed:',String(e?.message||e));
 progress('TTS ERROR: '+String(e?.message||e));
 }

 speaking=false;

 if(active){
  await ensureListening(100);
 }
}

async function acknowledge(){
 const replies=[
  'Thik hai Sir.',
  'Samajh gayi Sir.',
  'Haan Sir.',
  'Ji Sir, samajh gayi.'
 ];
 return replies[Math.floor(Math.random()*replies.length)];
}

async function processUtterance(text){
 const goal=String(text||'').trim();
 if(!goal||!active)return;

 const key=normalize(goal);
 if(!key||key===lastSubmitted)return;
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
 console.log('[FixMyPhone][PROGRESS] INPUT: '+goal);
 progress('INPUT: '+goal);

 ConversationContext.set({lastUserUtterance:goal});

 useAgentStore.getState().addTranscript({
  role:'user',
  text:goal,
  at:Date.now()
 });

 /*
  * User input milte hi short acknowledgement.
  * Iske baad AgentEngine immediately start hota hai.
  */
 console.log('[FixMyPhone][PROGRESS] ACKNOWLEDGEMENT');
 progress('ACKNOWLEDGEMENT');
 await say(await acknowledge());

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
  }

  if(result.status==='needs_input'&&!result.assistantReply){
   await say('Sir, ek chhoti si information chahiye.');
  }

  if(result.status==='failed'){
   await say('Sir, main screen dobara check karke try karti hoon.');
  }

 }catch(e){
  console.warn(
   '[FixMyPhone][AGENT] error:',
   String(e?.message||e)
  );

  await say('Sir, ek problem aa gayi. Main dobara try karti hoon.');

 }finally{
  processing=false;

  if(active&&!confirmationResolver){
   lastSubmitted='';
   await ensureListening(100);
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
 console.log('[FixMyPhone][PROGRESS] ASSISTANT ACTIVE mode='+nextMode);
 progress('ASSISTANT ACTIVE mode='+nextMode);
 wakeArmed=(nextMode==='24x7');
 processing=false;
 listening=false;
 speaking=false;
 restarting=false;
 lastSubmitted='';
 lastPartial='';
 confirmationResolver=null;

 clearTimeout(restartTimer);
 clearTimeout(acknowledgementTimer);

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

  console.log(
   '[FixMyPhone][STT EVENT]',
   JSON.stringify(e)
  );

  if(e.event==='start'){
   listening=true;
   restarting=false;
   return;
  }

  if(e.event==='end'){
   listening=false;

   /*
    * Android recognition session ended naturally.
    * Mic service remains active; only recognition session
    * is silently re-armed.
    */
   if(active&&!speaking){
    await ensureListening(100);
   }

   return;
  }

  if(e.event==='error'){
   listening=false;

   console.warn(
    '[FixMyPhone][STT ERROR]',
    e.text||'unknown'
   );

   if(active&&!speaking){
    await ensureListening(180);
   }

   return;
  }

  if(e.event==='partial'&&e.text){
   const raw=String(e.text).trim();
   const normalized=normalize(raw);

   if(!normalized||normalized===lastPartial)return;

   lastPartial=normalized;

   if(mode==='24x7'&&wakeArmed){
    const command=extractWakeCommand(raw);

    if(command){
     clearTimeout(acknowledgementTimer);

     acknowledgementTimer=setTimeout(()=>{
      if(active&&!processing){
       processUtterance(command);
      }
     },120);
    }
   }

   return;
  }

  if(e.event==='results'&&e.text){
   const raw=String(e.text).trim();

   if(!raw)return;

   clearTimeout(acknowledgementTimer);

   /*
    * Confirmation gets priority.
    */
   if(confirmationResolver){
    await processUtterance(raw);
    return;
   }

   /*
    * 24x7 requires Edith wake word.
    */
   if(mode==='24x7'&&wakeArmed){
    const command=extractWakeCommand(raw);

    if(command===null){
     lastSubmitted='';
     await ensureListening(80);
     return;
    }

    if(!command){
     await say('Ji Sir, boliye.');
     return;
    }

    if(!processing){
     await processUtterance(command);
    }

    return;
   }

   /*
    * Fix mode: every final utterance is a command.
    */
   if(!processing){
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

 await ensureListening(100);
}

export async function stopAssistant(){
 active=false;
 console.log('[FixMyPhone][PROGRESS] ASSISTANT STOPPED');
 progress('ASSISTANT STOPPED');
 wakeArmed=false;
 processing=false;
 listening=false;
 speaking=false;
 restarting=false;

 clearTimeout(restartTimer);
 clearTimeout(acknowledgementTimer);

 await stopListening();
 stopSpeaking();

 unsub?.();
 unsub=null;

 await AssistantBridge?.stopAssistant?.();
}

export const isAssistantActive=()=>active;
