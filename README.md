# TrickTrack 📍🚗

**TrickTrack** is a smart, automated GPS mileage tracker for Android built with modern Jetpack Compose. It automatically detects when you are driving - either via Bluetooth connection or movement activity - and logs your trips effortlessly. Designed for privacy and battery efficiency.

![App Screenshot](pictures/preview.png) ## ✨ Key Features

### 🧠 Smart Automation
* **Bluetooth Trigger:** Automatically starts tracking when connected to specific devices (e.g., Car Audio).
* **Activity Recognition:** Detects when you are in a vehicle and starts tracking automatically.
* **Stillness Detection:** Intelligently stops the trip when you park and stop moving.
* **Scheduling:** Set specific times or days when automation is allowed (e.g., "Work Hours Only") and save power, when tracking is disabled.

### 📍 Accurate Tracking
* **Live Distance:** Visual real-time distance updates in the notification shade.
* **Foreground Service:** Ensures reliable tracking even when the app is in the background.
* **Smart Suggestions:** Uses the **Photon API (OpenStreetMap)** to auto-fill place names (e.g., "Starbucks") instead of just raw addresses.

### 💰 Reporting & Finances
* **Expense Tracking:** Automatically calculates trip costs based on customizable mileage rates.
* **Advanced Filtering:** Easily filter your history by Date, Keywords, or Trip Type to find exactly what you need.
* **Export Data:** Export your trip logs to **PDF** or **CSV/Excel** for tax returns or employer reimbursement.

### 📊 Data Management
* **Favorites:** Save frequent locations (Home, Work) for quick logging.
* **Data Privacy:** All data is stored locally on your device using Room Database. No cloud servers.

### 🎨 Design & Theming
* **Adaptive Dark/Light Mode:** The UI seamlessly syncs with your system settings.
* **Material 3:** Built with the latest Android design standards for a modern, fluid, and intuitive user experience.
---

## 🛠️ Tech Stack

* **Language:** [Kotlin](https://kotlinlang.org/) (100%)
* **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Local Data:**
    * **Room:** For storing trips and favorites.
    * **DataStore:** For storing user preferences and settings.
* **Location & Maps:**
    * **Android Location Services (FusedLocationProvider):** For GPS tracking.
    * **Photon API:** For geocoding and address suggestions (OpenStreetMap).
* **Background Processing:**
    * **WorkManager:** For scheduling background tasks.
    * **Foreground Services:** For active trip recording.

---

## 🚀 Installation & Setup

1.  **Get the latest version (apk) from the releases page**
2.  **Install the app**

Play Store release is in progress
---

## 📸 Usage

1.  **Permissions:** Grant Location (Always Allow recommended for automation) and Notification permissions on first launch.
2.  **Settings:**
    * Go to Settings to add your **Bluetooth Device** (Car).
    * Configure your **Schedule** if you only want to track during work hours.
3.  **Manual Trip:** Tap the "Start" button on the home screen to start a manual trip.
4.  **Auto Trip:** Just drive! The app handles the rest.

---

## ⚖️ License & Attribution

This project is open-source.

* **Map Data:** © [OpenStreetMap contributors](https://www.openstreetmap.org/copyright).
* **Geocoding:** Powered by [Photon](https://github.com/komoot/photon).

---

Made with ❤️ using Kotlin & Jetpack Compose.