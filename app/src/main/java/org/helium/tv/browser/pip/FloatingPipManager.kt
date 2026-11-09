package org.helium.tv.browser.pip

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Log
import android.util.Rational
import android.view.View

/**
 * Manages Helium Floating Picture-in-Picture and Window Translucency
 */
class FloatingPipManager(private val activity: Activity) {

    companion object {
        private const val TAG = "FloatingPipManager"
    }

    private var isInPipMode = false
    private var windowAlpha = 1.0f

    fun enterSystemPip(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                val entered = activity.enterPictureInPictureMode(params)
                isInPipMode = entered
                Log.i(TAG, "Entered Android System PiP mode: $entered")
                return entered
            } catch (e: Exception) {
                Log.e(TAG, "Failed entering PiP mode", e)
            }
        }
        return false
    }

    fun setTranslucency(rootView: View, alpha: Float) {
        windowAlpha = alpha.coerceIn(0.2f, 1.0f)
        rootView.alpha = windowAlpha
        Log.d(TAG, "Helium window transparency set to: $windowAlpha")
    }

    fun cycleTranslucency(rootView: View): Float {
        // Cycle between 100%, 80%, 60%, 40%
        val nextAlpha = when {
            windowAlpha <= 0.45f -> 1.0f
            windowAlpha <= 0.65f -> 0.4f
            windowAlpha <= 0.85f -> 0.6f
            else -> 0.8f
        }
        setTranslucency(rootView, nextAlpha)
        return nextAlpha
    }

    fun isPipActive(): Boolean = isInPipMode

    fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        isInPipMode = isInPictureInPictureMode
    }
}
