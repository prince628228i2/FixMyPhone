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
let currentTtsText='';
let pendingUtterances=[];
let pendingTask=null;

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

function isEcho(text){
 const a=normalize(text);
 const b=normalize(currentTtsText);

 if(!a||!b)return false;
 if(a===b)return true;
 if(a.length>=8 && b.includes(a))return true;
 if(b.length>=8 && a.includes(b))return true;

 return false;
}

async function ensureListening(){
  // Native SpeechToTextModule owns continuous listening/recovery.
  // JS must never restart the recognizer after end/error.
  return;
}

async function startListeningImmediately(){
 if(!active)return;
 progress('STT: CONTINUOUS_NATIVE_OWNER');
}

async function say(text){
 if(!text||!active)return;

 clearTimeout(acknowledgementTimer);

 currentTtsText=String(text);
 speaking=true;

 console.log('[FixMyPhone][TTS]',text);
 progress('TTS: '+text);

 /*
  * IMPORTANT:
  * STT is deliberately started BEFORE waiting for TTS.
  * Therefore microphone recognition and TTS run together.
  */
 await startListeningImmediately();

 try{
  await speak(text);
 }catch(e){
  console.warn('[FixMyPhone][TTS] failed:',String(e?.message||e));
  progress('TTS ERROR: '+String(e?.message||e));
 }

 speaking=false;

 /*
  * Keep echo information briefly so the recognizer does not
  * immediately feed TTS output back into the agent.
  */
 setTimeout(()=>{
  if(!speaking)currentTtsText='';
 },700);

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

function queueUtterance(text){
 const key=normalize(text);
 if(!key)return;

 if(pendingUtterances.some(x=>normalize(x)===key))return;

 pendingUtterances.push(String(text).trim());

 /*
  * Keep the queue bounded. The latest meaningful command is
  * more useful than an old repeated recognition result.
  */
 if(pendingUtterances.length>3){
  pendingUtterances.shift();
 }

 progress('INPUT QUEUED: '+text);
}

async function processQueued(){
 if(!active||processing||confirmationResolver)return;

 const next=pendingUtterances.shift();

 if(next){
  await processUtterance(next);
 }
}

async function processUtterance(text){
 const answer=String(text||'').trim();
 if(!answer||!active)return;
 const baseGoal=pendingTask?.originalGoal||answer;
 const goal=pendingTask ? baseGoal+'\nUser answered: '+answer : answer;
 const key=normalize(answer);
 if(!goal||!active)return;


 if(!key)return;

 if(key===lastSubmitted)return;

 if(isEcho(answer)){
  progress('STT: ECHO_IGNORED '+goal);
  return;
 }

 if(confirmationResolver){
  if(isYes(answer)){
   const r=confirmationResolver;
   confirmationResolver=null;
   r(true);
   return;
  }

  if(isNo(answer)){
   const r=confirmationResolver;
   confirmationResolver=null;
   r(false);
   return;
  }
 }

 if(processing){
  queueUtterance(answer);
  return;
 }

 if(!pendingTask && isSmallTalk(answer)){
  await say('Ji Sir, main sun rahi hoon.');
  return;
 }

 lastSubmitted=key;
 processing=true;

 console.log('[FixMyPhone][PROGRESS] INPUT: '+goal);
 progress('INPUT: '+goal);

 ConversationContext.set({lastUserUtterance:answer,pendingTask:pendingTask?.originalGoal||null});

 useAgentStore.getState().addTranscript({
  role:'user',
  text:goal,
  at:Date.now()
 });

 try{
  progress('ACKNOWLEDGEMENT');
  await say(await acknowledge());

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

  progress('AGENT RESULT: '+JSON.stringify(result));

  if(result.status==='success'){
   pendingTask=null;
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

  if(result.status==='needs_input'){
   if(!pendingTask){ pendingTask={originalGoal:answer,createdAt:Date.now()}; progress('PENDING TASK SAVED: '+answer); }
   if(!result.assistantReply){ await say('Sir, ek chhoti si information chahiye.'); }
  }

  if(result.status==='failed'){
   pendingTask=null;
   await say('Sir, main screen dobara check karke try karti hoon.');
  }

 }catch(e){
  console.warn('[FixMyPhone][AGENT] error:',String(e?.message||e));

  progress('AGENT ERROR: '+String(e?.message||e));

  await say('Sir, ek problem aa gayi. Main dobara try karti hoon.');

 }finally{
  processing=false;
  confirmationResolver=null;
  lastSubmitted='';

  if(active){
await processQueued();
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
  currentTtsText='';
 lastSubmitted='';
 lastPartial='';
 confirmationResolver=null;
 pendingUtterances=[];
 pendingTask=null;

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

  progress('STT EVENT: '+JSON.stringify(e));

  if(e.event==='start'){
   listening=true;
   progress('STT: LISTENING');
   return;
  }

  if(e.event==='end'){
   listening=false;

   if(active){
}

   return;
  }

  if(e.event==='error'){
   listening=false;

   progress(
    'STT ERROR: '+String(e.text||'unknown')
   );

   return;
  }

  if(e.event==='partial'&&e.text){
   const raw=String(e.text).trim();
   const normalized=normalize(raw);

   if(!normalized||normalized===lastPartial)return;

   lastPartial=normalized;

   /*
    * Do not submit TTS echo as user speech.
    * Recognition itself remains active.
    */
   if(isEcho(raw)){
    return;
   }

   if(mode==='24x7'&&wakeArmed){
    const command=extractWakeCommand(raw);

    if(command!==null){
     clearTimeout(acknowledgementTimer);

     acknowledgementTimer=setTimeout(()=>{
      if(active){
       if(command)processUtterance(command);
       else say('Ji Sir, boliye.');
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

   if(isEcho(raw)){
    progress('STT: FINAL_ECHO_IGNORED');
    return;
   }

   /*
    * Confirmation always gets priority.
    */
   if(confirmationResolver){
    await processUtterance(raw);
    return;
   }

   /*
    * 24x7 mode requires Edith.
    */
   if(mode==='24x7'&&wakeArmed){
    const command=extractWakeCommand(raw);

    if(command===null){
     lastSubmitted='';
     return;
    }

    if(!command){
     await say('Ji Sir, boliye.');
     return;
    }

    if(processing){
     queueUtterance(command);
    }else{
     await processUtterance(command);
    }

    return;
   }

   /*
    * Fix mode accepts every final utterance.
    */
   if(processing){
    queueUtterance(raw);
   }else{
    await processUtterance(raw);
   }
  }
 });

 /*
  * App immediately goes to Home screen.
  */
 await AssistantBridge?.minimizeApp?.();

 /*
  * TTS and STT now start together inside say().
  */
 await say(greeting());

 await say(
  mode==='24x7'
   ? 'Hello Sir, mera naam Edith hai. Jab bhi aapko mujhse baat karni ho, sabse pehle Edith bolna hoga. Uske baad main aapki baat sunungi aur aapki madad karungi.'
   : 'Aapke phone mein kya problem hai? Aap mujhe bataiye, main use fix karne ki koshish karti hoon.'
 );

}

export async function stopAssistant(){
 active=false;

 console.log('[FixMyPhone][PROGRESS] ASSISTANT STOPPED');
 progress('ASSISTANT STOPPED');

 wakeArmed=false;
 processing=false;
 listening=false;
 speaking=false;
  currentTtsText='';
 confirmationResolver=null;
 pendingUtterances=[];
 pendingTask=null;

 clearTimeout(restartTimer);
 clearTimeout(acknowledgementTimer);

 try{
  await stopListening();
 }catch(e){}

 try{
  stopSpeaking();
 }catch(e){}

 unsub?.();
 unsub=null;

 try{
  await AssistantBridge?.stopAssistant?.();
 }catch(e){}
}

export const isAssistantActive=()=>active;
