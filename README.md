# NotifyMQTT

NotifyMQTT keeps MQTT subscriptions alive and routes messages to configurable behaviors.

## Subscription rules

```text
home/garage/door | ding
home/hot-tub/alerts | silent
zigbee2mqtt/+/availability | log
sensors/heartbeat | missing:15
```

`missing:X` sends one audible alert if no matching message arrives for X minutes. A new matching message resets the timer and re-arms the alert. `stale:X` and `watch:X` are accepted aliases.

`log` messages can be viewed in the app's log viewer. MQTT `+` and `#` wildcards are supported.

## Build

```bash
gradle --no-daemon testDebugUnitTest lintDebug assembleDebug
```

## License

MIT
