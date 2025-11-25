package com.ngobi.chesslock

import android.app.KeyguardManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class LockscreenReplacementActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle parameters from wake receiver
        val immediateOverride = intent.getBooleanExtra("immediate_override", false)
        val fromWakeReceiver = intent.getBooleanExtra("from_wake_receiver", false)
        
        // Setup lockscreen replacement BEFORE setting content
        setupFullLockscreenReplacement()
        
        // Create simple but effective interface
        createLockscreenInterface()
        
        // Handle back button with modern approach
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Prevent back button from exiting lockscreen
                Toast.makeText(this@LockscreenReplacementActivity, "Solve the puzzle to unlock", Toast.LENGTH_SHORT).show()
            }
        })
        
        if (fromWakeReceiver) {
            android.util.Log.d("LockscreenReplacement", "Launched from WakeReceiver with immediate override: $immediateOverride")
        }
    }
    
    private fun setupFullLockscreenReplacement() {
        // ANDROID 14 COMPATIBLE lockscreen replacement
        
        // Modern approach for Android 8.1+
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        
        // Request to appear over keyguard
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.requestDismissKeyguard(this, null)
        
        // CRITICAL: Set window flags to override everything
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        
        // Modern fullscreen replacement using window insets controller
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.systemBars())
        }
        
        // Additional Android 14 compatibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        // Hide system UI completely
        hideSystemUI()
    }
    
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ approach
            window.insetsController?.hide(WindowInsets.Type.systemBars())
            window.insetsController?.systemBarsBehavior = 
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            // Legacy approach
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
        
        // Hide system UI completely
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }
    
    private fun createLockscreenInterface() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(32, 64, 32, 64)
        }
        
        // Status indicator
        layout.addView(TextView(this).apply {
            text = getString(R.string.chesslock_active)
            textSize = 16f
            setTextColor(Color.RED)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        })
        
        // Main title with crown
        layout.addView(TextView(this).apply {
            text = getString(R.string.queen_piece)
            textSize = 64f
            setTextColor(Color.YELLOW)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        })
        
        layout.addView(TextView(this).apply {
            text = getString(R.string.chesslock_title)
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        })
        
        // Time display
        val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        layout.addView(TextView(this).apply {
            text = currentTime
            textSize = 48f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        })
        
        // Date display
        val currentDate = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault()).format(java.util.Date())
        layout.addView(TextView(this).apply {
            text = currentDate
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        })
        
        // Chess puzzle message
        layout.addView(TextView(this).apply {
            text = getString(R.string.solve_chess_puzzle)
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.DKGRAY)
        })
        
        // Puzzle area (simplified)
        layout.addView(TextView(this).apply {
            text = getString(R.string.chess_pieces_display)
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.GRAY)
        })
        
        // Unlock button (simulate puzzle completion)
        layout.addView(Button(this).apply {
            text = getString(R.string.puzzle_solved_unlock)
            textSize = 16f
            setOnClickListener {
                unlockDevice()
            }
            setPadding(16, 16, 16, 16)
        })
        
        // Emergency exit
        layout.addView(Button(this).apply {
            text = getString(R.string.emergency_exit)
            setOnClickListener { 
                finish()
            }
            setPadding(16, 8, 16, 8)
        })
        
        setContentView(layout)
    }
    
    private fun unlockDevice() {
        try {
            // Signal successful unlock
            Toast.makeText(this, "Device Unlocked!", Toast.LENGTH_SHORT).show()
            
            // Clear the lockscreen overlay
            finish()
            
            // Send broadcast that device should be unlocked
            val unlockIntent = android.content.Intent("com.ngobi.chesslock.UNLOCK_DEVICE")
            sendBroadcast(unlockIntent)
            
            android.util.Log.d("LockscreenReplacement", "Device unlock sequence completed")
            
        } catch (e: Exception) {
            android.util.Log.e("LockscreenReplacement", "Error during unlock: ${e.message}")
            Toast.makeText(this, "Unlock error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    

}
