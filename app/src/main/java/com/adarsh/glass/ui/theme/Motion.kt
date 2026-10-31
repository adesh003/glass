package com.adarsh.glass.ui.theme

/**
 * Single place to tune the feel of the panels (CLAUDE.md requirement).
 * P1 uses simple values; P2 replaces the open/close animation with spring physics.
 */
object Motion {
    /** Height of the invisible top edge touch strip (G1). */
    const val STRIP_HEIGHT_DP = 24f

    /** G2: left 70% of the strip opens Notification Center, right 30% opens Control Center. */
    const val LEFT_ZONE_FRACTION = 0.70f

    /** How far the finger must move down before a drag is recognised. */
    const val DRAG_START_THRESHOLD_DP = 6f

    /** G4 (P1 approximation): on release, open if the panel is pulled past this fraction. */
    const val OPEN_THRESHOLD = 0.35f

    /** While the panel is open, releasing above this fraction keeps it open; below it closes. */
    const val STAY_OPEN_THRESHOLD = 0.75f

    /** Release animation durations (ms). P2 swaps these for spring(). */
    const val OPEN_ANIM_MS = 260
    const val CLOSE_ANIM_MS = 220

    /** Max dim behind the panel at full open. */
    const val SCRIM_MAX_ALPHA = 0.55f

    /** Debug aid: faintly tint the two strip zones so the edge strip is findable while testing. */
    const val SHOW_STRIP_HINT = true
}
