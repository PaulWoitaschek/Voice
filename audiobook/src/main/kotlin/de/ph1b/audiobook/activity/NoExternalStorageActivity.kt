package de.ph1b.audiobook.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import de.ph1b.audiobook.R

class NoExternalStorageActivity : AppCompatActivity() {


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_external)

        val toolbar = findViewById(R.id.toolbar) as Toolbar

        setSupportActionBar(toolbar)
        supportActionBar.setTitle(R.string.no_external_storage_action_bar_title)
        supportActionBar.setDisplayHomeAsUpEnabled(false)
    }

    override fun onBackPressed() {
        if (BaseActivity.storageMounted()) {
            super.onBackPressed()
        } else {
            val i = Intent(Intent.ACTION_MAIN)
            i.addCategory(Intent.CATEGORY_HOME)
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
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
