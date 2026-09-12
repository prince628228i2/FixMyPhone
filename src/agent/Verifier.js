export async function verify(before,after,action){
 if(action?.action==='WAIT_FOR_STATE') return true;
 if(!after?.available) return false;
 if(['BACK','HOME','RECENTS'].includes(action?.action)) return true;

 const b=JSON.stringify(before||{});
 const a=JSON.stringify(after||{});

 if(action?.action==='TOGGLE_WIFI'){
  const desired=String(action?.params?.state||'').toLowerCase();
  const on=/\b(wifi|wi-fi|internet)\b/i.test(a) && (
   /checked["']?\s*[:=]\s*true/i.test(a) ||
   /isChecked["']?\s*[:=]\s*true/i.test(a) ||
   /"state"\s*:\s*"on"/i.test(a) ||
   /enabled["']?\s*[:=]\s*true/i.test(a)
  );
  if(desired==='on' && on)return true;
  if(desired==='off' && !on)return true;
 }

 return true;
}
