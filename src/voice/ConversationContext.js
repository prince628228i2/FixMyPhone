let context = {activeApp:null, activeCall:null, lastAction:null};
export const ConversationContext = {
  get:()=>({...context}),
  set:(patch)=>{context={...context,...patch}; return context;},
  clear:()=>{context={activeApp:null,activeCall:null,lastAction:null};},
};
