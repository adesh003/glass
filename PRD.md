# PRD — "Glass" : iOS-style Notification Center + Control Center for Nothing Phone 3a Lite

**Owner:** Adarsh
**Platform:** Android (Nothing OS, Android 15/16), no root
**Build tool:** Claude Code + Android Studio
**Distribution:** Personal sideload (APK), not Play Store

---

## 0. Reality Check (read first)

Android pe bina root ke **system notification shade ko replace nahi kar sakte**. Jo saare "iOS Control Center" apps karte hain (aur hum bhi karenge):

| What we do | How |
|---|---|
| Top edge swipe ko intercept karna | Thin invisible touch strip (overlay) at the top edge |
| Apna panel draw karna status bar ke upar | `AccessibilityService` + `TYPE_ACCESSIBILITY_OVERLAY` window |
| Saare notifications padhna, clear karna, actions/reply chalana | `NotificationListenerService` |
| Media player (song, play/pause, seek) | `MediaSessionManager` (via notification listener permission) |

**Hard limits (no root):**
- Nothing ka apna shade disable nahi hoga. Agar touch strip ke bahar swipe kiya to Nothing ka shade khulega. Solution: strip full width rakho; optionally Nothing ka shade sirf two-finger ya status-bar long press pe.
- Wi-Fi / Bluetooth / Mobile Data / Airplane mode ko **direct toggle nahi kar sakte** (Android 10+ restriction). Inke liye tap → system "Settings Panel" bottom sheet open hoga, ya long-press → settings page. Mobile data & airplane = sirf settings shortcut.
- Lock screen pe overlay limited hai — v1 mein sirf unlocked state.
- Window blur (real frosted glass behind panel) sirf tab chalega jab device `isCrossWindowBlurEnabled == true` ho. Nahi to fallback: tinted translucent background + wallpaper blur.
- Apple ka SF Pro font, SF Symbols icons, ya Apple assets copy mat karo. Look-alike banao (Inter font, Material Symbols Rounded / Lucide icons). Personal use ke liye bhi yahi safe hai.

---

## 1. Goal

Nothing Phone 3a Lite pe ek aisa panel jo **look, feel, gestures aur animation** mein iOS 18-style ho:
- Top-left / center swipe → **Notification Center**
- Top-right swipe → **Control Center**
- Spring physics, rubber-banding, haptics, glass blur — "same behaviour" feel.

### Non-goals (v1)
- Lock screen replacement
- Play Store release
- Dynamic Island (v2 idea)

---

## 2. Users & Use Cases
Single user (me). Daily driver. Must be smooth (60/120 fps), battery-friendly, and not crash/kill itself.

---

## 3. Features

### 3.1 Gesture Layer
| ID | Requirement |
|---|---|
| G1 | Invisible touch strip at top edge (height ~24dp, configurable), full width |
| G2 | Split zones: left 70% → Notification Center, right 30% → Control Center (configurable %) |
| G3 | Panel follows finger 1:1 while dragging (interactive, not tap-then-animate) |
| G4 | Release velocity decides open/close (fling threshold), else position > 35% → open |
| G5 | Rubber-band resistance when over-dragging beyond full open |
| G6 | Swipe up anywhere / tap empty area / back gesture → close |
| G7 | Strip hides in fullscreen apps (games, video) — detect via insets/accessibility events |
| G8 | Light haptic tick on open threshold & on toggle taps |

### 3.2 Notification Center (iOS style)
| ID | Requirement |
|---|---|
| N1 | Background: blurred wallpaper/content + dim, big clock + date at top (lock-screen style) |
| N2 | Notifications grouped **by app** as stacked cards (collapsed stack shows 2 cards peeking behind) |
| N3 | Tap stack → expands with spring animation; "Show less" + "×" (clear group) |
| N4 | Card: app icon, app name, relative time ("2m ago"), title (bold), text (2 lines) |
| N5 | Tap card → opens notification's `contentIntent`, panel closes |
| N6 | Swipe card left → reveals "Options" + "Clear"; full swipe = clear (with fly-out animation) |
| N7 | Long press → expanded view with notification actions + inline reply (RemoteInput) |
| N8 | "Notification Centre" header + "Clear all" (×) button that turns into "Clear" on tap (iOS behaviour) |
| N9 | Ongoing/silent notifications in separate lower section |
| N10 | Live updates: new notification slides in at top while panel is open |
| N11 | Empty state: "No Older Notifications" |

