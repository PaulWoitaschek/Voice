package voice.features.onboarding.welcome

import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.navigation.Destination
import voice.navigation.NavEntryProvider

@ContributesTo(AppScope::class)
interface OnboardingWelcomeProvider {

  val onboardingWelcomeViewModel: OnboardingWelcomeViewModel

  @Provides
  @IntoSet
  fun onboardingWelcomeNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.OnboardingWelcome> { key ->
    NavEntry(key) {
      OnboardingWelcome()
    }
  }
}
