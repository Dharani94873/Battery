package com.example

import android.content.Context
import androidx.room.Room
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.data.BatteryDatabase
import com.example.data.BatteryRepository
import com.example.data.BatterySettingsManager
import com.example.ui.BatteryDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BatteryViewModel
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
  fun greeting_screenshot() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, BatteryDatabase::class.java).build()
    val settingsManager = BatterySettingsManager(context)
    val repository = BatteryRepository(database.batteryDao, settingsManager)
    val viewModel = BatteryViewModel(context, repository)

    composeTestRule.setContent {
      MyApplicationTheme {
        BatteryDashboardScreen(
          viewModel = viewModel,
          onToggleGuard = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    database.close()
  }
}
