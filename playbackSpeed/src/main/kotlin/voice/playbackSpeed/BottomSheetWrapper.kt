package voice.playbackSpeed

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.launch

@Composable
private fun BottomSheetWrapper(
  parent: ViewGroup,
  composeView: ComposeView,
  content: @Composable (() -> Unit) -> Unit
) {
  val TAG = parent::class.java.simpleName
  val coroutineScope = rememberCoroutineScope()
  val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
  var isSheetOpened by remember { mutableStateOf(false) }

  ModalBottomSheetLayout(
    sheetBackgroundColor = Color.Transparent,
    sheetState = modalBottomSheetState,
    sheetContent = {
      content {
        // Action passed for clicking close button in the content
        coroutineScope.launch {
          modalBottomSheetState.hide() // will trigger the LaunchedEffect
        }
      }
    }
  ) {}

  BackHandler {
    coroutineScope.launch {
      modalBottomSheetState.hide() // will trigger the LaunchedEffect
    }
  }

  // Take action based on hidden state
  LaunchedEffect(modalBottomSheetState.currentValue) {
    when (modalBottomSheetState.currentValue) {
      ModalBottomSheetValue.Hidden -> {
        when {
          isSheetOpened -> parent.removeView(composeView)
          else -> {
            isSheetOpened = true
            modalBottomSheetState.show()
          }
        }
      }
      else -> {
        Log.i(TAG, "Bottom sheet ${modalBottomSheetState.currentValue} state")
      }
    }
  }
}

// Extension for Activity
fun Activity.showAsBottomSheet(content: @Composable (() -> Unit) -> Unit) {
  val viewGroup = this.findViewById(android.R.id.content) as ViewGroup
  addContentToView(viewGroup, content)
}

// Extension for Fragment
fun Fragment.showAsBottomSheet(content: @Composable (() -> Unit) -> Unit) {
  val viewGroup = requireActivity().findViewById(android.R.id.content) as ViewGroup
  addContentToView(viewGroup, content)
}

private fun addContentToView(
  viewGroup: ViewGroup,
  content: @Composable (() -> Unit) -> Unit
) {
  viewGroup.addView(
    ComposeView(viewGroup.context).apply {
      setContent {
        BottomSheetWrapper(viewGroup, this, content)
      }
    }
  )
}
