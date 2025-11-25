# ChessLock for Tecno/Infinix Devices - WORKING

## What Was Fixed

**MAJOR UPDATE:** ChessLock now works on **ALL Tecno/Infinix/Itel devices** without needing Accessibility Service!

### Technical Changes

1. Removed Accessibility Service requirement completely
2. Uses ScreenStateReceiver instead (works on ALL devices)
3. Properly signed with release certificate
4. Android 13+ compatible with queries tag
5. No "Restricted setting" errors anymore

## Installation Steps

### Step 1: Uninstall Old Version

If you have an old version installed:

Settings → Apps → ChessLock → Uninstall

### Step 2: Enable Unknown Sources

Settings → Security → Install unknown apps → Find your file manager (e.g., Files, My Files) → Enable "Allow from this source"

### Step 3: Install the Release APK

Transfer the new `app-release.apk` to your phone and install it.

### Step 4: Grant Overlay Permission

Open ChessLock app and:

1. Tap "Enable Draw Over Other Apps" button
2. Find ChessLock in the list
3. Toggle "Allow display over other apps" to ON
4. Return to ChessLock app

### Step 5: Enable ChessLock

Tap the big "ChessLock ENABLED" button in the app (It will turn green when active)

### Step 6: Test It

1. Lock your phone (press power button)
2. Unlock with your PIN/Pattern/Fingerprint
3. Chess lockscreen should appear immediately
4. Play chess to unlock your phone

## What Makes This Work on Tecno

**The Problem:** Tecno/Infinix devices block Accessibility Service for sideloaded apps.

**Our Solution:**

- ChessLock no longer uses Accessibility Service at all
- Uses ScreenStateReceiver instead (built into Android)
- Works on ALL devices including Tecno/Infinix/Itel
- No "Restricted setting" errors
- No special workarounds needed

## How to Use ChessLock

### In the App

- **Big green button:** ChessLock is enabled (lock phone to test)
- **Big red button:** ChessLock is disabled (phone works normally)
- Change game modes, difficulty, puzzles, etc.

### On Lockscreen

- Solve the chess puzzle OR defeat AI
- Tap "HINT" for help
- Tap "NEW PUZZLE" to switch puzzles
- Emergency unlock: Lose 10 games in a row (automatic bypass)

## Troubleshooting

### ChessLock doesnt appear when I lock phone

1. **Check if enabled:**
   - Open ChessLock app
   - Button should be GREEN and say "ChessLock ENABLED"
   - If red, tap it to enable

2. **Check overlay permission:**
   - Open ChessLock app
   - Tap "Enable Draw Over Other Apps"
   - Make sure ChessLock is allowed

3. **Restart the service:**
   - Tap "ChessLock DISABLED" (turn it red)
   - Tap again to enable (turn it green)
   - Lock phone to test

### App crashes or doesnt work

- Uninstall completely
- Restart phone
- Reinstall the new app-release.apk
- Follow steps above

## Success

If you see the chess lockscreen after unlocking your phone, it is working perfectly!

**No Accessibility Service needed on Tecno devices!**
