package com.adarsh.glass.ui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adarsh.glass.service.PanelController
import com.adarsh.glass.service.PanelKind
import com.adarsh.glass.ui.theme.GlassAccent
import com.adarsh.glass.ui.theme.GlassDark
import com.adarsh.glass.ui.theme.GlassOnDark
import com.adarsh.glass.ui.theme.GlassSurface
import com.adarsh.glass.ui.theme.Motion

/**
 * The full-screen overlay panel.
 *
 * The container slides so its leading edge tracks the finger 1:1 (translationY = (fraction-1)*h).
 * The content scales 0.92 -> 1 and fades in with drag progress (§3.4). A vertical [draggable]
 * drives drag-to-close and reports the release velocity so the service can fling/spring it.
 * When the device supports cross-window blur the background is translucent so the frosted blur
 * (applied on the window by the service) shows through; otherwise it falls back to a more opaque
 * tinted surface.
 */
@Composable
fun PanelContent() {
    val kind by PanelController.kind.collectAsState()
    val fraction by PanelController.fraction.collectAsState()
    val blur by PanelController.blurSupported.collectAsState()

    val panelBg = if (blur) GlassDark.copy(alpha = 0.55f) else GlassSurface.copy(alpha = 0.92f)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val heightPx = constraints.maxHeight.toFloat()
        val progress = fraction.coerceIn(0f, 1f)
        val contentScale = Motion.CONTENT_MIN_SCALE + (1f - Motion.CONTENT_MIN_SCALE) * progress

        val draggableState = rememberDraggableState { delta ->
            val raw = PanelController.fraction.value + delta / heightPx
            PanelController.onDragFraction?.invoke(raw)
        }

        // Dim scrim behind the panel, tied to drag progress.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = progress * Motion.SCRIM_MAX_ALPHA)),
        )

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = (fraction - 1f) * heightPx }
                .background(panelBg)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity -> PanelController.onDragSettle?.invoke(velocity) },
                ),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = contentScale
                        scaleY = contentScale
                        alpha = progress
                    },
            ) {
                when (kind) {
                    PanelKind.NOTIFICATIONS -> PlaceholderPanel("Notification Center", GlassOnDark)
                    PanelKind.CONTROLS -> PlaceholderPanel("Control Center", GlassAccent)
                    PanelKind.NONE -> Unit
                }
            }
        }
    }
}

@Composable
private fun PlaceholderPanel(title: String, accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    color = accent,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "P2 placeholder\nFling / swipe up or press Back to close",
                    color = GlassOnDark.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
