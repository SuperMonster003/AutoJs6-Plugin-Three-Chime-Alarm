# Third-party notices

3-Chime Alarm is licensed under the Mozilla Public License 2.0 (see `LICENSE`). The APK bundles the
components below; their licenses apply to those components only. The APK contains no native libraries.

## AutoJs6 host contract libraries

| Artifact | Source | License |
| --- | --- | --- |
| `libs/alarm-api.aar` | AutoJs6 host module `plugin-api/alarm-api` (https://github.com/SuperMonster003/AutoJs6) | MPL-2.0 |
| `libs/common-plugin-api.aar` | AutoJs6 host module `plugin-api/common-plugin-api` (https://github.com/SuperMonster003/AutoJs6) | MPL-2.0 |

Provenance and SHA-256 digests are recorded in `locks/host-api-aars.lock` and `libs/README.md`.

## AndroidX and Kotlin libraries (Maven)

| Library | Version | License |
| --- | --- | --- |
| AndroidX Core KTX (`androidx.core:core-ktx`) | 1.15.0 | Apache-2.0 |
| Kotlin standard library (`org.jetbrains.kotlin:kotlin-stdlib`, added by the Android Gradle Plugin) | follows the build's Kotlin version | Apache-2.0 |

## Recurrence rule libraries (Maven)

The repeat rules of alarms (RFC 5545 `RRULE`, roadmap T10) are evaluated with dmfs lib-recur and its two dependencies.
All three are pure Java.

| Library | Version | Source | License |
| --- | --- | --- | --- |
| lib-recur (`org.dmfs:lib-recur`) | 0.17.1 | https://github.com/dmfs/lib-recur | Apache-2.0 |
| rfc5545-datetime (`org.dmfs:rfc5545-datetime`) | 0.3 | https://github.com/dmfs/rfc5545-datetime | Apache-2.0 |
| jems2 (`org.dmfs:jems2`) | 2.23.1 | https://github.com/dmfs/jems | Apache-2.0 |

The jems2 artifact carries the following `NOTICE` (the Android build drops `META-INF/NOTICE` from the APK, so it is
reproduced here):

```text
Java gems, a collection of Java utilities.

Copyright 2017 dmfs GmbH- http://dmfs.org
```

## Android desugared JDK libraries

| Library | Version | Source | License |
| --- | --- | --- | --- |
| desugar_jdk_libs (`com.android.tools:desugar_jdk_libs`, `java.time` and other JDK APIs on API 24 and 25 devices) | 2.1.5 | https://github.com/google/desugar_jdk_libs | GPL-2.0 with the Classpath Exception |

The full text of the Apache License 2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.

Libraries used only by the JVM and device tests (JUnit, AndroidX Test) are not part of the APK.
