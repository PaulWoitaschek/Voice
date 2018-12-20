package de.ph1b.audiobook.uitools.noPauseAnimator

import android.animation.Animator
import android.animation.TimeInterpolator
import android.util.ArrayMap
import de.ph1b.audiobook.crashreporting.CrashReporter
import java.util.ArrayList

class NoPauseAnimator(private val animator: Animator) : Animator() {

  private val listeners = ArrayMap<Animator.AnimatorListener, Animator.AnimatorListener>()

  override fun addListener(listener: Animator.AnimatorListener) {
    val wrapper = AnimatorListenerWrapper(this, listener)
    if (!listeners.containsKey(listener)) {
      listeners[listener] = wrapper
      animator.addListener(wrapper)
    }
  }

  override fun cancel() {
    animator.cancel()
  }

  override fun end() {
    animator.end()
  }

  override fun getDuration(): Long {
    return animator.duration
  }

  override fun getInterpolator(): TimeInterpolator {
    return animator.interpolator
  }

  override fun setInterpolator(timeInterpolator: TimeInterpolator) {
    animator.interpolator = timeInterpolator
  }

  override fun getListeners(): ArrayList<Animator.AnimatorListener> {
    return ArrayList(listeners.keys)
  }

  override fun getStartDelay(): Long {
    return animator.startDelay
  }

  override fun setStartDelay(startDelay: Long) {
    animator.startDelay = startDelay
  }

  override fun isPaused(): Boolean {
    return animator.isPaused
  }

  override fun isRunning(): Boolean {
    return animator.isRunning
  }

  override fun isStarted(): Boolean {
    return animator.isStarted
  }

  override fun removeAllListeners() {
    super.removeAllListeners()
    listeners.clear()
    animator.removeAllListeners()
  }

  override fun removeListener(listener: Animator.AnimatorListener) {
    val wrapper = listeners[listener]
    if (wrapper != null) {
      listeners.remove(listener)
      animator.removeListener(wrapper)
    }
  }

  override fun setDuration(duration: Long): Animator {
    try {
      animator.duration = duration
    } catch (e: IllegalStateException) {
      CrashReporter.logException(e)
    }
    return this
  }

  override fun setTarget(target: Any?) {
    animator.setTarget(target)
  }

  override fun setupEndValues() {
    animator.setupEndValues()
  }

  override fun setupStartValues() {
    animator.setupStartValues()
  }

  override fun start() {
    animator.start()
  }
}

fun Animator.noPause() = NoPauseAnimator(this)
