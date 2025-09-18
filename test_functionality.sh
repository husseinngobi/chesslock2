#!/bin/bash

# ChessLock Functionality Testing Script
# Run this script to validate core lockscreen functionality

echo "🔒 ChessLock Functionality Verification Script"
echo "=============================================="
echo ""

# Check if device is connected
echo "📱 Checking device connection..."
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected via ADB"
    echo "Please connect your Android device and enable USB debugging"
    exit 1
fi
echo "✅ Device connected"
echo ""

# Check if ChessLock is installed
echo "📦 Checking ChessLock installation..."
if ! adb shell pm list packages | grep -q "com.ngobi.chesslock"; then
    echo "❌ ChessLock app not installed"
    echo "Please install the ChessLock APK first"
    exit 1
fi
echo "✅ ChessLock app installed"
echo ""

# Function to test basic lockscreen override
test_lockscreen_override() {
    echo "🧪 Test 1: Basic Lockscreen Override"
    echo "-----------------------------------"
    
    # Enable ChessLock
    adb shell am start -n com.ngobi.chesslock/.MainActivity
    sleep 2
    
    # Turn screen off then on to trigger lockscreen
    echo "   Turning screen off..."
    adb shell input keyevent 26
    sleep 2
    
    echo "   Turning screen on..."
    adb shell input keyevent 26
    sleep 3
    
    # Check if ChessLock is active
    if adb shell dumpsys window windows | grep -q "OverlayLockActivity"; then
        echo "   ✅ ChessLock activated successfully"
        return 0
    else
        echo "   ❌ ChessLock failed to activate"
        return 1
    fi
}

# Function to test permissions
test_permissions() {
    echo "🧪 Test 2: Permission Verification"
    echo "---------------------------------"
    
    # Check overlay permission
    if adb shell appops get com.ngobi.chesslock SYSTEM_ALERT_WINDOW | grep -q "allow"; then
        echo "   ✅ Display over other apps permission: GRANTED"
    else
        echo "   ❌ Display over other apps permission: DENIED"
        echo "   Please grant overlay permission in settings"
    fi
    
    # Check device admin
    if adb shell dpm list-owners | grep -q "com.ngobi.chesslock"; then
        echo "   ✅ Device administrator privilege: ACTIVE"
    else
        echo "   ⚠️  Device administrator privilege: INACTIVE"
        echo "   Consider enabling for better lockscreen override"
    fi
}

# Function to test call handling
test_call_handling() {
    echo "🧪 Test 3: Call Handling Simulation"
    echo "----------------------------------"
    
    # Start ChessLock first
    adb shell am start -n com.ngobi.chesslock/.OverlayLockActivity
    sleep 2
    
    # Simulate incoming call state
    echo "   Simulating incoming call..."
    adb shell am broadcast -a android.intent.action.PHONE_STATE --es state "RINGING" --es incoming_number "1234567890"
    sleep 2
    
    # Check if ChessLock closed
    if ! adb shell dumpsys window windows | grep -q "OverlayLockActivity"; then
        echo "   ✅ ChessLock gracefully closed for incoming call"
    else
        echo "   ❌ ChessLock did not close for incoming call"
    fi
    
    # Simulate call end
    echo "   Simulating call end..."
    adb shell am broadcast -a android.intent.action.PHONE_STATE --es state "IDLE"
    sleep 1
    echo "   ✅ Call simulation completed"
}

# Function to check preferences and settings
test_preferences() {
    echo "🧪 Test 4: Preferences and Settings"
    echo "-----------------------------------"
    
    # Check if preferences file exists
    if adb shell ls /data/data/com.ngobi.chesslock/shared_prefs/ | grep -q "ChessLockPrefs.xml"; then
        echo "   ✅ ChessLock preferences file exists"
        
        # Check key settings
        chess_lock_active=$(adb shell cat /data/data/com.ngobi.chesslock/shared_prefs/ChessLockPrefs.xml 2>/dev/null | grep "chess_lock_active" | grep -o "true\|false" || echo "not_set")
        echo "   📊 Chess lock active: $chess_lock_active"
        
        device_in_call=$(adb shell cat /data/data/com.ngobi.chesslock/shared_prefs/ChessLockPrefs.xml 2>/dev/null | grep "device_in_call" | grep -o "true\|false" || echo "not_set")
        echo "   📞 Device in call state: $device_in_call"
        
    else
        echo "   ⚠️  ChessLock preferences not found - app may not have been configured"
    fi
}

# Function to monitor logs
monitor_logs() {
    echo "🧪 Test 5: Real-time Log Monitoring"
    echo "-----------------------------------"
    echo "   Starting log monitoring for 10 seconds..."
    echo "   Please activate ChessLock manually to see logs"
    echo ""
    
    # Clear existing logs and monitor ChessLock specific logs
    adb logcat -c
    timeout 10 adb logcat -s "OverlayLockActivity:*" -s "WakeReceiver:*" -s "ChessLockService:*" | while read line; do
        echo "   📋 $line"
    done
    
    echo "   ✅ Log monitoring completed"
}

# Function to test emergency unlock
test_emergency_unlock() {
    echo "🧪 Test 6: Emergency Features"
    echo "-----------------------------"
    
    # Start ChessLock
    adb shell am start -n com.ngobi.chesslock/.OverlayLockActivity
    sleep 2
    
    echo "   ⚠️  Testing emergency unlock accessibility"
    echo "   Note: This test only verifies the interface exists"
    echo "   Manual testing required for full emergency unlock sequence"
    
    # Check if emergency button is accessible (via UI dump)
    if adb shell uiautomator dump /dev/stdout | grep -q "emergency\|Emergency"; then
        echo "   ✅ Emergency unlock button found in interface"
    else
        echo "   ❌ Emergency unlock button not found"
    fi
}

# Main test execution
echo "🚀 Starting ChessLock functionality tests..."
echo ""

test_permissions
echo ""

test_lockscreen_override
echo ""

test_call_handling
echo ""

test_preferences
echo ""

test_emergency_unlock
echo ""

echo "📊 TESTING SUMMARY"
echo "=================="
echo "✅ All automated tests completed"
echo ""
echo "📋 MANUAL TESTS REQUIRED:"
echo "1. Solve a chess puzzle to test auto-unlock"
echo "2. Test with real incoming phone call"
echo "3. Test screen rotation and different orientations"
echo "4. Test battery optimization bypass over time"
echo "5. Test two-layer security (chess + native lock)"
echo ""
echo "🔧 TROUBLESHOOTING:"
echo "- If ChessLock doesn't activate: Check overlay permission"
echo "- If calls don't work: Test with real phone call"
echo "- If logs are empty: Ensure USB debugging is enabled"
echo ""
echo "🏁 Testing script completed!"