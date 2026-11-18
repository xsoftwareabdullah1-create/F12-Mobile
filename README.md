# F12 Mobile

**A desktop-inspired web inspection browser for Android.**

F12 Mobile brings a compact browser and developer inspection workspace to Android phones. It is built with native Java, XML layouts, and Android WebView, with a dark interface designed for touch screens.

> Project status: **early foundation / MVP**. The current build is a starting point, not a full replacement for desktop Chrome DevTools.

## Current implementation

- [x] Android application project (Java + XML)
- [x] Embedded WebView browser with address navigation
- [x] Dark, desktop-inspired inspector layout
- [x] DOM/HTML snapshot panel (limited to the first 14,000 characters)
- [x] JavaScript console message capture
- [x] Basic Fetch and XMLHttpRequest request-event logging
- [x] Navigation history via Android Back
- [x] Basic redaction of common sensitive query parameter values
- [ ] Editable DOM tree and live CSS rule editor
- [ ] Request detail drawer (headers, payload, response preview)
- [ ] Breakpoints and JavaScript stepping debugger
- [ ] Device emulation and responsive viewport controls
- [ ] Storage/cookie inspector
- [ ] Performance timeline and accessibility tree
- [ ] Exportable HAR/session reports

## Build

### Requirements

- JDK 17
- Android SDK Platform 35
- Android Build Tools compatible with SDK 35
- Gradle 8.9

### Build a debug APK

From the repository root:

```bash
gradle assembleDebug
```

APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```

The GitHub Actions workflow in `.github/workflows/android.yml` builds the debug APK and uploads it as a workflow artifact.

## Compatibility

- Minimum Android: **8.1 (API 27)**
- Compile SDK: 35
- Target SDK: 35
- Language: Java 11 source compatibility (JDK 17 build runtime)

## Network inspector scope and privacy

The current Network panel records basic Fetch/XHR completion events from pages loaded inside this app. It does **not** capture every browser/network stack event, raw request or response bodies, or traffic from other apps.

OTP values, passwords, authorization tokens, and other secrets are not an intended inspection output. Common sensitive query parameter values are redacted in the current event logger. This is best-effort masking, not a complete secret-detection system; do not use the app to collect other people's credentials or private messages. Use test accounts and systems you are authorized to debug.

The JavaScript bridge is deliberately logging-only and exposes no SMS, contacts, account, filesystem, or device-control API. WebView is not a security boundary for untrusted websites; keep the app updated and use trusted sites while testing.

## Repository layout

```
.
├── app/
│   └── src/main/
│       ├── java/com/f12mobile/MainActivity.java
│       ├── res/layout/activity_main.xml
│       ├── res/values/styles.xml
│       └── AndroidManifest.xml
├── .github/workflows/android.yml
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Contributing

Issues and pull requests are welcome. Please include:
- Android version and device model
- Steps to reproduce
- Expected and actual behavior
- Relevant build or Logcat output (remove personal data and secrets)

Keep changes focused and avoid committing API keys, cookies, OTPs, access tokens, or personal data.

## License

This repository is distributed under the **F12 Mobile Community License 1.0 (F12CL-1.0)**. Read [LICENSE](LICENSE) before using, modifying, or redistributing this project. It is a project-specific license and is not an OSI-approved open-source license.
