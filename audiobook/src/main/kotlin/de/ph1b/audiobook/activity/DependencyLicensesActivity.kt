package de.ph1b.audiobook.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.utils.ResourceTypeWriter
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import javax.inject.Inject

/**
 * Activity displaying the used licenses.
 *
 * @author Paul Woitaschek
 */
class DependencyLicensesActivity : BaseActivity() {

    @Inject internal lateinit var resourceTypeWriter: ResourceTypeWriter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        App.component().inject(this)

        setContentView(R.layout.activity_dependency_licenses)

        val progressBar = findViewById(R.id.progressBar)

        // set home enabled for toolbar
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar.setDisplayHomeAsUpEnabled(true)

        val webView = findViewById(R.id.webView) as WebView
        webView.setWebViewClient(object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                // open external browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);

                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // sets progressbar and webviews visibilities correctly once the page is loaded
                progressBar.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }
        })

        // load content into webView
        resourceTypeWriter.rawToString(R.raw.dependency_licenses)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    webView.loadDataWithBaseURL(null, it, "text/html", "UTF-8", null)
                }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }
}