# 🌫️ Penang Haze Tracker (Android)

A real-time Air Pollutant Index (API / AQI) monitoring and haze tracking Android application engineered specifically for the state of **Penang, Malaysia** (Penang Island & Seberang Perai). Built using **Kotlin** and **Jetpack Compose (Material 3)**.

---

## 🌟 Key Features

### ☀️ 1. Clean White Light Theme by Default
- **Pristine Visual Hierarchy**: Designed with a clean, high-contrast light theme canvas by default, paired with crisp typography, generous whitespace, and responsive Material 3 surface elevation.
- **Instant Dark Mode Toggle**: Quick one-tap switch in the top app bar for users preferring a night/dark mode palette.

### 📍 2. Multi-Source Location & Real-Time Monitoring
- **GPS, Wi-Fi & 5G Network Detection**: Automatically pinpoints the user's nearest monitoring station across Penang Island (*Minden/USM, Balik Pulau, George Town Heritage, Batu Ferringhi, Bayan Lepas Industrial, Teluk Bahang*) and Seberang Perai Mainland (*Seberang Jaya, Prai Industrial, Bukit Mertajam, Nibong Tebal, Butterworth Container Terminal*).
- **Sub-Pollutant Breakdown**: Displays real-time concentrations for **PM2.5**, **PM10**, **O₃** (Ozone), **NO₂** (Nitrogen Dioxide), **SO₂** (Sulfur Dioxide), and **CO** (Carbon Monoxide).
- **Meteorological Context**: Shows ambient temperature, relative humidity, wind speed, wind direction (e.g. Southwest Monsoon transboundary haze trajectories), and atmospheric visibility.

### 📡 3. Dedicated Stations Sensor Radar
- **State-Wide KPI Overview**: Instant metrics displaying State Average API, Cleanest Zone, and Peak Zone.
- **District Categorization & Live Search**: Fast filter chips for *All (11)*, *Penang Island (6)*, and *Mainland (5)*, with a real-time search field for station and landmark queries.
- **Interactive Station Cards**: Each station card highlights the live API badge, AQI category, progress bar, and expandable actions:
  - **Set Active Zone**: Switch active monitoring zone instantly.
  - **View History**: Jump straight to historical data and trend charts.
  - **Google Maps Navigation**: One-tap intent to launch external Google Maps for exact navigation coordinates.

### 📈 4. Historical Trends & Interactive Scrubber Charts
- **Flexible Timeframes**: Analyze air quality trends across **7 Days**, **30 Days**, **3 Months**, **1 Year**, or a **Custom Date Range**.
- **Interactive Scrubber Chart**: Smooth gradient area charts with horizontal customizable threshold guides and interactive touch inspection displaying exact date, API reading, and AQI classification.
- **Key Summary Metrics**: Period average API, Peak Pollution Day, Cleanest Day, and total recorded days.
- **Classification Distribution**: Visual stacked breakdown categorized under Good, Moderate, and Unhealthy air quality.
- **Daily Observation Logs**: Detailed chronological logs with daily averages, min/max ranges, and particulate concentrations.

### ⚙️ 5. Customizable Alert Thresholds & Push Notifications
- **Personalized AQI Tier Sliders**: Custom cutoff controls for **Good** ($0-X$), **Moderate** ($X+1-Y$), and **Unhealthy** ($Y+1-Z$) limits.
- **Dynamic Color Spectrum**: Real-time visual spectrum reflecting custom boundary adjustments.
- **Granular Triggers**: Individual toggles for Moderate, Unhealthy, and Hazardous alert notifications.
- **Instant System Alerts**: Android notification channels dispatch alerts when local stations exceed threshold limits.
- **Simulation & Testing Tools**: Built-in spike triggers to test notification handling under simulated transboundary haze conditions.

### 🩺 6. Personalized Health Guidance & Protocols
- **Risk Profiles**: Tailored health advice for General Public, Sensitive Groups, Children/Elderly, Outdoor Athletes, and Respiratory Patients.
- **Actionable Precautions**: Mask recommendations (N95 / KF94), HEPA air purifier guidance, outdoor activity limits, and indoor window sealing advisories.

---

## 🛠️ Technology Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/) (100%)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3)
- **Architecture**: MVVM (Model-View-ViewModel) + Single Source of Truth with Kotlin Coroutines & StateFlow
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
│   │   ├── CardContainer.kt       # Reusable Material 3 elevated card container
│   │   ├── HazeGauge.kt           # Radial arc gauge & AQI level indicators
│   │   ├── HazeTrendChart.kt      # Historical interactive trend scrubber chart
│   │   └── PollutantGrid.kt       # Grid cards for PM2.5, PM10, O3, NO2, SO2, CO
│   ├── screens/
│   │   ├── DashboardScreen.kt     # Main air quality overview & quick stats
│   │   ├── StationsScreen.kt      # Dedicated 11-station radar with search, filters & KPIs
│   │   ├── HistoryScreen.kt       # 365-day trend analytics & scrubber chart
│   │   ├── HealthScreen.kt        # Personalized health recommendations
│   │   └── AlertsForecastScreen.kt# Custom threshold sliders & notification setup
│   └── theme/
│       ├── Color.kt               # Environmental color palette (Good, Mod, Unhealthy)
│       └── Theme.kt               # Dynamic light/dark Material 3 theming (White default)
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
