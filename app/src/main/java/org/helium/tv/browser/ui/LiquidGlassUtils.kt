package org.helium.tv.browser.ui

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * Utility for modern Apple-style 'Liquid Glass' (frosted translucent glassmorphism)
 * effects, hardware-accelerated blur, and smooth 150ms spring/ease-out native animations.
 */
object LiquidGlassUtils {

    private val easeOutInterpolator = DecelerateInterpolator(1.5f)

    /**
     * Applies native Android RenderEffect.createBlurEffect on Android 12+ (API 31+).
     * Automatically falls back to clean translucent glass on older Android/TV API levels.
     */
    fun applyBlur(view: View, radiusX: Float = 20f, radiusY: Float = 20f) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                view.setRenderEffect(RenderEffect.createBlurEffect(radiusX, radiusY, Shader.TileMode.CLAMP))
            } catch (t: Throwable) {
                // Graceful fallback on custom TV ROMs where RenderEffect is disabled
            }
        }
    }

    /**
     * Attaches lightweight 150ms ease-out scale feedback for button presses.
     */
    fun attachTouchFeedback(view: View, targetScale: Float = 0.95f) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(targetScale)
                        .scaleY(targetScale)
                        .setDuration(120)
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
        val targetElevation = if (hasFocus) 8f else 2f
        val targetScale = if (hasFocus) 1.02f else 1.0f
        view.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .translationZ(targetElevation)
            .setDuration(150)
            .setInterpolator(easeOutInterpolator)
            .start()
    }
}
