package com.adarsh.glass.ui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adarsh.glass.service.PanelController
import com.adarsh.glass.service.PanelKind
import com.adarsh.glass.ui.theme.GlassAccent
import com.adarsh.glass.ui.theme.GlassOnDark
import com.adarsh.glass.ui.theme.GlassSurface
import com.adarsh.glass.ui.theme.Motion

/**
 * The full-screen overlay panel. It reads [PanelController] state and slides so its leading
 * edge tracks the finger 1:1 (translationY = (fraction - 1) * height). A vertical drag on the
 * open panel drives drag-to-close back through [PanelController].
 */
@Composable
fun PanelContent() {
    val kind by PanelController.kind.collectAsState()
    val fraction by PanelController.fraction.collectAsState()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val heightPx = constraints.maxHeight.toFloat()

        // Dim scrim behind the panel, tied to drag progress.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = fraction * Motion.SCRIM_MAX_ALPHA)),
        )

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = (fraction - 1f) * heightPx }
                .background(GlassSurface.copy(alpha = 0.97f))
                .pointerInput(heightPx) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val next = (PanelController.fraction.value + dragAmount / heightPx)
                                .coerceIn(0f, 1f)
                            PanelController.onDragFraction?.invoke(next)
                        },
                        onDragEnd = {
                            PanelController.onDragSettle?.invoke(PanelController.fraction.value)
                        },
                        onDragCancel = {
                            PanelController.onDragSettle?.invoke(PanelController.fraction.value)
                        },
                    )
                },
        ) {
            when (kind) {
                PanelKind.NOTIFICATIONS -> PlaceholderPanel(
                    title = "Notification Center",
                    accent = GlassOnDark,
                )
                PanelKind.CONTROLS -> PlaceholderPanel(
                    title = "Control Center",
                    accent = GlassAccent,
                )
                PanelKind.NONE -> Unit
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
                    text = "P1 placeholder\nSwipe up or press Back to close",
                    color = GlassOnDark.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
