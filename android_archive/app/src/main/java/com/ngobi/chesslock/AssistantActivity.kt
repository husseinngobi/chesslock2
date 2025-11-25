package com.ngobi.chesslock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast

/**
 * Assistant Activity for ChessLock
 * Handles assistant app intents (home button long-press) to quickly activate ChessLock
 */
class AssistantActivity : Activity() {

    companion object {
        private const val TAG = "AssistantActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "Assistant activity triggered via ${intent.action}")
        
        // Handle assistant intent
        when (intent.action) {
            Intent.ACTION_ASSIST -> handleAssistIntent()
            "android.service.voice.VoiceInteractionService" -> handleVoiceIntent()
            "com.ngobi.chesslock.ACTIVATE_LOCK" -> handleDirectActivation()
            "com.ngobi.chesslock.GESTURE_TAP" -> handleGestureTap()
            "com.ngobi.chesslock.GESTURE_DOUBLE_TAP" -> handleGestureDoubleTap()
            "com.ngobi.chesslock.GESTURE_LONG_PRESS" -> handleGestureLongPress()
            else -> handleGenericAssistant()
        }
        
        // Finish immediately - we don't need a UI for assistant action
        finish()
    }

    private fun handleAssistIntent() {
        Log.d(TAG, "Handling ACTION_ASSIST intent")
        
        // Quick check for basic requirements
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "ChessLock: Overlay permission required", Toast.LENGTH_LONG).show()
            openMainActivity()
            return
        }
        
        // Try to activate ChessLock immediately
        activateChessLock()
    }

    private fun handleVoiceIntent() {
        Log.d(TAG, "Handling voice interaction intent")
        
        // For voice interactions, provide audio feedback
        Toast.makeText(this, "ChessLock activated via voice assistant", Toast.LENGTH_SHORT).show()
        activateChessLock()
    }

    private fun handleGenericAssistant() {
        Log.d(TAG, "Handling generic assistant intent")
        activateChessLock()
    }

    private fun handleDirectActivation() {
        Log.d(TAG, "Handling direct ChessLock activation")
        activateChessLock()
    }

    private fun handleGestureTap() {
        Log.d(TAG, "Handling launcher gesture: tap")
        Toast.makeText(this, "ChessLock activated via tap gesture", Toast.LENGTH_SHORT).show()
        activateChessLock()
    }

    private fun handleGestureDoubleTap() {
        Log.d(TAG, "Handling launcher gesture: double tap")
        Toast.makeText(this, "ChessLock activated via double tap", Toast.LENGTH_SHORT).show()
        activateChessLock()
    }

    private fun handleGestureLongPress() {
        Log.d(TAG, "Handling launcher gesture: long press")
        Toast.makeText(this, "ChessLock activated via long press", Toast.LENGTH_SHORT).show()
        activateChessLock()
    }

    private fun activateChessLock() {
        try {
            // Use the same intelligent lock strategy as MainActivity
            val strategy = LockStrategyManager.getBestLockStrategy(this)
            val lockSuccess = LockStrategyManager.executeLockStrategy(this, strategy)
            
            if (lockSuccess) {
                Log.d(TAG, "ChessLock activated successfully via assistant")
                Toast.makeText(this, "🔐 ChessLock activated!", Toast.LENGTH_SHORT).show()
                
                // Mark as active and start service
                val prefs = getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("chess_lock_active", true)
                    .putBoolean("activated_via_assistant", true)
                    .apply()
                
                // Start foreground service for persistence
                val serviceIntent = Intent(this, ChessLockService::class.java)
                startForegroundService(serviceIntent)
                
            } else {
                Log.w(TAG, "Failed to activate ChessLock via assistant")
                Toast.makeText(this, "ChessLock setup required - opening app", Toast.LENGTH_LONG).show()
                openMainActivity()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error activating ChessLock via assistant", e)
            Toast.makeText(this, "ChessLock error - opening app", Toast.LENGTH_LONG).show()
            openMainActivity()
        }
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("triggered_by_assistant", true)
        }
        startActivity(intent)
    }
}