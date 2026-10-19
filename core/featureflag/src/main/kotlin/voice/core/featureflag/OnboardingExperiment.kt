package voice.core.featureflag

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

enum class OnboardingVariant(val key: String) {
  Control("control"),
  Guided("guided"),
}

@SingleIn(AppScope::class)
@Inject
class OnboardingExperiment(
  @OnboardingVariantFeatureFlagQualifier
  private val flag: FeatureFlag<String>,
) {

  private var started: OnboardingVariant? = null

  // on the first start, the remote config can arrive halfway through, which mustn't switch the flow
  val variant: OnboardingVariant get() = started ?: read()

  fun start(): OnboardingVariant = read().also { started = it }

  private fun read(): OnboardingVariant {
    val key = flag.get()
    return OnboardingVariant.entries.firstOrNull { it.key == key } ?: OnboardingVariant.Control
  }
}
