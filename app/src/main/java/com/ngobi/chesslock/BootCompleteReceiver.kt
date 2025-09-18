package com.ngobi.chesslock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Boot Complete Receiver - Ensures ChessLock auto-activates after device restart
 * Critical for maintaining lockscreen functionality across reboots
 */
class BootCompleteReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootCompleteReceiver", "Received action: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d("BootCompleteReceiver", "Device boot completed - checking ChessLock auto-activation")
                handleBootCompleted(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d("BootCompleteReceiver", "App updated - re-initializing ChessLock")
                handleBootCompleted(context)
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                // Only handle our own package replacement
                if (intent.dataString?.contains(context.packageName) == true) {
                    Log.d("BootCompleteReceiver", "ChessLock package replaced - re-initializing")
                    handleBootCompleted(context)
                }
            }
        }
    }
    
    private fun handleBootCompleted(context: Context) {
        val prefs = context.getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
        val wasChessLockActive = prefs.getBoolean("chess_lock_was_active_before_reboot", false)
        val autoActivateAfterBoot = prefs.getBoolean("auto_activate_after_boot", true)
        
        Log.d("BootCompleteReceiver", "Boot check - was active: $wasChessLockActive, auto-activate: $autoActivateAfterBoot")
        
        // Check if permissions are still granted after reboot
        val hasOverlayPermission = android.provider.Settings.canDrawOverlays(context)
        val hasAdminPermission = LockAdminReceiver.isAdminActive(context)
        
        if (hasOverlayPermission && hasAdminPermission) {
            if (autoActivateAfterBoot && wasChessLockActive) {
                // Auto-activate ChessLock after boot
                prefs.edit().putBoolean("chess_lock_active", true).apply()
                
                // Start foreground service to maintain ChessLock
                try {
                    val serviceIntent = Intent(context, ChessLockService::class.java).apply {
                        putExtra("auto_start_after_boot", true)
                    }
                    context.startForegroundService(serviceIntent)
                    Log.d("BootCompleteReceiver", "✓ ChessLock auto-activated after boot")
                } catch (e: Exception) {
                    Log.e("BootCompleteReceiver", "Failed to start ChessLock service after boot: ${e.message}")
                }
            } else {
                Log.d("BootCompleteReceiver", "ChessLock not auto-activated (auto: $autoActivateAfterBoot, was active: $wasChessLockActive)")
            }
        } else {
            Log.w("BootCompleteReceiver", "Missing permissions after boot - overlay: $hasOverlayPermission, admin: $hasAdminPermission")
            // Deactivate ChessLock if permissions were revoked
            prefs.edit().putBoolean("chess_lock_active", false).apply()
        }
    }
}