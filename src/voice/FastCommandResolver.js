import { ActionType } from '../actions/ActionSchema';
export function resolveFastCommand(input) {
  const t = String(input || '').trim().toLowerCase();
  if (!t) return null;
  if (/^(go )?back$/.test(t)) return action(ActionType.BACK);
  if (/^(go )?home$/.test(t)) return action(ActionType.HOME);
  if (/^(show )?recent(s)?$/.test(t)) return action(ActionType.RECENTS);
  if (/^(turn )?speaker(phone)? on$/.test(t)) return action(ActionType.SPEAKER_ON);
  if (/^(turn )?speaker(phone)? off$/.test(t)) return action(ActionType.SPEAKER_OFF);
  const m = t.match(/^open (.+)$/); if (m) return action(ActionType.OPEN_APP,{appName:m[1]});
  const c = t.match(/^(call|phone|dial) (.+)$/); if (c) return action(ActionType.RESOLVE_CONTACT,{name:c[2]});
  const wifi = t.match(/^(turn )?wi-?fi (on|off)$/); if (wifi) return action(ActionType.TOGGLE_WIFI,{state:wifi[2]});
  const vol = t.match(/^(set )?volume (0|[1-9]|[1-9][0-9]|100)$/); if (vol) return action(ActionType.SET_VOLUME,{level:Number(vol[2])});
  return null;
}
function action(name, params={}) { return {id:`fast-${Date.now()}`, action:name, params, requiresConfirmation:name===ActionType.CALL}; }
export function resolveFastCommandPlan(input) {
  const a=resolveFastCommand(input); return a ? {type:'ACTION_PLAN',goal:input,actions:[a]} : null;
}
