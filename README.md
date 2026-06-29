# NotifyMQTT

NotifyMQTT is a small Android app that keeps an MQTT subscription alive and turns incoming broker messages into phone notifications.

The project is intentionally built outside of the Play Store flow. GitHub Actions builds pull requests and can publish installable APK artifacts from tags or manual release runs.

## Features

- Configure broker host, port, TLS, credentials, client ID, and subscribed topics.
- Subscribe to multiple MQTT topics, one topic per line.
- Run a foreground MQTT listener service for reliable modern Android background execution.
- Post a phone notification for every incoming MQTT message.
- Toggle audible notification ding on or off.
- Optional start-on-boot toggle.
- Open-source Kotlin + Jetpack Compose Android project.

## Android support

This app targets Android 16 / API 36 and has `minSdk` 35. It is not intended to support legacy Android versions.

Modern Android requires long-running background network listeners to run as a foreground service with a visible service notification. NotifyMQTT keeps that service notification low-priority and silent, then posts separate message notifications when MQTT payloads arrive.

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

## MQTT topic examples

```text
home/garage/door
home/hot-tub/alerts
zigbee2mqtt/+/availability
```

## Notes for reliable background use

After installing the APK, open Android app settings for NotifyMQTT and allow notifications. For best reliability on battery-managed phones, set the app battery mode to unrestricted.

## License

MIT
