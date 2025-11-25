@echo off
REM ========================================
REM ChessLock - Tecno Device ADB Installer
REM ========================================
REM
REM This script installs ChessLock and grants Accessibility permission
REM on Tecno/Infinix devices that block manual Accessibility setup.
REM
REM REQUIREMENTS:
REM 1. USB Debugging enabled on phone
REM 2. Phone connected to PC via USB
REM 3. ADB drivers installed (comes with Android SDK)
REM
REM ========================================

echo.
echo ========================================
echo    ChessLock ADB Installer for Tecno
echo ========================================
echo.

REM Find Android SDK path from local.properties
for /f "tokens=2 delims==" %%a in ('findstr "sdk.dir" "android\local.properties"') do set SDK_PATH=%%a
set SDK_PATH=%SDK_PATH: =%
set ADB=%SDK_PATH%\platform-tools\adb.exe

if not exist "%ADB%" (
    echo ERROR: ADB not found at: %ADB%
    echo.
    echo Please make sure Android SDK is installed.
    pause
    exit /b 1
)

echo Using ADB: %ADB%
echo.

REM Check if device is connected
echo Checking for connected devices...
"%ADB%" devices
echo.

echo Step 1: Installing ChessLock APK...
"%ADB%" install -r -g "android\app\build\outputs\apk\release\app-release.apk"
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Installation failed!
    echo.
    echo Make sure:
    echo   1. USB Debugging is enabled
    echo   2. Phone is connected via USB
    echo   3. You allowed USB debugging popup on phone
    pause
    exit /b 1
)
echo.

echo Step 2: Granting SYSTEM_ALERT_WINDOW permission...
"%ADB%" shell pm grant com.example.chesslock android.permission.SYSTEM_ALERT_WINDOW
echo.

echo Step 3: Enabling Accessibility Service...
echo NOTE: This uses ADB to bypass Tecno's restriction
"%ADB%" shell settings put secure enabled_accessibility_services com.example.chesslock/.ChessLockAccessibilityService
"%ADB%" shell settings put secure accessibility_enabled 1
echo.

echo ========================================
echo    Installation Complete!
echo ========================================
echo.
echo Next steps:
echo   1. Open ChessLock app on your phone
echo   2. Tap the green "ChessLock ENABLED" button
echo   3. Lock your phone
echo   4. Unlock with fingerprint/PIN
echo   5. Chess lockscreen should appear!
echo.
echo If it doesn't work, run this script again.
echo.
pause
