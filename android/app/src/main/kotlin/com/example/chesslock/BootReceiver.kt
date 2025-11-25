package com.example.chesslock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("ChessLock", "🚀 Device booted - checking if lockscreen is enabled")
            
            context?.let {
                val prefs = it.getSharedPreferences("chesslock_prefs", Context.MODE_PRIVATE)
                val isEnabled = prefs.getBoolean("lockscreen_enabled", false)
                
                if (isEnabled) {
                    Log.d("ChessLock", "✅ Starting ChessLock service")
                    LockscreenService.start(it)
                } else {
                    Log.d("ChessLock", "❌ ChessLock is disabled")
                }
            }
        }
    }
}
