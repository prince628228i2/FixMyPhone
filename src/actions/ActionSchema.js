/**
 * Canonical action schema shared conceptually with the Kotlin-side enum in
 * android/app/src/main/java/com/fixmyphone/executor/ActionSchema.kt
 *
 * The AI (and the FastCommandResolver) must only ever emit actions from this
 * set. ActionValidator rejects anything else before it reaches the native
 * bridge — the model's output is never executed as free-form code.
 */

export const ActionType = Object.freeze({
  RESOLVE_CONTACT: 'RESOLVE_CONTACT',
  CALL: 'CALL',
  SPEAKER_ON: 'SPEAKER_ON',
  SPEAKER_OFF: 'SPEAKER_OFF',
  OPEN_APP: 'OPEN_APP',
  TAP: 'TAP',
  LONG_PRESS: 'LONG_PRESS',
  TYPE_TEXT: 'TYPE_TEXT',
  CLEAR_TEXT: 'CLEAR_TEXT',
  SWIPE: 'SWIPE',
  SCROLL: 'SCROLL',
  SET_VOLUME: 'SET_VOLUME',
  TOGGLE_WIFI: 'TOGGLE_WIFI',
  TOGGLE_BLUETOOTH: 'TOGGLE_BLUETOOTH',
  TOGGLE_AIRPLANE_MODE: 'TOGGLE_AIRPLANE_MODE',
  MEDIA_PLAY: 'MEDIA_PLAY',
  MEDIA_PAUSE: 'MEDIA_PAUSE',
  MEDIA_SEEK: 'MEDIA_SEEK',
  BACK: 'BACK',
  HOME: 'HOME',
  RECENTS: 'RECENTS',
  ASK_USER: 'ASK_USER',
  WAIT_FOR_STATE: 'WAIT_FOR_STATE',
});

// Per-action required param keys, used by ActionValidator.
export const ActionParamSchema = {
  [ActionType.RESOLVE_CONTACT]: ['name'],
  [ActionType.CALL]: ['contactRef'],
  [ActionType.SPEAKER_ON]: [],
  [ActionType.SPEAKER_OFF]: [],
  [ActionType.OPEN_APP]: ['appName'],
  [ActionType.TAP]: ['target'],
  [ActionType.LONG_PRESS]: ['target'],
  [ActionType.TYPE_TEXT]: ['target', 'text'],
  [ActionType.CLEAR_TEXT]: ['target'],
  [ActionType.SWIPE]: ['direction'],
  [ActionType.SCROLL]: ['direction'],
  [ActionType.SET_VOLUME]: ['level'],
  [ActionType.TOGGLE_WIFI]: ['state'],
  [ActionType.TOGGLE_BLUETOOTH]: ['state'],
  [ActionType.TOGGLE_AIRPLANE_MODE]: ['state'],
  [ActionType.MEDIA_PLAY]: [],
  [ActionType.MEDIA_PAUSE]: [],
  [ActionType.MEDIA_SEEK]: ['positionMs'],
  [ActionType.BACK]: [],
  [ActionType.HOME]: [],
  [ActionType.RECENTS]: [],
  [ActionType.ASK_USER]: ['question'],
  [ActionType.WAIT_FOR_STATE]: ['condition', 'timeoutMs'],
};

// Actions that must never execute without explicit user confirmation,
// regardless of what the AI plan says.
export const ALWAYS_CONFIRM = new Set([
  ActionType.CALL,
]);

/**
 * @typedef {Object} FixMyPhoneAction
 * @property {string} id
 * @property {keyof typeof ActionType} action
 * @property {Object} params
 * @property {boolean} requiresConfirmation
 */

/**
 * @typedef {Object} ActionPlan
 * @property {'ACTION_PLAN'} type
 * @property {string} goal
 * @property {FixMyPhoneAction[]} actions
 */

export function isKnownAction(actionName) {
  return Object.prototype.hasOwnProperty.call(ActionParamSchema, actionName);
}
