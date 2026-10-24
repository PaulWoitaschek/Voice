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
public annotation class ThemeModeStore

@Qualifier
public annotation class ThemeColorSchemeStore

/** Whether the dark theme paints its background black instead of a dark tone. */
@Qualifier
public annotation class PureBlackStore

@Qualifier
public annotation class FadeOutStore

@Qualifier
public annotation class AmountOfBatteryOptimizationRequestedStore

@Qualifier
public annotation class ReviewPromptStateStore

@Qualifier
public annotation class FolderPickerMovedDialogShownStore

/**
 * Each survey URL the listener opened or dismissed, with the day it happened on as an epoch day.
 */
@Qualifier
public annotation class HandledSurveysStore

@Qualifier
public annotation class AnalyticsConsentStore

@Qualifier
public annotation class DeveloperMenuUnlockedStore

@Qualifier
public annotation class FeatureFlagOverridesStore

@Qualifier
public annotation class CoverSeedColorsStore

@Qualifier
public annotation class SupporterStatusStore

/**
 * Whether the bookmarks screen had the history open last, so it opens on that tab again. It starts
 * on the history.
 */
@Qualifier
public annotation class HistoryTabOpenStore

/**
 * The speed the listener last played at or picked, which a book that was never played starts at.
 */
@Qualifier
public annotation class LastPlaybackSpeedStore

@Qualifier
public annotation class LockScreenSliderStore
