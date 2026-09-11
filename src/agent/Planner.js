import {createActionPlan} from '../ai/AIProvider';
import {resolveFastCommandPlan} from '../voice/FastCommandResolver';
import {ActionType} from '../actions/ActionSchema';
export async function planTask(goal,snapshot,conversation){
 const fast=resolveFastCommandPlan(goal); if(fast)return fast;
 return createActionPlan({goal,snapshot,conversation,actions:Object.values(ActionType)});
}
