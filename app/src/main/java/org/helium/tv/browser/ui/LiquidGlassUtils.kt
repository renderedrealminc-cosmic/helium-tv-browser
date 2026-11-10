package org.helium.tv.browser.ui

import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * Utility for modern Apple-style 'Liquid Glass' crisp high-contrast glassmorphism,
 * hardware-accelerated rendering, and smooth 150ms spring/ease-out native animations.
 */
object LiquidGlassUtils {

    private val easeOutInterpolator = DecelerateInterpolator(1.5f)

    /**
     * Attaches lightweight 150ms ease-out scale feedback for button presses.
     * Keeps all buttons, icons, and text razor-sharp without blur distortion.
     */
    fun attachTouchFeedback(view: View, targetScale: Float = 0.94f) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(targetScale)
                        .scaleY(targetScale)
                        .setDuration(100)
                        .setInterpolator(easeOutInterpolator)
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(150)
                        .setInterpolator(easeOutInterpolator)
                        .start()
                }
            }
            false // allow click listener to proceed
        }
    }

    /**
     * Smooth 150ms focus/expansion animation for address bars and search pills.
     */
    fun animatePillFocus(view: View, hasFocus: Boolean) {
        val targetElevation = if (hasFocus) 8f else 3f
        val targetScale = if (hasFocus) 1.015f else 1.0f
        view.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .translationZ(targetElevation)
            .setDuration(150)
            .setInterpolator(easeOutInterpolator)
            .start()
    }
}
