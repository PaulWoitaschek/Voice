package de.ph1b.audiobook.features.imagepicker

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.view.MenuItemCompat
import android.support.v7.view.ActionMode
import android.support.v7.widget.Toolbar
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import com.squareup.picasso.Picasso
import d
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.find
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.uitools.ImageHelper
import de.ph1b.audiobook.uitools.visible
import i
import io.reactivex.subjects.BehaviorSubject
import java.net.URLEncoder
import javax.inject.Inject

/**
 * Hosts the image picker.
 */
class ImagePickerController(bundle: Bundle) : BaseController(bundle) {

  constructor(book: Book) : this(Bundle().apply {
    putLong(NI_BOOK_ID, book.id)
  })

  init {
    App.component.inject(this)
  }

  @Inject lateinit var repo: BookRepository
  @Inject lateinit var imageHelper: ImageHelper

  private var actionMode: ActionMode? = null
  private lateinit var cropOverlay: CropOverlay
  private lateinit var webViewContainer: View
  private lateinit var webView: WebView
  private lateinit var fab: FloatingActionButton
  private lateinit var toolbar: Toolbar

  private val actionModeCallback = object : ActionMode.Callback {
    override fun onPrepareActionMode(p0: ActionMode?, menu: Menu?): Boolean {
      return false
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
      if (item?.itemId == R.id.confirm) {
        // obtain screenshot
        val cropRect = cropOverlay.selectedRect
        cropOverlay.selectionOn = false

        webViewContainer.isDrawingCacheEnabled = true
        webViewContainer.buildDrawingCache()
        val cache: Bitmap = webViewContainer.drawingCache
        val screenShot = Bitmap.createBitmap(cache, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
        webViewContainer.isDrawingCacheEnabled = false
        cache.recycle()

        // save screenshot
        imageHelper.saveCover(screenShot, book.coverFile())
        screenShot.recycle()
        Picasso.with(activity).invalidate(book.coverFile())
        actionMode?.finish()
        router.popCurrentController()
        return true
      }
      return false
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
      activity.menuInflater.inflate(R.menu.crop_menu, menu)
      return true
    }

    override fun onDestroyActionMode(p0: ActionMode?) {
      cropOverlay.selectionOn = false

      // unset the margin needed for the image crop again
      (webView.layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(0, 0, 0, 0)
      fab.show()
    }
  }

  private var webViewIsLoading = BehaviorSubject.createDefault(false)
  private val book by lazy {
    val id = bundle.getLong(NI_BOOK_ID)
    repo.bookById(id)!!
  }
  private val originalUrl by lazy {
    val encodedSearch = URLEncoder.encode("${book.name} cover", Charsets.UTF_8.name())
    "https://www.google.com/search?safe=on&site=imghp&tbm=isch&tbs=isz:lt,islt:qsvga&q=$encodedSearch"
  }

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    val view = inflater.inflate(R.layout.activity_image_picker, container, false)

    val progressBar = view.findViewById(R.id.progressBar)
    val noNetwork = view.findViewById(R.id.noNetwork)
    cropOverlay = view.find(R.id.cropOverlay)
    webViewContainer = view.find(R.id.webViewContainer)
    webView = view.find(R.id.webView)
    fab = view.find(R.id.fab)
    toolbar = view.find(R.id.toolbar)

    with(webView.settings) {
      setSupportZoom(true)
      builtInZoomControls = true
      displayZoomControls = false
      javaScriptEnabled = true
      userAgentString = "Mozilla/5.0 (Linux; U; Android 4.4; en-us; Nexus 4 Build/JOP24G) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"
    }
    // necessary, else the image capturing does not include the web view. Very performance costly.
    webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    webView.setWebViewClient(object : WebViewClient() {

      override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        i { "page started with $url" }
        webViewIsLoading.onNext(true)
      }

      override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        i { "page stopped with $url" }
        webViewIsLoading.onNext(false)
      }

      @Suppress("OverridingDeprecatedMember")
      override fun onReceivedError(view: WebView, errorCode: Int, description: String?, failingUrl: String?) {
        d { "received webViewError. Set webVeiw invisible" }
        view.loadUrl(ABOUT_BLANK)
        progressBar.visible = false
        noNetwork.visible = true
        webViewContainer.visible = false
      }
    })

    // after first successful load set visibilities
    webViewIsLoading
        .distinctUntilChanged()
        .filter { it == true }
        .subscribe {
          // sets progressbar and webviews visibilities correctly once the page is loaded
          i { "WebView is now loading. Set webView visible" }
          progressBar.visible = false
          noNetwork.visible = false
          webViewContainer.visible = true
        }

    webView.loadUrl(originalUrl)

    fab.setOnClickListener {
      cropOverlay.selectionOn = true

      // set the margin needed for the image crop
      val margin = webView.context.resources.getDimensionPixelOffset(R.dimen.half_circle_size)
      (webView.layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(margin, margin, margin, margin)
      actionMode = activity.startSupportActionMode(actionModeCallback)
      fab.hide()
    }

    setupToolbar()

    return view
  }

  private fun setupToolbar() {
    // necessary, else the action mode will be themed wrongly
    activity.setSupportActionBar(toolbar)

    toolbar.setTitle(R.string.cover)

    toolbar.setNavigationIcon(R.drawable.close)
    toolbar.setNavigationOnClickListener { activity.onBackPressed() }

    toolbar.inflateMenu(R.menu.image_picker)
    toolbar.setOnMenuItemClickListener {
      when (it.itemId) {
        R.id.refresh -> {
          webView.reload()
          true
        }
        R.id.home -> {
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
    val rotateView = layoutInflater().inflate(R.layout.rotate_view, null).apply {
      animation = rotation
      setOnClickListener { webView.reload() }
    }
    MenuItemCompat.setActionView(refreshItem, rotateView)

    webViewIsLoading
        .filter { it == true }
        .filter { !rotation.hasStarted() }
        .doOnNext { i { "is loading. Start animation" } }
        .subscribe {
          rotation.start()
        }

    rotation.setAnimationListener(object : Animation.AnimationListener {
      override fun onAnimationRepeat(p0: Animation?) {
        if (webViewIsLoading.value == false) {
          i { "we are in the refresh round. cancel now." }
          rotation.cancel()
          rotation.reset()
        }
      }

      override fun onAnimationEnd(p0: Animation?) {
      }

      override fun onAnimationStart(p0: Animation?) {
      }
    })
  }

  override fun onRestoreViewState(view: View, savedViewState: Bundle) {
    // load the last page loaded or the original one of there is none
    val url: String? = savedViewState.getString(SI_URL)
    webView.loadUrl(url)
  }

  override fun handleBack(): Boolean {
    if (webView.canGoBack()) {
      webView.goBack()
      return true
    } else return false
  }

  override fun onSaveViewState(view: View, outState: Bundle) {
    if (webView.url != ABOUT_BLANK) {
      outState.putString(SI_URL, webView.url)
    }
  }

  companion object {

    private const val NI_BOOK_ID = "ni"
    private const val ABOUT_BLANK = "about:blank"
    private const val SI_URL = "savedUrl"
  }
}
