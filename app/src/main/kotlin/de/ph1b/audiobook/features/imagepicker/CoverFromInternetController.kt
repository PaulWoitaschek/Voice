package de.ph1b.audiobook.features.imagepicker

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.afollestad.materialcab.attached.AttachedCab
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialcab.createCab
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.getBookId
import de.ph1b.audiobook.data.putBookId
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.databinding.ImagePickerBinding
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.conductor.popOrBack
import de.ph1b.audiobook.scanner.CoverSaver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import voice.common.conductor.ViewBindingController
import java.net.URLEncoder
import javax.inject.Inject

private const val NI_BOOK_ID = "ni"
private const val SI_URL = "savedUrl"

class CoverFromInternetController(bundle: Bundle) : ViewBindingController<ImagePickerBinding>(bundle, ImagePickerBinding::inflate) {

  constructor(bookId: Book.Id) : this(Bundle().apply {
    putBookId(NI_BOOK_ID, bookId)
  })

  init {
    appComponent.inject(this)
  }

  @Inject
  lateinit var repo: BookRepository

  @Inject
  lateinit var coverSaver: CoverSaver

  private var cab: AttachedCab? = null

  private val book by lazy {
    val id = bundle.getBookId(NI_BOOK_ID)!!
    runBlocking {
      repo.flow(id).first()!!
    }
  }
  private val originalUrl by lazy {
    val encodedSearch = URLEncoder.encode("${book.content.name} cover", Charsets.UTF_8.name())
    "https://www.google.com/search?safe=on&site=imghp" +
      "&tbm=isch&tbs=isz:lt,islt:qsvga&q=$encodedSearch"
  }

  @SuppressLint("SetJavaScriptEnabled")
  override fun ImagePickerBinding.onBindingCreated() {
    with(webView.settings) {
      setSupportZoom(true)
      builtInZoomControls = true
      displayZoomControls = false
      javaScriptEnabled = true
    }
    webView.webViewClient = object : WebViewClient() {

      @Suppress("OverridingDeprecatedMember")
      override fun onReceivedError(
        view: WebView,
        errorCode: Int,
        description: String?,
        failingUrl: String?
      ) {
        view.loadUrl(originalUrl)
      }
    }

    webView.loadUrl(originalUrl)

    fab.setOnClickListener {
      cropOverlay.selectionOn = true
      showCab()
      fab.hide()
    }

    setupToolbar()
  }

  private fun ImagePickerBinding.showCab() {
    cab = activity!!.createCab(R.id.cabStub) {
      menu(R.menu.crop_menu)
      closeDrawable(R.drawable.close)
      onSelection { item ->
        if (item.itemId == R.id.confirm) {
          lifecycleScope.launch {
            val bitmap = takeWebViewScreenshot()
            saveCover(bitmap)
            destroy()
            router.popCurrentController()
          }
          true
        } else {
          false
        }
      }
      onDestroy {
        cropOverlay.selectionOn = false
        fab.show()
        true
      }
      slideDown()
    }
  }

  @Suppress("DEPRECATION")
  private fun ImagePickerBinding.takeWebViewScreenshot(): Bitmap {
    webView.isDrawingCacheEnabled = true
    webView.buildDrawingCache()
    val drawingCache = webView.drawingCache
    val bitmap = drawingCache.copy(drawingCache.config, false)
    webView.isDrawingCacheEnabled = false
    return bitmap
  }

  private suspend fun ImagePickerBinding.saveCover(bitmap: Bitmap) {
    val cropRect = cropOverlay.selectedRect
    val left = cropRect.left
    val top = cropRect.top
    val width = cropRect.width()
    val height = cropRect.height()

    val screenShot = Bitmap.createBitmap(
      bitmap,
      left,
      top,
      width,
      height
    )
    bitmap.recycle()

    coverSaver.save(book.id, screenShot)
    screenShot.recycle()
  }

  @SuppressLint("InflateParams")
  private fun ImagePickerBinding.setupToolbar() {
    toolbar.setNavigationOnClickListener { popOrBack() }
    toolbar.setOnMenuItemClickListener {
      when (it.itemId) {
        R.id.reset -> {
          webView.loadUrl(originalUrl)
          true
        }
        R.id.refresh -> {
          webView.reload()
          true
        }
        else -> false
      }
    }
  }

  override fun onRestoreViewState(view: View, savedViewState: Bundle) {
    // load the last page loaded or the original one of there is none
    val url: String? = savedViewState.getString(SI_URL)
    if (url != null) {
      binding.webView.loadUrl(url)
    }
  }

  override fun handleBack(): Boolean {
    if (cab.destroy()) {
      return true
    }

    return false
  }

  override fun onSaveViewState(view: View, outState: Bundle) {
    outState.putString(SI_URL, binding.webView.url)
  }
}
