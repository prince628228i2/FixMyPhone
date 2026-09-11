import {ActionParamSchema,ALWAYS_CONFIRM,isKnownAction} from './ActionSchema';
export function validateAction(a){
 if(!a||typeof a!=='object') return {valid:false,reason:'invalid_action'};
 if(!isKnownAction(a.action)) return {valid:false,reason:`unknown_action:${a.action}`};
 for(const k of ActionParamSchema[a.action]||[]) if(a.params?.[k]===undefined||a.params?.[k]===null) return {valid:false,reason:`missing_param:${k}`};
 if(ALWAYS_CONFIRM.has(a.action)&&a.requiresConfirmation!==true) return {valid:false,reason:`confirmation_required:${a.action}`};
 return {valid:true};
}
export function validatePlan(p){
 if(!p||p.type!=='ACTION_PLAN'||!Array.isArray(p.actions)) return {valid:false,reason:'invalid_plan'};
 if(p.actions.length>25) return {valid:false,reason:'too_many_actions'};
 for(const a of p.actions){const v=validateAction(a);if(!v.valid)return v}
 return {valid:true};
}
