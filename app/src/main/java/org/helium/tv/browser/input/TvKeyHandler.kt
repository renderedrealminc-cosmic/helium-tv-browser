package org.helium.tv.browser.input

import android.view.KeyEvent

interface TvActionListener {
    fun onToggleMousePointer()
    fun onOpenExtensionManager()
    fun onToggleAdBlocker()
    fun onTogglePipMode()
    fun onOpenMenu()
    fun onNavigateBack(): Boolean
    fun onMediaKey(keyCode: Int): Boolean
}

class TvKeyHandler(
    private val virtualMouse: VirtualMouseOverlay,
    private val actionListener: TvActionListener
) {
    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // First check TV dedicated color & function keys
        when (keyCode) {
            // TV Remote RED button -> Extensions
            KeyEvent.KEYCODE_PROG_RED -> {
                actionListener.onOpenExtensionManager()
                return true
            }

            // TV Remote GREEN button -> Toggle Virtual Mouse Pointer Mode
            KeyEvent.KEYCODE_PROG_GREEN -> {
                actionListener.onToggleMousePointer()
                return true
            }

            // TV Remote YELLOW button -> AdBlocker Quick Toggle
            KeyEvent.KEYCODE_PROG_YELLOW -> {
                actionListener.onToggleAdBlocker()
                return true
            }

            // TV Remote BLUE button -> Helium Floating PiP Mode
            KeyEvent.KEYCODE_PROG_BLUE -> {
                actionListener.onTogglePipMode()
                return true
            }

            // TV Remote MENU key
            KeyEvent.KEYCODE_MENU -> {
                actionListener.onOpenMenu()
                return true
            }

            // Media control keys for videos
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                return actionListener.onMediaKey(keyCode)
            }

            // BACK button
            KeyEvent.KEYCODE_BACK -> {
                return actionListener.onNavigateBack()
            }
        }

        // If virtual mouse is active, dispatch D-Pad keys to it
        if (virtualMouse.isPointerActive()) {
            if (virtualMouse.handleKeyDown(keyCode, event)) {
                return true
            }
        }

        return false
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (virtualMouse.isPointerActive()) {
            if (virtualMouse.handleKeyUp(keyCode, event)) {
                return true
            }
        }
        return false
    }
}
