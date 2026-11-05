# Staged binary artifacts

This directory holds the exact, hash-locked binary artifacts the plugin consumes. Gradle never resolves
them from sibling repositories or from `mavenLocal()`: `app/build.gradle.kts` reads `../locks/host-api-aars.lock`
and fails configuration when a staged file is missing, listed with an invalid digest or different from its
locked SHA-256.

## Host contract AARs (lock `../locks/host-api-aars.lock`)

Release artifacts of the AutoJs6 host, consumed with `implementation` because the plugin runs in its own
process and needs the AIDL stubs and constants at run time.

| File | Host module | Content | SHA-256 |
| --- | --- | --- | --- |
| `alarm-api.aar` | `plugin-api/alarm-api` | `IAlarmPlugin`, `IAlarmEventCallback`, `IAlarmProgressCallback`, `AlarmContract`, `AlarmOptionKeys`, `AlarmResultKeys`, `AlarmEventKeys`, `AlarmLimits`, `AlarmErrorCodes`, `AlarmBundles`, `AlarmJson`, the typed models | `f2727527de88f9811f67b0e23bbf27a8d6a9eb3237f61e74ff31534ea3aaf00b` |
| `common-plugin-api.aar` | `plugin-api/common-plugin-api` | `PluginInfo`, `IPluginInfoProvider`, `PluginActions`, `PluginCapabilityKeys`, `AutoJs6HostSettingsContract` | `9eaba785db6128f9a0ded272ab976f37835e9fa17a49cc250a6a75ddf76cf769` |

Provenance (2026-11-05, roadmap P2): both AARs come from host commit `0924ecefcc`, the commit that introduces
`plugin-api/alarm-api` (roadmap P1.1); no later host commit touches that module. `alarm-api.aar` is the release
build made with that commit (128,727 B); a clean clone checked out at `0924ecefcc` rebuilds the same
`classes.jar` (CRC32 `d3f9dc94`) and differs only in the line endings of `proguard.txt` (the clone checks files
out with CRLF). `common-plugin-api.aar` rebuilt from that clone is byte-identical to the artifact staged before
(3-Stamp Mail, 2026-10-21, host 6.8.0 build 5697): at `0924ecefcc` the module is still the one of host commit
`1a564ee85d` (2026-10-16). Later host commits add the plugin settings transfer provider to `common-plugin-api`
(1.1.0); restage both AARs from one host commit when the plugin adopts it.

Do not commit locally assembled debug AARs or rename debug outputs to bypass this policy.

To update: assemble `:plugin-api:<module>:assembleRelease` in the host, copy the AAR here, record its
lowercase SHA-256 in the lock and in this table, and list it in `THIRD_PARTY_NOTICES.md`.
