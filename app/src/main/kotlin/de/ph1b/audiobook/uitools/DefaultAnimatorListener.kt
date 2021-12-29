package de.ph1b.audiobook.uitools

import android.animation.Animator

interface DefaultAnimatorListener : Animator.AnimatorListener {
  override fun onAnimationRepeat(animator: Animator) {}
  override fun onAnimationEnd(animator: Animator) {}
  override fun onAnimationCancel(animator: Animator) {}
  override fun onAnimationStart(animator: Animator) {}
}
