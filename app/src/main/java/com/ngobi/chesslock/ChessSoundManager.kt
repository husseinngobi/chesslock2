package com.ngobi.chesslock

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit
import androidx.preference.PreferenceManager

/**
 * Manages chess move sound effects with volume control
 * Provides realistic chess.com-style sound feedback
 */
class ChessSoundManager(private val context: Context) {
    
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var soundPool: SoundPool? = null
    private var soundIds = mutableMapOf<SoundType, Int>()
    private var isInitialized = false
    
    enum class SoundType {
        MOVE,           // Regular piece move
        CAPTURE,        // Piece capture
        CASTLE,         // Castling move
        CHECK,          // Check warning
        CHECKMATE,      // Game end - checkmate
        PUZZLE_SOLVED,  // Puzzle completion
        ILLEGAL_MOVE,   // Invalid move attempt
        PIECE_SELECT    // Piece selection
    }
    
    init {
        initializeSoundPool()
    }
    
    private fun initializeSoundPool() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            
            soundPool = SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(audioAttributes)
                .build()
            
            loadSounds()
            isInitialized = true
        } catch (e: Exception) {
            // Fallback to system sounds if custom sounds fail
            isInitialized = false
        }
    }
    
    private fun loadSounds() {
        // For now, we'll use system sounds with different patterns
        // In a real implementation, you would load actual chess sound files here
        
        // Create different sound patterns using system notification tones
        // This gives different audio feedback for different move types
        useSystemSoundFallbacks()
    }
    
    private fun useSystemSoundFallbacks() {
        // Use system notification sound as fallback
        // In a real implementation, you'd include actual chess sound files
        try {
            soundPool?.let { pool ->
                // All sounds use the same system notification for now
                val systemSoundId = android.media.RingtoneManager.getDefaultUri(
                    android.media.RingtoneManager.TYPE_NOTIFICATION
                )
                // Note: This is a simplified fallback
                // Real implementation would have distinct sound files
            }
        } catch (e: Exception) {
            // Silent fallback
        }
    }
    
    /**
     * Play a chess move sound effect
     */
    fun playSound(soundType: SoundType) {
        if (!isSoundEnabled() || !isInitialized || soundPool == null) {
            return
        }
        
        val volume = getSoundVolume()
        val soundId = soundIds[soundType]
        
        if (soundId != null && soundId > 0) {
            soundPool?.play(soundId, volume, volume, 0, 0, 1.0f)
        } else {
            // Play system sound as fallback
            playSystemSoundFallback(soundType)
        }
    }
    
    private fun playSystemSoundFallback(soundType: SoundType) {
        try {
            // Use ToneGenerator for chess-like sounds with volume control
            val toneGenerator = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_NOTIFICATION,
                (getSoundVolume() * 100).toInt()
            )
            
            when (soundType) {
                SoundType.MOVE -> {
                    // Short, subtle click for regular moves
                    toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 100)
                }
                SoundType.CAPTURE -> {
                    // Slightly different tone for captures
                    toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 150)
                }
                SoundType.CASTLE -> {
                    // Double beep for castling
                    toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 100)
                    Thread.sleep(50)
                    toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 100)
                }
                SoundType.CHECK -> {
                    // Warning tone for check
                    toneGenerator.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                }
                SoundType.CHECKMATE, SoundType.PUZZLE_SOLVED -> {
                    // Victory tone
                    toneGenerator.startTone(android.media.ToneGenerator.TONE_CDMA_CONFIRM, 300)
                }
                SoundType.ILLEGAL_MOVE -> {
                    // Error tone
                    toneGenerator.startTone(android.media.ToneGenerator.TONE_CDMA_CALLDROP_LITE, 150)
                }
                SoundType.PIECE_SELECT -> {
                    // Soft selection sound
                    toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 50)
                }
            }
            
            // Release resources after a delay
            Handler(Looper.getMainLooper()).postDelayed({
                toneGenerator.release()
            }, 500)
            
        } catch (e: Exception) {
            // Silent fallback if ToneGenerator fails
        }
    }
    
    /**
     * Check if sound effects are enabled in settings
     */
    private fun isSoundEnabled(): Boolean {
        return prefs.getBoolean("soundEnabled", true)
    }
    
    /**
     * Get sound volume from settings (0.0 to 1.0)
     */
    private fun getSoundVolume(): Float {
        return prefs.getFloat("soundVolume", 0.7f)
    }
    
    
    companion object {
        @Volatile
        private var INSTANCE: ChessSoundManager? = null
        
        fun getInstance(context: Context): ChessSoundManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChessSoundManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
