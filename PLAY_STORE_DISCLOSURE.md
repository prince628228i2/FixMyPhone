# Play Store / Accessibility disclosure draft

Fix My Phone uses Android AccessibilityService to perform user-requested phone automation.

Core functionality requiring Accessibility:
1. Read the active window's accessibility hierarchy to identify visible controls.
2. Tap, long-press, type, clear text, scroll and swipe in response to a user command.
3. Perform Android global Back, Home and Recents actions.

The service does not independently browse the phone, run arbitrary code, or silently perform tasks. The user starts a command and can stop the agent. Actions are constrained by a fixed schema and validated before execution.

Before Play Store submission, complete the current Google Play Console Accessibility API declaration and ensure the final disclosure, privacy policy, data-safety answers and app behavior exactly match the shipped implementation.


### Voice-first operation
When the user starts Fix My Phone or 24×7 AI, the app minimizes and continues the requested voice interaction through a microphone foreground service. The user can stop the assistant from the app controls/notification or by stopping the task. Accessibility is used only to inspect and interact with the device UI for user-requested commands.
