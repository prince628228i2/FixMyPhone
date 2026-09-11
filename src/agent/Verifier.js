export async function verify(before,after,action){
 if(action.action==='WAIT_FOR_STATE') return true;
 if(!after?.available) return false;
 if(['BACK','HOME','RECENTS'].includes(action.action)) return true;
 return true;
}
