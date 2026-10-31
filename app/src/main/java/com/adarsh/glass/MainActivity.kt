package com.adarsh.glass

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adarsh.glass.service.PanelAccessibilityService
import com.adarsh.glass.ui.theme.GlassTheme

class MainActivity : ComponentActivity() {

    private val serviceEnabled = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlassTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Home(
                        enabled = serviceEnabled.value,
                        modifier = Modifier.padding(innerPadding),
                        onOpenAccessibility = ::openAccessibilitySettings,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        serviceEnabled.value = isAccessibilityServiceEnabled()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, PanelAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
}

@Composable
private fun Home(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onOpenAccessibility: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Glass",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "iOS-style panels — Phase P1 (gesture + sliding panel).",
            style = MaterialTheme.typography.bodyMedium,
        )

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (enabled) "Accessibility service: ON" else "Accessibility service: OFF",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                Text(
                    text = if (enabled) {
                        "The top edge strip is active. Swipe down from the very top to open a panel."
                    } else {
                        "Turn Glass on in Accessibility settings to activate the top edge strip."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = onOpenAccessibility,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open Accessibility settings")
                }
            }
        }

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Setup notes",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "1. Tap the button above → Accessibility → Glass → turn on.\n" +
                        "2. Sideloaded apps hit a \"Restricted setting\" block. If so: App info → ⋮ (top-right) → Allow restricted settings, then enable Glass.\n" +
                        "3. Come back here — the status should read ON.\n" +
                        "4. Swipe down from the very top edge: left side opens Notification Center, right side opens Control Center.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
