package com.ngobi.chesslock

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.*

class PreviewActivity : AppCompatActivity() {
    
    private var previewBoardView: ChessBoardView? = null
    private var previewTimeDisplay: TextView? = null
    private var previewDateDisplay: TextView? = null
    private var previewPrompt: TextView? = null
    private var previewDifficultyBadge: TextView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        // Set up action bar
        supportActionBar?.title = "ChessLock Preview"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)

        // Initialize preview components
        initializePreviewComponents()
        
        val puzzlePrompt = findViewById<TextView>(R.id.puzzlePrompt)
        val difficultyGroup = findViewById<RadioGroup>(R.id.difficultyGroup)
        val boardStyleGroup = findViewById<RadioGroup>(R.id.boardStyleGroup)
        val pieceStyleGroup = findViewById<RadioGroup>(R.id.pieceStyleGroup)
        val soundEnabledSwitch = findViewById<SwitchCompat>(R.id.soundEnabledSwitch)
        val applyButton = findViewById<Button>(R.id.applyButton)
        val resetButton = findViewById<Button>(R.id.resetButton)

        // Load saved settings
        val savedDifficulty = prefs.getString("difficulty", "medium") ?: "medium"
        val savedBoardStyle = prefs.getString("board_style", "classic") ?: "classic"
        val savedPieceStyle = prefs.getString("piece_style", "traditional") ?: "traditional"
        val soundEnabled = prefs.getBoolean("sound_enabled", true)
        
        initializePreviewComponents()

        when (savedDifficulty) {
            "easy" -> difficultyGroup.check(R.id.easyRadio)
            "medium" -> difficultyGroup.check(R.id.mediumRadio)
            "hard" -> difficultyGroup.check(R.id.hardRadio)
        }

        when (savedBoardStyle) {
            "classic" -> boardStyleGroup.check(R.id.classicRadio)
            "modern" -> boardStyleGroup.check(R.id.modernRadio)
        }

        when (savedPieceStyle) {
            "traditional" -> pieceStyleGroup.check(R.id.traditionalRadio)
            "minimalist" -> pieceStyleGroup.check(R.id.minimalistRadio)
        }

        soundEnabledSwitch.isChecked = soundEnabled

        val puzzle = PuzzleManager.getRandomChessPuzzle()
        puzzlePrompt.text = puzzle.prompt

        // Save settings when changed
        difficultyGroup.setOnCheckedChangeListener { _, checkedId ->
            val difficulty = when (checkedId) {
                R.id.easyRadio -> "easy"
                R.id.mediumRadio -> "medium"
                R.id.hardRadio -> "hard"
                else -> "medium"
            }
            prefs.edit {
                putString("difficulty", difficulty)
            }
            
            // Immediately update preview to show new difficulty
            updatePreviewContent()
            Log.d("PreviewActivity", "Difficulty changed to: $difficulty")
        }

        boardStyleGroup.setOnCheckedChangeListener { _, checkedId ->
            val boardStyle = when (checkedId) {
                R.id.classicRadio -> "classic"
                R.id.modernRadio -> "modern"
                else -> "classic"
            }
            prefs.edit {
                putString("board_style", boardStyle)
            }
            
            // Immediately update chess board theme
            previewBoardView?.updateTheme()
            Log.d("PreviewActivity", "Board style changed to: $boardStyle")
        }

        pieceStyleGroup.setOnCheckedChangeListener { _, checkedId ->
            val pieceStyle = when (checkedId) {
                R.id.traditionalRadio -> "traditional"
                R.id.minimalistRadio -> "minimalist"
                else -> "traditional"
            }
            prefs.edit {
                putString("piece_style", pieceStyle)
            }
            
            // Immediately update chess piece style
            previewBoardView?.updateTheme()
            Log.d("PreviewActivity", "Piece style changed to: $pieceStyle")
        }

        soundEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit {
                putBoolean("sound_enabled", isChecked)
            }
        }

        applyButton.setOnClickListener {
            // Test the lockscreen with full chess functionality
            try {
                // Use the comprehensive OverlayLockActivity with preview mode
                val intent = Intent(this, OverlayLockActivity::class.java).apply {
                    putExtra("is_preview", true)
                    putExtra("from_preview_activity", true)
                }
                startActivity(intent)
                Toast.makeText(this, "✅ ChessLock Preview Launched!\nThis is exactly how your lockscreen will appear.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("PreviewActivity", "Preview launch failed: ${e.message}")
                Toast.makeText(this, "Preview launch failed: ${e.message}", Toast.LENGTH_LONG).show()
                
                // Fallback to simplified preview
                showSafePreview()
            }
        }

        resetButton.setOnClickListener {
            // Reset all settings to defaults
            prefs.edit {
                putString("difficulty", "medium")
                putString("board_style", "classic")
                putString("piece_style", "traditional")
                putBoolean("sound_enabled", true)
            }
            
            // Update UI to reflect defaults
            difficultyGroup.check(R.id.mediumRadio)
            boardStyleGroup.check(R.id.classicRadio)
            pieceStyleGroup.check(R.id.traditionalRadio)
            soundEnabledSwitch.isChecked = true
            
            // Immediately update preview to show reset settings
            updatePreviewContent()
            Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
            Log.d("PreviewActivity", "Settings reset to defaults")
        }
    }
    
    private fun initializePreviewComponents() {
        // Try to find preview components from the current layout
        try {
            previewBoardView = findViewById<ChessBoardView>(R.id.chessBoardView)
            previewTimeDisplay = findViewById<TextView>(R.id.hourText)
            previewDateDisplay = findViewById<TextView>(R.id.minuteText)
            previewPrompt = findViewById<TextView>(R.id.puzzlePrompt)
            previewDifficultyBadge = null // Not available in current layout
            
            updateTimeAndDate()
            updatePreviewContent()
            
            Log.d("PreviewActivity", "✓ Preview components initialized successfully")
        } catch (e: Exception) {
            // If preview components fail, show error message
            Log.e("PreviewActivity", "Preview components error: ${e.message}")
            Toast.makeText(this, "Preview components not found: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updatePreviewContent() {
        try {
            val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
            val difficulty = prefs.getString("difficulty", "medium") ?: "medium"
            
            val puzzle = PuzzleManager.getRandomChessPuzzle()
            
            // Update chess board
            previewBoardView?.updateTheme()
            
            // Update preview board
            previewBoardView?.setFEN(puzzle.fen, puzzle.solutionFEN)
            
            // Update preview prompt
            previewPrompt?.text = puzzle.prompt
            
            // Update difficulty badge 
            previewDifficultyBadge?.let { badge ->
                val (badgeText, badgeColor) = when (difficulty) {
                    "easy" -> "EASY" to android.R.color.holo_green_light
                    "hard" -> "HARD" to android.R.color.holo_red_light
                    else -> "MEDIUM" to android.R.color.holo_orange_light
                }
                
                badge.text = badgeText
                badge.setBackgroundColor(getColor(badgeColor))
            }
        } catch (e: Exception) {
            // If preview content update fails, show error message
            Toast.makeText(this, "Preview update error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateTimeAndDate() {
        try {
            val now = Calendar.getInstance().time
            val timeFormat = android.text.format.DateFormat.getTimeFormat(this)
            val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            
            previewTimeDisplay?.text = timeFormat.format(now)
            previewDateDisplay?.text = dateFormat.format(now)
        } catch (e: Exception) {
            // If time update fails, just skip it
            Toast.makeText(this, "Time display update failed", Toast.LENGTH_SHORT).show()
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    private fun showSafePreview() {
        val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
        val difficulty = prefs.getString("difficulty", "medium") ?: "medium"
        val theme = prefs.getString("theme", "classic") ?: "classic"
        val playerColor = prefs.getString("player_color", "white") ?: "white"
        
        AlertDialog.Builder(this)
            .setTitle("🎯 Lockscreen Preview")
            .setMessage("""
                ChessLock Configuration:
                
                🎲 Difficulty: ${difficulty.uppercase()}
                🎨 Board Theme: ${theme.replace("_", " ").uppercase()}
                ♟️ Playing As: ${playerColor.uppercase()} pieces
                
                When you lock your device, you'll see:
                • Chess puzzle to solve
                • Current time and date
                • Emergency unlock option
                
                The lockscreen will use your selected theme and difficulty settings.
            """.trimIndent())
            .setPositiveButton("Got it", null)
            .show()
    }
}