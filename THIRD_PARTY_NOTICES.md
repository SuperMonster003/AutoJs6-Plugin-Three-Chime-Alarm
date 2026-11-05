# Third-party notices

3-Chime Alarm is licensed under the Mozilla Public License 2.0 (see `LICENSE`). The APK bundles the
components below; their licenses apply to those components only. The APK contains no native libraries.

## AutoJs6 host contract libraries

| Artifact | Source | License |
| --- | --- | --- |
| `libs/common-plugin-api.aar` | AutoJs6 host module `plugin-api/common-plugin-api` (https://github.com/SuperMonster003/AutoJs6) | MPL-2.0 |

Provenance and SHA-256 digests are recorded in `locks/host-api-aars.lock` and `libs/README.md`.

## AndroidX and Kotlin libraries (Maven)

| Library | Version | License |
| --- | --- | --- |
| AndroidX Core KTX (`androidx.core:core-ktx`) | 1.15.0 | Apache-2.0 |
| Kotlin standard library (`org.jetbrains.kotlin:kotlin-stdlib`, added by the Android Gradle Plugin) | follows the build's Kotlin version | Apache-2.0 |

Libraries used only by the JVM and device tests (JUnit, AndroidX Test) are not part of the APK.
