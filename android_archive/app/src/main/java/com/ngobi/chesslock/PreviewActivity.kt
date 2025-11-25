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
    
    // 🏆 Chess engine for preview victory detection
    private lateinit var chessEngine: ChessEngine
    private var isEngineInitialized = false
    private var currentPuzzle: ChessPuzzle? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        // Set up action bar
        supportActionBar?.title = "ChessLock Preview"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)

        // Initialize preview components
        initializePreviewComponents()
        
        // 🏆 Initialize chess engine for victory detection
        initializeChessEngine()
        
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
            "wooden" -> boardStyleGroup.check(R.id.woodenRadio)
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
                putBoolean("sound_enabled", true)
            }
            
            // Update UI to reflect defaults
            difficultyGroup.check(R.id.mediumRadio)
            boardStyleGroup.check(R.id.classicRadio)
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
            
            // Get puzzle and store it
            currentPuzzle = PuzzleManager.getRandomChessPuzzle()
            val puzzle = currentPuzzle!!
            
            // Initialize chess engine with puzzle
            if (isEngineInitialized) {
                chessEngine.resetGame()
                chessEngine.loadPosition(puzzle.fen)
            }
            
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
            
            Log.d("PreviewActivity", "✅ Preview content updated with puzzle: ${puzzle.prompt}")
            
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
    
    // 🏆 CHESS ENGINE INTEGRATION FOR PREVIEW VICTORY DETECTION
    
    /**
     * Initialize chess engine for preview mode victory detection
     */
    private fun initializeChessEngine() {
        try {
            chessEngine = ChessEngine()
            
            // Set up victory detection callback using the public method
            chessEngine.setOnGameEndListener { gameResult ->
                handlePreviewVictory(gameResult)
            }
            
            isEngineInitialized = true
            Log.d("PreviewActivity", "✅ Chess engine initialized for preview mode")
            
        } catch (e: Exception) {
            Log.e("PreviewActivity", "❌ Failed to initialize chess engine: ${e.message}")
            isEngineInitialized = false
        }
    }
    
    /**
     * Handle victory detection in preview mode
     */
    private fun handlePreviewVictory(gameResult: ChessEngine.GameResult) {
        Log.i("PreviewActivity", "🏆 PREVIEW VICTORY DETECTED: ${gameResult.description}")
        
        val isPlayerVictory = when (gameResult) {
            ChessEngine.GameResult.WHITE_WINS_CHECKMATE,
            ChessEngine.GameResult.BLACK_WINS_CHECKMATE -> {
                Log.i("PreviewActivity", "✅ Checkmate detected in preview!")
                true
            }
            else -> {
                Log.d("PreviewActivity", "🎮 Non-victory game end: ${gameResult.description}")
                false
            }
        }
        
        if (isPlayerVictory) {
            // Show victory message and refresh to new puzzle
            runOnUiThread {
                Toast.makeText(this, "🏆 Victory! Loading new puzzle...", Toast.LENGTH_SHORT).show()
                
                // Auto-refresh to new puzzle after brief delay
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    refreshPreviewPuzzle()
                }, 2000) // 2 second delay to show victory
            }
        } else {
            // For draws/losses, also refresh puzzle
            runOnUiThread {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    refreshPreviewPuzzle()
                }, 2000)
            }
        }
    }
    
    /**
     * Refresh preview with a new puzzle
     */
    private fun refreshPreviewPuzzle() {
        try {
            Log.i("PreviewActivity", "🔄 Refreshing preview with new puzzle...")
            
            // Get new puzzle
            currentPuzzle = PuzzleManager.getRotatedPuzzle()
            
            // Initialize chess engine with new puzzle
            if (isEngineInitialized && currentPuzzle != null) {
                chessEngine.resetGame()
                chessEngine.loadPosition(currentPuzzle!!.fen)
            }
            
            // Update preview content with new puzzle
            currentPuzzle?.let { puzzle ->
                previewBoardView?.setFEN(puzzle.fen, puzzle.solutionFEN)
                previewPrompt?.text = puzzle.prompt
            }
            
            Log.i("PreviewActivity", "✅ Preview refreshed with: ${currentPuzzle?.prompt}")
            
        } catch (e: Exception) {
            Log.e("PreviewActivity", "❌ Error refreshing preview puzzle: ${e.message}")
        }
    }
}