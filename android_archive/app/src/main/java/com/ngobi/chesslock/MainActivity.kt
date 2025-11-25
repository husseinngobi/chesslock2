package com.ngobi.chesslock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var statusCard: MaterialCardView
    private lateinit var statusText: TextView
    private lateinit var statusIcon: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupClickListeners()
        
        // Run chess rules compliance verification for client confidence
        runChessRulesVerification()
    }
    
    /**
     * Verify chess rules compliance for client confidence
     */
    private fun runChessRulesVerification() {
        try {
            Log.i("MainActivity", "🏆 Running Chess Rules Compliance Verification...")
            
            // Quick verification summary
            val summary = ChessRulesVerification.quickVerificationSummary()
            Log.i("MainActivity", summary)
            
            // Run complete verification in background
            Thread {
                val isCompliant = ChessRulesVerification.runCompleteVerification()
                runOnUiThread {
                    if (isCompliant) {
                        Log.i("MainActivity", "✅ ChessLock is FULLY COMPLIANT with FIDE chess rules!")
                    } else {
                        Log.e("MainActivity", "❌ Chess rules compliance issues detected!")
                    }
                }
            }.start()
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during chess verification: ${e.message}")
        }
    }
    
    private fun initializeViews() {
        statusCard = findViewById(R.id.statusCard)
        statusText = findViewById(R.id.statusText)
        statusIcon = findViewById(R.id.statusIcon)
    }
    
    private fun setupClickListeners() {
        val openLockScreenButton = findViewById<Button>(R.id.openLockScreenButton)
        val previewLockScreenButton = findViewById<Button>(R.id.previewLockScreenButton)
        val openSettingsButton = findViewById<Button>(R.id.openSettingsButton)
        val openStatsButton = findViewById<Button>(R.id.openStatsButton)
        val openDonateButton = findViewById<Button>(R.id.openDonateButton)
        val setupButton = findViewById<Button>(R.id.setupButton)
        val exitAppButton = findViewById<Button>(R.id.exitAppButton)

        openLockScreenButton.setOnClickListener {
            if (isReadyToLock()) {
                launchOverlayLock()
            } else {
                showSetupRequiredDialog()
            }
        }

        previewLockScreenButton.setOnClickListener {
            // Temporarily disabled due to compilation issues
            // val intent = Intent(this, AccuratePreviewActivity::class.java)
            // startActivity(intent)
            val intent = Intent(this, PreviewActivity::class.java)
            startActivity(intent)
        }

        openSettingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        openStatsButton.setOnClickListener {
            val intent = Intent(this, StatsActivity::class.java)
            startActivity(intent)
        }

        openDonateButton.setOnClickListener {
            val intent = Intent(this, DonateActivity::class.java)
            startActivity(intent)
        }
        
        setupButton.setOnClickListener {
            startSetupProcess()
        }
        
        exitAppButton.setOnClickListener {
            showExitConfirmationDialog()
        }
    }
    
    private fun isReadyToLock(): Boolean {
        // Check overlay permission (required for all strategies)
        if (!Settings.canDrawOverlays(this)) {
            return false
        }
        
        // Check strategy-specific requirements
        val strategy = LockStrategyManager.getBestLockStrategy(this)
        return when (strategy) {
            LockStrategyManager.LockStrategy.ACCESSIBILITY_SERVICE -> {
                LockStrategyManager.isAccessibilityServiceEnabled(this)
            }
            LockStrategyManager.LockStrategy.DEVICE_ADMIN -> {
                LockStrategyManager.isDeviceAdminEnabled(this)
            }
            LockStrategyManager.LockStrategy.HUAWEI_EMUI -> {
                true // No additional permissions needed for Huawei EMUI
            }
            LockStrategyManager.LockStrategy.OVERLAY_ONLY -> {
                true // Only overlay permission needed
            }
        }
    }
    
    private fun startSetupProcess() {
        val steps = mutableListOf<String>()
        val strategy = LockStrategyManager.getBestLockStrategy(this)
        
        // Always check overlay permission
        if (!Settings.canDrawOverlays(this)) {
            steps.add("Enable overlay permission for lockscreen functionality")
        }
        
        // Check strategy-specific requirements
        when (strategy) {
            LockStrategyManager.LockStrategy.ACCESSIBILITY_SERVICE -> {
                if (!LockStrategyManager.isAccessibilityServiceEnabled(this)) {
                    steps.add("Enable ChessLock Accessibility Service (Android 9+ with fingerprint support)")
                }
            }
            LockStrategyManager.LockStrategy.DEVICE_ADMIN -> {
                if (!LockStrategyManager.isDeviceAdminEnabled(this)) {
                    steps.add("Grant device administrator permission for secure locking")
                }
            }
            LockStrategyManager.LockStrategy.HUAWEI_EMUI -> {
                // No additional setup needed for Huawei EMUI
            }
            LockStrategyManager.LockStrategy.OVERLAY_ONLY -> {
                steps.add("Note: Using overlay-only mode (limited functionality)")
            }
        }
        
        if (steps.isEmpty()) {
            Toast.makeText(this, "✅ ChessLock is already set up and ready!", Toast.LENGTH_SHORT).show()
            updateStatusDisplay()
            return
        }
        
        val strategyInfo = when (strategy) {
            LockStrategyManager.LockStrategy.ACCESSIBILITY_SERVICE -> "Using Accessibility Service (recommended for Android 9+)"
            LockStrategyManager.LockStrategy.DEVICE_ADMIN -> "Using Device Administrator (for older Android versions)"
            LockStrategyManager.LockStrategy.HUAWEI_EMUI -> "Using Huawei EMUI system lock"
            LockStrategyManager.LockStrategy.OVERLAY_ONLY -> "Using overlay-only mode"
        }
        
        val message = "ChessLock needs the following permissions to work properly:\n\n" +
                "Strategy: $strategyInfo\n\n" +
                steps.joinToString(separator = "\n• ", prefix = "• ") +
                "\n\nWould you like to set these up now?"
        
        AlertDialog.Builder(this)
            .setTitle("🔐 ChessLock Setup")
            .setMessage(message)
            .setPositiveButton("Setup Now") { _, _ ->
                performNextSetupStep(strategy)
            }
            .setNegativeButton("Later") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    private fun performNextSetupStep(strategy: LockStrategyManager.LockStrategy) {
        // First priority: overlay permission
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }
        
        // Second priority: strategy-specific permission
        when (strategy) {
            LockStrategyManager.LockStrategy.ACCESSIBILITY_SERVICE -> {
                if (!LockStrategyManager.isAccessibilityServiceEnabled(this)) {
                    LockStrategyManager.requestAccessibilityServicePermission(this)
                }
            }
            LockStrategyManager.LockStrategy.DEVICE_ADMIN -> {
                if (!LockStrategyManager.isDeviceAdminEnabled(this)) {
                    LockStrategyManager.requestDeviceAdminPermission(this)
                }
            }
            else -> {
                // No additional setup needed
                Toast.makeText(this, "✅ Setup complete!", Toast.LENGTH_SHORT).show()
                updateStatusDisplay()
            }
        }
    }
    
    private fun showSetupRequiredDialog() {
        // Use the enhanced setup guide dialog
        val setupDialog = SetupGuideDialog.newInstance()
        setupDialog.show(supportFragmentManager, "SetupGuideDialog")
    }

    override fun onResume() {
        super.onResume()
        updateStatusDisplay()
    }
    
    // Public method for external calls (e.g., from SetupGuideDialog)
    fun updateStatusDisplay() {
        updateStatusDisplayInternal()
    }
    
    private fun updateStatusDisplayInternal() {
        val overlayPermission = Settings.canDrawOverlays(this)
        val strategy = LockStrategyManager.getBestLockStrategy(this)
        val strategyReady = when (strategy) {
            LockStrategyManager.LockStrategy.ACCESSIBILITY_SERVICE -> {
                LockStrategyManager.isAccessibilityServiceEnabled(this)
            }
            LockStrategyManager.LockStrategy.DEVICE_ADMIN -> {
                LockStrategyManager.isDeviceAdminEnabled(this)
            }
            LockStrategyManager.LockStrategy.HUAWEI_EMUI -> true
            LockStrategyManager.LockStrategy.OVERLAY_ONLY -> true
        }
        
        val strategyName = when (strategy) {
            LockStrategyManager.LockStrategy.ACCESSIBILITY_SERVICE -> "Accessibility Service (Android 9+)"
            LockStrategyManager.LockStrategy.DEVICE_ADMIN -> "Device Administrator"
            LockStrategyManager.LockStrategy.HUAWEI_EMUI -> "Huawei EMUI"
            LockStrategyManager.LockStrategy.OVERLAY_ONLY -> "Overlay Only"
        }
        
        when {
            overlayPermission && strategyReady -> {
                statusIcon.text = "✅"
                statusText.text = "ChessLock is ready! Using $strategyName."
                statusCard.setCardBackgroundColor(getColor(R.color.difficulty_easy))
            }
            overlayPermission || strategyReady -> {
                statusIcon.text = "⚠️"
                val missingPermission = if (!overlayPermission) "overlay permission" else "$strategyName permission"
                statusText.text = "ChessLock partially set up. Missing $missingPermission."
                statusCard.setCardBackgroundColor(getColor(R.color.difficulty_medium))
            }
            else -> {
                statusIcon.text = "❌"
                statusText.text = "ChessLock needs setup. Strategy: $strategyName. Tap 'Complete Setup'."
                statusCard.setCardBackgroundColor(getColor(R.color.difficulty_hard))
            }
        }
    }

    private fun launchOverlayLock() {
        // Check overlay permission first
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission required for ChessLock", Toast.LENGTH_LONG).show()
            requestOverlayPermission()
            return
        }
        
        // Set ChessLock as active in preferences and save state for boot recovery
        val prefs = getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("chess_lock_active", true)
            .putBoolean("chess_lock_was_active_before_reboot", true) // For boot recovery
            .apply()
        
        // Use intelligent lock strategy based on platform
        val strategy = LockStrategyManager.getBestLockStrategy(this)
        Log.d("MainActivity", "Using lock strategy: $strategy")
        
        val lockSuccess = LockStrategyManager.executeLockStrategy(this, strategy)
        
        if (lockSuccess) {
            Toast.makeText(this, "🔐 ChessLock activated!", Toast.LENGTH_SHORT).show()
            
            // Start persistent foreground service for ongoing management
            try {
                val serviceIntent = Intent(this, ChessLockService::class.java)
                startForegroundService(serviceIntent)
                Log.d("MainActivity", "✓ ChessLock foreground service started")
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to start ChessLock service: ${e.message}")
            }
        } else {
            // If lock strategy failed, show appropriate message
            val statusMessage = LockStrategyManager.getSetupStatusMessage(this)
            Toast.makeText(this, "Setup required: $statusMessage", Toast.LENGTH_LONG).show()
            showSetupRequiredDialog()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }
    
    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("🚪 Exit ChessLock")
            .setMessage("Are you sure you want to exit the app?\n\nNote: This will only close the app interface. If ChessLock is active, the lockscreen functionality will continue working until you disable it in settings.")
            .setPositiveButton("Exit App") { _, _ ->
                exitApplication()
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Settings") { _, _ ->
                // Open settings instead of exiting
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            .show()
    }
    
    private fun exitApplication() {
        // Gracefully close the app without affecting lockscreen functionality
        finishAffinity() // Closes all activities in the task
        System.exit(0) // Cleanly exit the process
    }
    
    private fun requestAdminPermission() {
        LockAdminReceiver.requestAdminPermission(this)
    }
}