### 3.3 Control Center (iOS style)
Grid layout (4 columns), rounded-square tiles, glass background.

| ID | Tile | Behaviour (no-root reality) |
|---|---|---|
| C1 | Connectivity block (2×2): Airplane, Mobile Data, Wi-Fi, Bluetooth | Tap → system Settings Panel / settings screen. Long-press → expands block (iOS style) with names |
| C2 | Media player (2×2) | Artwork, title, artist, prev/play/next, seek. Live via MediaSession |
| C3 | Screen rotation lock | Direct toggle (WRITE_SETTINGS) |
| C4 | Do Not Disturb / Focus | Direct toggle (Notification Policy Access) |
| C5 | Brightness slider (vertical, tall pill) | Direct (WRITE_SETTINGS). Drag fills like iOS |
| C6 | Volume slider (vertical pill) | Direct (AudioManager) |
| C7 | Flashlight | Direct (CameraManager.setTorchMode) |
| C8 | Timer / Calculator / Camera / Screen record shortcuts | Launch intents |
| C9 | Edit mode (v2) | Rearrange/add tiles |

Active tile = filled colour (blue/white/orange like iOS), inactive = glass dark.

### 3.4 Animation & Feel Spec
| Element | Spec |
|---|---|
| Panel open/close | Spring, dampingRatio ≈ 0.85, stiffness ≈ 400 (tune to feel) |
| Panel scale-in | Content scales 0.92 → 1.0 + fades 0 → 1 as drag progresses |
| Background blur | Radius 0 → 60px tied to drag progress |
| Tile press | Scale to 0.94 on press, spring back |
| Slider drag | Rubber-band at 0% and 100% (slight stretch), haptic at edges |
| Stack expand | Cards translate from stacked offset to list positions, staggered 30ms |
| Notification clear | Swipe out + collapse height spring |
| Frame rate | Target 120Hz-capable smoothness, no jank on drag |

### 3.5 Settings App (the launcher activity)
- Onboarding: permissions step-by-step (see §5)
- Toggles: enable/disable service, zone split %, strip height, haptics on/off, blur on/off, dark/light theme
- Battery optimisation exemption shortcut
- Tile order (v2)

---

## 4. Tech Stack
| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (inside overlay via `ComposeView` with manual Lifecycle/SavedState owners) |
| Animation | Compose `Animatable`, `spring()`, `draggable`/`pointerInput` with `VelocityTracker` |
| Blur | `WindowManager.LayoutParams.blurBehindRadius` (API 31+, if supported) else `RenderEffect` on wallpaper snapshot |
| Services | `AccessibilityService` (overlay + gestures), `NotificationListenerService` |
| State | Single `StateFlow` repository shared between services and UI |
| Min SDK | 31 (Android 12) — target latest |
| Build | Gradle (Kotlin DSL), Android Studio |

### Architecture
```
app/
 ├─ service/
 │   ├─ PanelAccessibilityService.kt   // owns overlay windows + edge strip
 │   └─ NotifListenerService.kt        // feeds notifications + media sessions
 ├─ data/
 │   ├─ NotificationRepository.kt      // StateFlow<List<NotifGroup>>
 │   ├─ ControlsRepository.kt          // brightness, volume, dnd, torch, rotation
 │   └─ MediaRepository.kt
 ├─ ui/
 │   ├─ notifcenter/  (NotificationCenter, NotifStack, NotifCard, SwipeToClear)
 │   ├─ controlcenter/ (ControlCenter, Tile, ConnectivityBlock, MediaTile, VerticalSlider)
 │   ├─ theme/ (Glass colours, Inter font, shapes)
 │   └─ overlay/ (OverlayHost.kt — ComposeView in WindowManager)
 └─ settings/ (MainActivity, onboarding, preferences via DataStore)
```

---

## 5. Permissions (onboarding order)
1. **Notification access** — Settings → Notification access → Glass
2. **Accessibility** — Settings → Accessibility → Glass.
   Sideloaded app pe Android "Restricted setting" block karega → App info → ⋮ (top right) → **Allow restricted settings**, phir accessibility on karo.
