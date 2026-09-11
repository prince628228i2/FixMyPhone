import {NativeModules} from 'react-native';
const N=NativeModules.ActionDispatcherNative;
export async function dispatchNative(action){if(!N) return {status:'failed',reason:'native_dispatcher_unavailable'}; return N.dispatch(action);}
