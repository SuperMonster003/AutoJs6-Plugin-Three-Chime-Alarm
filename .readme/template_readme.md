<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="{{ repo_url }}/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="{{ repo_url }}/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="{{ icon_alt }}" border="0" width="128" />
    </picture>
  </p>

  <p>{{ text_plugin_synopsis }}</p>

  <p>
    <a href="{{ repo_url }}/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/{{ repo_slug }}?label=Release"/></a>
    <a href="{{ repo_url }}/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/{{ repo_slug }}?color=A24232&label=Issues"/></a>
    <a href="{{ license_url }}"><img alt="GitHub License" src="https://img.shields.io/github/license/{{ repo_slug }}?color=534BAE&label=License"/></a>
  </p>
</div>

******

### {{ h3_languages_with_ascii }}

******

{{ p_languages_all_supported_for_readme }}:

{{ placeholder_ul_languages_all_supported }}

******

### {{ h3_introduction }}

******

{{ p_introduction_what }}

{{ p_introduction_how }}

> {{ p_status_note }}

******

### {{ h3_features }}

******

{{ placeholder_features }}

******

### {{ h3_screenshots }}

******

{{ p_screenshots }}

| {{ text_screenshot_list }} | {{ text_screenshot_edit }} | {{ text_screenshot_ring }} | {{ text_screenshot_settings }} |
| :---: | :---: | :---: | :---: |
| <img src="{{ repo_url }}/blob/master/docs/images/screenshots/list-light.png?raw=true" alt="list-light" width="200" /> | <img src="{{ repo_url }}/blob/master/docs/images/screenshots/edit-light.png?raw=true" alt="edit-light" width="200" /> | <img src="{{ repo_url }}/blob/master/docs/images/screenshots/ring-light.png?raw=true" alt="ring-light" width="200" /> | <img src="{{ repo_url }}/blob/master/docs/images/screenshots/settings-light.png?raw=true" alt="settings-light" width="200" /> |
| <img src="{{ repo_url }}/blob/master/docs/images/screenshots/list-dark.png?raw=true" alt="list-dark" width="200" /> | <img src="{{ repo_url }}/blob/master/docs/images/screenshots/edit-dark.png?raw=true" alt="edit-dark" width="200" /> | <img src="{{ repo_url }}/blob/master/docs/images/screenshots/ring-dark.png?raw=true" alt="ring-dark" width="200" /> | <img src="{{ repo_url }}/blob/master/docs/images/screenshots/settings-dark.png?raw=true" alt="settings-dark" width="200" /> |

| {{ text_screenshot_timers }} | {{ text_screenshot_stopwatch }} |
| :---: | :---: |
| <img src="{{ repo_url }}/blob/master/docs/images/screenshots/timers-light.png?raw=true" alt="timers-light" width="200" /> | <img src="{{ repo_url }}/blob/master/docs/images/screenshots/stopwatch-light.png?raw=true" alt="stopwatch-light" width="200" /> |
| <img src="{{ repo_url }}/blob/master/docs/images/screenshots/timers-dark.png?raw=true" alt="timers-dark" width="200" /> | <img src="{{ repo_url }}/blob/master/docs/images/screenshots/stopwatch-dark.png?raw=true" alt="stopwatch-dark" width="200" /> |

******

### {{ h3_usage }}

******

{{ placeholder_usage_steps }}

> {{ p_usage_note }}

******

### {{ h3_script_api }}

******

{{ p_script_api_intro }}:

```javascript
let a = alarms.add({ time: "07:30", repeat: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", label: "Wake up" });
a.bind("./after-wake.js", { events: ["dismiss"] });

alarms.add({ time: "18:00", repeat: "FREQ=MONTHLY;BYDAY=-1FR", label: "Monthly report" });
alarms.add({ time: "06:10", date: "2026-11-04", repeat: { freq: "daily", interval: 3 }, tag: "shift" });

alarms.on("ring", (e) => toast(e.label));

alarms.timers.start("25m", { label: "Tea" });

alarms.system.setAlarm({ hour: 6, minute: 50, label: "Flight" });
```

{{ placeholder_api_points }}

******

### {{ h3_system_clock }}

******

{{ p_system_clock_intro }}:

{{ placeholder_system_clock_points }}

******

### {{ h3_compatibility }}

******

{{ p_compatibility_host }}

{{ p_compatibility_matrix }}:

{{ placeholder_compatibility_table }}

******

### {{ h3_faq }}

******

{{ placeholder_faq }}

******

### {{ h3_security }}

******

{{ p_permissions_intro }}:

{{ placeholder_permission_points }}

{{ p_security_intro }}

{{ placeholder_security_points }}

******

### {{ h3_plugin_interface }}

******

{{ p_plugin_interface }}:

```text
application id: {{ plugin_application_id }}
plugin id: {{ plugin_id }}
engine: {{ plugin_engine }}
variant: {{ plugin_variant }}
info service: {{ info_action }} (category {{ info_category }})
capability service: {{ discovery_action }} (category {{ discovery_category }})
wake action: {{ wake_action }}
minimum host build: {{ required_host_version_code }}
native libraries: libandroidx.graphics.path.so only (Compose graphics-path, 4 ABIs, uncompressed, 16 KB aligned; a single APK for every ABI)
```

******

### {{ h3_roadmap }}

******

{{ p_roadmap }}

- [{{ text_link_roadmap }}]({{ roadmap_url }})

******

### {{ h3_release_history }}

******

{{ placeholder_latest_release_history }}

##### {{ h5_for_more_release_history }}

* {{ placeholder_read_more_in_changelog_md }}

******

### {{ h3_build }}

******

{{ p_build_intro }}

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### {{ h3_license }}

******

{{ p_license }}

- [{{ text_link_third_party }}]({{ third_party_url }})

******

### {{ h3_links }}

******

- {{ text_link_autojs6 }}: {{ autojs6_url }}
