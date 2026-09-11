import {NativeModules, NativeEventEmitter} from 'react-native';
const Native=NativeModules.TextToSpeech;
let emitter;
export const speak=(text,options={})=>{
 if(!Native?.speak) return Promise.resolve();
 if(!emitter) emitter=new NativeEventEmitter(Native);
 return new Promise(resolve=>{
   const sub=emitter.addListener('onTtsEvent',e=>{if(e.event==='done'||e.event==='error'){sub.remove();resolve();}});
   Native.speak(String(text),options).catch?.(()=>{sub.remove();resolve();});
 });
};
export const stopSpeaking=()=>Native?.stop?.();
export const waitForSpeech=(ms=100)=>new Promise(r=>setTimeout(r,ms));
