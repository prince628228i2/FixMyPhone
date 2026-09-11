import {NativeModules,NativeEventEmitter} from 'react-native';

const Native=NativeModules.TextToSpeech;
let emitter;

export const subscribeTtsEvents=(cb)=>{
 if(!Native)return()=>{};
 emitter ||= new NativeEventEmitter(Native);
 const sub=emitter.addListener('onTtsEvent',cb);
 return()=>sub.remove();
};

export const speak=(text,options={})=>{
 if(!Native?.speak)return Promise.resolve();
 emitter ||= new NativeEventEmitter(Native);
 return new Promise(resolve=>{
  let finished=false;
  const done=()=>{
   if(finished)return;
   finished=true;
   sub.remove();
   resolve();
  };
  const sub=emitter.addListener('onTtsEvent',e=>{
   if(e.event==='done'||e.event==='error')done();
  });
  try{
   const result=Native.speak(String(text),options);
   result?.catch?.(done);
  }catch(e){done();}
 });
};

export const stopSpeaking=()=>Native?.stop?.();
export const waitForSpeech=(ms=100)=>new Promise(r=>setTimeout(r,ms));
