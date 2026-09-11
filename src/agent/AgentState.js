import {create} from 'zustand';
export const useAgentStore=create((set)=>({
 task:'',status:'IDLE',actionsDone:0,retries:0,stopRequested:false,lastError:null,transcript:[],
 set:(patch)=>set(patch),addTranscript:(x)=>set(s=>({transcript:[...s.transcript,x].slice(-100)})),
 reset:()=>set({task:'',status:'IDLE',actionsDone:0,retries:0,stopRequested:false,lastError:null,transcript:[]}),
 stop:()=>set({stopRequested:true,status:'STOPPING'})
}));
