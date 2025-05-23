package voice.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class VoiceJUnitRunner : AndroidJUnitRunner() {

  override fun newApplication(
    cl: ClassLoader?,
    className: String?,
    context: Context?,
  ): Application {
    return super.newApplication(cl, TestApp::class.java.name, context)
  }
}
