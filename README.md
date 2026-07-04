# NotifyMQTT

NotifyMQTT is a small Android app that keeps MQTT subscriptions alive and routes incoming broker messages to configurable phone notification or logging behaviors.

The project is intentionally built outside of the Play Store flow. GitHub Actions builds pull requests and can publish installable APK artifacts from tags or manual release runs.

## Features

- Configure broker host, port, TLS, credentials, client ID, and subscribed topics.
- Subscribe to multiple MQTT topic filters with per-subscription behavior.
- Route each matching subscription to one of three behaviors: audible notification, silent notification, or log only.
- Keep the existing audible and silent Android notification channels.
- Log-only messages write to Android Logcat and to a rotating app-private log file without posting a notification.
- Run a foreground MQTT listener service for reliable modern Android background execution.
- Optional start-on-boot toggle.
- Open-source Kotlin + Jetpack Compose Android project.

## Android support

This app targets Android 16 / API 36 and has `minSdk` 35. It is not intended to support legacy Android versions.

Modern Android requires long-running background network listeners to run as a foreground service with a visible service notification. NotifyMQTT keeps that service notification low-priority and silent, then applies the configured behavior when MQTT payloads arrive.

## Subscription behavior rules

Configure one subscription per line. Add a behavior after a `|` separator:

```text
home/garage/door | ding
home/hot-tub/alerts | silent
zigbee2mqtt/+/availability | log
telemetry/# | log
```

Supported behaviors:

- `ding` — post through the audible MQTT message notification channel.
- `silent` — post through the silent MQTT message notification channel.
- `log` — write the message to Logcat and the app-private rotating message log without posting a notification.

Plain topic lines remain backward-compatible:

```text
home/garage/door
```

A plain topic uses the app's **Ding for plain topic lines** toggle to choose audible or silent notifications.

Rules are evaluated in order and the first matching rule wins. MQTT `+` and `#` wildcard filters are supported.

Log-only messages use Logcat tag `NotifyMQTT` and are also written under the app's private files directory as:

```text
mqtt-messages.log
mqtt-messages.log.1
```

The active log rotates at approximately 1 MiB.

## Local development

Install Android Studio Meerkat `2024.3.1` or newer with the Android 16 SDK / API 36 platform and build tools installed.

```bash
gradle --no-daemon testDebugUnitTest lintDebug assembleDebug
```

The generated debug APK is created under:

```text
app/build/outputs/apk/debug/
```

## Release APKs

A GitHub release is created when you push a tag like:

```bash
git tag v0.1.0
git push origin v0.1.0
```

You can also run the **Release APK** workflow manually. The current workflow publishes an installable debug APK, which keeps the project usable with no repository secret setup.

## Notes for reliable background use

After installing the APK, open Android app settings for NotifyMQTT and allow notifications. For best reliability on battery-managed phones, set the app battery mode to unrestricted.

For offline MQTT catch-up, configure a stable Client ID and use a broker/session policy that retains queued QoS 1 messages long enough for the phone to reconnect.

## License

MIT
