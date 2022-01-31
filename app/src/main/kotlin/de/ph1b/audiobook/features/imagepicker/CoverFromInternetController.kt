package de.ph1b.audiobook.features.imagepicker

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isVisible
import com.afollestad.materialcab.attached.AttachedCab
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialcab.createCab
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.getBookId
import de.ph1b.audiobook.data.putBookId
import de.ph1b.audiobook.data.repo.BookRepo2
import de.ph1b.audiobook.databinding.ImagePickerBinding
import de.ph1b.audiobook.features.CoverSaver
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.conductor.popOrBack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import voice.common.conductor.ViewBindingController
import java.net.URLEncoder
import javax.inject.Inject

private const val NI_BOOK_ID = "ni"
private const val ABOUT_BLANK = "about:blank"
private const val SI_URL = "savedUrl"

class CoverFromInternetController(bundle: Bundle) : ViewBindingController<ImagePickerBinding>(bundle, ImagePickerBinding::inflate) {

  constructor(bookId: Book2.Id) : this(Bundle().apply {
    putBookId(NI_BOOK_ID, bookId)
  })

  init {
    appComponent.inject(this)
  }

  @Inject
  lateinit var repo: BookRepo2

  @Inject
  lateinit var coverSaver: CoverSaver

  private var cab: AttachedCab? = null

  private var webViewIsLoading = MutableStateFlow(false)
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
      userAgentString =
        "Mozilla/5.0 (Linux; U; Android 4.4; en-us; Nexus 4 Build/JOP24G) " +
          "AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"
    }
    webView.webViewClient = object : WebViewClient() {

      override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        Timber.i("page started with $url")
        webViewIsLoading.value = true
      }

      override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        Timber.i("page stopped with $url")
        webViewIsLoading.value = false
      }

      @Suppress("OverridingDeprecatedMember")
      override fun onReceivedError(
        view: WebView,
        errorCode: Int,
        description: String?,
        failingUrl: String?
      ) {
        Timber.d("received webViewError. Set webView invisible")
        view.loadUrl(ABOUT_BLANK)
        progressBar.isVisible = false
        noNetwork.isVisible = true
        webViewContainer.isVisible = false
      }
    }

    // after first successful load set visibilities
    lifecycleScope.launch {
      webViewIsLoading
        .filter { it }
        .collect {
          // sets progressbar and webviews visibilities correctly once the page is loaded
          Timber.i("WebView is now loading. Set webView visible")
          progressBar.isVisible = false
          noNetwork.isVisible = false
          webViewContainer.isVisible = true
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
        else -> false
      }
    }

    // set the rotating icon
    val menu = toolbar.menu
    val refreshItem = menu.findItem(R.id.refresh)
    val rotation = AnimationUtils.loadAnimation(activity, R.anim.rotate).apply {
      repeatCount = Animation.INFINITE
    }
    val rotateView = LayoutInflater.from(activity).inflate(R.layout.rotate_view, null).apply {
      animation = rotation
      setOnClickListener { webView.reload() }
    }
    rotateView.setOnClickListener {
      if (webView.url == ABOUT_BLANK) {
        webView.loadUrl(originalUrl)
      } else webView.reload()
    }
    refreshItem.actionView = rotateView

    lifecycleScope.launch {
      webViewIsLoading
        .filter { it }
        .filter { !rotation.hasStarted() }
        .collect {
          Timber.i("is loading. Start animation")
          rotation.start()
        }
    }

    rotation.setAnimationListener(
      object : Animation.AnimationListener {
        override fun onAnimationRepeat(p0: Animation?) {
          if (!webViewIsLoading.value) {
            Timber.i("we are in the refresh round. cancel now.")
            rotation.cancel()
            rotation.reset()
          }
        }

        override fun onAnimationEnd(p0: Animation?) {
        }

        override fun onAnimationStart(p0: Animation?) {
        }
      }
    )
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

    if (binding.noNetwork.isVisible) {
      return false
    }

    if (binding.webView.canGoBack()) {
      binding.webView.goBack()
      return true
    }

    return false
  }

  override fun onSaveViewState(view: View, outState: Bundle) {
    if (binding.webView.url != ABOUT_BLANK) {
      outState.putString(SI_URL, binding.webView.url)
    }
  }
}
