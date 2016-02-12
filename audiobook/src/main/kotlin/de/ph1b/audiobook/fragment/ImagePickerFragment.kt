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

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import butterknife.bindView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.actionBar
import de.ph1b.audiobook.dialog.EditCoverDialogFragment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.uitools.setGone
import de.ph1b.audiobook.uitools.setVisible
import de.ph1b.audiobook.utils.BookVendor
import okhttp3.HttpUrl
import timber.log.Timber
import java.net.URLEncoder
import javax.inject.Inject

/**
 * todo
 */
class ImagePickerFragment : Fragment(), EditCoverDialogFragment.Callback {

    init {
        App.component().inject(this)
    }

    @Inject internal lateinit var bookVendor: BookVendor


    private val webView: WebView by  bindView(R.id.webView)
    private val progressBar: View by  bindView(R.id.progressBar)
    private val callback by lazy { context as Callback }

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

        val id = arguments.getLong(NI_ID)
        val book = bookVendor.byId(id)!!

        val encodedSearch = URLEncoder.encode("${book.name} cover", Charsets.UTF_8.name())
        webView.loadUrl("https://www.google.com/search?safe=on&site=imghp&tbm=isch&q=$encodedSearch")
        webView.settings.javaScriptEnabled = true
        webView.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val httpUrl = HttpUrl.parse(url);
                val values = httpUrl.queryParameterValues("imgurl")
                if (values.isNotEmpty()) {
                    Timber.i("img url values are $values")
                    val first = values.first()
                    val editCover = EditCoverDialogFragment.newInstance(this@ImagePickerFragment, book, first)
                    editCover.show(fragmentManager, FM_EDIT_COVER)
                    return true
                }
                return super.shouldOverrideUrlLoading(view, url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // sets progressbar and webviews visibilities correctly once the page is loaded
                progressBar.setGone()
                webView.setVisible()
            }
        });
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.image_picker, menu)
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
        private val FM_EDIT_COVER = TAG + EditCoverDialogFragment.TAG
        const val NI_ID = "niName"

        fun newInstance(book: Book) = ImagePickerFragment().apply {
            arguments = Bundle().apply { putLong(NI_ID, book.id) }
        }
    }
}