package com.ngobi.chesslock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

class WakeReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WakeReceiver", "Received action: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                Log.d("WakeReceiver", "Screen turned on - checking ChessLock activation")
                handleScreenWake(context, "SCREEN_ON")
            }
            Intent.ACTION_USER_PRESENT -> {
                Log.d("WakeReceiver", "User present (device unlocked by system) - intercepting for ChessLock")
                handleScreenWake(context, "USER_PRESENT")
            }
            Intent.ACTION_SCREEN_OFF -> {
                Log.d("WakeReceiver", "Screen turned off - preparing ChessLock for next wake")
                handleScreenOff(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d("WakeReceiver", "Boot completed - ChessLock ready for operation")
                // Boot handling is done by BootCompleteReceiver, but we log for monitoring
            }
            "android.intent.action.PHONE_STATE" -> {
                Log.d("WakeReceiver", "Phone state changed - may affect lockscreen")
                handlePhoneState(context, intent)
            }
            "android.intent.action.NEW_OUTGOING_CALL" -> {
                Log.d("WakeReceiver", "Outgoing call - temporarily disable ChessLock")
                handleCallState(context, true)
            }
        }
    }
    
    private fun handleScreenWake(context: Context, source: String) {
        // Check if ChessLock is active and should intercept
        val prefs = context.getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
        val isChessLockActive = prefs.getBoolean("chess_lock_active", false)
        val isInCall = prefs.getBoolean("device_in_call", false)
        
        Log.d("WakeReceiver", "Screen wake from $source - ChessLock active: $isChessLockActive, in call: $isInCall")
        
        if (isChessLockActive && !isInCall) {
            // Check if we have overlay permission and admin access
            if (Settings.canDrawOverlays(context) && LockAdminReceiver.isAdminActive(context)) {
                Log.d("WakeReceiver", "Launching ChessLock overlay for wake source: $source")
                launchChessLockOverlay(context, source)
            } else {
                Log.w("WakeReceiver", "Missing permissions - overlay: ${Settings.canDrawOverlays(context)}, admin: ${LockAdminReceiver.isAdminActive(context)}")
                // Auto-deactivate if permissions are missing
                prefs.edit().putBoolean("chess_lock_active", false).apply()
            }
        } else {
            Log.d("WakeReceiver", "ChessLock not activated for wake - active: $isChessLockActive, in call: $isInCall")
        }
    }
    
    private fun handleScreenOff(context: Context) {
        // Mark that screen is off and prepare for next wake
        val prefs = context.getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("last_screen_off_time", System.currentTimeMillis())
            .putBoolean("screen_was_off", true)
            .apply()
        
        Log.d("WakeReceiver", "Screen off - ChessLock prepared for next wake")
    }
    
    private fun handlePhoneState(context: Context, intent: Intent) {
        val state = intent.getStringExtra("state")
        val prefs = context.getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
        
        when (state) {
            "RINGING", "OFFHOOK" -> {
                // Incoming or active call - temporarily disable ChessLock
                prefs.edit().putBoolean("device_in_call", true).apply()
                Log.d("WakeReceiver", "Call active - ChessLock temporarily disabled")
            }
            "IDLE" -> {
                // Call ended - re-enable ChessLock if it was active
                prefs.edit().putBoolean("device_in_call", false).apply()
                Log.d("WakeReceiver", "Call ended - ChessLock re-enabled")
            }
        }
    }
    
    private fun handleCallState(context: Context, isOutgoing: Boolean) {
        val prefs = context.getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("device_in_call", isOutgoing).apply()
        Log.d("WakeReceiver", "Outgoing call - ChessLock temporarily disabled")
    }
    
    private fun launchChessLockOverlay(context: Context, source: String = "UNKNOWN") {
        try {
            // IMMEDIATE LAUNCH - Use OverlayLockActivity with full chess functionality
            val overlayIntent = Intent(context, OverlayLockActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK 
                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    or Intent.FLAG_ACTIVITY_NO_ANIMATION  // Prevent flash during transition
                    or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    or Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
                    or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT // Ensure it comes to front
                )
                putExtra("from_wake_receiver", true)
                putExtra("immediate_override", true) // Signal for instant override
                putExtra("is_preview", false) // This is the real lockscreen
                putExtra("wake_source", source) // Track what triggered the wake
            }
            
            // Launch immediately with highest priority
            context.startActivity(overlayIntent)
            
            // Additional aggressive measures to ensure override
            val prefs = context.getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putLong("last_override_attempt", System.currentTimeMillis())
                .putString("last_wake_source", source)
                .apply()
            
            Log.d("WakeReceiver", "OverlayLockActivity launched from $source with aggressive override")
        } catch (e: Exception) {
            Log.e("WakeReceiver", "Failed to launch ChessLock overlay from $source: ${e.message}", e)
            
            // Fallback to simplified lockscreen replacement if needed
            try {
                val fallbackIntent = Intent(context, LockscreenReplacementActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("from_wake_receiver", true)
                    putExtra("immediate_override", true)
                    putExtra("wake_source", source)
                }
                context.startActivity(fallbackIntent)
                Log.d("WakeReceiver", "Fallback to LockscreenReplacementActivity from $source")
            } catch (fallbackException: Exception) {
                Log.e("WakeReceiver", "Both overlay launch attempts failed from $source: ${fallbackException.message}", fallbackException)
            }
        }
    }
}
