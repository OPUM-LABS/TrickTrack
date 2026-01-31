# Project Context: TrickTrack

## 1. Core Philosophy
* **Type:** Android App (Mileage Logbook)
* **Values:** Privacy-first, Offline-first, Open Source.
* **Monetization:** None (Donation based). No Ads.
* **Language:** Kotlin.

## 2. Architecture & Tech Stack
* **UI:** Jetpack Compose (Material 3).
* **Navigation:** Jetpack Compose Navigation.
* **DI (Dependency Injection):** Hilt (Dagger).
* **Database:** Room Database.
* **Network:** OkHttp (No Retrofit).
    * *Reason:* Lightweight, only 2 endpoints used.
* **Async:** Kotlin Coroutines & Flow.

## 3. External Services (Self-Hostable)
* **Routing (Distance):** OSRM (Open Source Routing Machine).
    * *Default:* `http://router.project-osrm.org/route/v1/driving`
    * *Validation:* Must check for JSON `code: "Ok"`.
* **Geocoding (Address Search):** Photon (OpenStreetMap).
    * *Default:* `https://photon.komoot.io/api`
    * *Validation:* Must check for GeoJSON `type: "FeatureCollection"`.
* **User Control:** Users can change these URLs in "Advanced Settings".

## 4. Key Implementation Details
* **Distance Calculation:** Not automatic. Triggered by an "Auto" button in the `ManualTripDialog`.
    * Logic resides in `ManualTripViewModel` -> `DistanceRepository`.
* **Settings UI:**
    * Uses `ExpandableSettingsGroup` composable for accordion-style layout.
    * Data stored in `AppPreferences` (SharedPreferences wrapper).
    * **Tracking Settings:**
        * Automatic Tracking (on/off)
        * Bluetooth Trigger (on/off, select device)
        * Automatic Tracking Schedule (daily schedule, target: automatic, bluetooth, or both)
        * Smart Location Snapping (on/off, radius in meters)
        * Stillness Timer (in seconds)
        * Minimum Speed (in km/h)
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
* **Data Safety:**
    * Strict privacy. "Background Location" permission is required for automatic tracking.

## 5. Current Status / Known Constraints
* **API Limits:** 
* Public OSRM/Photon APIs have rate limits; user agent must be set correctly.
* **Hilt State:** Hilt is fully active.
* **Localization:** The app is localized in English, German, Italian and French.