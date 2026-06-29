# NotifyMQTT

NotifyMQTT is a small Android app that keeps an MQTT subscription alive and turns incoming broker messages into phone notifications.

The project is intentionally built outside of the Play Store flow. GitHub Actions builds pull requests and can publish installable APK artifacts from tags or manual release runs.

## Features

- Configure broker host, port, TLS, username, password, client ID, and subscribed topics.
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

You can also run the **Release APK** workflow manually.

### Optional signing setup

For stable side-load updates, configure these repository secrets:

- `ANDROID_KEYSTORE_BASE64` — base64 of your `.jks` / `.keystore`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

When those secrets exist, the workflow builds a signed release APK. Without them, it publishes a debug APK artifact instead so the project is still usable with zero secret setup.

Create a local keystore with:

```bash
keytool -genkeypair \
  -v \
  -keystore notifymqtt-release.jks \
  -alias notifymqtt \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
base64 -w 0 notifymqtt-release.jks
```

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
