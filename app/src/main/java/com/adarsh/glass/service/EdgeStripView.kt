package com.adarsh.glass.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import com.adarsh.glass.ui.theme.Motion

/**
 * The invisible full-width touch strip at the very top edge (G1/G2).
 *
 * It owns the DOWN of the open gesture, so Android keeps delivering the whole drag to this
 * view even once the finger leaves the thin strip bounds. Left [LEFT_ZONE_FRACTION] width
 * opens Notification Center, the rest opens Control Center. On release it reports the fling
 * velocity (px/sec, downward positive) so the service can decide open/close and seed the spring.
 */
@SuppressLint("ViewConstructor")
class EdgeStripView(
    context: Context,
    private val startThresholdPx: Float,
    private val screenHeightPx: () -> Int,
    private val onStart: (PanelKind) -> Unit,
    private val onDrag: (Float) -> Unit,
    private val onEnd: (fraction: Float, velocityPxPerSec: Float) -> Unit,
) : View(context) {

    private var startY = 0f
    private var dragging = false
    private var kind = PanelKind.NOTIFICATIONS
    private var velocityTracker: VelocityTracker? = null
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        // Allow onDraw even though we have no background (needed for the debug zone hints).
        setWillNotDraw(!Motion.SHOW_STRIP_HINT)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startY = event.rawY
                dragging = false
                kind = if (event.x < width * Motion.LEFT_ZONE_FRACTION) {
                    PanelKind.NOTIFICATIONS
                } else {
                    PanelKind.CONTROLS
                }
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(event)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                val dy = event.rawY - startY
                if (!dragging && dy > startThresholdPx) {
                    dragging = true
                    onStart(kind)
                }
                if (dragging) {
                    val h = screenHeightPx().coerceAtLeast(1)
                    onDrag((dy / h).coerceIn(0f, 1f))
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    val h = screenHeightPx().coerceAtLeast(1)
                    val vy = velocityTracker?.let {
                        it.addMovement(event)
                        it.computeCurrentVelocity(1000)
                        it.yVelocity
                    } ?: 0f
                    onEnd(((event.rawY - startY) / h).coerceIn(0f, 1f), vy)
                }
                dragging = false
                velocityTracker?.recycle()
                velocityTracker = null
                return true
            }
        }
        return false
    }

    override fun onDraw(canvas: Canvas) {
        if (!Motion.SHOW_STRIP_HINT) return
        val splitX = width * Motion.LEFT_ZONE_FRACTION
        hintPaint.color = 0x22FFFFFF // faint white = Notification zone
        canvas.drawRect(0f, 0f, splitX, height.toFloat(), hintPaint)
        hintPaint.color = 0x330A84FF // faint blue = Control zone
        canvas.drawRect(splitX, 0f, width.toFloat(), height.toFloat(), hintPaint)
    }
}
