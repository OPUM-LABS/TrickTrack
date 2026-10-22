# Changelog

All notable changes to TrickTrack will be documented in this file.

---

## [2.7.1] - 2026-10-22

This update focuses on significant power consumption optimizations for background tracking, refined motion detection, and visual polish across the app settings.

---

# ⚡ Performance & Battery
* **Smart GPS Wake-up:** Replaced continuous GPS polling with low-power sensor and displacement monitoring, automatically downgrading back to low-power standby if stationary.
* **Elevation Timeout & Indoor Failsafe:** Added automatic GPS elevation timeouts and indoor accuracy guards so high-accuracy location never drains battery indefinitely when indoors.
* **Graduated Movement Sensitivity:** Movement Sensitivity is now enabled by default and out of experimental status with user-configurable wake-up radiuses.

---

# 🎨 UI & Design
* **Color Picker Reset Icon:** Replaced the sparkling icon in the custom color picker with a recognizable reset/default icon for Material You / system colors.

---

# 🛠️ Fixes & Reliability
* **App Bundle Language Retention:** Disabled language splitting in app bundles to preserve all offline fallback translations across languages.
* **Inspection & Code Cleanliness:** Addressed lint checks, simplified call chains, and removed obsolete string resources.

---

## [2.7.0] - 2026-09-23

This release brings powerful new workflow enhancements to make managing your trips faster and more resilient than ever, including one-tap batch approvals and battery shutdown protection.

---

# 🚀 Features
* **Accept All Review Trips:** Approve all pending review trips at once directly from the top bar with a clean confirmation sheet.
* **Bulk Edit Selected Trips:** Select multiple logged trips to update their classification, vehicle, or description simultaneously with selective toggles.
* **Trip Merge:** Combine multiple consecutive legs into a single trip with combined route and distance.

---

# 🛡️ Reliability
* **Shutdown Protection & Trip Recovery:** If your phone runs out of battery or shuts down while driving, your active trip is safely cached and restored for review upon restarting.
* **Interrupted Trip Destination Editing:** Easily adjust the destination and recalculate route distance for any interrupted trip directly from the Review card.
* **Optimized Battery Life:** Movement Sensitivity is now enabled by default, putting GPS to sleep when stationary to significantly cut battery consumption.

---

# 🎨 UI & Design
* **Consistent Dialog Actions:** Unified circular accept and decline buttons across all confirmation dialogs.
* **Visual Polish:** Improved contrast, typography, and layout responsiveness across tracking and review cards.

---

# 🛠️ Fixes & Improvements
* **Odometer Chain of Trust:** Robust cascade handling when editing past odometer values or toggling tracking modes.
* **Localization Updates:** Full translations updated across English, German, French, and Italian.

---
