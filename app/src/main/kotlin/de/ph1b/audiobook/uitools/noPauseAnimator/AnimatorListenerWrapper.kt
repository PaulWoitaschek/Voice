package de.ph1b.audiobook.uitools.noPauseAnimator

import android.animation.Animator

class AnimatorListenerWrapper(
  private val animator: Animator,
  private val listener: Animator.AnimatorListener
) : Animator.AnimatorListener {

  override fun onAnimationStart(animator: Animator) {
    listener.onAnimationStart(this.animator)
  }

  override fun onAnimationEnd(animator: Animator) {
    listener.onAnimationEnd(this.animator)
  }

  override fun onAnimationCancel(animator: Animator) {
    listener.onAnimationCancel(this.animator)
  }

  override fun onAnimationRepeat(animator: Animator) {
    listener.onAnimationRepeat(this.animator)
  }
}
