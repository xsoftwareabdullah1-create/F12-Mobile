# F12 Mobile

<p align="center">
  <img src="assets/f12-mobile-logo.svg" alt="F12 Mobile logo" width="104" />
</p>

<h1 align="center">F12 Mobile</h1>
<p align="center"><strong>A lightweight, touch-friendly web inspection workspace for Android.</strong></p>
<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-315EAA?style=flat-square" alt="Android" />
  <img src="https://img.shields.io/badge/Language-Java-orange?style=flat-square" alt="Java" />
  <img src="https://img.shields.io/badge/UI-XML-6B8FCB?style=flat-square" alt="XML" />
  <img src="https://img.shields.io/badge/Minimum%20Android-8.1-6C9B83?style=flat-square" alt="Android 8.1+" />
</p>

F12 Mobile combines a compact Android browser with practical page-inspection tools. The interface uses a restrained light palette, clear panel hierarchy, and native Java/XML components to keep the app approachable on phone screens.

> **Project status:** active MVP. Some panels provide focused inspection rather than full desktop DevTools parity. See the capability matrix below for exact scope.

## Highlights

- Embedded WebView browser with back, forward, reload, and URL navigation.
- DOM outline, CSS-selector inspection, and a simple selected-node HTML edit action.
- Temporary live CSS injection and removal.
- JavaScript console capture and expression runner.
- Network event list with request headers, payload and response previews where page instrumentation can observe them.
- HAR 1.2-style session export through Android's document picker.
- Storage overview with local-storage keys, cookie names, and IndexedDB database-name discovery where supported.
- Performance entry timeline from the browser Performance API.
- Responsive viewport-width control.
- DOM-derived accessibility outline.
- Remote WebView debugging for desktop Chrome.

## Feature scope

| Area | Current behavior | Important limitation |
|---|---|---|
| Elements | DOM outline, selector inspection, simple HTML edit | Not a full drag-and-drop DOM tree editor |
| Styles | Inject and remove live CSS rules | Does not expose the browser's full computed-style cascade |
| Network | Resource events and instrumented Fetch/XHR detail previews | Native WebView resource hooks do not expose request bodies or response bodies; page hooks see only supported Fetch/XHR calls |
| Debugger | WebView remote debugging | Breakpoints and step controls are provided by desktop Chrome DevTools, not embedded in this app |
| Device preview | Adjustable viewport width | Not full device emulation; DPR, hardware, sensors, and complete touch profiles are not simulated |
| Storage | Local-storage keys, cookie names, IndexedDB database names (if API exists) | Secret values are intentionally not shown; no complete cookie editor or IndexedDB record editor |
| Performance | Navigation/resource/performance entries | Not a full frame-by-frame rendering profiler |
| Accessibility | DOM-derived labels, roles, IDs and controls | Not the complete platform accessibility tree |
| Reports | HAR 1.2-style JSON export | Partial session data; unavailable headers/timings are omitted or represented as empty |

## JavaScript debugging with Chrome

Android WebView exposes remote debugging, but its public Android API does not provide an in-app breakpoint/step-debugger interface.

1. Enable Developer options and USB debugging on the Android device.
2. Connect the device to a desktop with Chrome installed.
3. Open `chrome://inspect/#devices` in desktop Chrome.
4. Find the F12 Mobile WebView and choose **Inspect**.
5. Use Chrome DevTools Sources to set breakpoints, pause execution, and step through JavaScript.

Remote debugging is enabled in this build. Only debug pages and accounts you are authorized to inspect.

## Privacy and security

- Sensitive header names such as Authorization, Cookie, Set-Cookie, and API-key headers are masked in captured request metadata.
- Common secret-like fields in previews and query parameters are redacted on a best-effort basis. Redaction is not a guarantee; avoid inspecting real credentials or personal data.
- Cookie values and local-storage values are not displayed in the Storage panel.
- The app does not read SMS, email inboxes, OTP notifications, or traffic from other apps.
- No JavaScript bridge to privileged native Android functionality is exposed to inspected pages.
- Cleartext HTTP is disabled by default.
- Network previews may contain page data. Export reports only from sites and sessions you are authorized to test; review the report before sharing it.

## Build from source

### Requirements

- JDK 17
- Android SDK Platform 35
- Android Gradle Plugin 8.6.1
- Gradle 8.9

### Build a debug APK

```bash
gradle --no-daemon clean lintDebug assembleDebug
```

Output:

```
app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions runs lint and assembles the debug APK on pushes to `main`, pull requests, and manual dispatch. Download the `f12-mobile-debug` artifact from the Actions run.

## Compatibility

| Setting | Value |
|---|---|
| Minimum Android | 8.1 (API 27) |
| Compile SDK | 35 |
| Target SDK | 35 |
| App language | Java |
| Layout system | Android XML |
| Browser engine | Android System WebView |

## Project structure

```
app/src/main/
├── java/com/f12mobile/MainActivity.java
├── res/
│   ├── drawable/
│   │   ├── bg_field.xml
│   │   └── ic_f12_logo.xml
│   ├── layout/activity_main.xml
│   └── values/styles.xml
└── AndroidManifest.xml
.github/workflows/android.yml
```

## Contributing

Bug reports and focused pull requests are welcome. Include the Android version, device model, reproduction steps, expected behavior, actual behavior, and relevant redacted logs. Never commit API keys, credentials, cookies, OTPs, or private session exports.

## License

This project uses the **F12 Mobile Community License 1.0 (F12CL-1.0)**. Read [LICENSE](LICENSE) before use or redistribution. This is a project-specific license and is not an OSI-approved open-source license.
