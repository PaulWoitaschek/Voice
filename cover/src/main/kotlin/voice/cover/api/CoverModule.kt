package voice.cover.api

import dev.zacsweers.metro.ContributesTo
import dagger.Module
import dagger.Provides
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create
import voice.common.AppScope
import voice.remoteconfig.core.RemoteConfig
import javax.inject.Singleton

@ContributesTo(AppScope::class)
@Module
object CoverModule {

  @Provides
  @Singleton
  fun client(remoteConfig: RemoteConfig): OkHttpClient = OkHttpClient.Builder()
    .addInterceptor { chain ->
      chain.proceed(
        chain.request()
          .newBuilder()
          .addHeader(
            name = "User-Agent",
            value = remoteConfig.string(key = "user_agent", defaultValue = "Mozilla/5.0"),
          )
          .build(),
      )
    }
    .build()

  @Provides
  @Singleton
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
