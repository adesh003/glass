package com.adarsh.glass.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import com.adarsh.glass.ui.overlay.ComposeOverlayView
import com.adarsh.glass.ui.panel.PanelContent
import com.adarsh.glass.ui.theme.GlassTheme
import com.adarsh.glass.ui.theme.Motion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Owns all overlay windows for Glass (CLAUDE.md): the invisible top edge strip and the
 * full-screen panel, both added with TYPE_ACCESSIBILITY_OVERLAY (no SYSTEM_ALERT_WINDOW needed).
 *
 * P2 feel: the panel follows the finger 1:1 with rubber-band over-drag, and on release a spring
 * animation (seeded with the fling velocity) settles it open or closed. Haptic tick on crossing
 * the open threshold, and cross-window blur behind the panel when the device supports it.
 */
class PanelAccessibilityService : AccessibilityService() {

    private lateinit var wm: WindowManager
    private var strip: EdgeStripView? = null
    private var panel: ComposeOverlayView? = null
    private var panelLp: WindowManager.LayoutParams? = null

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val anim = Animatable(0f)
    private var animJob: Job? = null

    private var density = 1f
    private var blurSupported = false
    private var lastBlurRadius = -1

    private val vibrator: Vibrator? by lazy {
        val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WindowManager::class.java)
        density = resources.displayMetrics.density
        blurSupported = wm.isCrossWindowBlurEnabled
        PanelController.blurSupported.value = blurSupported

        PanelController.onDragFraction = { raw ->
            animJob?.cancel()
            applyDragFraction(raw)
        }
        PanelController.onDragSettle = { velocityPxPerSec ->
            settleFromDrag(velocityPxPerSec)
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
            onDrag = { f -> applyDragFraction(f) },
            onEnd = { _, velocity -> settleFromDrag(velocity) },
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

    // ---- Drag -> fraction -------------------------------------------------

    private fun onDragStart(kind: PanelKind) {
        animJob?.cancel()
        PanelController.kind.value = kind
        setFraction(0f)
        addPanel()
    }

    /** Raw finger-driven fraction with rubber-band past full open (G5) + haptic tick (G8). */
    private fun applyDragFraction(raw: Float) {
        val f = if (raw <= 1f) {
            raw.coerceAtLeast(0f)
        } else {
            1f + (((raw - 1f) * Motion.OVERSCROLL_DAMP)).coerceAtMost(Motion.MAX_OVERSCROLL)
        }
        maybeHaptic(PanelController.fraction.value, f)
        setFraction(f)
    }

    /** Sets the shared fraction and updates the window blur. No rubber-band (used by the spring). */
    private fun setFraction(v: Float) {
        PanelController.fraction.value = v
        applyBlur(v)
    }

    // ---- Release / spring -------------------------------------------------

    private fun settleFromDrag(velocityPxPerSec: Float) {
        val fraction = PanelController.fraction.value
        val vFrac = velocityPxPerSec / screenHeight().coerceAtLeast(1)
        val target = when {
            vFrac > Motion.FLING_VELOCITY_FRACTION -> 1f
            vFrac < -Motion.FLING_VELOCITY_FRACTION -> 0f
            fraction > Motion.OPEN_THRESHOLD -> 1f
            else -> 0f
        }
        settleTo(target, vFrac)
    }

    private fun settleTo(target: Float, initialVelocityFractionPerSec: Float) {
        animJob?.cancel()
        animJob = scope.launch {
            anim.snapTo(PanelController.fraction.value)
            anim.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = Motion.SPRING_DAMPING,
                    stiffness = Motion.SPRING_STIFFNESS,
                ),
                initialVelocity = initialVelocityFractionPerSec,
            ) {
                setFraction(value)
            }
            if (target == 0f) removePanel() else enablePanelBack()
        }
    }

    // ---- Panel window -----------------------------------------------------

    private fun addPanel() {
        if (panel != null) return
        val overlay = ComposeOverlayView(this)
        overlay.setContent { GlassTheme { PanelContent() } }

        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        if (blurSupported) {
            flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }

        lastBlurRadius = -1
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
                settleTo(0f, 0f)
                true
            } else {
                false
            }
        }
    }

    private fun applyBlur(v: Float) {
        if (!blurSupported) return
        val overlay = panel ?: return
        val lp = panelLp ?: return
        val radius = (v.coerceIn(0f, 1f) * dp(Motion.BLUR_MAX_DP)).toInt()
        if (abs(radius - lastBlurRadius) >= Motion.BLUR_STEP_PX || (radius == 0 && lastBlurRadius != 0)) {
            lp.blurBehindRadius = radius
            runCatching { wm.updateViewLayout(overlay.view, lp) }
            lastBlurRadius = radius
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
        lastBlurRadius = -1
        PanelController.kind.value = PanelKind.NONE
        PanelController.fraction.value = 0f
    }

    // ---- Haptics (G8) -----------------------------------------------------

    private fun maybeHaptic(prev: Float, next: Float) {
        if (!Motion.HAPTICS_ENABLED) return
        val t = Motion.OPEN_THRESHOLD
        if (prev < t && next >= t) tick()
    }

    private fun tick() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching {
            v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        }
    }

    // ---- AccessibilityService plumbing -----------------------------------

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used in P2. Fullscreen detection (G7) arrives in a later phase.
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        scope.cancel()
        super.onDestroy()
    }

    private fun teardown() {
        animJob?.cancel()
        animJob = null
        removePanel()
        removeStrip()
        PanelController.onDragFraction = null
        PanelController.onDragSettle = null
    }
}
