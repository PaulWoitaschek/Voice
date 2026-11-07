package voice.features.cover.api

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create
import voice.core.remoteconfig.api.RemoteConfig

@Qualifier annotation class CoverClient

@BindingContainer
@ContributesTo(AppScope::class)
object CoverModule {

  @Provides
  @SingleIn(AppScope::class)
  @CoverClient
  fun client(
    client: OkHttpClient,
    remoteConfig: RemoteConfig,
  ): OkHttpClient =
    client
      .newBuilder()
      .addInterceptor { chain ->
        chain.proceed(
          chain
            .request()
            .newBuilder()
            .header(
              name = "User-Agent",
              value = remoteConfig.string(key = "user_agent", defaultValue = "Mozilla/5.0"),
            )
            .build(),
        )
      }
      .build()

  @Provides
  @SingleIn(AppScope::class)
  fun internalApi(@CoverClient client: OkHttpClient): InternalCoverApi {
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