3. **Modify system settings** (WRITE_SETTINGS) — brightness/rotation
4. **Do Not Disturb access** — DND tile
5. **Battery: Unrestricted** — App info → Battery → Unrestricted (Nothing OS aggressively kills background services)
6. Camera permission not needed for torch (CameraManager torch works without it)

---

## 6. Milestones
| Phase | Deliverable | Done when |
|---|---|---|
| P0 | Project setup, CLAUDE.md, empty app installs on phone | App opens on phone via USB/wireless debugging |
| P1 | Accessibility service + top edge strip + plain panel that slides down | Swipe from top shows a coloured box, follows finger, closes on swipe up |
| P2 | Spring physics, velocity fling, rubber-band, haptics, blur | Feels like iOS when dragging |
| P3 | Notification listener + Notification Center UI (cards, grouping, stacks) | Real notifications show, tap opens app |
| P4 | Swipe-to-clear, clear all, actions, inline reply | Can clear & reply to WhatsApp from panel |
| P5 | Control Center: tiles, sliders, media player | Brightness/volume/torch/DND/rotation work; media plays/pauses |
| P6 | Settings app + onboarding + fullscreen detection | Fresh install → guided setup works end to end |
| P7 | Polish + battery + bug pass | 1 week daily use without crash / service death |

---

## 7. Success Criteria
- Drag latency feels 1:1, no visible stutter
- Service survives 24h+ without being killed
- Notification Center reflects new notifications within 1s
- Zero accidental triggers during normal scrolling

---

## 8. Risks
| Risk | Mitigation |
|---|---|
| Nothing OS kills service | Battery unrestricted + foreground-like behaviour via accessibility (usually survives) |
| Edge strip conflicts with app's own top gestures | Make strip thin, add toggle to pause, hide in fullscreen |
| Blur unsupported | Fallback translucent style |
| Compose in overlay leaks / crashes | Proper ViewTreeLifecycleOwner + SavedStateRegistryOwner setup on ComposeView |
| Future Android sideload restrictions | Install via Android Studio/ADB as developer build |

---

# BUILD GUIDE — Claude Code ke saath kaise banaye

## Step 1 — Setup (NO Android Studio, laptop pe kuch heavy nahi)
Build cloud mein hoga (GitHub Actions), code Claude Code likhega, APK seedha phone pe download.

1. **GitHub account** banao (free) → new **private** repo `glass`.
2. **Claude Code on the web** (claude.ai/code) kholo → GitHub connect karo → `glass` repo select karo.
   (Laptop pe kuch install nahi; browser hi kaafi hai. Chaaho to laptop pe CLI Claude Code bhi chal jayega — wo light hai, sirf Node.js chahiye.)
3. Phone pe: Settings → About phone → **Build number pe 7 tap** → Developer options on (baad mein kaam aayega).
4. Phone pe Chrome/Files ko **"Install unknown apps"** allow karo (APK install ke liye).

## Step 2 — Project + auto-build banao
Is PRD ko repo mein `PRD.md` naam se daal do, phir Claude Code ko pehla prompt:

```
TASK: Phase P0
- Create a new Android project from scratch (no Android Studio): Kotlin, Jetpack Compose,
  Gradle Kotlin DSL + Gradle wrapper, app name Glass, package com.adarsh.glass, minSdk 31.
- Add GitHub Actions workflow .github/workflows/build.yml:
  on every push → setup JDK 17 + Android SDK → ./gradlew assembleDebug →
  upload app-debug.apk as artifact AND attach it to a GitHub Release tagged build-<run_number>
  (so I can download it directly on my phone).
- Use a fixed debug keystore committed in repo so every new APK installs as an update
  (no uninstall needed).
- Add CLAUDE.md from PRD.md Step 3.
- Commit and push. Tell me where to download the APK.
```

## Phone pe install ka loop (har phase)
1. Claude Code push kare → GitHub repo → **Actions** tab → build green hone do (~3–5 min).
2. Phone pe GitHub → **Releases** → latest `app-debug.apk` download → install (update).
3. Test → feedback Claude Code ko → repeat.

