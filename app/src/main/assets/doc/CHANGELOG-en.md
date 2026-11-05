******

### Release history

******

# v1.0.0

###### 2026/11/05

* `Hint` Development preview: the AutoJs6 Plugin Center can discover the plugin; alarms, timers, the stopwatch, the plugin screens and the alarms script module are not delivered yet
* `Hint` Requires AutoJs6 6.8.1 (6641) or later (the exact minimum build will be filled in once the host changes land)
* `Hint` The current icon is a candidate design and may still change
* `Feature` Plugin skeleton: Wake activation protocol, Plugin Center identity service and alarm capability service
* `Feature` No native libraries: a single APK for every device
* `Feature` README, Plugin Center instructions and changelogs in 10 languages, generated from JSON sources
* `Dependency` Attach AndroidX Core KTX 1.15.0
* `Dependency` Attach lib-recur 0.17.1 (calculates the repeat rules of alarms)
* `Dependency` Attach Desugar JDK Libs 2.1.5 (date and time zone calculations on Android 7.x)
