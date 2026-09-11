import { NativeModules } from 'react-native';

const { ActionDispatcherNative } = NativeModules;

/**
 * Thin wrapper around the native ActionDispatcherNative module (see
 * android/.../executor/ActionDispatcherModule.kt). Mirrors the pattern in
 * AccessibilityBridge.js: this is the only file that touches
 * NativeModules.ActionDispatcherNative directly.
 *
 * @param {import('../actions/ActionSchema').FixMyPhoneAction} action
 * @returns {Promise<{status: 'executed'|'failed'|'requires_user', reason?: string}>}
 */
export async function dispatchNativeAction(action) {
  return ActionDispatcherNative.dispatch(action);
}
