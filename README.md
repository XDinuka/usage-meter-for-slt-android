# Usage Meter for SLT — Android

An Android home screen widget and companion app for tracking your Sri Lanka Telecom (SLT) broadband usage. Sign in with your [MySLT portal](https://myslt.slt.lk) credentials to see your data usage, bonus data, extra GB, and add-on bundles - right from your home screen.

This is the Android port of the [iOS app by prabch](https://github.com/prabch/Usage-Meter-for-SLT). Both projects are open for contributions.

> **Disclaimer:** This app is an independent, community-built project and is not affiliated with, endorsed by, or officially connected to Sri Lanka Telecom (SLT) in any way.

---

## Features

- **Multiple accounts** — add as many MySLT accounts as you like; each gets its own card
- **Home screen widgets**
  - 2×1 usage widget — scrollable progress bars per data type, configurable per widget
  - 1×1 metric widget — single circular progress arc for one chosen data point
- **Per-widget configuration** — theme (light/dark/system), background opacity, which data points to show, custom colors per data type, subscriber ID visibility, last-updated timestamp
- **Reconfigurable widgets** — long-press any widget to reconfigure it after placement
- **Cached data** — the app shows last-known values instantly on open; pull down to refresh
- **Dark / light / system theme** — override or follow system setting
- **Configurable refresh interval** — 15, 30, or 60 minutes

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- Android SDK 26+
- A MySLT account (broadband customers of Sri Lanka Telecom)

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/Usage-Meter-for-SLT.git
   cd Usage-Meter-for-SLT/usage-meter-for-slt-android
   ```

2. Create a `local.properties` file in the project root (if it doesn't already exist) and add your SLT API client ID:
   ```properties
   SLT_CLIENT_ID=your_client_id_here
   ```
   > The client ID is required to authenticate with the SLT API gateway. You can find it by inspecting the MySLT web portal's network requests.

3. Open the project in Android Studio and run on a device or emulator.

---

## Project Structure

```
app/src/main/java/com/xdinuka/sltusagemeter/
├── data/
│   ├── auth/          # Account profiles, encrypted token storage
│   ├── model/         # API response models (Moshi)
│   ├── network/       # Retrofit + OkHttp, per-profile auth interceptors
│   ├── prefs/         # App preferences, usage cache
│   └── repository/    # SltRepository — single source of truth
├── ui/
│   ├── home/          # Home screen, account cards, inline add-account form
│   ├── login/         # Login screen & view model
│   ├── main/          # Main scaffold, floating nav bar
│   ├── settings/      # Theme & refresh interval settings
│   ├── theme/         # Material3 colour schemes
│   └── usage/         # Usage detail screen
└── widget/
    ├── SltWidget.kt              # 2×1 Glance widget
    ├── SltMetricWidget.kt        # 1×1 circular metric widget
    ├── SltWidgetWorker.kt        # WorkManager background refresh
    ├── SltWidgetReceiver.kt      # Glance receiver + work scheduling
    ├── SltWidgetConfigActivity   # Widget configuration UI
    ├── MetricWidgetConfigActivity
    ├── WidgetConfig.kt           # Config data classes + colour palette
    └── WidgetStateStore.kt       # Per-widget state persistence
```

---

## Tech Stack

| Layer | Library |
|-------|---------|
| UI | Jetpack Compose + Material3 |
| Widgets | Glance API |
| DI | Hilt |
| Networking | Retrofit + OkHttp + Moshi |
| Background work | WorkManager |
| Secure storage | EncryptedSharedPreferences |
| Build | AGP 9 · Gradle 9 · Kotlin 2 |

---

## Contributing

Contributions are welcome! Both this Android app and the [original iOS app](https://github.com/prabch/Usage-Meter-for-SLT) are open for contributions.

- **Bug reports & feature requests** — open an issue
- **Pull requests** — fork the repo, make your changes on a feature branch, and open a PR against `main`
- **iOS version** — see [prabch/Usage-Meter-for-SLT](https://github.com/prabch/Usage-Meter-for-SLT)

Please keep pull requests focused on a single change and include a brief description of what you changed and why.

---

## Related

- **iOS app** — [prabch/Usage-Meter-for-SLT](https://github.com/prabch/Usage-Meter-for-SLT) — the original inspiration for this Android port
- **MySLT portal** — [myslt.slt.lk](https://myslt.slt.lk)

---

## License

See [LICENSE](LICENSE).
