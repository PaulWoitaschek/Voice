package voice.app

import android.app.Application
import androidx.datastore.core.DataStore
import com.squareup.anvil.annotations.MergeComponent
import dagger.BindsInstance
import dagger.Component
import voice.app.injection.AppComponent
import voice.common.AppScope
import voice.common.BookId
import voice.common.pref.CurrentBookStore
import voice.data.repo.BookContentRepo
import voice.data.repo.ChapterRepo
import voice.playback.PlayerController
import javax.inject.Singleton

@Singleton
@MergeComponent(
  scope = AppScope::class,
)
interface TestComponent : AppComponent {

  val playerController: PlayerController

  @get:CurrentBookStore
  val currentBookStore: DataStore<BookId?>

  val bookContentRepo: BookContentRepo

  val chapterRepo: ChapterRepo

  @Component.Factory
  interface Factory {
    fun create(@BindsInstance application: Application): TestComponent
  }

  companion object {
    fun factory(): Factory = DaggerTestComponent.factory()
  }
}
