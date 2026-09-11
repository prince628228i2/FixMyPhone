# Release checklist

- [ ] Replace support/privacy contact placeholders.
- [ ] Create a unique Android signing keystore and store credentials only in GitHub Secrets.
- [ ] Set final applicationId, versionCode and versionName.
- [ ] Set target/compile SDK to the current Play-required levels before release.
- [ ] Test Android 10–current supported versions.
- [ ] Test Accessibility disclosure and permission flow.
- [ ] Test CALL confirmation and emergency stop.
- [ ] Test voice permission denial and offline/unavailable recognition.
- [ ] Test AI provider errors, malformed JSON and rate limits.
- [ ] Verify no API key is present in source, logs, APK/AAB resources or Git history.
- [ ] Complete Play Console Data Safety and Accessibility API declaration.
- [ ] Upload AAB to internal testing first.
- [ ] Verify privacy policy URL and store listing.
