# 🌫️ Penang Haze Tracker (Android)

A real-time Air Pollutant Index (API / AQI) monitoring and haze tracking Android application engineered specifically for the state of **Penang, Malaysia** (Penang Island & Seberang Perai). Built using **Kotlin** and **Jetpack Compose (Material 3)**.

---

## 🌟 Key Features

### 📍 1. Multi-Source Location & Real-Time Monitoring
- **GPS, Wi-Fi & 5G Network Detection**: Automatically pinpoints the user's nearest monitoring station across Penang Island (*Minden/USM, Balik Pulau, George Town Heritage, Batu Ferringhi, Bayan Lepas Industrial, Teluk Bahang*) and Seberang Perai Mainland (*Seberang Jaya, Prai Industrial, Bukit Mertajam, Nibong Tebal, Butterworth Container Terminal*).
- **Sub-Pollutant Breakdown**: Displays real-time concentrations for **PM2.5**, **PM10**, **O₃** (Ozone), **NO₂** (Nitrogen Dioxide), **SO₂** (Sulfur Dioxide), and **CO** (Carbon Monoxide).
- **Meteorological Context**: Shows ambient temperature, relative humidity, wind speed, wind direction (e.g. Southwest Monsoon transboundary haze trajectories), and atmospheric visibility.

### 🗺️ 2. Interactive Map Panel (Google Maps & Multi-Layer Tiles)
- **Google Maps Engine**: Integrated Google Maps layers with instant switching between **Roadmap**, **Satellite (Hybrid)**, **Terrain**, and **OpenStreetMap (OSM)** fallback.
- **Color-Coded Station Markers**: 11 official and regional air quality stations with real-time numeric API badges and pulsing focus indicators.
- **Atmospheric Heatmap Dispersion**: Toggleable regional haze dispersion gradients reflecting localized air quality intensity.
- **Penang Infrastructure Overlays**: Visual routes for the **Penang 1st Bridge** (*Gelugor to Prai*) and **Penang 2nd Bridge** (*Sultan Abdul Halim Muadzam Shah Bridge*).
- **Quick Camera Actions**: Smooth gestures for Zoom In/Out, Re-center Penang, Focus on GPS Location, and one-tap opening in the native **Google Maps** app for navigation.

### 📈 3. Historical Trends & Interactive Scrubber Charts
- **Flexible Timeframes**: Analyze air quality trends across **7 Days**, **30 Days**, **3 Months**, **1 Year**, or a **Custom Date Range**.
- **Interactive Scrubber Chart**: Smooth gradient area charts with horizontal customizable threshold guides and interactive touch inspection displaying exact date, API reading, and AQI classification.
- **Key Summary Metrics**: Period average API, Peak Pollution Day, Cleanest Day, and total recorded days.
- **Classification Distribution**: Visual stacked breakdown categorized under Good, Moderate, and Unhealthy air quality.
- **Daily Observation Logs**: Detailed chronological logs with daily averages, min/max ranges, and particulate concentrations.

### ⚙️ 4. Customizable Alert Thresholds & Push Notifications
- **Personalized AQI Tier Sliders**: Custom cutoff controls for **Good** ($0-X$), **Moderate** ($X+1-Y$), and **Unhealthy** ($Y+1-Z$) limits.
- **Dynamic Color Spectrum**: Real-time visual spectrum reflecting custom boundary adjustments.
- **Granular Triggers**: Individual toggles for Moderate, Unhealthy, and Hazardous alert notifications.
- **Instant System Alerts**: Android notification channels dispatch alerts when local stations exceed threshold limits.
- **Simulation & Testing Tools**: Built-in spike triggers to test notification handling under simulated transboundary haze conditions.

### 🩺 5. Personalized Health Guidance & Protocols
- **Risk Profiles**: Tailored health advice for General Public, Sensitive Groups, Children/Elderly, Outdoor Athletes, and Respiratory Patients.
- **Actionable Precautions**: Mask recommendations (N95 / KF94), HEPA air purifier guidance, outdoor activity limits, and indoor window sealing advisories.

---

## 🛠️ Technology Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/) (100%)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3)
- **Architecture**: MVVM (Model-View-ViewModel) + Single Source of Truth with Kotlin Coroutines & StateFlow
- **Maps & Geospatial**: Interactive Leaflet / Google Maps WebView bridge with custom Touch Intercept disallow handling & Compose Fallback Canvas
- **Data Persistence**: Android SharedPreferences & Room Database integration
- **Testing**: Robolectric & Roborazzi unit/UI testing
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) with Version Catalog (`libs.versions.toml`)

---

## 📱 Project Structure

```
app/src/main/java/com/example/
├── data/
│   ├── model/
│   │   ├── HazeModels.kt          # HazeReading, PenangStation, HazeLevel, PollutantBreakdown
│   │   └── HealthProfile.kt       # User health profiles, risk tiers, advice
│   └── repository/
│       ├── HazeRepository.kt      # Real-time data feed & historical trend generator
│       └── LocationManager.kt     # GPS, Wi-Fi, and 5G network location resolver
├── ui/
│   ├── HazeViewModel.kt           # Central UI state management & notification triggers
│   ├── MainScreen.kt              # Scaffold, bottom navigation bar & screen routing
│   ├── components/
│   │   ├── GoogleMapPanelView.kt  # Google Maps & tile map panel with JS bridge
│   │   ├── HazeGauge.kt           # Radial arc gauge & AQI level indicators
│   │   ├── HazeTrendChart.kt      # Historical interactive trend scrubber chart
│   │   ├── PenangInteractiveMapView.kt # Custom Compose vector map fallback
│   │   └── PollutantGrid.kt       # Grid cards for PM2.5, PM10, O3, NO2, SO2, CO
│   ├── screens/
│   │   ├── DashboardScreen.kt     # Main air quality overview & quick stats
│   │   ├── PenangMapScreen.kt     # Interactive map view with station cards
│   │   ├── HistoryScreen.kt       # 365-day trend analytics & scrubber chart
│   │   ├── HealthScreen.kt        # Personalized health recommendations
│   │   └── AlertsForecastScreen.kt# Custom threshold sliders & notification setup
│   └── theme/
│       ├── Color.kt               # Environmental color palette (Good, Mod, Unhealthy)
│       └── Theme.kt               # Dynamic light/dark Material 3 theming
```

---

## 🚀 Getting Started & Build Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK (API Level 26 minimum, API 34 target)

### Building the Application
1. Clone this repository:
   ```bash
   git clone <REPO_URL>
   cd penang-haze-tracker
   ```
2. Build the debug APK:
   ```bash
   gradle :app:assembleDebug
   ```
3. Run local unit & Robolectric tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```

---

## 📄 License
Distributed under the Apache 2.0 License. See `LICENSE` for more information.
