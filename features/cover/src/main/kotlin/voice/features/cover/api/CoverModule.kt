package voice.features.cover.api

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.UserAgentFeatureFlagQualifier

@ContributesTo(AppScope::class)
@BindingContainer
object CoverModule {

  @Provides
  @SingleIn(AppScope::class)
  fun client(
    @UserAgentFeatureFlagQualifier
    userAgent: FeatureFlag<String>,
  ): OkHttpClient = OkHttpClient.Builder()
    .addInterceptor { chain ->
      chain.proceed(
        chain.request()
          .newBuilder()
          .addHeader(
            name = "User-Agent",
            value = userAgent.get(),
          )
          .build(),
      )
    }
    .build()

  @Provides
  @SingleIn(AppScope::class)
  fun internalApi(client: OkHttpClient): InternalCoverApi {
    val json = Json {
      ignoreUnknownKeys = true
    }
    return Retrofit.Builder()
      .addConverterFactory(ScalarsConverterFactory.create())
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .baseUrl("https://duckduckgo.com/")
      .client(client)
      .build()
      .create()
  }
}
