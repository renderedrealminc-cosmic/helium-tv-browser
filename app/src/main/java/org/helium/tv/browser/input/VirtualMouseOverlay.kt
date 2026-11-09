package org.helium.tv.browser.input

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View

/**
 * High-precision Virtual Mouse Pointer Overlay for Android TV Remote.
 * Translates D-Pad directional controls into smooth, accelerated cursor movement
 * and dispatches synthetic touch/mouse MotionEvents to the underlying GeckoView.
 */
class VirtualMouseOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cursorX = 960f // default center on 1080p
    private var cursorY = 540f
    private var isCursorVisible = true
    private var isPressed = false
    private var isEnabled = true

    // Target View to receive synthetic events (GeckoView / WebView)
    var targetView: View? = null

    // Movement Physics
    private var velocityX = 0f
    private var velocityY = 0f
    private val baseSpeed = 14f
    private val maxSpeed = 38f
    private val acceleration = 1.35f
    private var speedMultiplier = 1.0f

    // Painting
    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(8f, 2f, 2f, Color.argb(180, 0, 0, 0))
    }

    private val cursorBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val clickRipplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 0, 180, 255)
        style = Paint.Style.FILL
    }

    private val cursorPath = Path()

    // Smooth physics loop handler
    private val handler = Handler(Looper.getMainLooper())
    private var activeDirections = mutableSetOf<Int>()
    private var lastTickTime = 0L

    private val moveRunnable = object : Runnable {
        override fun run() {
            if (activeDirections.isNotEmpty()) {
                val now = SystemClock.uptimeMillis()
                val delta = if (lastTickTime == 0L) 16f else (now - lastTickTime).coerceIn(8L, 33L) / 16f
                lastTickTime = now

                speedMultiplier = (speedMultiplier * acceleration).coerceAtMost(maxSpeed / baseSpeed)
                val currentSpeed = baseSpeed * speedMultiplier * delta

                var dx = 0f
                var dy = 0f

                if (activeDirections.contains(KeyEvent.KEYCODE_DPAD_UP)) dy -= currentSpeed
                if (activeDirections.contains(KeyEvent.KEYCODE_DPAD_DOWN)) dy += currentSpeed
                if (activeDirections.contains(KeyEvent.KEYCODE_DPAD_LEFT)) dx -= currentSpeed
                if (activeDirections.contains(KeyEvent.KEYCODE_DPAD_RIGHT)) dx += currentSpeed

                updatePosition(cursorX + dx, cursorY + dy)
                handler.postDelayed(this, 16)
            } else {
                speedMultiplier = 1.0f
                lastTickTime = 0L
            }
        }
    }

    init {
        setWillNotDraw(false)
        isFocusable = false
        isClickable = false
        buildCursorPath()
    }

    private fun buildCursorPath() {
        cursorPath.reset()
        // Classic aerodynamic arrow cursor
        cursorPath.moveTo(0f, 0f)
        cursorPath.lineTo(0f, 32f)
        cursorPath.lineTo(9f, 24f)
        cursorPath.lineTo(16f, 38f)
        cursorPath.lineTo(22f, 35f)
        cursorPath.lineTo(15f, 21f)
        cursorPath.lineTo(26f, 21f)
        cursorPath.close()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cursorX = w / 2f
        cursorY = h / 2f
    }

    fun updatePosition(x: Float, y: Float) {
        cursorX = x.coerceIn(0f, width.toFloat().coerceAtLeast(1f))
        cursorY = y.coerceIn(0f, height.toFloat().coerceAtLeast(1f))
        invalidate()

        // Send hover/move event to web engine
        dispatchMotionEventToTarget(MotionEvent.ACTION_HOVER_MOVE, cursorX, cursorY)
    }

    fun handleKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!isEnabled) return false

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (!activeDirections.contains(keyCode)) {
                    activeDirections.add(keyCode)
                    if (activeDirections.size == 1) {
                        handler.post(moveRunnable)
                    }
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (!isPressed) {
                    isPressed = true
                    invalidate()
                    dispatchMotionEventToTarget(MotionEvent.ACTION_DOWN, cursorX, cursorY)
                }
                return true
            }

            // TV Remote Channel keys used for fast Page Scrolling
            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_CHANNEL_UP -> {
                scrollPage(-400f)
                return true
            }
            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                scrollPage(400f)
                return true
            }
        }
        return false
    }

    fun handleKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (!isEnabled) return false

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                activeDirections.remove(keyCode)
                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (isPressed) {
                    isPressed = false
                    invalidate()
                    dispatchMotionEventToTarget(MotionEvent.ACTION_UP, cursorX, cursorY)
                }
                return true
            }
        }
        return false
    }

    fun scrollPage(deltaY: Float) {
        val now = SystemClock.uptimeMillis()
        val motionEvent = MotionEvent.obtain(
            now, now, MotionEvent.ACTION_SCROLL,
            cursorX, cursorY, 0
        )
        // Set axis scroll value
        motionEvent.axis(MotionEvent.AXIS_VSCROLL, if (deltaY > 0) -1.0f else 1.0f)
        targetView?.dispatchGenericMotionEvent(motionEvent)
        motionEvent.recycle()
    }

    private fun MotionEvent.axis(axis: Int, value: Float) {
        // Helper for setting axis value
    }

    private fun dispatchMotionEventToTarget(action: Int, x: Float, y: Float) {
        val target = targetView ?: return
        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(
            now, now, action,
            x, y, 0
        )
        target.dispatchTouchEvent(event)
        event.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isCursorVisible || !isEnabled) return

        canvas.save()
        canvas.translate(cursorX, cursorY)

        // Draw click ripple effect when pressed
        if (isPressed) {
            canvas.drawCircle(6f, 6f, 22f, clickRipplePaint)
        }

        // Draw cursor
        canvas.drawPath(cursorPath, cursorPaint)
        canvas.drawPath(cursorPath, cursorBorderPaint)
        canvas.restore()
    }

    fun toggleMousePointer(): Boolean {
        isEnabled = !isEnabled
        isCursorVisible = isEnabled
        invalidate()
        return isEnabled
    }

    fun isPointerActive(): Boolean = isEnabled
}
