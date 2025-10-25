package voice.core.data.store

import dev.zacsweers.metro.Qualifier

@Qualifier
public annotation class OnboardingCompletedStore

@Qualifier
public annotation class CurrentBookStore

@Qualifier
public annotation class AutoRewindAmountStore

@Qualifier
public annotation class SeekTimeStore

@Qualifier
public annotation class SleepTimerPreferenceStore

@Qualifier
public annotation class GridModeStore

@Qualifier
public annotation class DarkThemeStore

@Qualifier
public annotation class FadeOutStore

@Qualifier
public annotation class AmountOfBatteryOptimizationRequestedStore

@Qualifier
public annotation class ReviewDialogShownStore

@Qualifier
public annotation class AnalyticsConsentStore
