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
import com.bluelinelabs.conductor.Controller
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.R
import de.ph1b.audiobook.common.ImageHelper
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.features.SyntheticViewController
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.conductor.popOrBack
import de.ph1b.audiobook.misc.coverFile
import de.ph1b.audiobook.misc.getUUID
import de.ph1b.audiobook.misc.putUUID
import kotlinx.android.synthetic.main.image_picker.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Inject

class CoverFromInternetController(bundle: Bundle) : SyntheticViewController(bundle) {

  init {
    appComponent.inject(this)
  }

  @Inject
  lateinit var repo: BookRepository
  @Inject
  lateinit var imageHelper: ImageHelper

  private var cab: AttachedCab? = null

  private var webViewIsLoading = ConflatedBroadcastChannel(false)
  private val book by lazy {
    val id = bundle.getUUID(NI_BOOK_ID)
    repo.bookByIdBlocking(id)!!
  }
  private val originalUrl by lazy {
    val encodedSearch = URLEncoder.encode("${book.name} cover", Charsets.UTF_8.name())
    "https://www.google.com/search?safe=on&site=imghp" +
      "&tbm=isch&tbs=isz:lt,islt:qsvga&q=$encodedSearch"
  }

  override val layoutRes = R.layout.image_picker

  @SuppressLint("SetJavaScriptEnabled")
  override fun onViewCreated() {
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
        webViewIsLoading.offer(true)
      }

      override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        Timber.i("page stopped with $url")
        webViewIsLoading.offer(false)
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
      webViewIsLoading.asFlow()
        .distinctUntilChanged()
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

  private fun showCab() {
    cab = activity!!.createCab(R.id.cabStub) {
      menu(R.menu.crop_menu)
      closeDrawable(R.drawable.close)
      onSelection { item ->
        if (item.itemId == R.id.confirm) {
          val bitmap = takeWebViewScreenshot()
          saveCover(bitmap)
          this.destroy()
          router.popCurrentController()
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
  private fun takeWebViewScreenshot(): Bitmap {
    webView.isDrawingCacheEnabled = true
    webView.buildDrawingCache()
    val drawingCache = webView.drawingCache
    val bitmap = drawingCache.copy(drawingCache.config, false)
    webView.isDrawingCacheEnabled = false
    return bitmap
  }

  private fun saveCover(bitmap: Bitmap) {
    val cropRect = cropOverlay.selectedRect
    val left = cropRect.left
    val top = cropRect.top
    val width = cropRect.width()
    val height = cropRect.height()

    GlobalScope.launch {
      val screenShot = Bitmap.createBitmap(
        bitmap,
        left,
        top,
        width,
        height
      )
      bitmap.recycle()
      val coverFile = book.coverFile()
      imageHelper.saveCover(screenShot, coverFile)
      screenShot.recycle()
      Picasso.get().invalidate(coverFile)
      val targetController = targetController
      withContext(Dispatchers.Main) {
        if (targetController?.isAttached == true) {
          targetController as Callback
          targetController.onBookCoverChanged(book.id)
        }
      }
    }
  }

  @SuppressLint("InflateParams")
  private fun setupToolbar() {
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
      webViewIsLoading.asFlow()
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
    webView.loadUrl(url)
  }

  override fun handleBack(): Boolean {
    if (cab.destroy()) {
      return true
    }

    if (noNetwork.isVisible) {
      return false
    }

    if (webView.canGoBack()) {
      webView.goBack()
      return true
    }

    return false
  }

  override fun onSaveViewState(view: View, outState: Bundle) {
    if (webView.url != ABOUT_BLANK) {
      outState.putString(SI_URL, webView.url)
    }
  }

  companion object {

    operator fun <T> invoke(
      bookId: UUID,
      target: T
    ): CoverFromInternetController where T : Controller, T : Callback {
      val args = Bundle().apply {
        putUUID(NI_BOOK_ID, bookId)
      }
      return CoverFromInternetController(args).apply {
        targetController = target
      }
    }

    private const val NI_BOOK_ID = "ni"
    private const val ABOUT_BLANK = "about:blank"
    private const val SI_URL = "savedUrl"
  }

  interface Callback {
    fun onBookCoverChanged(bookId: UUID)
  }
}
