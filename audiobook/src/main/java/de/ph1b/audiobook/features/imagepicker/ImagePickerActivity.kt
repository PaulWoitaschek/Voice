/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.features.imagepicker

import Slimber
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.view.MenuItemCompat
import android.support.v7.view.ActionMode
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import butterknife.bindView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.activity.BaseActivity
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.layoutInflater
import de.ph1b.audiobook.uitools.ImageHelper
import de.ph1b.audiobook.uitools.setInvisible
import de.ph1b.audiobook.uitools.setVisible
import de.ph1b.audiobook.utils.BookVendor
import rx.subjects.BehaviorSubject
import java.io.Serializable
import java.net.URLEncoder
import javax.inject.Inject

/**
 * Hosts the image picker.
 */
class ImagePickerActivity : BaseActivity() {

    init {
        App.component().inject(this)
    }

    @Inject internal lateinit var bookVendor: BookVendor
    @Inject internal lateinit var imageHelper: ImageHelper

    private val webView: WebView by  bindView(R.id.webView)
    private val progressBar: View by  bindView(R.id.progressBar)
    private val noNetwork: View by bindView(R.id.noNetwork)
    private val webViewContainer: View by bindView(R.id.webViewContainer)
    private val cropView: CropOverlay by bindView(R.id.cropOverlay)
    private val fab: FloatingActionButton by bindView(R.id.fab)

    private var actionMode: ActionMode ? = null

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onPrepareActionMode(p0: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean {
            if (p1?.itemId == R.id.confirm) {
                val b = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888);
                val c = Canvas(b);
                webView.layout(webView.left, webView.top, webView.right, webView.bottom);
                webView.draw(c);

                val cropRect = cropView.selectedRect
                val cropped = Bitmap.createBitmap(b, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())

                imageHelper.saveCover(cropped, book.coverFile())
                finish()
                return true
            }
            return false
        }

        override fun onCreateActionMode(p0: ActionMode?, menu: Menu?): Boolean {
            menuInflater.inflate(R.menu.crop_menu, menu)
            return true
        }

        override fun onDestroyActionMode(p0: ActionMode?) {
            cropView.selectionOn = false
            fab.show()
        }
    }

    private var webViewIsLoading = BehaviorSubject.create(false)
    private val book by lazy {
        val args = intent.getSerializableExtra(NI) as Args
        bookVendor.byId(args.bookId)!!
    }
    private val originalUrl by lazy {
        val encodedSearch = URLEncoder.encode("${book.name} cover", Charsets.UTF_8.name())
        "https://www.google.com/search?safe=on&site=imghp&tbm=isch&tbs=isz:lt,islt:qsvga&q=$encodedSearch"
    }

    private val toolBar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image_picker)
        setSupportActionBar(toolBar)
        supportActionBar!!.apply {
            setDisplayHomeAsUpEnabled(true)
            title = ""
        }

        with(webView.settings) {
            setSupportZoom(true);
            builtInZoomControls = true;
            displayZoomControls = false;
            javaScriptEnabled = true
            userAgentString = "Mozilla/5.0 (Linux; U; Android 4.4; en-us; Nexus 4 Build/JOP24G) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";
        }
        webView.setWebViewClient(object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                Slimber.i { "page started with $url" }
                webViewIsLoading.onNext(true)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                Slimber.i { "page stopped with $url" }
                webViewIsLoading.onNext(false)
            }

            @Suppress("OverridingDeprecatedMember")
            override fun onReceivedError(view: WebView, errorCode: Int, description: String?, failingUrl: String?) {
                Slimber.d { "received webViewError. Set webVeiw invisible" }
                view.loadUrl(ABOUT_BLANK)
                progressBar.setInvisible()
                noNetwork.setVisible()
                webViewContainer.setInvisible()
            }
        });

        // after first successful load set visibilities
        webViewIsLoading
                .distinctUntilChanged()
                .filter { it == true }
                .subscribe {
                    // sets progressbar and webviews visibilities correctly once the page is loaded
                    Slimber.i { "WebView is now loading. Set webView visible" }
                    progressBar.setInvisible()
                    noNetwork.setInvisible()
                    webViewContainer.setVisible()
                }

        // load the last page loaded or the original one of there is none
        val toLoad = savedInstanceState?.getString(SI_URL) ?: originalUrl
        webView.loadUrl(toLoad)

        fab.setOnClickListener {
            cropView.selectionOn = true
            actionMode = startSupportActionMode(actionModeCallback)
            fab.hide()
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (webView.url != ABOUT_BLANK) {
            outState.putString(SI_URL, webView.url)
        }

        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.image_picker, menu)

        // set the rotating icon
        val refreshItem = menu.findItem(R.id.refresh)
        val rotation = AnimationUtils.loadAnimation(this, R.anim.rotate).apply {
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
                .doOnNext { Slimber.i { "is loading. Start animation" } }
                .subscribe {
                    rotation.start()
                }

        rotation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(p0: Animation?) {
                if (webViewIsLoading.value == false ) {
                    Slimber.i { "we are in the refresh round. cancel now." }
                    rotation.cancel()
                    rotation.reset()
                }
            }

            override fun onAnimationEnd(p0: Animation?) {
            }

            override fun onAnimationStart(p0: Animation?) {
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        R.id.refresh      -> {
            webView.reload()
            true
        }
        R.id.home         -> {
            webView.loadUrl(originalUrl)
            true
        }
        else              -> false
    }

    companion object {

        // val TAG = ImagePickerActivity::class.java.simpleName

        private val NI = "ni"
        private val ABOUT_BLANK = "about:blank"
        private val SI_URL = "savedUrl"


        fun arguments(args: Args) = Bundle().apply {
            putSerializable(NI, args)
        }

    }

    data class Args(val bookId: Long) : Serializable
}