## Step 3 — CLAUDE.md (project memory for Claude Code)
Project root mein `CLAUDE.md` banao:

```
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
```

## Step 4 — Phase-wise prompts (paste one at a time in Claude Code)

**[PROMPT P1 — Gesture + basic panel]**
```
TASK: Phase P1 from PRD.md
BUILD:
- PanelAccessibilityService with accessibility_service_config.xml
- Thin full-width edge strip overlay at top (24dp), split zones per G2
- Dragging from strip opens a full-screen overlay panel that follows the finger 1:1
- Left zone = placeholder "Notification Center" box, right zone = "Control Center" box
- Swipe up or back closes panel
- Basic MainActivity with buttons that open Accessibility settings
OUTPUT: compile, install, then tell me exact test steps on the phone.
```

**[PROMPT P2 — iOS feel]**
```
TASK: Phase P2 from PRD.md (section 3.4 animation spec)
BUILD:
- Replace linear movement with Animatable + spring(dampingRatio 0.85, stiffness 400)
- VelocityTracker fling logic per G4, rubber-band per G5
- Content scale 0.92→1 and alpha tied to drag progress
- Background blur: blurBehindRadius if isCrossWindowBlurEnabled, else translucent fallback
- Haptic ticks per G8
- All numbers in ui/theme/Motion.kt
OUTPUT: compile, install, test steps. List which values to tweak if it feels too bouncy/slow.
```

**[PROMPT P3 — Notification Center]**
```
TASK: Phase P3 from PRD.md (N1–N5, N9–N11)
BUILD:
- NotifListenerService feeding NotificationRepository (StateFlow, grouped by package)
- Notification Center UI: clock+date header, grouped stacks (2 peeking cards), expand/collapse spring
- Card layout per N4, tap opens contentIntent and closes panel
- Onboarding button for notification access
OUTPUT: compile, install, test steps with real WhatsApp/Gmail notifications.
```

**[PROMPT P4 — Interactions]**
```
TASK: Phase P4 from PRD.md (N6–N8)
BUILD: swipe-left reveal Options/Clear, full swipe clear with fly-out + height collapse,
long-press expanded card with actions + RemoteInput inline reply, iOS "×" → "Clear" button.
OUTPUT: compile, install, test steps.
```

**[PROMPT P5 — Control Center]**
```
TASK: Phase P5 from PRD.md (C1–C8)
BUILD: 4-column glass grid, connectivity 2×2 block (opens Settings Panels — no direct toggle),
media tile via MediaSessionManager, vertical brightness + volume pill sliders with rubber-band,
torch, DND, rotation lock, shortcut tiles. Tile press scale 0.94 spring.
Handle missing permissions gracefully (tile shows lock badge → opens permission screen).
OUTPUT: compile, install, test steps.
```

**[PROMPT P6 — Settings + onboarding]**
```
TASK: Phase P6 from PRD.md (section 3.5 + section 5)
BUILD: step-by-step onboarding with live permission status (green ticks), DataStore prefs for
zone split, strip height, haptics, blur, theme. Fullscreen detection to hide strip (G7).
Explain "Allow restricted settings" step in onboarding text.
OUTPUT: compile, install, fresh-install test steps.
```

**[PROMPT P7 — Polish]**
```
TASK: Phase P7. Review whole codebase for: memory leaks in overlay ComposeViews,
recomposition hotspots during drag, battery drain, crash on service restart.
Fix issues, then give me a 1-week testing checklist.
```

## Step 5 — Testing tips
- Har phase ke baad phone pe actually use karo — feel adjust karna ho to sirf `Motion.kt` ke numbers badlo aur Claude ko bolo "spring thoda kam bouncy, stiffness 500".
- Bug/crash aaye to app ke **Settings → Crash & Logs → Share** se log copy karke Claude Code mein paste karo: "ye crash fix karo".
- Har prompt ke "OUTPUT: compile, install" ka matlab ab hai: push → Actions green → Release mein APK.
- Reference ke liye iPhone screen recordings (slow-mo) dekh ke specific behaviour describe karo — "stack expand hote waqt cards ek ke baad ek aate hain" type detail jitni precise, utna better result.
- Git use karo (`git init`), har working phase ke baad commit — kuch toota to wapas aa sakte ho.
