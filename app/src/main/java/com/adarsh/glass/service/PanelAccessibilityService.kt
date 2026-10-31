package com.adarsh.glass.service

import android.accessibilityservice.AccessibilityService
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.animation.DecelerateInterpolator
import com.adarsh.glass.ui.overlay.ComposeOverlayView
import com.adarsh.glass.ui.panel.PanelContent
import com.adarsh.glass.ui.theme.GlassTheme
import com.adarsh.glass.ui.theme.Motion

/**
 * Owns all overlay windows for Glass (CLAUDE.md): the invisible top edge strip and the
 * full-screen panel. Both are added with TYPE_ACCESSIBILITY_OVERLAY, which an accessibility
 * service may draw without the SYSTEM_ALERT_WINDOW permission.
 */
class PanelAccessibilityService : AccessibilityService() {

    private lateinit var wm: WindowManager
    private var strip: EdgeStripView? = null
    private var panel: ComposeOverlayView? = null
    private var panelLp: WindowManager.LayoutParams? = null
    private var animator: ValueAnimator? = null
    private var density = 1f

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WindowManager::class.java)
        density = resources.displayMetrics.density

        // Let the Compose panel drive its own drag-to-close.
        PanelController.onDragFraction = { f ->
            animator?.cancel()
            PanelController.fraction.value = f
        }
        PanelController.onDragSettle = { f ->
            animateFraction(if (f > Motion.STAY_OPEN_THRESHOLD) 1f else 0f)
        }

        addStrip()
    }

    private fun dp(v: Float) = (v * density)

    private fun screenHeight(): Int = wm.currentWindowMetrics.bounds.height()

    // ---- Edge strip -------------------------------------------------------

    private fun addStrip() {
        if (strip != null) return
        val view = EdgeStripView(
            context = this,
            startThresholdPx = dp(Motion.DRAG_START_THRESHOLD_DP),
            screenHeightPx = ::screenHeight,
            onStart = { kind -> onDragStart(kind) },
            onDrag = { f -> PanelController.fraction.value = f },
            onEnd = { f -> onDragEnd(f) },
        )
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dp(Motion.STRIP_HEIGHT_DP).toInt(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }

        runCatching { wm.addView(view, lp) }
        strip = view
    }

    private fun removeStrip() {
        strip?.let { runCatching { wm.removeView(it) } }
        strip = null
    }

    // ---- Panel ------------------------------------------------------------

    private fun onDragStart(kind: PanelKind) {
        animator?.cancel()
        PanelController.kind.value = kind
        PanelController.fraction.value = 0f
        addPanel()
    }

    private fun onDragEnd(fraction: Float) {
        animateFraction(if (fraction > Motion.OPEN_THRESHOLD) 1f else 0f)
    }

    private fun addPanel() {
        if (panel != null) return
        val overlay = ComposeOverlayView(this)
        overlay.setContent { GlassTheme { PanelContent() } }
        // Not focusable while dragging so adding it mid-gesture never steals the strip's touch.
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }

        runCatching { wm.addView(overlay.view, lp) }
        overlay.onAttached()
        panel = overlay
        panelLp = lp
    }

    /** Once fully open, make the window focusable so the Back gesture/key closes the panel (G6). */
    private fun enablePanelBack() {
        val overlay = panel ?: return
        val lp = panelLp ?: return
        lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        runCatching { wm.updateViewLayout(overlay.view, lp) }
        overlay.view.isFocusableInTouchMode = true
        overlay.view.requestFocus()
        overlay.view.setOnKeyListener { _, keyCode, ev ->
            if (keyCode == KeyEvent.KEYCODE_BACK && ev.action == KeyEvent.ACTION_UP) {
                animateFraction(0f)
                true
            } else {
                false
            }
        }
    }

    private fun removePanel() {
        panel?.let {
            it.view.setOnKeyListener(null)
            runCatching { wm.removeView(it.view) }
            it.onDetached()
        }
        panel = null
        panelLp = null
        PanelController.kind.value = PanelKind.NONE
        PanelController.fraction.value = 0f
    }

    private fun animateFraction(target: Float) {
        animator?.cancel()
        val start = PanelController.fraction.value
        if (start == target) {
            if (target == 0f) removePanel() else enablePanelBack()
            return
        }
        animator = ValueAnimator.ofFloat(start, target).apply {
            duration = if (target > start) Motion.OPEN_ANIM_MS.toLong() else Motion.CLOSE_ANIM_MS.toLong()
            interpolator = DecelerateInterpolator() // P2 replaces this with spring physics
            addUpdateListener { PanelController.fraction.value = it.animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (target == 0f) removePanel() else enablePanelBack()
                }
            })
            start()
        }
    }

    // ---- AccessibilityService plumbing -----------------------------------

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used in P1. Fullscreen detection (G7) arrives in a later phase.
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun teardown() {
        animator?.cancel()
        animator = null
        removePanel()
        removeStrip()
        PanelController.onDragFraction = null
        PanelController.onDragSettle = null
    }
}
