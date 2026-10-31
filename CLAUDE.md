# Project: Glass — iOS-style panels for Android (no root)
- Read PRD.md before any task. Follow the phase asked, nothing extra.
- Kotlin + Jetpack Compose only. No XML layouts except accessibility config.
- Overlay windows live in PanelAccessibilityService using TYPE_ACCESSIBILITY_OVERLAY.
- Compose in overlays: ComposeView with custom LifecycleOwner + SavedStateRegistryOwner.
- Never use Apple fonts/icons/assets. Use Inter font + Material Symbols Rounded.
- No Android Studio and no USB/adb. Builds run on GitHub Actions only.
- After every change: push, check the Actions build, fix all errors until it is green.
- I install APKs from GitHub Releases on the phone. Never change the signing key.
- Include an in-app "Crash & Logs" screen (Settings) that stores exceptions + key logs
  to a file with a Share button, since I can't use logcat.
- Keep animation values in ui/theme/Motion.kt so I can tune feel in one place.
- Explain what I must test on the phone at the end of each task.
