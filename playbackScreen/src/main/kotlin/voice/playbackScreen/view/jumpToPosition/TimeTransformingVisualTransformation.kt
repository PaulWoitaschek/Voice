package voice.playbackScreen.view.jumpToPosition

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

internal object TimeTransformingVisualTransformation : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText {
    val originalText = text.text
    val transformedText = StringBuilder(originalText).apply {
      if (originalText.length > 4) {
        insert(length - 4, ":")
      }
      if (originalText.length > 2) {
        insert(length - 2, ":")
      }
    }.toString()
    val offsetMapping = object : OffsetMapping {

      override fun originalToTransformed(offset: Int): Int = offset + additionalOffset(offset)

      override fun transformedToOriginal(offset: Int): Int = offset - additionalOffset(offset)

      private fun additionalOffset(offset: Int): Int = when {
        offset > 4 -> 2
        offset > 2 -> 1
        else -> 0
      }
    }

    return TransformedText(
      AnnotatedString(transformedText),
      offsetMapping,
    )
  }
}
