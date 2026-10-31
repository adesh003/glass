package com.adarsh.glass.ui.theme

/**
 * Single place to tune the feel of the panels (CLAUDE.md requirement).
 *
 * Quick tuning guide:
 *  - Too bouncy on release        -> raise SPRING_DAMPING toward 1.0 (e.g. 0.9).
 *  - Too slow / floaty to settle  -> raise SPRING_STIFFNESS (e.g. 500-700).
 *  - Flings too easily open/close -> raise FLING_VELOCITY_FRACTION.
 *  - Over-drag feels too loose     -> lower OVERSCROLL_DAMP / MAX_OVERSCROLL.
 *  - Blur too strong/weak          -> change BLUR_MAX_DP.
 */
object Motion {
    /** Height of the invisible top edge touch strip (G1). */
    const val STRIP_HEIGHT_DP = 24f

    /** G2: left 70% of the strip opens Notification Center, right 30% opens Control Center. */
    const val LEFT_ZONE_FRACTION = 0.70f

    /** How far the finger must move down before a drag is recognised. */
    const val DRAG_START_THRESHOLD_DP = 6f

    /** G4: on release (no fling), open if the panel is pulled past this fraction, else close. */
    const val OPEN_THRESHOLD = 0.35f

    // ---- Spring (P2, §3.4) ----
    const val SPRING_DAMPING = 0.85f
    const val SPRING_STIFFNESS = 400f

    /** G4: |velocity| in fraction-of-screen per second above which a fling decides direction. */
    const val FLING_VELOCITY_FRACTION = 1.2f

    // ---- Rubber-band over-drag (G5) ----
    /** Fraction of the over-drag distance that is actually applied past full open. */
    const val OVERSCROLL_DAMP = 0.35f

    /** Hard cap on how far past full open the panel can be dragged. */
    const val MAX_OVERSCROLL = 0.08f

    // ---- Content scale-in (§3.4) ----
    const val CONTENT_MIN_SCALE = 0.92f

    /** Max dim behind the panel at full open. */
    const val SCRIM_MAX_ALPHA = 0.55f

    // ---- Background blur (§3.4) ----
    const val BLUR_MAX_DP = 36f

    /** Only push a new blur radius to the window once it moves by this many px (avoids churn). */
    const val BLUR_STEP_PX = 3

    // ---- Haptics (G8) ----
    const val HAPTICS_ENABLED = true

    /** Debug aid: faintly tint the two strip zones so the edge strip is findable while testing. */
    const val SHOW_STRIP_HINT = true
}
