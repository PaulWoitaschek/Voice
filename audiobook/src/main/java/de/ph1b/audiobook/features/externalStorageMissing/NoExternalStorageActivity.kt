package de.ph1b.audiobook.features.externalStorageMissing

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.BaseActivity
import de.ph1b.audiobook.misc.find

class NoExternalStorageActivity : AppCompatActivity() {

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_no_external)

    val toolbar = find<Toolbar>(R.id.toolbarInclude)
    toolbar.setTitle(R.string.no_external_storage_action_bar_title)
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
