import { NativeModules, NativeEventEmitter } from 'react-native';

const { AccessibilityBridge: NativeAccessibilityBridge } = NativeModules;

/**
 * Thin JS wrapper around the native AccessibilityBridge module (see
 * android/.../accessibility/AccessibilityBridgeModule.kt). This is the ONLY
 * file in src/ that should import NativeModules.AccessibilityBridge
 * directly — everything else (Observer, ActionDispatcher in later parts)
 * goes through this wrapper so the native contract stays in one place.
 */

export async function isAccessibilityServiceEnabled() {
  return NativeAccessibilityBridge.isServiceEnabled();
}

export function openAccessibilitySettings() {
  NativeAccessibilityBridge.openAccessibilitySettings();
}

/**
 * Returns the current UI snapshot: { available, packageName, nodeCount, nodes }
 * or { available: false, reason } if there's no active window.
 */
export async function getScreenSnapshot() {
  return NativeAccessibilityBridge.getSnapshot();
}

/**
 * Performs one schema-defined UI action (TAP, LONG_PRESS, TYPE_TEXT,
 * CLEAR_TEXT, SCROLL, SWIPE, BACK, HOME, RECENTS).
 * Returns { status: 'executed' | 'failed' | 'requires_user', reason? }.
 *
 * This does NOT validate the action against ActionSchema.js — that's
 * ActionValidator's job (Part 4), which must run before this is ever
 * called from the Agent Engine.
 */
export async function performAccessibilityAction(actionName, params = {}) {
  return NativeAccessibilityBridge.performAction(actionName, params);
}

/**
 * Subscribes to window-change events emitted from the native service.
 * Returns an unsubscribe function.
 */
export function subscribeToWindowChanges(callback) {
  const emitter = new NativeEventEmitter(NativeAccessibilityBridge);
  const subscription = emitter.addListener('onWindowChanged', callback);
  return () => subscription.remove();
}
