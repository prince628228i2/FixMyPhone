import {useAgentStore} from './AgentState';
import {observe} from './Observer';
import {planTask} from './Planner';
import {dispatchAction} from '../actions/ActionDispatcher';
const LIMIT=25, TIMEOUT=60000;
export async function runAgent(goal,{confirmAction,conversation,onAssistantReply}={}) {
 const store=useAgentStore.getState(); store.set({task:goal,status:'PLANNING',lastError:null,stopRequested:false});
 const started=Date.now();
 try {
  let snap=await observe(), plan=await planTask(goal,snap,conversation);
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
   store.set({status:'EXECUTING'});
   const before=snap;
   const result=await dispatchAction(action,{confirmed:true});
   if(result.status!=='executed') throw new Error(result.reason||'Action failed');
   store.set({actionsDone:useAgentStore.getState().actionsDone+1});
   snap=await observe(); store.set({status:'VERIFYING'});
  }
  store.set({status:'SUCCESS'}); return {status:'success',assistantReply:plan?.assistantReply};
 } catch(e){store.set({status:'FAILED',lastError:String(e?.message||e)});return {status:'failed',error:String(e?.message||e)}}
}
