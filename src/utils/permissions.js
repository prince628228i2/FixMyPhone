import {PermissionsAndroid,Platform} from 'react-native';

export async function requestMicrophonePermission(){
  if(Platform.OS!=='android') return true;
  const p=PermissionsAndroid.PERMISSIONS.RECORD_AUDIO;
  const current=await PermissionsAndroid.check(p);
  if(current) return true;
  const r=await PermissionsAndroid.request(p,{
    title:'Microphone permission',
    message:'Fix My Phone needs microphone access so Edith can hear your voice and help you.',
    buttonPositive:'Allow',
    buttonNegative:'Deny',
  });
  return r===PermissionsAndroid.RESULTS.GRANTED;
}

export async function requestCorePermissions(){
  if(Platform.OS!=='android')return true;
  const p=[PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,PermissionsAndroid.PERMISSIONS.READ_CONTACTS,PermissionsAndroid.PERMISSIONS.CALL_PHONE];
  const r=await PermissionsAndroid.requestMultiple(p);
  return Object.values(r).every(x=>x===PermissionsAndroid.RESULTS.GRANTED);
}
