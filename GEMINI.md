# Project Context: TrickTrack

## 1. Core Philosophy
* **Type:** Android App (Mileage Logbook)
* **Values:** Privacy-first, Offline-first, Open Source.
* **Monetization:** None (Donation based). No Ads.
* **Language:** Kotlin.

## 2. Architecture & Tech Stack
* **UI:** Jetpack Compose (Material 3).
* **Navigation:** Jetpack Compose Navigation.
* **DI (Dependency Injection):** None (no Hilt/Dagger). Shared singletons live in `TripApplication` (database, `TripRepository`, `UserPreferencesRepository`, `GeocoderHelper`, `BluetoothRepository`); ViewModels are created via `ViewModelFactory` / `SettingsViewModelFactory`.
* **Database:** Room Database (`AppDatabase`). Schemas are exported to `app/schemas/`; every version bump needs a `Migration` and the new schema JSON committed.
* **Network:** OkHttp (No Retrofit).
    * *Reason:* Lightweight, only 2 services used.
    * Always use the shared client `AppHttpClient.client` (or `.newBuilder()` for other timeouts). It sets the TrickTrack User-Agent: photon.komoot.io rejects OkHttp's default agent with HTTP 403.
    * Close responses with `execute().use { }`.
* **Logging:** `AppLogger.log()` writes the in-app log file on a background thread (also shown in Settings > Diagnostics > Logs). Never log full coordinates, addresses or request URLs; use `AppLogger.sanitizeLocation()`. Release builds strip `Log.d`/`Log.v` via R8.
* **Async:** Kotlin Coroutines & Flow.

## 3. External Services (Self-Hostable)
* **Routing (Distance):** OSRM (Open Source Routing Machine).
    * *Default:* `https://router.project-osrm.org/route/v1/driving`
    * *Validation:* Must check for JSON `code: "Ok"`.
* **Geocoding:** Photon (OpenStreetMap), for BOTH address search and coordinates-to-address (reverse).
    * *Default:* `https://photon.komoot.io/api` (reverse endpoint derived as `.../reverse`).
    * Implemented in `PhotonGeocoder` + `PhotonAddressFormatter`, used via `GeocoderHelper`.
    * Do NOT use Android's `android.location.Geocoder`: it sends data to Google, which the privacy policy rules out.
    * *Validation:* Must check for GeoJSON `type: "FeatureCollection"`.
* **User Control:** Users can change these URLs in "Advanced Settings".

## 4. Key Implementation Details
* **Distance Calculation (manual trips):** Not automatic. Triggered by the "Calculate" button in the manual trip sheet.
    * Logic resides in `TripsViewModel.calculateDistance()` -> `GeocoderHelper` (address to coordinates) -> `DistanceRepository` (OSRM).
* **Settings UI:**
    * Uses `ExpandableSettingsGroup` composable for accordion-style layout.
    * Most settings are stored in `UserPreferencesRepository` (DataStore). Server URLs (OSRM/Photon) are in `AppPreferences` (SharedPreferences).
    * **Tracking Settings:**
        * Automatic Tracking (on/off)
        * Bluetooth Trigger (on/off, select device)
        * Automatic Tracking Schedule (daily schedule, target: automatic, bluetooth, or both)
        * Smart Location Snapping (on/off, radius in meters)
        * Stillness Timer (in seconds)
        * Minimum Speed (in km/h)
        * Movement Sensitivity (on/off, wake-up radius in meters / feet)
        * Minimum Trip Distance (in meters / feet)
        * Default Trip Type (business/personal)
    * **Reporting:**
        * Configure Export Fields (Date, Start/End Time, Start/End Location, Distance, Type, Expenses)
        * Calculate Trip Expenses (on/off, rate per km, currency)
    * **Backup and Restore:**
        * Manual Backup/Restore
        * Automatic Backups (on/off, folder, frequency: daily, weekly, monthly)
        * If weekly, day of the week can be selected.
        * If monthly, day of the month can be selected (1-28).
    * **Advanced Settings:**
        * API Settings (OSRM and Photon URLs)
    * **Diagnostics:**
        * Permissions Check
        * View Logs
* **Background tracking:** `LocationService` (foreground service). Active trips are checkpointed (`ActiveTripCheckpointManager`) and recovered by `ActiveTripRecoveryHelper` after a crash or shutdown; trips still running when the service is destroyed are saved with `stopReason = SERVICE_STOPPED`.
* **Data Safety:**
    * Strict privacy. "Background Location" permission is required for automatic tracking.
    * Trip data (database, checkpoint, logs, map tiles) is excluded from Google cloud backup (`res/xml/data_extraction_rules.xml`, `backup_rules.xml`). Keep it that way and keep `PRIVACY.md` in sync with any data flow change.
    * Backup restore runs in a single Room transaction (all-or-nothing).

## 5. Current Status / Known Constraints
* **API Limits:** 
* Public OSRM/Photon APIs have rate limits; user agent must be set correctly.
* **Localization:** The app is localized in English, German, Italian and French.

## 6. Changelog & Release Notes Format
Every time `CHANGELOG.md` is changed or release notes are requested, ALWAYS update BOTH Part 1 and Part 2 simultaneously:

### Part 1: GitHub Release & `CHANGELOG.md` Template
Follow this exact layout:
```markdown
[1-2 friendly introductory sentences summarizing the update]

---

# 🎨 [Category / Title 1]
* **[Topic]:** [Clear, concise explanation]

---

# ✨ [Category / Title 2]
* **[Topic]:** [Clear, concise explanation]

---
```
*(Standard emojis for section titles: 🚀 New Features, 🎨 UI & Design, ✨ Enhancements, 🛠️ Fixes, 🛡️ Reliability, ⚡ Performance)*

### Part 2: Google Play Store Release Notes ("What's new")
Provide concise, plain-text bullet points (**strictly under 500 characters** in total per language including the link, no markdown) in all 4 supported languages:
* `en-US` (`app/src/main/play/release-notes/en-US/default.txt`)
* `de-DE` (`app/src/main/play/release-notes/de-DE/default.txt`)
* `fr-FR` (`app/src/main/play/release-notes/fr-FR/default.txt`)
* `it-IT` (`app/src/main/play/release-notes/it-IT/default.txt`)

Format:
• [User benefit / feature 1]
• [User benefit / feature 2]
• [Polish / fix]

Full changelog: https://github.com/OPUM-LABS/TrickTrack/releases/tag/vx.x.x