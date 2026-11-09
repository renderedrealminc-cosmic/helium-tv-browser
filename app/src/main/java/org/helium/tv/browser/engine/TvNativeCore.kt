package org.helium.tv.browser.engine

import android.util.Log

/**
 * JNI Bridge to high-performance C++ Native Hardware & Frame Rate Engine
 */
object TvNativeCore {
    private const val TAG = "TvNativeCore"
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("helium_native")
            nativeInitHardwareOptimizer()
            isNativeLoaded = true
            Log.i(TAG, "C++ libhelium_native.so successfully loaded")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library 'helium_native' not yet compiled. Running in fallback mode.", e)
            isNativeLoaded = false
        }
    }

    fun recordFrame() {
        if (isNativeLoaded) {
            nativeRecordFrame()
        }
    }

    fun getCurrentFps(): Double {
        return if (isNativeLoaded) nativeGetFps() else 60.0
    }

    fun getDroppedFrames(): Int {
        return if (isNativeLoaded) nativeGetDroppedFrames() else 0
    }

    fun getHardwareStats(): String {
        return if (isNativeLoaded) nativeGetHardwareCapabilities() else "Fallback Engine"
    }

    // C++ JNI Declarations
    private external fun nativeInitHardwareOptimizer(): Boolean
    private external fun nativeRecordFrame()
    private external fun nativeGetFps(): Double
    private external fun nativeGetDroppedFrames(): Int
    private external fun nativeGetHardwareCapabilities(): String
}
