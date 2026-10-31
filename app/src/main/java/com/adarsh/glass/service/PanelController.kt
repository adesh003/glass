package com.adarsh.glass.service

import kotlinx.coroutines.flow.MutableStateFlow

/** Which panel the current gesture is driving. */
enum class PanelKind { NONE, NOTIFICATIONS, CONTROLS }

/**
 * Shared state between [PanelAccessibilityService] (which owns the overlay windows and the
 * drag gesture) and the Compose panel UI. A single object is fine: there is exactly one
 * service instance and one panel at a time.
 *
 * [fraction]: 0 = fully closed (panel above the top edge), 1 = fully open.
 */
object PanelController {
    val kind = MutableStateFlow(PanelKind.NONE)
    val fraction = MutableStateFlow(0f)

    /** Wired by the service so the Compose panel can drive its own drag-to-close. */
    var onDragFraction: ((Float) -> Unit)? = null
    var onDragSettle: ((Float) -> Unit)? = null
}
