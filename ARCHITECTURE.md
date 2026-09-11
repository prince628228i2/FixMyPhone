# FIX MY PHONE — Architecture (Part 1)

## Final Architecture

```
REACT NATIVE (JS/JSX): Screens, Voice UI, Agent Orchestrator, Context Store
        |  Native Modules (Bridge)
ANDROID NATIVE (Kotlin): AccessibilityService, ForegroundService, Telephony,
                          Voice, Contacts, Media/Audio, Notifications, Device
```

Flow:
```
Voice Input (STT)
  -> Intent Resolver (JS)
       -> Fast Path (regex/rules) -> Action Executor (native)
       -> (ambiguous) AI Agent Engine -> AIProvider -> Action Plan (JSON)
            -> Action Validator (schema check)
            -> Action Executor (native bridge) -> AccessibilityService / Android APIs
            -> Observer (accessibility events / state polling)
            -> Verifier -> Success? -> Complete
                        -> No -> Re-plan (back to AI Agent Engine)
```

Phone Fix and 24x7 CMD share this same engine; they differ only in the initial
goal framing given to the agent.

## Directory Tree
See project root for the live structure. Native modules live under
`android/app/src/main/java/com/fixmyphone/{accessibility,agent,executor,
voice,service,device,safety}`. JS side lives under `src/{components,screens,
ai,agent,voice,context,actions,services,utils}`.

## Permissions (see AndroidManifest.xml)
BIND_ACCESSIBILITY_SERVICE, FOREGROUND_SERVICE(+MICROPHONE), RECORD_AUDIO,
READ_CONTACTS, CALL_PHONE, READ_PHONE_STATE, MODIFY_AUDIO_SETTINGS,
BLUETOOTH_CONNECT, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, CHANGE_WIFI_STATE,
POST_NOTIFICATIONS, app launcher discovery via a limited MAIN/LAUNCHER <queries> declaration.

## AI Action Schema
Canonical enum + required-param map, mirrored 1:1 in:
- `src/actions/ActionSchema.js`
- `android/app/src/main/java/com/fixmyphone/executor/ActionSchema.kt`

Every AI-emitted action must exist in this enum. Nothing outside it is ever
executed.

## Agent State Machine
`IDLE -> PLANNING -> EXECUTING -> OBSERVING -> VERIFYING -> (SUCCESS |
REPLANNING | FAILED | AWAITING_USER) -> IDLE`

Guardrails: maxActions (25), maxRetries (3), timeoutMs (60000), stopRequested
checked before every action dispatch.

## Build Strategy (GitHub Actions)
`.github/workflows/android-build.yml` — checkout, setup Node + Java 17 +
Android SDK, cache Gradle/node_modules, npm ci, inject signing config from
GitHub Secrets, `./gradlew bundleRelease`, upload AAB artifact. AI provider
API keys are never baked into the build — entered at runtime, stored via
EncryptedSharedPreferences.

## Build Progress Log
- [x] Part 1 — architecture & planning (this file)
- [x] Part 2 — project foundation (package.json, gradle, manifest, App.jsx, ActionSchema)
- [x] Part 3 — AccessibilityService (native UI inspection/control)
- [x] Part 4 — Action Validator + Action Dispatcher
- [x] Part 5 — Agent Engine (state machine, Planner, Observer, Verifier)
- [x] Part 6 — Voice (STT/TTS native modules) + FastCommandResolver
- [x] Part 7 — Device modules (Telephony, Contacts, Connectivity, Media, Volume, AppLauncher)
- [x] Part 8 — AI Provider abstraction (Gemini/OpenAI) + Safety/EmergencyStop
- [x] Part 9 — UI screens (Home, Phone Fix, CMD mode, Settings) + GitHub Actions workflow
- [x] Part 10 — Play Store readiness (privacy policy text, Data Safety mapping, release checklist)
