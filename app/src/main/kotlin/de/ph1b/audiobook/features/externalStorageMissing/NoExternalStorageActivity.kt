package de.ph1b.audiobook.features.externalStorageMissing

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.ph1b.audiobook.R
import de.ph1b.audiobook.common.storageMounted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class NoExternalStorageActivity : AppCompatActivity() {

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_no_external)
  }

  override fun onBackPressed() {
    runBlocking {
      if (storageMounted()) {
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
      if (storageMounted()) {
        onBackPressed()
      }
    }
  }
}
