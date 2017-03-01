package de.ph1b.audiobook.misc

import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.RouterTransaction


// convenient way to setup a router transaction
fun Controller.asTransaction(
  pushChangeHandler: ControllerChangeHandler? = null,
  popChangeHandler: ControllerChangeHandler? = null
) = RouterTransaction.with(this).apply {
  pushChangeHandler?.let { pushChangeHandler(it) }
  popChangeHandler?.let { popChangeHandler(it) }
}