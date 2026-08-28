# TruthScan Android App

Kotlin-based Android app with floating screenshot analysis using Gemini API.

## Features

- 🔍 **Floating Icon**: Draggable overlay button for easy access
- 📸 **Screen Capture**: One-tap screenshot using MediaProjection API
- 🧠 **AI Analysis**: Real-time credibility assessment via Gemini 2.5 Flash
- 📊 **Visual Feedback**: Inline overlay showing analysis results
- ⏱️ **Usage Tracking**: Daily limit enforcement (5 checks free)

## Requirements

- **Android**: API 26 (Android 8.0) and above
- **IDE**: Android Studio 2023.1 or later
- **JDK**: OpenJDK 11+

## Setup & Build

### 1. Clone & Open in Android Studio

```bash
cd android/
# Open with Android Studio
```

### 2. Configure Backend URL

Edit `app/build.gradle.kts` and update:

```kotlin
buildConfigField("String", "BACKEND_URL", "\"https://your-vercel-domain.vercel.app\"")
buildConfigField("String", "APP_SHARED_SECRET", "\"your_app_shared_secret\"")
```

Make sure these values **match** the backend environment variables.

### 3. Build & Install

```bash
# Sync Gradle (Auto-triggered in Android Studio)
./gradlew build

# Install to device/emulator (via Android Studio or CLI)
./gradlew installDebug
```

Or simply click **Run** in Android Studio.

## First Run

1. **Launch** the app on your device
2. **Tap "Start Service"** → System requests permissions in sequence:
   - Floating window permission (Settings → Special app access → Display over other apps)
   - Notification permission (Android 13+)
   - Screenshot capture permission (system dialog)
3. **Service starts** → Floating icon appears on screen
4. **Click floating icon** in any app → Captures screenshot → Shows analysis result in overlay

## Architecture

```
MainActivity
  ↓
  └─→ PermissionHelper (check system alerts)
  └─→ MediaProjectionManager (system capture dialog)
  └─→ FloatingService (foreground service)
       ├─→ ScreenCaptureManager (VirtualDisplay + ImageReader)
       ├─→ GeminiRepository (API calls to backend)
       ├─→ ResultOverlayView (display analysis)
       └─→ UsageTracker (daily limit via SharedPreferences)
```

## Key Components

### FloatingService.kt
- Runs as foreground service (survives app close)
- Manages floating icon (draggable overlay)
- Handles screenshot capture + API calls
- Shows result overlay with 8-second auto-dismiss

### ScreenCaptureManager.kt
- Uses MediaProjection API (Android 5.0+)
- Creates VirtualDisplay + ImageReader
- Compresses JPEG (70% quality) to ~500KB-1MB
- Encodes to base64 for API transmission

### GeminiRepository.kt
- Retrofit + OkHttp wrapper (not used, raw OkHttp for simplicity)
- Calls `POST /api/analyze` on backend
- Sends: `{ imageBase64, mimeType }`
- Receives: `{ success, data: { credibility, verdict, reason[], suggestions[] } }`
- 30-second read timeout (Gemini analysis can take 5-10s)

### UsageTracker.kt
- SharedPreferences daily count per date
- Format: `usage_yyyy-MM-dd`
- Free limit: 5 checks/day (hardcoded, change `DAILY_FREE_LIMIT` to adjust)

## Known Limitations & Workarounds

### 1. MediaProjection Permission
- Android **requires user consent every service restart**
- Once per app session, not per screenshot
- To re-authorize: Stop service → Restart app → Tap "Start Service" again

### 2. Screenshot Resolution & Compression
- Full-screen capture sent as JPEG (70% quality)
- Scaled down to max 1080×1920 to respect Vercel 4.5MB body limit
- Typical size: 300–800KB

### 3. Daily Limit Detection
- Checked locally in SharedPreferences
- **Not enforced server-side** (MVP testing only)
- For production, move count to backend + authentication

### 4. Floating Window Permission
- Must be manually enabled in **Settings → Apps → Special app access → Display over other apps**
- No programmatic way to request on Android 6+

### 5. Reliability on Long Service Life
- Service can be killed by OS if low on memory
- Will lose MediaProjection object (needs restart)
- Consider using `Service.startForeground()` with persistent notification (already implemented)

## Testing Checklist

- [ ] App launches, shows main screen
- [ ] Tapping "Start Service" prompts permissions in order
- [ ] Floating icon appears after service starts
- [ ] Dragging floating icon works smoothly
- [ ] Tapping floating icon triggers screenshot
- [ ] Result overlay appears with credibility score
- [ ] Can tap "Close" button to dismiss overlay
- [ ] After 5 screenshots, button shows "Daily limit reached"
- [ ] Closing and reopening app resets daily count (date-based)
- [ ] No crashes on low-memory devices (< 2GB)

## Debugging

### Common Issues

**Issue**: Floating icon doesn't appear
- **Fix**: Check "Display over other apps" in Settings
- **Fix**: Service killed by OS (check logs: `adb logcat | grep TruthScan`)

**Issue**: Screenshot capture fails
- **Fix**: Verify MediaProjection permission was granted
- **Fix**: Check device storage (ImageReader needs temporary buffer)

**Issue**: API call times out
- **Fix**: Check backend is deployed and running
- **Fix**: Check internet connectivity
- **Fix**: Verify BACKEND_URL is correct (no trailing slash)

**Issue**: Analysis result doesn't show
- **Fix**: Check OkHttp response logging (add logging interceptor)
- **Fix**: Verify `x-app-secret` header matches backend `APP_SHARED_SECRET`

### View Logs

```bash
# Real-time logs
adb logcat | grep TruthScan

# Kill & restart app
adb shell am force-stop com.truthscan.app
adb shell am start -n com.truthscan.app/.MainActivity
```

## Performance Notes

- Screenshot capture: **1–3 seconds** (depending on device)
- Compression: **< 1 second**
- Network upload: **500ms–2s** (depends on bandwidth)
- Gemini processing: **5–10 seconds**
- **Total**: ~10–15 seconds per check

## Future Improvements

- [ ] Add paid tier (50 checks/day) with in-app purchase
- [ ] Move usage tracking to backend
- [ ] Add detailed analysis breakdown (sources, fact-check links)
- [ ] Support video frame extraction
- [ ] Offline mode with cached results
- [ ] Export analysis history as PDF

## License

MIT
