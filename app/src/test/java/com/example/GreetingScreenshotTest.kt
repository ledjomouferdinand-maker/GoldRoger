package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserWallet
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.TaskFlowTheme
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
    composeTestRule.setContent {
      TaskFlowTheme {
        HomeScreen(
          wallet = UserWallet(
            id = 1,
            creditsBalance = 42,
            subscriptionTier = SubscriptionTier.PRO,
            totalTasksCompleted = 12,
            totalTimeSavedMinutes = 480,
            totalSpentEuros = 98.0
          ),
          tasks = emptyList(),
          onSelectModule = {},
          onOpenWallet = {},
          onOpenAdmin = {},
          onExecuteQuickTask = { _, _, _, _ -> }
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
