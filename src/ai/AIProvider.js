import { getProvider } from './ProviderFactory';

const progress=(message)=>{
 try{const {NativeModules}=require('react-native');NativeModules.ProgressLogger?.log?.(String(message));}catch(e){}
};

export async function createActionPlan(input){
 progress('DIAG: AI_PROVIDER_REQUEST goal='+JSON.stringify(input?.goal));
 try{
  const p=await getProvider();
  progress('DIAG: AI_PROVIDER_READY '+String(p?.constructor?.name||'unknown'));
  const result=await p.plan(input);
  progress('DIAG: AI_PROVIDER_RESPONSE '+JSON.stringify(result).slice(0,16000));
  return result;
 }catch(e){
  progress('DIAG: AI_PROVIDER_FAILED '+String(e?.message||e));
  throw e;
 }
}
