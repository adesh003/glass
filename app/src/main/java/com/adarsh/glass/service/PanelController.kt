package com.adarsh.glass.service

import kotlinx.coroutines.flow.MutableStateFlow

/** Which panel the current gesture is driving. */
enum class PanelKind { NONE, NOTIFICATIONS, CONTROLS }

/**
 * Shared state between [PanelAccessibilityService] (which owns the overlay windows, the drag
 * gesture and the spring physics) and the Compose panel UI. A single object is fine: there is
 * exactly one service instance and one panel at a time.
 *
 * [fraction]: 0 = fully closed (panel above the top edge), 1 = fully open. During over-drag it
 * can briefly exceed 1 (rubber-band) and during a spring it can briefly overshoot.
 */
object PanelController {
    val kind = MutableStateFlow(PanelKind.NONE)
    val fraction = MutableStateFlow(0f)

    /** True when the device supports cross-window blur (frosted glass); drives the panel's alpha. */
    val blurSupported = MutableStateFlow(false)

    /** The Compose panel reports a raw drag fraction; the service applies rubber-band + haptics. */
    var onDragFraction: ((Float) -> Unit)? = null

    /** The Compose panel reports the end-of-drag velocity (px/sec, downward positive). */
    var onDragSettle: ((Float) -> Unit)? = null
}
