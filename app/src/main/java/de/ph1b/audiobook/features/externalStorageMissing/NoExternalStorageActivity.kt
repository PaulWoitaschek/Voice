package de.ph1b.audiobook.features.externalStorageMissing

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.BaseActivity
import kotlinx.android.synthetic.main.activity_no_external.*
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

class NoExternalStorageActivity : AppCompatActivity() {

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_no_external)
    toolbar.setTitle(R.string.no_external_storage_action_bar_title)
  }

  override fun onBackPressed() {
    runBlocking {
      if (BaseActivity.storageMounted()) {
        super.onBackPressed()
      } else {
        val i = Intent(Intent.ACTION_MAIN)
        i.addCategory(Intent.CATEGORY_HOME)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(i)
      }
    }
  }

  public override fun onResume() {
    super.onResume()
    GlobalScope.launch(Dispatchers.Main) {
      if (BaseActivity.storageMounted()) {
        onBackPressed()
      }
    }
  }
}
