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

package de.ph1b.audiobook.fragment

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.MenuItemCompat
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import butterknife.bindView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.actionBar
import de.ph1b.audiobook.dialog.EditCoverDialogFragment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.layoutInflater
import de.ph1b.audiobook.uitools.setInvisible
import de.ph1b.audiobook.uitools.setVisible
import de.ph1b.audiobook.utils.BookVendor
import i
import okhttp3.HttpUrl
import rx.subjects.BehaviorSubject

import java.io.Serializable
import java.net.URLEncoder
import javax.inject.Inject

/**
 * Shows a web view with covers and lets the user pick one.
 */
class ImagePickerFragment : Fragment(), EditCoverDialogFragment.Callback {

    init {
        App.component().inject(this)
    }

    @Inject internal lateinit var bookVendor: BookVendor

    private val webView: WebView by  bindView(R.id.webView)
    private val progressBar: View by  bindView(R.id.progressBar)
    private val noNetwork: View by bindView(R.id.noNetwork)
    private val callback by lazy { context as Callback }
    private var webViewIsLoading = BehaviorSubject.create(false)
    private val book by lazy {
        val args = arguments.getSerializable(NI) as Args
        bookVendor.byId(args.bookId)!!
    }
    private val originalUrl by lazy {
        val encodedSearch = URLEncoder.encode("${book.name} cover", Charsets.UTF_8.name())
        "https://www.google.com/search?safe=on&site=imghp&tbm=isch&q=$encodedSearch"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.image_picker, container, false);
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        actionBar().apply {
            setHomeAsUpIndicator(R.drawable.close)
            setDisplayHomeAsUpEnabled(true)
            title = ""
        }

        // load the last page loaded or the original one of there is none
        val toLoad = savedInstanceState?.getString(SI_URL) ?: originalUrl
        webView.loadUrl(toLoad)
        webView.settings.javaScriptEnabled = true
        webView.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val httpUrl = HttpUrl.parse(url);
                val values = httpUrl.queryParameterValues("imgurl")
                // intercept images
                if (values.isNotEmpty()) {
                    i { "img url values are $values" }
                    val first = values.first()
                    val editCover = EditCoverDialogFragment.newInstance(this@ImagePickerFragment, book, first)
                    editCover.show(fragmentManager, FM_EDIT_COVER)
                    return true
                } else {
                    return super.shouldOverrideUrlLoading(view, url)
                }
            }

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
                view.loadUrl(ABOUT_BLANK)
                progressBar.setInvisible()
                noNetwork.setVisible()
                webView.setInvisible()
            }
        });

        // after first successful load set visibilities
        webViewIsLoading
                .distinctUntilChanged()
                .filter { it == true }
                .subscribe {
                    // sets progressbar and webviews visibilities correctly once the page is loaded
                    progressBar.setInvisible()
                    noNetwork.setInvisible()
                    webView.setVisible()
                }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (webView.url != ABOUT_BLANK) {
            outState.putString(SI_URL, webView.url)
        }

        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.image_picker, menu)

        // set the rotating icon
        val refreshItem = menu.findItem(R.id.refresh)
        val rotation = AnimationUtils.loadAnimation(context, R.anim.rotate).apply {
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
                if (webViewIsLoading.value == false ) {
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

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            callback.editDone()
            true
        }
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

    fun backPressed(): Boolean {
        if (webView.canGoBack()) {
            webView.goBack()
            return true
        } else {
            return false
        }
    }

    override fun editBookFinished() {
        callback.editDone()
    }

    interface Callback {
        fun editDone()
    }

    companion object {

        val TAG = ImagePickerFragment::class.java.simpleName

        private val NI = "ni"
        private val ABOUT_BLANK = "about:blank"
        private val SI_URL = "savedUrl"
        private val FM_EDIT_COVER = TAG + EditCoverDialogFragment.TAG

        fun newInstance(args: Args) = ImagePickerFragment().apply {
            arguments = Bundle().apply { putSerializable(NI, args) }
        }

        data class Args(val bookId: Long) : Serializable
    }
}