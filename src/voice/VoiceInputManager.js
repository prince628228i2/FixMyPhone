import {NativeModules,NativeEventEmitter} from 'react-native';

const Native=NativeModules.SpeechToText;
let emitter;

export async function startListening(locale='hi-IN'){
 if(!Native?.startListening){
  throw new Error('Speech recognition unavailable');
 }
 return Native.startListening(locale);
}

export const stopListening=()=>Native?.stopListening?.();

export function subscribeVoiceEvents(cb){
 if(!Native)return()=>{};
 emitter ||= new NativeEventEmitter(Native);
 const sub=emitter.addListener('onSpeechEvent',cb);
 return()=>sub.remove();
}
