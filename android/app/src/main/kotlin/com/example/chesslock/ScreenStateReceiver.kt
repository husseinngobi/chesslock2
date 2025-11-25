package com.example.chesslock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * ScreenStateReceiver - Alternative to Accessibility Service
 * 
 * This receiver listens for screen on/off events and triggers the chess lockscreen.
 * Works on ALL Android devices including Tecno/Infinix/Itel which block Accessibility.
 * 
 * How it works:
 * 1. User locks phone (screen turns off)
 * 2. User unlocks with PIN/Pattern (screen turns on)
 * 3. This receiver intercepts SCREEN_ON event
 * 4. Shows chess lockscreen overlay
 */
class ScreenStateReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        
        val action = intent?.action ?: return
        
        when (action) {
            Intent.ACTION_SCREEN_OFF -> {
                Log.d("ChessLock", "📱 Screen turned OFF")
                // Screen is off - prepare for lockscreen
            }
            
            Intent.ACTION_SCREEN_ON -> {
                Log.d("ChessLock", "📱 Screen turned ON - checking if we should show chess")
                
                // Check if ChessLock is enabled
                val prefs = context.getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
                val isEnabled = prefs.getBoolean("chesslockEnabled", true)
                
                if (isEnabled) {
                    Log.d("ChessLock", "✅ ChessLock enabled - triggering chess overlay via Accessibility Service")
                    
                    // Trigger the chess overlay via AccessibilityService
                    val intent = Intent(ChessLockAccessibilityService.ACTION_SHOW_OVERLAY).apply {
                        setPackage(context.packageName) // Explicit broadcast for Android 8+
                    }
                    context.sendBroadcast(intent)
                } else {
                    Log.d("ChessLock", "❌ ChessLock disabled in settings")
                }
            }
            
            Intent.ACTION_USER_PRESENT -> {
                // User unlocked device (PIN/Pattern entered successfully)
                Log.d("ChessLock", "🔓 User authenticated with system lock")
            }
        }
    }
}
