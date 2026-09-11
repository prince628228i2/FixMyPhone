function extract(text){const a=String(text).replace(/```json|```/g,'').trim(); const s=a.indexOf('{'),e=a.lastIndexOf('}'); return JSON.parse(a.slice(s,e+1));}
export class OpenAIProvider {
 constructor(key,model='gpt-4o-mini'){this.key=key;this.model=model}
 async plan({goal,snapshot,conversation,actions}) {
  if(!this.key) throw new Error('OpenAI API key is not configured');
  const system='Return JSON only. You are a female-voice phone assistant. Solve with the fewest necessary actions. Infer obvious details. Ask ONLY a necessary question when execution genuinely cannot continue. If needed, actions=[] and needsUserInput=true. Use only supplied actions. CALL always requires confirmation. Keep assistantReply short Hindi/Hinglish.';
  const user=`Goal: ${goal}\nActions: ${JSON.stringify(actions)}\nScreen: ${JSON.stringify(snapshot).slice(0,12000)}\nConversation: ${JSON.stringify(conversation||{}).slice(0,5000)}\nSchema: {"type":"ACTION_PLAN","goal":string,"assistantReply":string,"needsUserInput":boolean,"actions":[{"id":string,"action":string,"params":object,"requiresConfirmation":boolean}]}`;
  const r=await fetch('https://api.openai.com/v1/chat/completions',{method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${this.key}`},body:JSON.stringify({model:this.model,messages:[{role:'system',content:system},{role:'user',content:user}],temperature:0,response_format:{type:'json_object'}})});
  if(!r.ok) throw new Error(`OpenAI HTTP ${r.status}`); const j=await r.json(); return extract(j.choices?.[0]?.message?.content||'{}');
 }
}
