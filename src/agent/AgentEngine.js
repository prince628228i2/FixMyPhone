console.log('[FixMyPhone][AGENT] AgentEngine loaded');
import {NativeModules} from 'react-native';
import {useAgentStore} from './AgentState';

const progress=(message)=>{
 try{NativeModules.ProgressLogger?.log?.(String(message));}catch(e){}
};
import {observe} from './Observer';
import {planTask} from './Planner';
import {validatePlan} from '../actions/ActionValidator';
import {verify} from './Verifier';
import {dispatchAction} from '../actions/ActionDispatcher';
const LIMIT=25, TIMEOUT=60000;
export async function runAgent(goal,{confirmAction,conversation,onAssistantReply}={}) {
 const store=useAgentStore.getState(); store.set({task:goal,status:'PLANNING',lastError:null,stopRequested:false}); console.log('[FixMyPhone][PROGRESS] AGENT: PLANNING: '+goal); progress('AGENT: PLANNING: '+goal);
 const started=Date.now();
 try {
  console.log('[FixMyPhone][PROGRESS] AGENT: OBSERVING'); progress('AGENT: OBSERVING'); let snap=await observe(); console.log('[FixMyPhone][PROGRESS] AGENT: PLANNING'); progress('AGENT: PLANNING'); let plan=await planTask(goal,snap,conversation);
progress('DIAG: SCREEN_SNAPSHOT_BEFORE '+JSON.stringify(snap).slice(0,12000));
progress('DIAG: AI_PLAN '+JSON.stringify(plan).slice(0,12000));
const pv=validatePlan(plan);
if(!pv.valid) throw new Error('Invalid AI plan: '+pv.reason);
console.log('[FixMyPhone][PROGRESS] AGENT: PLAN_READY actions='+(plan?.actions?.length||0));
progress('AGENT: PLAN_READY actions='+(plan?.actions?.length||0));
  if (plan?.assistantReply && onAssistantReply) await onAssistantReply(plan.assistantReply);
  if (plan?.needsUserInput) { store.set({status:'AWAITING_USER'}); return {status:'needs_input',assistantReply:plan.assistantReply}; }
  for(const action of plan.actions||[]){
   const s=useAgentStore.getState(); if(s.stopRequested) return {status:'stopped'};
   if(Date.now()-started>TIMEOUT) throw new Error('Task timeout');
   if(s.actionsDone>=LIMIT) throw new Error('Action limit reached');
   if(action.requiresConfirmation){
    store.set({status:'AWAITING_USER'});
    const ok=confirmAction?await confirmAction(action):false;
    if(!ok) return {status:'cancelled'};
   }
   store.set({status:'EXECUTING'}); console.log('[FixMyPhone][PROGRESS] ACTION: '+JSON.stringify(action));
progress('DIAG: ACTION_START '+JSON.stringify(action));
progress('ACTION: '+JSON.stringify(action));
   const before=snap;
   const result=await dispatchAction(action,{confirmed:true});
   console.log('[FixMyPhone][PROGRESS] ACTION RESULT: '+JSON.stringify(result));
progress('DIAG: ACTION_RESULT '+JSON.stringify(result));
progress('ACTION RESULT: '+JSON.stringify(result));
if(result.status!=='executed') throw new Error(result.reason||'Action failed');
   store.set({actionsDone:useAgentStore.getState().actionsDone+1});
   snap=await observe();
store.set({status:'VERIFYING'});
progress('DIAG: SCREEN_SNAPSHOT_AFTER '+JSON.stringify(snap).slice(0,12000));
progress('AGENT: VERIFYING');
const verified=await verify(before,snap,action);
progress('DIAG: VERIFY_RESULT action='+action.action+' verified='+verified);
if(!verified) throw new Error('Verification failed for '+action.action);
  }
  store.set({status:'SUCCESS'}); console.log('[FixMyPhone][PROGRESS] AGENT: SUCCESS'); progress('AGENT: SUCCESS'); return {status:'success',assistantReply:plan?.assistantReply};
 } catch(e){console.warn('[FixMyPhone][PROGRESS] AGENT: FAILED '+String(e?.message||e)); progress('AGENT: FAILED '+String(e?.message||e)); store.set({status:'FAILED',lastError:String(e?.message||e)});return {status:'failed',error:String(e?.message||e)}}
}
