package voice.app.misc.conductor

import android.content.Context
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.asTransaction
import voice.app.uitools.VerticalChangeHandler

fun Controller.asVerticalChangeHandlerTransaction(): RouterTransaction {
  return asTransaction(VerticalChangeHandler(), VerticalChangeHandler())
}

val Controller.context: Context get() = activity!!
j
