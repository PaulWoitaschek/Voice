package de.ph1b.audiobook.uitools

import android.animation.Animator
import android.animation.ObjectAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Property
import android.view.animation.DecelerateInterpolator

/**
 * This code was modified by me, Paul Woitaschek. All these changes are licensed under GPLv3. The
 * original source can be found here: {@see https://github.com/alexjlockwood/material-pause-play-
 * * animation/blob/master/app/src/main/java/com/alexjlockwood/example/playpauseanimation/
 * * PlayPauseView.java}
 *
 * The original licensing is as follows:
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Alex Lockwood
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
class PlayPauseDrawable : Drawable() {

  private var pauseBarHeight = 0F
  private var pauseBarWidth = 0F
  private var pauseBarDistance = 0F
  private val leftPauseBar = Path()
  private val rightPauseBar = Path()
  private val interpolator = DecelerateInterpolator()
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
    color = Color.WHITE
  }
  private var progress: Float = 0F
    set(progress) {
      field = progress
      invalidateSelf()
    }
  private val progressProperty = object : Property<PlayPauseDrawable, Float>(Float::class.java, "progress") {
    override fun get(d: PlayPauseDrawable) = d.progress
    override fun set(d: PlayPauseDrawable, value: Float) {
      d.progress = value
    }
  }
  private var isPlay: Boolean = false
  private var animator: Animator? = null

  /** Linear interpolate between a and b with parameter t.  */
  private fun interpolate(a: Float, b: Float, t: Float) = a + (b - a) * t

  override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)

    pauseBarHeight = 7.0f / 12.0f * (bounds.height().toFloat())
    pauseBarWidth = pauseBarHeight / 3.0f
    pauseBarDistance = pauseBarHeight / 3.6f
  }

  override fun draw(canvas: Canvas) {
    leftPauseBar.rewind()
    rightPauseBar.rewind()

    // move to center of canvas
    canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())

    // The current distance between the two pause bars.
    val barDist = interpolate(pauseBarDistance, 0.0f, progress) - 1 // -1 because else there is a small gap on some devices
    // The current width of each pause bar.
    val barWidth = interpolate(pauseBarWidth, pauseBarHeight / 1.75f, progress)
    // The current position of the left pause bar's top left coordinate.
    val firstBarTopLeft = interpolate(0.0f, barWidth, progress)
    // The current position of the right pause bar's top right coordinate.
    val secondBarTopRight = interpolate(2.0f * barWidth + barDist, barWidth + barDist, progress)

    // Draw the left pause bar. The left pause bar transforms into the
    // top half of the play button triangle by animating the position of the
    // rectangle's top left coordinate and expanding its bottom width.
    leftPauseBar.moveTo(0.0f, 0.0f)
    leftPauseBar.lineTo(firstBarTopLeft, -pauseBarHeight)
    leftPauseBar.lineTo(barWidth, -pauseBarHeight)
    leftPauseBar.lineTo(barWidth, 0.0f)
    leftPauseBar.close()

    // Draw the right pause bar. The right pause bar transforms into the
    // bottom half of the play button triangle by animating the position of the
    // rectangle's top right coordinate and expanding its bottom width.
    rightPauseBar.moveTo(barWidth + barDist, 0.0f)
    rightPauseBar.lineTo(barWidth + barDist, -pauseBarHeight)
    rightPauseBar.lineTo(secondBarTopRight, -pauseBarHeight)
    rightPauseBar.lineTo(2.0f * barWidth + barDist, 0.0f)
    rightPauseBar.close()

    canvas.save()

    // Translate the play button a tiny bit to the right so it looks more centered.
    canvas.translate(interpolate(0.0f, pauseBarHeight / 8.0f, progress), 0.0f)

    // (1) Pause --> Play: rotate 0 to 90 degrees clockwise.
    // (2) Play --> Pause: rotate 90 to 180 degrees clockwise.
    val rotationProgress = if (isPlay) 1.0f - progress else progress
    val startingRotation = if (isPlay) 90.0f else 0.0f
    canvas.rotate(interpolate(startingRotation, startingRotation + 90.0f, rotationProgress), bounds.width() / 2.0f, bounds.height() / 2.0f)

    // Position the pause/play button in the center of the drawable's bounds.
    canvas.translate(bounds.width() / 2.0f - ((2.0f * barWidth + barDist) / 2.0f), bounds.height() / 2.0f + (pauseBarHeight / 2.0f))

    // Draw the two bars that form the animated pause/play button.
    canvas.drawPath(leftPauseBar, paint)
    canvas.drawPath(rightPauseBar, paint)

    canvas.restore()
  }

  fun transformToPause(animated: Boolean) {
    if (isPlay) {
      jumpToCurrentState()
      if (animated) {
        toggle()
      } else {
        isPlay = false
        progress = 0.0f
        invalidateSelf()
      }
    }
  }

  fun transformToPlay(animated: Boolean) {
    if (!isPlay) {
      jumpToCurrentState()
      if (animated) {
        toggle()
      } else {
        isPlay = true
        progress = 1.0f
        invalidateSelf()
      }
    }
  }

  override fun jumpToCurrentState() {
    animator?.cancel()
    progress = if (isPlay) 1.0f else 0.0f
  }

  private fun toggle() {
    animator?.cancel()
    val from = if (isPlay) 1.0f else 0.0f
    val to = if (isPlay) 0.0f else 1.0f
    isPlay = !isPlay
    animator = ObjectAnimator.ofFloat(this, progressProperty, from, to)
        .also {
          it.interpolator = interpolator
          it.duration = 275
          it.start()
        }
  }

  override fun setAlpha(alpha: Int) {
    paint.alpha = alpha
    invalidateSelf()
  }

  override fun setColorFilter(cf: ColorFilter?) {
    paint.colorFilter = cf
    invalidateSelf()
  }

  override fun getOpacity() = PixelFormat.TRANSLUCENT
}
