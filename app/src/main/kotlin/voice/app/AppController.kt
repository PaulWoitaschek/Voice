package voice.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import kotlinx.coroutines.launch
import voice.app.features.GalleryPicker
import voice.app.features.bookOverview.EditCoverDialogController
import voice.app.features.imagepicker.CoverFromInternetController
import voice.app.injection.appComponent
import voice.app.misc.conductor.asTransaction
import voice.bookOverview.views.BookOverviewScreen
import voice.common.compose.ComposeController
import voice.common.navigation.Screen
import voice.common.pref.CurrentBook
import voice.data.Book
import voice.folderPicker.FolderPicker
import voice.logging.core.Logger
import voice.migration.views.Migration
import voice.playbackScreen.BookPlayController
import voice.settings.views.Settings
import javax.inject.Inject

class AppController : ComposeController() {

  init {
    appComponent.inject(this)
  }

  @field:[Inject CurrentBook]
  lateinit var currentBookIdPref: DataStore<Book.Id?>

  @Inject
  lateinit var galleryPicker: GalleryPicker

  @Composable
  override fun Content() {
    val navController = rememberAnimatedNavController()
    AnimatedNavHost(navController = navController, startDestination = Screen.BookOverview.route) {
      composable(Screen.BookOverview.route) {
        BookOverviewScreen(
          onSettingsClick = {
            navController.navigate(Screen.Settings.route)
          },
          onBookMigrationClick = {
            navController.navigate(Screen.Migration.route)
          },
          toFolderOverview = {
            navController.navigate(Screen.FolderPicker.route)
          },
          toBook = { bookId ->
            lifecycleScope.launch {
              currentBookIdPref.updateData { bookId }
              router.pushController(BookPlayController(bookId).asTransaction())
            }
          },
          onCoverFromInternetClick = { bookId ->
            router.pushController(
              CoverFromInternetController(bookId)
                .asTransaction()
            )
          },
        ) { bookId ->
          galleryPicker.pick(bookId, this@AppController)
        }
      }
      composable(Screen.Settings.route) {
        Settings(
          onCloseScreenClicked = {
            navController.popBackStack()
          },
          toSupport = {
            visitUri("https://github.com/PaulWoitaschek/Voice".toUri())
          },
          toTranslations = {
            visitUri("https://www.transifex.com/projects/p/voice".toUri())
          }
        )
      }
      composable(Screen.Migration.route) {
        Migration(
          onCloseClicked = {
            navController.popBackStack()
          }
        )
      }
      composable(Screen.FolderPicker.route) {
        FolderPicker(
          onCloseClick = {
            navController.popBackStack()
          }
        )
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    val arguments = galleryPicker.parse(requestCode, resultCode, data)
    if (arguments != null) {
      EditCoverDialogController(arguments).showDialog(router)
    }
  }

  private fun visitUri(uri: Uri) {
    try {
      startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (exception: ActivityNotFoundException) {
      Logger.w(exception)
    }
  }
}
