package com.ngobi.chesslock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)

        // UI Elements (simplified layout)
        val difficultyGroup = findViewById<RadioGroup>(R.id.difficultyGroup)
        val colorGroup = findViewById<RadioGroup>(R.id.colorGroup)
        val themeSpinner = findViewById<Spinner>(R.id.themeSpinner)
        val nativeLockSwitch = findViewById<SwitchCompat>(R.id.nativeLockSwitch)
        val resetButton = findViewById<Button>(R.id.resetSettingsButton)
        val statsButton = findViewById<Button>(R.id.openStatsButton)
        val donateButton = findViewById<Button>(R.id.openDonateButton)
        val adminButton = findViewById<Button>(R.id.enableAdminButton)
        val overlayButton = findViewById<Button>(R.id.enableOverlayButton)
        val activateButton = findViewById<Button>(R.id.activateButton)
        val previewButton = findViewById<Button>(R.id.previewButton)
        val exitAppButton = findViewById<Button>(R.id.exitAppButton)

        // Load saved settings
        val savedDifficulty = prefs.getString("difficulty", "medium")
        val savedColor = prefs.getString("player_color", "white")
        val savedTheme = prefs.getString("theme", "classic")
        val savedNativeLock = prefs.getBoolean("nativeLock", false)

        // Apply saved values
        when (savedDifficulty) {
            "easy" -> difficultyGroup.check(R.id.easy)
            "medium" -> difficultyGroup.check(R.id.medium)
            "hard" -> difficultyGroup.check(R.id.hard)
        }

        when (savedColor) {
            "white" -> colorGroup.check(R.id.whiteColor)
            "black" -> colorGroup.check(R.id.blackColor)
            "random" -> colorGroup.check(R.id.randomColor)
        }

        // Setup native lock switch
        nativeLockSwitch.isChecked = savedNativeLock

        // Setup theme spinner
        val themes = BoardTheme.getAvailableThemes().toTypedArray()
        themeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themes)
        themeSpinner.setSelection(themes.indexOf(savedTheme))

        // Save difficulty
        difficultyGroup.setOnCheckedChangeListener { _, checkedId ->
            val difficulty = when (checkedId) {
                R.id.easy -> "easy"
                R.id.medium -> "medium"
                R.id.hard -> "hard"
                else -> "medium"
            }
            prefs.edit { putString("difficulty", difficulty) }
        }

        // Save player color
        colorGroup.setOnCheckedChangeListener { _, checkedId ->
            val color = when (checkedId) {
                R.id.whiteColor -> "white"
                R.id.blackColor -> "black"
                R.id.randomColor -> "random"
                else -> "white"
            }
            prefs.edit { putString("player_color", color) }
        }

        // Save theme selection
        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                prefs.edit { putString("theme", themes[position]) }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Save native lock setting
        nativeLockSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("nativeLock", isChecked) }
            if (isChecked) {
                Toast.makeText(this, "✅ Chess puzzles will lead to native lock screen", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "✅ Chess puzzles will unlock directly", Toast.LENGTH_LONG).show()
            }
        }



        // Activate ChessLock button
        activateButton.setOnClickListener {
            if (LockAdminReceiver.isAdminActive(this) && Settings.canDrawOverlays(this)) {
                val isActive = prefs.getBoolean("chess_lock_active", false)
                if (isActive) {
                    // Deactivate ChessLock
                    prefs.edit { putBoolean("chess_lock_active", false) }
                    activateButton.text = "🔐 Activate ChessLock"
                    Toast.makeText(this, "ChessLock deactivated", Toast.LENGTH_SHORT).show()
                } else {
                    // Activate ChessLock
                    prefs.edit { putBoolean("chess_lock_active", true) }
                    activateButton.text = "🔓 Deactivate ChessLock"
                    Toast.makeText(this, "ChessLock activated! Your device will be protected by chess puzzles.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Please enable Device Admin and Overlay permissions first", Toast.LENGTH_LONG).show()
            }
        }

        // Preview button
        previewButton.setOnClickListener {
            startActivity(Intent(this, PreviewActivity::class.java))
        }

        // Update activate button text based on current state
        updateActivateButtonText()

        // Reset to defaults
        resetButton.setOnClickListener {
            prefs.edit {
                putString("difficulty", "medium")
                putString("player_color", "white")
                putString("theme", "classic")
                putBoolean("nativeLock", false)
                putBoolean("chess_lock_active", false)
            }
            recreate()
            Toast.makeText(this, "Settings reset to default", Toast.LENGTH_SHORT).show()
        }

        // Navigate to Stats
        statsButton.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        // Navigate to Donate
        donateButton.setOnClickListener {
            startActivity(Intent(this, DonateActivity::class.java))
        }

        // Request Device Admin permission
        adminButton.setOnClickListener {
            val adminComponent = ComponentName(this, LockAdminReceiver::class.java)
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

            if (dpm.isAdminActive(adminComponent)) {
                Toast.makeText(this, "Admin already enabled", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable ChessLock to control device locking")
                }
                startActivity(intent)
            }
        }

        // Request Overlay Permission
        overlayButton.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
            }
        }

        // Exit App Button
        exitAppButton.setOnClickListener {
            showExitConfirmationDialog()
        }
    }
    
    private fun updateActivateButtonText() {
        val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
        val isActive = prefs.getBoolean("chess_lock_active", false)
        val activateButton = findViewById<Button>(R.id.activateButton)
        
        if (isActive) {
            activateButton.text = "🔓 Deactivate ChessLock"
        } else {
            activateButton.text = "🔐 Activate ChessLock"
        }
    }
    
    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("🚪 Exit ChessLock")
            .setMessage("Are you sure you want to exit the app?\n\nNote: This will only close the app interface. Any active lockscreen functionality will continue working.")
            .setPositiveButton("Exit App") { _, _ ->
                exitApplication()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun exitApplication() {
        // Gracefully close the app without affecting lockscreen functionality
        finishAffinity() // Closes all activities in the task
        System.exit(0) // Cleanly exit the process
    }
}