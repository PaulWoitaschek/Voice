package voice.app.features

import android.app.Activity
import android.content.Intent
import voice.app.features.bookOverview.EditCoverDialogController
import voice.common.BookId
import javax.inject.Inject
import javax.inject.Singleton

private const val REQUEST_CODE = 7

@Singleton
class GalleryPicker
@Inject constructor() {

  private var pickForBookId: BookId? = null

  fun pick(bookId: BookId, activity: Activity) {
    pickForBookId = bookId
    val intent = Intent(Intent.ACTION_PICK)
      .setType("image/*")
    activity.startActivityForResult(intent, REQUEST_CODE)
  }

  fun parse(requestCode: Int, resultCode: Int, data: Intent?): EditCoverDialogController.Arguments? {
    if (requestCode != REQUEST_CODE) {
      return null
    }
    return if (resultCode == Activity.RESULT_OK) {
      val imageUri = data?.data
      val bookId = pickForBookId
      if (imageUri == null || bookId == null) {
        null
      } else {
        EditCoverDialogController.Arguments(imageUri, bookId)
      }
    } else {
      null
    }
  }
}
