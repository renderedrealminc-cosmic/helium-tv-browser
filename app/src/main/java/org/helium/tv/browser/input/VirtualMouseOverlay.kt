package org.helium.tv.browser.input

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View

/**
 * Futuristic Neon Ring Virtual Mouse Cursor for Android TV.
 * Provides pixel-precise navigation on 10-foot TV screens with glowing visual feedback.
 */
class VirtualMouseOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cursorX = 960f
    private var cursorY = 540f
    private var isCursorVisible = true
    private var isPressed = false
    private var isEnabled = true

    // Target View to receive synthetic events (GeckoView / WebView)
    var targetView: View? = null

    // Movement Physics
    private val baseSpeed = 15f
    private val maxSpeed = 42f
    private val acceleration = 1.35f
    private var speedMultiplier = 1.0f

    // Painting: Outer Neon Ring & Center Dot
    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        setShadowLayer(14f, 0f, 0f, Color.parseColor("#8000E5FF"))
    }

    private val innerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3000E5FF")
        style = Paint.Style.FILL
    }

    private val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(6f, 0f, 0f, Color.WHITE)
    }

    private val clickRipplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AA00E5FF")
        style = Paint.Style.FILL
    }

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

            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_CHANNEL_UP -> {
                scrollPage(-450f)
                return true
            }
            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                scrollPage(450f)
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
        targetView?.dispatchGenericMotionEvent(motionEvent)
        motionEvent.recycle()
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

        val ringRadius = if (isPressed) 14f else 18f
        val dotRadius = if (isPressed) 4f else 3.5f

        // Pressed click ripple
        if (isPressed) {
            canvas.drawCircle(0f, 0f, 26f, clickRipplePaint)
        }

        // Inner glowing translucent core
        canvas.drawCircle(0f, 0f, ringRadius, innerGlowPaint)

        // Outer Neon Ring
        canvas.drawCircle(0f, 0f, ringRadius, outerRingPaint)

        // Precision Center White Dot
        canvas.drawCircle(0f, 0f, dotRadius, centerDotPaint)

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
