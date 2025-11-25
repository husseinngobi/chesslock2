package com.ngobi.chesslock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * ChessLock Foreground Service - Maintains persistent lockscreen functionality
 * Prevents Android from killing ChessLock and ensures reliable operation
 */
class ChessLockService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "chesslock_service"
        private const val CHANNEL_NAME = "ChessLock Service"
    }
    
    private lateinit var wakeReceiver: WakeReceiver
    
    override fun onCreate() {
        super.onCreate()
        Log.d("ChessLockService", "ChessLock service created")
        
        createNotificationChannel()
        registerWakeReceiver()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ChessLockService", "ChessLock service started")
        
        val autoStartAfterBoot = intent?.getBooleanExtra("auto_start_after_boot", false) ?: false
        
        if (autoStartAfterBoot) {
            Log.d("ChessLockService", "Service started after boot - ChessLock ready")
        }
        
        // Start foreground service with notification
        val notification = createServiceNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // Ensure ChessLock is marked as active
        val prefs = getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("chess_lock_active", true).apply()
        
        // Return START_STICKY to ensure service restarts if killed
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d("ChessLockService", "ChessLock service destroyed")
        
        try {
            unregisterReceiver(wakeReceiver)
        } catch (e: Exception) {
            Log.w("ChessLockService", "Error unregistering wake receiver: ${e.message}")
        }
        
        // Mark ChessLock as inactive when service stops
        val prefs = getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("chess_lock_active", false).apply()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps ChessLock running in the background"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createServiceNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("♛ ChessLock Active")
            .setContentText("Chess puzzle lockscreen is protecting your device")
            .setSmallIcon(R.drawable.ic_chess_queen)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .build()
    }
    
    private fun registerWakeReceiver() {
        try {
            wakeReceiver = WakeReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
                priority = 1000 // High priority to intercept quickly
            }
            
            registerReceiver(wakeReceiver, filter)
            Log.d("ChessLockService", "✓ Wake receiver registered with high priority")
            
        } catch (e: Exception) {
            Log.e("ChessLockService", "Failed to register wake receiver: ${e.message}")
        }
    }
}