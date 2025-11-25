package com.ngobi.chesslock

import android.accessibilityservice.AccessibilityService
import android.app.Service
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility Service for ChessLock to provide screen locking on Android 9+ devices
 * while maintaining fingerprint unlock functionality.
 */
class ChessLockAccessibilityService : AccessibilityService() {
    
    companion object {
        const val ACTION_LOCK = "com.ngobi.chesslock.LOCK"
        const val ACTION_LAUNCH_CHESS = "com.ngobi.chesslock.LAUNCH_CHESS"
        private const val TAG = "ChessLockAccessibility"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No need to handle accessibility events for lock screen functionality
        Log.d(TAG, "onAccessibilityEvent: ${event?.eventType}")
    }

    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt: Accessibility service interrupted")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")
        
        when (intent?.action) {
            ACTION_LOCK -> {
                lockScreenWithChess()
            }
            ACTION_LAUNCH_CHESS -> {
                launchChessGame()
            }
        }
        
        return Service.START_STICKY
    }

    /**
     * Lock the screen using accessibility service and launch chess game
     */
    private fun lockScreenWithChess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Log.d(TAG, "Performing global lock screen action")
            val lockResult = performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            
            if (lockResult) {
                Log.d(TAG, "Lock screen successful, launching chess overlay")
                // Give a small delay for lock screen to activate, then launch chess
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    launchChessGame()
                }, 500)
            } else {
                Log.e(TAG, "Failed to lock screen via accessibility service")
                // Fallback to direct chess launch
                launchChessGame()
            }
        } else {
            Log.w(TAG, "Global lock action not available on this Android version")
            launchChessGame()
        }
    }

    /**
     * Launch the chess game overlay
     */
    private fun launchChessGame() {
        try {
            val chessIntent = Intent(this, OverlayLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or 
                         Intent.FLAG_ACTIVITY_CLEAR_TOP or
                         Intent.FLAG_ACTIVITY_SINGLE_TOP or
                         Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            startActivity(chessIntent)
            Log.d(TAG, "Chess game launched successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch chess game", e)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "ChessLock Accessibility Service connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "ChessLock Accessibility Service unbound")
        return super.onUnbind(intent)
    }
}