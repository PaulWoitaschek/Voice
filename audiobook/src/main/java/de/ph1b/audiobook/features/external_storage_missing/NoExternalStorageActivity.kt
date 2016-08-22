package de.ph1b.audiobook.features.external_storage_missing

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.BaseActivity
import de.ph1b.audiobook.misc.setupActionbar
import kotlinx.android.synthetic.main.activity_no_external.*
import kotlinx.android.synthetic.main.toolbar.view.*

class NoExternalStorageActivity : AppCompatActivity() {


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_external)

        setupActionbar(toolbar = toolbarInclude.toolbar, titleRes = R.string.no_external_storage_action_bar_title, homeAsUpEnabled = false)
    }

    override fun onBackPressed() {
        if (BaseActivity.storageMounted()) {
            super.onBackPressed()
        } else {
            val i = Intent(Intent.ACTION_MAIN)
            i.addCategory(Intent.CATEGORY_HOME)
            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(i)
        }
    }

    public override fun onResume() {
        super.onResume()
        if (BaseActivity.storageMounted()) {
            onBackPressed()
        }
    }
}
