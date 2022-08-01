package voice.app.misc.conductor

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler

/**
 * Like [com.bluelinelabs.conductor.ChangeHandlerFrameLayout], but as a CoordinatorLayout
 */
class ChangeHandlerCoordinatorLayout :
  androidx.coordinatorlayout.widget.CoordinatorLayout,
  ControllerChangeHandler.ControllerChangeListener {

  private var inProgressTransactionCount: Int = 0

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr,
  )

  override fun onInterceptTouchEvent(ev: MotionEvent): Boolean =
    inProgressTransactionCount > 0 || super.onInterceptTouchEvent(ev)

  override fun onChangeStarted(
    to: Controller?,
    from: Controller?,
    isPush: Boolean,
    container: ViewGroup,
    handler: ControllerChangeHandler,
  ) {
    inProgressTransactionCount++
  }

  override fun onChangeCompleted(
    to: Controller?,
    from: Controller?,
    isPush: Boolean,
    container: ViewGroup,
    handler: ControllerChangeHandler,
  ) {
    inProgressTransactionCount--
  }
}
