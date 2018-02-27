package de.ph1b.audiobook.features.externalStorageMissing

import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.ActivityNoExternalBinding
import de.ph1b.audiobook.features.BaseActivity

class NoExternalStorageActivity : AppCompatActivity() {

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val binding =
      DataBindingUtil.setContentView<ActivityNoExternalBinding>(this, R.layout.activity_no_external)

    val toolbar = binding.toolbarInclude!!.toolbar
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
