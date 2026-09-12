import {createActionPlan} from '../ai/AIProvider';
import {ActionType} from '../actions/ActionSchema';

export async function planTask(goal,snapshot,conversation){
  return createActionPlan({
    goal,
    snapshot,
    conversation,
    actions:Object.values(ActionType)
  });
}
