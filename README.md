# Fix My Phone

A React Native + Kotlin Android assistant that can inspect the active UI through an Accessibility Service and execute a constrained action schema.

## Current build
- React Native 0.73.6 / Android / Kotlin
- Accessibility inspection and gestures
- Native action validation + dispatcher
- Agent loop with limits and stop flag
- Voice input / TTS
- Gemini / OpenAI provider abstraction
- Encrypted runtime API-key storage
- Device actions: calls, speakerphone, volume, app launch, media, settings fallbacks
- GitHub Actions release workflow
- Play Store disclosure and release checklist

## Local build
```bash
npm install
npx react-native start
npx react-native run-android
```
For a release build:
```bash
cd android && ./gradlew assembleRelease
```

## AI setup
Open Settings in the app (when integrated into your navigation shell) and enter a Gemini or OpenAI key. Keys are stored with Android encrypted preferences and are not embedded in the APK.

## Accessibility
The service must be enabled manually in Android Accessibility settings. It is used only to inspect and interact with the active UI for user-requested automation.

## Important Android limitations
Modern Android restricts background/third-party toggling of Wi-Fi, Bluetooth and airplane mode. Those actions therefore open the relevant Settings screen or return `requires_user` rather than pretending the toggle succeeded.

## Safety
CALL is an ALWAYS_CONFIRM action. All actions are validated in JavaScript and Kotlin, and a native safety guard can stop dispatch.


## Voice-first modes

The Home screen intentionally exposes two primary choices: **Fix My Phone** and **24×7 AI**. Starting either mode launches the microphone foreground service, speaks a time-aware greeting in the configured female-preferred TTS voice, minimizes the app, and keeps the voice loop available.

During a task, speech recognition is restarted after each user utterance. New utterances are queued while Accessibility actions run, so the user can talk naturally without interrupting the current action. The AI is instructed to ask only questions that are genuinely required to continue. Sensitive actions such as calls require spoken confirmation.

Android still controls background microphone and battery behavior; the app uses a microphone foreground service and persistent notification rather than claiming unrestricted background execution.

## 24×7 AI / Edith wake-word behavior

- Tapping **24×7 AI** minimizes the app and starts the assistant session.
- The female TTS introduction identifies the assistant as **Edith** and tells the user to say “Edith” before speaking to her.
- In 24×7 mode, recognized speech is locally gated by the wake-word check in the app session: utterances without “Edith” are ignored by the AI/action pipeline.
- Saying “Edith” alone activates a short acknowledgement; saying “Edith, <command>” sends only the command portion to the AI/action pipeline.
- Fix My Phone mode remains immediately conversational after its initial problem prompt.
- Sensitive actions continue to use the confirmation/safety guardrails.

> Android still controls microphone/background-service indicators and restrictions. The wake-word gate in this build prevents non-wake-word utterances from being sent into the AI/action pipeline; it is not a claim that Android provides a hardware-level private wake-word detector.
