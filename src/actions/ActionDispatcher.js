import {validateAction} from './ActionValidator';
import {dispatchNative} from './ActionDispatcherNative';
export async function dispatchAction(action, {confirmed=false}={}) {
 const v=validateAction(action); if(!v.valid) return {status:'failed',reason:v.reason};
 if(action.requiresConfirmation && !confirmed) return {status:'requires_user',reason:'confirmation_required'};
 return dispatchNative(action);
}
