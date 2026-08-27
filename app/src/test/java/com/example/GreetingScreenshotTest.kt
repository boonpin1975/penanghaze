package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.location.PenangStationsData
import com.example.data.model.AirQualityReading
import com.example.data.model.HazeLevel
import com.example.data.model.LocationSource
import com.example.ui.components.HazeGauge
import com.example.ui.theme.PenangHazeTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun haze_gauge_screenshot() {
    val sampleReading = AirQualityReading(
      station = PenangStationsData.STATIONS[0],
      apiValue = 68,
      hazeLevel = HazeLevel.MODERATE,
      pm25 = 24.5,
      pm10 = 48.0,
      o3 = 36.0,
      no2 = 18.0,
      so2 = 6.0,
      co = 0.8,
      temperature = 30.5,
      humidity = 76,
      windSpeedKmH = 14.2,
      windDirection = "SSW 195°",
      locationSource = LocationSource.GPS,
      networkPointDetail = "GPS Lock (±4.2m accuracy)",
      distanceKm = 0.0
    )

    composeTestRule.setContent {
      PenangHazeTheme(darkTheme = true) {
        HazeGauge(reading = sampleReading)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
