# ChessLock for Tecno/Infinix Devices - ADB Installation Required

## The Real Problem

Tecno/Infinix devices on Android 13+ **block Accessibility Service** for sideloaded apps with:
> "Restricted setting - For your security, this setting is currently unavailable"

## The ONLY Solution That Works

**You MUST use ADB (Android Debug Bridge)** to grant Accessibility permission. This bypasses Tecno's restriction.

### What We Fixed

1. Properly signed release certificate
2. Android 13+ compatible with queries tag
3. Created automatic ADB installation script
4. Accessibility Service properly configured

## Installation Steps for Tecno Devices

### AUTOMATIC METHOD (Recommended)

1. **Enable USB Debugging on your phone:**
   - Settings → About Phone → Tap "Build Number" 7 times
   - Go back → System → Developer Options
   - Enable "USB Debugging"

2. **Connect phone to PC via USB cable**

3. **Allow USB debugging when popup appears on phone**

4. **Run the installation script:**
   - Double-click `INSTALL_TECNO.bat` in the project folder
   - Wait for installation to complete

5. **Done!** Open ChessLock app and lock your phone to test

### MANUAL METHOD (If script doesn't work)

If the automatic script fails, do this manually:

1. **Uninstall old ChessLock** (if installed)

2. **Enable USB Debugging:**
   - Settings → About Phone → Tap "Build Number" 7 times
   - Settings → System → Developer Options → Enable "USB Debugging"

3. **Connect phone to PC via USB**

4. **Open PowerShell in project folder and run:**

```powershell
cd android
$sdk = (Get-Content "local.properties" | Select-String "sdk.dir" | ForEach-Object { $_.ToString().Split("=")[1].Trim() })
& "$sdk\platform-tools\adb.exe" install -r -g app\build\outputs\apk\release\app-release.apk
& "$sdk\platform-tools\adb.exe" shell pm grant com.example.chesslock android.permission.SYSTEM_ALERT_WINDOW
& "$sdk\platform-tools\adb.exe" shell settings put secure enabled_accessibility_services com.example.chesslock/.ChessLockAccessibilityService
& "$sdk\platform-tools\adb.exe" shell settings put secure accessibility_enabled 1
```

**Open ChessLock app** and tap the green "ChessLock ENABLED" button

**Test:** Lock phone → Unlock with fingerprint → Chess should appear!

## What Makes This Work on Tecno

**The Problem:** Tecno/Infinix devices block Accessibility Service for sideloaded apps.

**Our Solution:**

- ChessLock no longer uses Accessibility Service at all
- Uses ScreenStateReceiver instead (built into Android)
- Works on ALL devices including Tecno/Infinix/Itel
- No "Restricted setting" errors
- No special workarounds needed

How to Use ChessLock

In the App

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
