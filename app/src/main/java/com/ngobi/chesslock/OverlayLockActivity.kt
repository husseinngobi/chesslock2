package com.ngobi.chesslock

import android.app.KeyguardManager
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import kotlin.random.Random
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.telephony.TelephonyManager
import android.content.Context
import android.content.BroadcastReceiver
import android.content.IntentFilter

class OverlayLockActivity : AppCompatActivity() {

    private lateinit var currentPuzzle: ChessPuzzle
    private lateinit var boardView: ChessBoardView
    private lateinit var emergencyButton: Button
    private lateinit var promptView: TextView
    private lateinit var hintButton: Button
    private var chessTimerView: ChessTimerView? = null  // Professional chess timer
    
    // 🧠 CHESS RULES ENGINE - The heart of game logic
    private lateinit var chessEngine: ChessEngine
    private var isEngineInitialized = false
    private lateinit var keyguardManager: KeyguardManager
    
    // 🤖 AI OPPONENT SYSTEM
    private var isAIMode = true // AI vs User gameplay enabled
    private var userColor = "white" // User's side (white or black)
    private var isUserTurn = true // Track whose turn it is
    private var isAIThinking = false // Prevent multiple AI moves at once
    
    // Handler for delayed operations (like checkmate detection)
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    
    private var isUnlockAuthorized = false
    private var unlockReason = ""
    private var emergencyClickCount = 0
    private val emergencyClickThreshold = 5
    private var isPreviewMode = false
    
    // Timer system for puzzle solving - Professional implementation
    private var puzzleStartTime = 0L
    private lateinit var timerDisplay: TextView
    
    // Screen compatibility for professional responsive design
    private lateinit var screenInfo: ScreenCompatibilityManager.ScreenInfo
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Initialize system managers
            keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            
            // Get screen compatibility information for responsive design
            screenInfo = ScreenCompatibilityManager.getScreenInfo(this)
            ScreenCompatibilityManager.applyResponsiveLayout(this, screenInfo)
            
            // Check if this is preview mode or launched from wake receiver
            isPreviewMode = intent.getBooleanExtra("is_preview", false) 
                || intent.getBooleanExtra("preview_mode", false)
            val fromWakeReceiver = intent.getBooleanExtra("from_wake_receiver", false)
            val immediateOverride = intent.getBooleanExtra("immediate_override", false)
            
            Log.d("OverlayLockActivity", "Launch parameters - Preview: $isPreviewMode, WakeReceiver: $fromWakeReceiver, Immediate: $immediateOverride")
            Log.d("OverlayLockActivity", "Screen: ${screenInfo.widthDp}x${screenInfo.heightDp}dp, Size: ${screenInfo.screenSize}, Tablet: ${screenInfo.isTablet}")
            
            // ENHANCED CALL HANDLING: Check if we're in a call and should exit gracefully
            if (!isPreviewMode && isInCall()) {
                Log.d("OverlayLockActivity", "🔗 Device in call - ChessLock should not interfere")
                finish() // Exit gracefully to allow call interface
                return
            }
            
            // Always setup the window (both preview and lock modes need proper window setup)
            setupLockscreenWindow()
            
            // Handle immediate override from WakeReceiver to prevent native flash (only for lock mode)
            if (!isPreviewMode && fromWakeReceiver && immediateOverride) {
                executeImmediateOverride()
            }
            
            // Try to load the chess layout with error handling
            try {
                setContentView(R.layout.activity_lock_screen_responsive_clean)
                Log.d("OverlayLockActivity", "✓ Chess layout loaded successfully")
            } catch (e: Exception) {
                Log.e("OverlayLockActivity", "Failed to load chess layout: ${e.message}")
                createEmergencyInterface()
                return
            }
            
            // 🧠 INITIALIZE CHESS RULES ENGINE FIRST
            initializeChessEngine()
            
            initializeViews()
            applyResponsiveDesign() // Apply screen-specific optimizations
            loadChessPuzzle()
            setupEmergencyUnlock()
            setupBackPressedHandler()
            
            // Monitor for incoming calls during ChessLock session
            setupCallMonitoring()
            
            Log.d("OverlayLockActivity", "✓ OverlayLockActivity initialization complete")
            
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "Critical error in onCreate: ${e.message}")
            Log.e("OverlayLockActivity", "Stack trace: ${e.stackTrace.joinToString("\n")}")
            // Create emergency interface if normal setup fails
            createEmergencyInterface()
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // 🎨 REFRESH THEME ON RESUME
        // This ensures any theme changes made in preview are reflected
        if (::boardView.isInitialized) {
            try {
                boardView.updateTheme()
                Log.d("OverlayLockActivity", "🎨 Theme refreshed on resume")
            } catch (e: Exception) {
                Log.e("OverlayLockActivity", "Theme refresh on resume failed: ${e.message}")
            }
        }
        
        // Refresh call monitoring when resuming
        if (!isPreviewMode && !isInCall()) {
            Log.d("OverlayLockActivity", "🔒 ChessLock resumed - ensuring lockscreen override")
        }
    }
    
    /**
     * Create emergency interface when normal initialization fails
     */
    private fun createEmergencyInterface() {
        try {
            setContentView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(getColor(android.R.color.black))
                setPadding(32, 32, 32, 32)
                
                addView(TextView(this@OverlayLockActivity).apply {
                    text = getString(R.string.emergency_mode_title)
                    textSize = 20f
                    setTextColor(getColor(android.R.color.white))
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, 32)
                })
                
                addView(Button(this@OverlayLockActivity).apply {
                    text = getString(R.string.emergency_unlock_button)
                    setOnClickListener { 
                        finish()
                    }
                })
            })
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "Emergency interface creation failed: ${e.message}")
            finish()
        }
    }
    
    /**
     * Apply professional responsive design optimizations
     */
    private fun applyResponsiveDesign() {
        try {
            // Apply responsive layout optimizations
            ScreenCompatibilityManager.applyResponsiveLayout(this, screenInfo)
            
            // Optimize specific components for screen size
            ScreenCompatibilityManager.optimizeChessBoard(boardView, screenInfo)
            chessTimerView?.let { 
                ScreenCompatibilityManager.optimizeTimer(it, screenInfo) 
            }
            
            Log.d("OverlayLockActivity", "✓ Applied responsive design for ${screenInfo.screenSize} screen")
            
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "Error applying responsive design: ${e.message}")
        }
    }
    
    private fun setupLockscreenWindow() {
        // ANDROID 9 OPTIMIZED LOCKSCREEN OVERRIDE
        
        // 1. Android 9 (API 28) Primary Implementation
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            // Dedicated Android 9 lockscreen replacement
            setupAndroid9Lockscreen()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ approach
            setupModernLockscreen()
        } else {
            // Fallback for any edge cases
            setupAndroid9Lockscreen()
        }
    }
    
    /**
     * 🎯 ANDROID 9 SPECIFIC LOCKSCREEN REPLACEMENT
     * Optimized specifically for API 28 devices
     */
    private fun setupAndroid9Lockscreen() {
        Log.d("OverlayLockActivity", "🎯 Setting up Android 9 lockscreen replacement")
        
        // 1. ANDROID 9 WINDOW FLAGS - Proven working combination
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        
        // 2. Essential window flags for Android 9 lockscreen override
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        
        // 3. Android 9 display cutout support
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        // 4. Android 9 keyguard dismissal
        if (keyguardManager.isKeyguardLocked) {
            keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    super.onDismissSucceeded()
                    Log.d("OverlayLockActivity", "✅ Android 9 keyguard dismissed successfully")
                }
                
                override fun onDismissError() {
                    super.onDismissError()
                    Log.w("OverlayLockActivity", "⚠️ Android 9 keyguard dismissal failed, using fallback")
                }
                
                override fun onDismissCancelled() {
                    super.onDismissCancelled()
                    Log.w("OverlayLockActivity", "⚠️ Android 9 keyguard dismissal cancelled")
                }
            })
        }
        
        // 5. Android 9 system UI configuration
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        
        // 6. Android 9 window type for overlay
        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
    }
    
    /**
     * 🔮 MODERN LOCKSCREEN REPLACEMENT (Android 10+)
     * For newer devices with updated APIs
     */
    private fun setupModernLockscreen() {
        Log.d("OverlayLockActivity", "🔮 Setting up modern lockscreen replacement")
        
        // Modern window setup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        keyguardManager.requestDismissKeyguard(this, null)
        
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
    
    /**
     * Force ChessLock to foreground using safe techniques
     */
    private fun bringToForeground() {
        try {
            // Method 1: Window focus manipulation (safe for regular apps)
            window.decorView.requestFocus()
            
            // Method 3: Ensure we're the top activity
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Modern approach - request system attention
                val intent = Intent(this, OverlayLockActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            
        } catch (_: Exception) {
            // Fallback if system methods fail
            forceOverrideNativeLock()
        }
    }
    
    /**
     * Nuclear option - force override using all available techniques
     */
    private fun forceOverrideNativeLock() {
        try {
            // Technique 1: Request keyguard dismissal using modern API
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
            
            // Technique 2: Force window to top of Z-order
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            )
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            
            // Technique 3: Aggressive re-launch if needed
            val intent = Intent(this, OverlayLockActivity::class.java)
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK 
                or Intent.FLAG_ACTIVITY_CLEAR_TOP 
                or Intent.FLAG_ACTIVITY_SINGLE_TOP
                or Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            startActivity(intent)
            
        } catch (_: Exception) {
            // Last resort - just ensure we stay visible
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    /**
     * Execute immediate override to prevent native lockscreen flash
     */
    private fun executeImmediateOverride() {
        try {
            // Technique 1: Instant keyguard disable
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
            
            // Technique 2: Force window to absolute front immediately
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
            
            // Technique 3: Bring to front multiple times for insurance
            runOnUiThread {
                repeat(3) {
                    window.decorView.requestFocus()
                    bringToForeground()
                }
            }
            
            // Technique 4: Block status bar access
            try {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            } catch (_: Exception) {
                // Fallback for system UI manipulation
            }
            
        } catch (_: Exception) {
            // Emergency fallback - at least stay visible
            forceOverrideNativeLock()
        }
    }
    
    /**
     * 🧠 CHESS RULES ENGINE INITIALIZATION
     * Sets up comprehensive chess logic with game rule enforcement
     */
    private fun initializeChessEngine() {
        try {
            Log.d("OverlayLockActivity", "🧠 Initializing chess rules engine...")
            
            chessEngine = ChessEngine()
            isEngineInitialized = true
            
            // Set up game event listeners
            chessEngine.setOnGameEndListener { gameResult ->
                handleGameEnd(gameResult)
            }
            
            chessEngine.setOnMoveListener { moveResult ->
                handleMoveResult(moveResult)
            }
            
            chessEngine.setOnCheckListener { side ->
                handleCheckEvent(side)
            }
            
            // Log initial engine state
            Log.d("OverlayLockActivity", "🔍 Engine initial state:")
            Log.d("OverlayLockActivity", "   - FEN: ${chessEngine.getBoardFEN()}")
            Log.d("OverlayLockActivity", "   - Legal moves: ${chessEngine.getLegalMoves().size}")
            Log.d("OverlayLockActivity", "   - White's turn: ${chessEngine.isWhiteTurn()}")
            Log.d("OverlayLockActivity", "   - In check: ${chessEngine.isInCheck()}")
            Log.d("OverlayLockActivity", "   - Game ended: ${chessEngine.isGameEnded()}")
            
            Log.i("OverlayLockActivity", "✅ Chess engine initialized successfully with full rule enforcement")
            
            // 🤖 INITIALIZE AI OPPONENT SYSTEM
            initializeAIOpponent()
            
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "❌ Failed to initialize chess engine: ${e.message}", e)
            isEngineInitialized = false
            
            // Create fallback notification
            showChessEngineError()
        }
    }
    
    /**
     * 🎯 GAME EVENT HANDLERS
     * Handle chess engine events and game outcomes
     */
    
    private fun handleGameEnd(gameResult: ChessEngine.GameResult) {
        Log.i("OverlayLockActivity", "🏁 Game ended: ${gameResult.description}")
        
        when (gameResult) {
            ChessEngine.GameResult.WHITE_WINS_CHECKMATE -> {
                val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
                val playerColor = prefs.getString("player_color", "white") ?: "white"
                if (playerColor == "white" || playerColor == "random") {
                    handlePuzzleSolved(getString(R.string.checkmate_puzzle_solved))
                } else {
                    Toast.makeText(this, getString(R.string.checkmate_you_lost), Toast.LENGTH_SHORT).show()
                    resetPuzzle()
                }
            }
            ChessEngine.GameResult.BLACK_WINS_CHECKMATE -> {
                val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
                val playerColor = prefs.getString("player_color", "white") ?: "white"
                if (playerColor == "black" || playerColor == "random") {
                    handlePuzzleSolved(getString(R.string.checkmate_puzzle_solved))
                } else {
                    Toast.makeText(this, getString(R.string.checkmate_you_lost), Toast.LENGTH_SHORT).show()
                    resetPuzzle()
                }
            }
            ChessEngine.GameResult.STALEMATE -> {
                Toast.makeText(this, getString(R.string.stalemate_draw), Toast.LENGTH_SHORT).show()
                resetPuzzle()
            }
            ChessEngine.GameResult.DRAW_50_MOVE_RULE -> {
                Toast.makeText(this, "🤝 Draw by 50-move rule!", Toast.LENGTH_SHORT).show()
                resetPuzzle()
            }
            ChessEngine.GameResult.DRAW_INSUFFICIENT_MATERIAL -> {
                Toast.makeText(this, "🤝 Draw by insufficient material!", Toast.LENGTH_SHORT).show()
                resetPuzzle()
            }
            ChessEngine.GameResult.DRAW_THREEFOLD_REPETITION -> {
                Toast.makeText(this, "🤝 Draw by threefold repetition!", Toast.LENGTH_SHORT).show()
                resetPuzzle()
            }
            else -> {
                // Game still in progress or other states
                Log.d("OverlayLockActivity", "Game continues: ${gameResult.description}")
            }
        }
    }
    
    /**
     * 🎉 PUZZLE COMPLETION HANDLER
     * Called when the chess engine determines the puzzle is solved
     */
    private fun handlePuzzleSolved(message: String) {
        Log.i("OverlayLockActivity", "🎉 Puzzle solved via chess engine!")
        Log.i("OverlayLockActivity", "🔓 INITIATING AUTOMATIC UNLOCK SEQUENCE")
        
        // Show success message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        
        // Call the original puzzle solved logic which will unlock the device
        onPuzzleSolved()
    }
    
    private fun handleMoveResult(moveResult: ChessEngine.MoveResult) {
        if (moveResult.success) {
            Log.d("OverlayLockActivity", "✅ Valid move executed")
            
            // Update board display
            boardView.setFEN(moveResult.boardFEN, currentPuzzle.solutionFEN)
            
            // Check for special move types
            when {
                moveResult.isCheck -> {
                    Toast.makeText(this, "⚠️ Check!", Toast.LENGTH_SHORT).show()
                }
                moveResult.isCapture -> {
                    Toast.makeText(this, "💥 Capture!", Toast.LENGTH_SHORT).show()
                }
                moveResult.isCastling -> {
                    Toast.makeText(this, "🏰 Castling!", Toast.LENGTH_SHORT).show()
                }
                moveResult.isPromotion -> {
                    Toast.makeText(this, "👑 Promotion!", Toast.LENGTH_SHORT).show()
                }
                moveResult.isEnPassant -> {
                    Toast.makeText(this, "🎯 En passant!", Toast.LENGTH_SHORT).show()
                }
            }
            
        } else {
            Log.w("OverlayLockActivity", "❌ Invalid move: ${moveResult.errorMessage}")
            Toast.makeText(this, "❌ ${moveResult.errorMessage}", Toast.LENGTH_SHORT).show()
            
            // Provide feedback about legal moves
            if (moveResult.legalMoves.isNotEmpty()) {
                Log.d("OverlayLockActivity", "Available legal moves: ${moveResult.legalMoves.size}")
            }
        }
    }
    
    private fun handleCheckEvent(side: com.github.bhlangonijr.chesslib.Side) {
        val colorName = if (side == com.github.bhlangonijr.chesslib.Side.WHITE) "White" else "Black"
        Log.i("OverlayLockActivity", "⚠️ $colorName king is in check!")
        
        // Visual and audio feedback for check
        Toast.makeText(this, "⚠️ Check!", Toast.LENGTH_SHORT).show()
        
        // Could add sound effect here if sound is enabled
        // ChessSoundManager.playCheckSound()
    }
    
    private fun showChessEngineError() {
        runOnUiThread {
            Toast.makeText(
                this,
                "⚠️ Chess engine failed to load. Some features may be limited.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * 🤖 AI OPPONENT INITIALIZATION
     * Sets up user vs AI chess gameplay
     */
    private fun initializeAIOpponent() {
        try {
            // Get user's preferred color from settings
            val sharedPrefs = getSharedPreferences("chesslock_preferences", MODE_PRIVATE)
            userColor = sharedPrefs.getString("player_color", "white") ?: "white"
            
            Log.d("OverlayLockActivity", "🤖 AI Opponent initialized")
            Log.d("OverlayLockActivity", "   - User plays: $userColor")
            Log.d("OverlayLockActivity", "   - AI plays: ${if (userColor == "white") "black" else "white"}")
            
            // Initialize turn tracking
            updateTurnState()
            
            // If AI should move first (user is black), start AI turn
            if (userColor == "black" && chessEngine.isWhiteTurn()) {
                Log.d("OverlayLockActivity", "🤖 AI (white) should move first")
                scheduleAIMove()
            }
            
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "🤖 AI initialization failed: ${e.message}")
            isAIMode = false // Fall back to manual mode
        }
    }
    
    /**
     * 🔄 UPDATE TURN STATE
     * Determines whose turn it is based on engine state and user color
     */
    private fun updateTurnState() {
        if (!isEngineInitialized) return
        
        val isWhiteTurn = chessEngine.isWhiteTurn()
        isUserTurn = (userColor == "white" && isWhiteTurn) || (userColor == "black" && !isWhiteTurn)
        
        Log.d("OverlayLockActivity", "🔄 Turn updated: isWhiteTurn=$isWhiteTurn, userColor=$userColor, isUserTurn=$isUserTurn")
    }
    
    /**
     * 🎯 CHECK IF MOVE IS ALLOWED
     * User can only move their own pieces on their turn
     */
    private fun isUserMoveAllowed(): Boolean {
        if (!isAIMode) return true // Manual mode allows all moves
        if (!isUserTurn) {
            Log.w("OverlayLockActivity", "🚫 User move blocked - it's AI's turn")
            return false
        }
        return true
    }
    
    /**
     * 🤖 SCHEDULE AI MOVE
     * Triggers AI to think and make a move after a short delay
     */
    private fun scheduleAIMove() {
        if (!isAIMode || !isEngineInitialized || isAIThinking) return
        
        if (isUserTurn) {
            Log.d("OverlayLockActivity", "🤖 AI move not needed - it's user's turn")
            return
        }
        
        isAIThinking = true
        Log.d("OverlayLockActivity", "🤖 AI thinking...")
        
        // Give AI a short thinking time for better user experience
        handler.postDelayed({
            executeAIMove()
        }, 1000) // 1 second thinking time
    }
    
    /**
     * 🤖 EXECUTE AI MOVE
     * Generates and executes the AI's chosen move
     */
    private fun executeAIMove() {
        if (!isAIMode || !isEngineInitialized || isUserTurn) {
            isAIThinking = false
            return
        }
        
        try {
            Log.d("OverlayLockActivity", "🤖 AI executing move...")
            
            val aiMove = chessEngine.generateAIMove()
            if (aiMove == null) {
                Log.w("OverlayLockActivity", "🤖 AI could not generate a move")
                isAIThinking = false
                return
            }
            
            Log.i("OverlayLockActivity", "🤖 AI selected move: ${aiMove.from}-${aiMove.to}")
            
            // Execute the AI move through the engine
            val moveResult = chessEngine.makeMove(aiMove.from.toString(), aiMove.to.toString())
            
            if (moveResult.success) {
                Log.i("OverlayLockActivity", "✅ AI move executed successfully")
                
                // Update the board display
                runOnUiThread {
                    boardView.setFEN(moveResult.boardFEN, currentPuzzle.solutionFEN)
                    
                    // Show AI move feedback
                    val moveDescription = buildString {
                        append("🤖 AI: ${aiMove.from}-${aiMove.to}")
                        if (moveResult.isCheck) append(" Check!")
                        if (moveResult.isCapture) append(" Capture!")
                        if (moveResult.isCastling) append(" Castling!")
                    }
                    Toast.makeText(this@OverlayLockActivity, moveDescription, Toast.LENGTH_SHORT).show()
                }
                
                // Update turn state after successful AI move
                updateTurnState()
                
                // Handle game end detection
                handleMoveResult(moveResult)
                
            } else {
                Log.e("OverlayLockActivity", "❌ AI move failed: ${moveResult.errorMessage}")
            }
            
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "🤖 AI move execution error: ${e.message}")
        } finally {
            isAIThinking = false
        }
    }
    
    private fun resetPuzzle() {
        try {
            if (isEngineInitialized) {
                chessEngine.resetGame()
                chessEngine.loadPosition(currentPuzzle.fen)
            }
            
            // Reload the puzzle on the board
            boardView.setFEN(currentPuzzle.fen, currentPuzzle.solutionFEN)
            
            Log.d("OverlayLockActivity", "🔄 Puzzle reset to starting position")
            
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "Error resetting puzzle: ${e.message}")
        }
    }
    
    /**
     * 🎯 MOVE VALIDATION WITH CHESS ENGINE AND TURN ENFORCEMENT
     * Converts board coordinates to chess notation and validates with engine
     */
    private fun validateMoveWithEngine(from: Pair<Int, Int>, to: Pair<Int, Int>): Boolean {
        Log.d("OverlayLockActivity", "� Temporarily allowing all moves for debugging")
        Log.d("OverlayLockActivity", "Move: ${coordinatesToChessNotation(from)} -> ${coordinatesToChessNotation(to)}")
        
        if (!isEngineInitialized) {
            Log.w("OverlayLockActivity", "Chess engine not initialized, using fallback validation")
            return validateMoveBasicRules(from, to)
        }
        
        // 🤖 AI TURN ENFORCEMENT - Block user moves during AI's turn
        if (!isUserMoveAllowed()) {
            runOnUiThread {
                Toast.makeText(this, "🤖 Wait for AI's move...", Toast.LENGTH_SHORT).show()
            }
            return false
        }

        return try {
            // Convert board coordinates to chess notation
            val fromSquare = coordinatesToChessNotation(from)
            val toSquare = coordinatesToChessNotation(to)
            
            Log.d("OverlayLockActivity", "Testing move: $fromSquare -> $toSquare")
            
            // Use enhanced chess engine for validation with educational feedback
            val validationResult = chessEngine.validateMoveWithExplanation(fromSquare, toSquare)
            
            if (validationResult.isValid) {
                Log.i("OverlayLockActivity", "Move validated successfully: $fromSquare -> $toSquare")
                Log.d("OverlayLockActivity", "Move type: ${validationResult.educationalTip}")
                
                // Show positive feedback about the move type
                runOnUiThread {
                    Toast.makeText(this, validationResult.educationalTip, Toast.LENGTH_SHORT).show()
                }
                
                // Make the move in the engine
                chessEngine.makeMove(fromSquare, toSquare)
                
                // Check for game end immediately after the move
                val gameResult = chessEngine.getGameResult()
                Log.d("OverlayLockActivity", "Game result after move: ${gameResult.description}")
                
                if (gameResult != ChessEngine.GameResult.IN_PROGRESS) {
                    Log.d("OverlayLockActivity", "Game ended with result: ${gameResult.description}")
                    handler.post { handleGameEnd(gameResult) }
                }
                
                return true
            } else {
                Log.w("OverlayLockActivity", "Invalid move: $fromSquare -> $toSquare")
                Log.w("OverlayLockActivity", "Reason: ${validationResult.reason}")
                
                // Show educational feedback to help user learn chess rules
                runOnUiThread {
                    Toast.makeText(this, 
                        "❌ ${validationResult.reason}\n💡 ${validationResult.educationalTip}", 
                        Toast.LENGTH_LONG).show()
                }
                
                // Log debug information
                val legalMoves = chessEngine.getLegalMoves()
                Log.d("OverlayLockActivity", "Legal moves available: ${legalMoves.size}")
                if (legalMoves.isNotEmpty()) {
                    legalMoves.take(5).forEach { move ->
                        Log.d("OverlayLockActivity", "   Legal: $move")
                    }
                }
                
                return false
            }
            
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "Exception validating move: ${e.message}", e)
            // Fallback to basic validation if engine fails
            return validateMoveBasicRules(from, to)
        }
    }
    
    /**
     * 🔄 SYNCHRONIZE CHESS ENGINE WITH BOARD
     * Called after ChessBoardView successfully executes a move
     */
    private fun synchronizeEngineWithBoard(from: Pair<Int, Int>, to: Pair<Int, Int>) {
        if (!isEngineInitialized) {
            Log.w("OverlayLockActivity", "Chess engine not initialized, skipping synchronization")
            return
        }
        
        try {
            // Convert board coordinates to chess notation
            val fromSquare = coordinatesToChessNotation(from)
            val toSquare = coordinatesToChessNotation(to)
            
            Log.d("OverlayLockActivity", "🔄 Synchronizing engine with executed move: $fromSquare -> $toSquare")
            
            // Execute the move in the chess engine to keep it synchronized
            val moveResult = chessEngine.makeMove(fromSquare, toSquare)
            
            if (moveResult.success) {
                Log.i("OverlayLockActivity", "✅ Engine synchronized with move: $fromSquare -> $toSquare")
                
                // 🎯 CRITICAL: Check for checkmate/game end IMMEDIATELY after move
                try {
                    val gameResult = chessEngine.getGameResult()
                    Log.d("OverlayLockActivity", "🔍 Post-move game state: ${gameResult.description}")
                    
                    // Handle checkmate or other game endings immediately
                    if (gameResult != ChessEngine.GameResult.IN_PROGRESS) {
                        Log.i("OverlayLockActivity", "🏁 GAME ENDED: $gameResult - triggering automatic unlock!")
                        
                        // Delay slightly to let the move animation finish, then handle result
                        handler.postDelayed({
                            handleGameEnd(gameResult)
                        }, 300)
                    } else {
                        Log.d("OverlayLockActivity", "⏳ Game continues...")
                        
                        // 🤖 UPDATE TURN STATE AND TRIGGER AI MOVE IF NEEDED
                        updateTurnState()
                        if (isAIMode && !isUserTurn && !isAIThinking) {
                            Log.d("OverlayLockActivity", "🤖 User move completed, scheduling AI response...")
                            scheduleAIMove()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("OverlayLockActivity", "Error checking game result after move: ${e.message}")
                }
                
                // The engine handles all game state tracking including:
                // - Check detection
                // - Checkmate/stalemate
                // - Game end conditions
                // The engine listeners will automatically trigger appropriate responses
                
            } else {
                Log.e("OverlayLockActivity", "❌ Failed to synchronize engine: ${moveResult.errorMessage}")
                // This shouldn't happen since the move was already validated
            }
            
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "Error synchronizing engine: ${e.message}")
        }
    }
    
    /**
     * 🗺️ COORDINATE CONVERSION UTILITIES
     * Convert between board array coordinates and chess notation
     */
    private fun coordinatesToChessNotation(coords: Pair<Int, Int>): String {
        val (row, col) = coords
        
        // Chess notation: files (columns) are a-h, ranks (rows) are 1-8
        // Board array: [0,0] is top-left, but chess a1 is bottom-left
        val file = ('a' + col).toString()
        val rank = (8 - row).toString() // Flip row: array row 0 = chess rank 8
        
        val result = file + rank
        Log.d("OverlayLockActivity", "🗺️ Coordinate conversion: ($row,$col) -> $result")
        return result
    }
    
    /**
     * 🎮 ENHANCED GAME MANAGEMENT
     * Methods to handle puzzle loading with engine integration
     */
    private fun loadPuzzleIntoEngine() {
        if (!isEngineInitialized) {
            Log.w("OverlayLockActivity", "⚠️ Engine not initialized, cannot load puzzle")
            return
        }
        
        try {
            Log.d("OverlayLockActivity", "🧩 Loading puzzle into chess engine...")
            Log.d("OverlayLockActivity", "   - Puzzle ID: ${currentPuzzle.id}")
            Log.d("OverlayLockActivity", "   - Puzzle FEN: ${currentPuzzle.fen}")
            
            // Load the puzzle position into the chess engine
            val loaded = chessEngine.loadPosition(currentPuzzle.fen)
            
            if (loaded) {
                Log.i("OverlayLockActivity", "🧩 Puzzle loaded into chess engine: ${currentPuzzle.prompt}")
                
                // Log detailed engine state after puzzle loading
                Log.d("OverlayLockActivity", "🔍 Engine state after puzzle load:")
                Log.d("OverlayLockActivity", "   - Engine FEN: ${chessEngine.getBoardFEN()}")
                Log.d("OverlayLockActivity", "   - Expected FEN: ${currentPuzzle.fen}")
                Log.d("OverlayLockActivity", "   - FEN match: ${chessEngine.getBoardFEN() == currentPuzzle.fen}")
                
                // Verify the engine is ready
                val legalMoves = chessEngine.getLegalMoves()
                Log.d("OverlayLockActivity", "   - Legal moves available: ${legalMoves.size}")
                
                if (legalMoves.isNotEmpty()) {
                    Log.d("OverlayLockActivity", "   - Sample legal moves:")
                    legalMoves.take(3).forEach { move ->
                        Log.d("OverlayLockActivity", "     * $move")
                    }
                }
                
                Log.d("OverlayLockActivity", "   - White's turn: ${chessEngine.isWhiteTurn()}")
                Log.d("OverlayLockActivity", "   - In check: ${chessEngine.isInCheck()}")
                Log.d("OverlayLockActivity", "   - Game ended: ${chessEngine.isGameEnded()}")
                
                // 🔍 CRITICAL: Verify board synchronization between ChessBoardView and Engine
                verifyBoardSynchronization()
                
            } else {
                Log.e("OverlayLockActivity", "❌ Failed to load puzzle into chess engine")
            }
            
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "❌ Error loading puzzle into engine: ${e.message}", e)
        }
    }
    
    /**
     * 🔍 BOARD SYNCHRONIZATION VERIFICATION
     * Ensures ChessBoardView and ChessEngine have the same position
     */
    private fun verifyBoardSynchronization() {
        try {
            Log.d("OverlayLockActivity", "🔍 Verifying board synchronization...")
            
            // Get FEN from both sources
            val engineFEN = chessEngine.getBoardFEN()
            val boardViewFEN = boardView.getCurrentFEN()
            
            Log.d("OverlayLockActivity", "   🤖 Engine FEN:    $engineFEN")
            Log.d("OverlayLockActivity", "   🎨 BoardView FEN: $boardViewFEN")
            
            // Compare just the piece positions (first part of FEN)
            val enginePieces = engineFEN.split(" ")[0]
            val boardViewPieces = boardViewFEN.split(" ")[0]
            
            if (enginePieces == boardViewPieces) {
                Log.i("OverlayLockActivity", "✅ Board synchronization verified - positions match!")
            } else {
                Log.e("OverlayLockActivity", "❌ BOARD SYNCHRONIZATION MISMATCH!")
                Log.e("OverlayLockActivity", "   🤖 Engine pieces:    $enginePieces")
                Log.e("OverlayLockActivity", "   🎨 BoardView pieces: $boardViewPieces")
                
                // This could explain why moves are invalid - the engine and board don't match!
                Toast.makeText(this, "⚠️ Board synchronization issue detected", Toast.LENGTH_LONG).show()
            }
            
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "Error verifying board synchronization: ${e.message}")
        }
    }
    
    /**
     * 🔄 FALLBACK MOVE VALIDATION
     * Basic chess rules validation when engine fails
     */
    private fun validateMoveBasicRules(from: Pair<Int, Int>, to: Pair<Int, Int>): Boolean {
        try {
            val (fromRow, fromCol) = from
            val (toRow, toCol) = to
            
            // Basic bounds checking
            if (fromRow !in 0..7 || fromCol !in 0..7 || toRow !in 0..7 || toCol !in 0..7) {
                Log.d("OverlayLockActivity", "🔄 Fallback: Out of bounds")
                return false
            }
            
            // Can't move to same square
            if (fromRow == toRow && fromCol == toCol) {
                Log.d("OverlayLockActivity", "🔄 Fallback: Same square")
                return false
            }
            
            // Get current board state from ChessBoardView
            val currentFEN = boardView.getCurrentFEN()
            val boardPieces = FenParser.parse(currentFEN)
            
            // Check if there's a piece at the source
            val pieceAtSource = boardPieces[fromRow][fromCol]
            if (pieceAtSource == ' ') {
                Log.d("OverlayLockActivity", "🔄 Fallback: No piece at source")
                return false
            }
            
            // Check if destination has our own piece (can't capture own pieces)
            val pieceAtDestination = boardPieces[toRow][toCol]
            if (pieceAtDestination != ' ') {
                val sourceIsWhite = pieceAtSource.isUpperCase()
                val destIsWhite = pieceAtDestination.isUpperCase()
                if (sourceIsWhite == destIsWhite) {
                    Log.d("OverlayLockActivity", "🔄 Fallback: Can't capture own piece")
                    return false
                }
            }
            
            Log.d("OverlayLockActivity", "🔄 Fallback validation passed basic rules")
            return true
            
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "Error in fallback validation: ${e.message}")
            return false
        }
    }
    
    private fun initializeViews() {
        try {
            Log.d("OverlayLockActivity", "Starting view initialization...")
            
            // Check if layout was properly inflated
            val rootView = findViewById<View>(android.R.id.content)
            if (rootView == null) {
                Log.e("OverlayLockActivity", "Root view is null - layout inflation failed")
                createMinimalInterface()
                return
            }
            
            // Initialize core views with null checks
            promptView = findViewById(R.id.puzzlePrompt)
            if (promptView == null) {
                Log.e("OverlayLockActivity", "❌ puzzlePrompt not found in layout")
                createMinimalInterface()
                return
            }
            Log.d("OverlayLockActivity", "✓ Found puzzlePrompt")
            
            boardView = findViewById(R.id.chessBoard)
            if (boardView == null) {
                Log.e("OverlayLockActivity", "❌ chessBoard not found in layout - CRITICAL ERROR")
                createMinimalInterface()
                return
            }
            Log.d("OverlayLockActivity", "✓ Found chessBoard")
            
            // Initialize chess timer for professional gameplay
            chessTimerView = findViewById(R.id.chess_timer)
            if (chessTimerView != null) {
                Log.d("OverlayLockActivity", "✓ Found chess_timer")
                try {
                    ScreenCompatibilityManager.optimizeTimer(chessTimerView!!, screenInfo)
                } catch (e: Exception) {
                    Log.w("OverlayLockActivity", "Timer optimization failed: ${e.message}")
                }
            } else {
                Log.w("OverlayLockActivity", "Chess timer not found in layout")
            }
            
            emergencyButton = findViewById(R.id.emergencyUnlockButton)
            if (emergencyButton == null) {
                Log.e("OverlayLockActivity", "❌ emergencyUnlockButton not found in layout")
                createMinimalInterface()
                return
            }
            Log.d("OverlayLockActivity", "✓ Found emergencyUnlockButton")
            
            hintButton = findViewById(R.id.hintButton)
            if (hintButton != null) {
                Log.d("OverlayLockActivity", "✓ Found hintButton")
            } else {
                Log.w("OverlayLockActivity", "hintButton not found")
            }
            
            timerDisplay = findViewById(R.id.puzzleTimer)
            if (timerDisplay != null) {
                Log.d("OverlayLockActivity", "✓ Found puzzleTimer")
            } else {
                Log.w("OverlayLockActivity", "puzzleTimer not found")
            }
            
            // Apply responsive sizing based on screen compatibility
            try {
                ScreenCompatibilityManager.optimizeChessBoard(boardView, screenInfo)
                Log.d("OverlayLockActivity", "✓ Chess board optimized for screen")
            } catch (e: Exception) {
                Log.w("OverlayLockActivity", "Chess board optimization failed: ${e.message}")
            }
            
            // Update board theme on initialization - handle gracefully if it fails
            try {
                boardView.updateTheme()
                Log.d("OverlayLockActivity", "✓ Board theme updated")
            } catch (e: Exception) {
                Log.e("OverlayLockActivity", "Board theme update failed: ${e.message}")
                // Don't fail initialization for theme issues
            }
            
            // Initialize optional UI elements
            val timeDisplay = findViewById<TextView>(R.id.timeDisplay)
            val dateDisplay = findViewById<TextView>(R.id.dateDisplay)
            val difficultyLabel = findViewById<TextView>(R.id.difficultyLabel)
            val undoButton = findViewById<Button>(R.id.undoButton)
            val nextPuzzleButton = findViewById<Button>(R.id.nextPuzzleButton)
            
            if (timeDisplay != null && dateDisplay != null) {
                setupTimeAndDate(timeDisplay, dateDisplay)
                Log.d("OverlayLockActivity", "✓ Time and date setup complete")
            }
            
            if (undoButton != null) {
                setupUndoButton(undoButton)
                Log.d("OverlayLockActivity", "✓ Undo button setup complete")
            }
            
            if (nextPuzzleButton != null) {
                setupNextPuzzleButton(nextPuzzleButton)
                Log.d("OverlayLockActivity", "✓ Next puzzle button setup complete")
            }
            
            if (difficultyLabel != null) {
                setupDifficultyBadge(difficultyLabel)
                Log.d("OverlayLockActivity", "✓ Difficulty badge setup complete")
            }
            
            Log.d("OverlayLockActivity", "🎉 View initialization completed successfully - Chess board should be visible!")
            
        } catch (e: Exception) {
            // Log detailed error and create minimal interface
            Log.e("OverlayLockActivity", "CRITICAL ERROR in view initialization: ${e.message}")
            Log.e("OverlayLockActivity", "Stack trace: ${e.stackTrace.joinToString("\n")}")
            createMinimalInterface()
        }
        
        // Show preview mode indicator
        if (isPreviewMode) {
            val previewIndicator = TextView(this).apply {
                text = getString(R.string.preview_mode_indicator)
                textSize = 12f
                setTextColor(getColor(android.R.color.holo_orange_light))
                gravity = Gravity.CENTER
                setPadding(8, 4, 8, 4)
                setBackgroundColor(getColor(android.R.color.black))
            }
            
            // Add to the root layout
            val rootLayout = findViewById<ViewGroup>(android.R.id.content)
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP
            }
            rootLayout.addView(previewIndicator, layoutParams)
        }
    }
    
    private fun setupTimeAndDate(timeDisplay: TextView, dateDisplay: TextView) {
        val timeFormat = android.text.format.DateFormat.getTimeFormat(this)
        val dateFormat = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault())
        
        val updateTime = object : Runnable {
            override fun run() {
                val now = java.util.Calendar.getInstance().time
                timeDisplay.text = timeFormat.format(now)
                dateDisplay.text = dateFormat.format(now)
                
                // Add battery status for modern lockscreen feel
                val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
                val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                if (batteryLevel > 0) {
                    val batteryIcon = when {
                        batteryLevel > 80 -> "🔋"
                        batteryLevel > 50 -> "🔋"
                        batteryLevel > 20 -> "🪫"
                        else -> "🪫"
                    }
                    dateDisplay.text = getString(R.string.date_battery_format, dateFormat.format(now), batteryIcon, batteryLevel)
                }
                
                timeDisplay.postDelayed(this, 1000) // Update every second
            }
        }
        updateTime.run()
    }
    
    private fun setupUndoButton(undoButton: Button) {
        undoButton.setOnClickListener {
            boardView.undoLastMove()
            Toast.makeText(this, "Move undone", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupNextPuzzleButton(nextPuzzleButton: Button) {
        nextPuzzleButton.setOnClickListener {
            // Load a new puzzle
            val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
            val currentCount = prefs.getInt("current_session_count", 0)
            
            // Increment session count to get next puzzle
            prefs.edit {
                putInt("current_session_count", currentCount + 1)
            }
            
            // Reload with new puzzle
            loadChessPuzzle()
            
            Toast.makeText(this, "🧩 New puzzle loaded!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupDifficultyBadge(difficultyLabel: TextView) {
        val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
        
        // Show "CHESS.COM" instead of difficulty level
        difficultyLabel.text = "CHESS.COM"
        
        // Set consistent styling
        val badge = difficultyLabel.parent as? View
        badge?.setBackgroundResource(R.drawable.difficulty_badge_bg)
        
        Log.d("OverlayLockActivity", "✓ Updated badge to show CHESS.COM branding")
    }
    
    private fun loadChessPuzzle() {
        val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
        val playerColor = prefs.getString("player_color", "white") ?: "white"
        val lastSolvedId = prefs.getString("last_solved_puzzle_id", "") ?: ""
        val sessionCount = prefs.getInt("current_session_count", 0)
        
        Log.i("OverlayLockActivity", "🧩 Loading Chess.com puzzle - session #$sessionCount")
        
        // Handle random color selection
        val actualPlayerColor = if (playerColor == "random") {
            if (Random.nextBoolean()) "white" else "black"
        } else {
            playerColor
        }
        
        // Update chess board player color (AI automatically plays opposite)
        boardView.updatePlayerColor(actualPlayerColor == "white")
        
        // 🏆 GET ONE OF 10 FAMOUS CHESS.COM PUZZLES
        // Cycle through all 10 puzzles to ensure variety
        val puzzleIndex = sessionCount % 10
        currentPuzzle = PuzzleManager.getPuzzle(puzzleIndex)
        
        Log.i("OverlayLockActivity", "🎯 Selected puzzle: ${currentPuzzle.id} - ${currentPuzzle.prompt}")
        
        promptView.text = currentPuzzle.prompt // Use prompt directly (no string formatting needed)
        boardView.setFEN(currentPuzzle.fen, currentPuzzle.solutionFEN)
        
        // 🎨 ENSURE THEME IS ALWAYS UP-TO-DATE
        // Update theme every time puzzle loads to sync with preview changes
        try {
            boardView.updateTheme()
            Log.d("OverlayLockActivity", "🎨 Board theme refreshed for puzzle")
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "Theme refresh failed: ${e.message}")
        }
        
        // 🧠 LOAD PUZZLE INTO CHESS ENGINE
        loadPuzzleIntoEngine()
        
        // Timer will start on first move (not immediately)
        // Reset timer display
        puzzleStartTime = 0L
        timerDisplay.text = getString(R.string.timer_initial)
        
        // Update session count
        prefs.edit {
            putInt("current_session_count", sessionCount + 1)
        }
        
        boardView.onPuzzleSolved = {
            Log.w("OverlayLockActivity", "🚨 PUZZLE SOLVED CALLBACK TRIGGERED - This should only happen on checkmate!")
            Log.w("OverlayLockActivity", "📍 Stack trace for puzzle solved trigger:", Exception("Stack trace"))
            onPuzzleSolved()
        }
        
        // 🧠 CONNECT CHESS ENGINE TO BOARD VIEW
        // All moves must go through the chess engine for validation
        boardView.onMoveAttempted = { from, to ->
            validateMoveWithEngine(from, to)
        }
        
        // 🔄 SYNCHRONIZE ENGINE AFTER SUCCESSFUL MOVES
        // When ChessBoardView executes a move, update the engine to match
        boardView.onMoveExecuted = { from, to ->
            synchronizeEngineWithBoard(from, to)
        }
        
        // Setup first move timer activation - Professional chess timer starts on first move
        boardView.onFirstMoveAttempted = {
            startPuzzleTimer()
            
            // Start professional chess timer when user moves against AI
            chessTimerView?.let { timer ->
                Log.d("OverlayLockActivity", "Starting chess timer on first move against AI")
                timer.startTimer() // No parameters needed
            }
        }
        
        // Setup hint functionality
        hintButton.setOnClickListener {
            Log.d("OverlayLockActivity", "🔍 Hint button clicked - showing hint only")
            showHint()
        }
    }
    
    private fun setupEmergencyUnlock() {
        emergencyButton.setOnClickListener {
            emergencyClickCount++
            
            when (emergencyClickCount) {
                1, 2, 3 -> {
                    val remaining = emergencyClickThreshold - emergencyClickCount
                    Toast.makeText(
                        this,
                        "🚨 Emergency unlock: tap $remaining more times",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                4 -> {
                    Toast.makeText(
                        this,
                        "⚠️ Emergency unlock: tap 1 more time to confirm",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                emergencyClickThreshold -> {
                    showEmergencyUnlockDialog()
                }
            }
            
            // Reset counter after 3 seconds if not completed
            lifecycleScope.launch {
                delay(3000)
                if (emergencyClickCount < emergencyClickThreshold) {
                    emergencyClickCount = 0
                }
            }
        }
    }
    
    private fun showHint() {
        Log.d("OverlayLockActivity", "💡 SHOWING HINT - This should NOT trigger any unlock")
        
        // 🤖 AI MODE RESTRICTION - Only show hints for user's side and turn
        if (isAIMode) {
            if (!isUserTurn) {
                Toast.makeText(this, "🤖 Hints only available during your turn", Toast.LENGTH_SHORT).show()
                Log.d("OverlayLockActivity", "💡 Hint blocked - it's AI's turn")
                return
            }
            
            // Additional context about whose side the user is playing
            val sideInfo = "You are playing as ${userColor.capitalize()}"
            Log.d("OverlayLockActivity", "💡 $sideInfo - showing hint for user's side only")
        }
        
        if (currentPuzzle.hint.isNotEmpty()) {
            val hintTitle = if (isAIMode) "💡 Hint (${userColor.capitalize()} pieces)" else "💡 Hint"
            
            AlertDialog.Builder(this)
                .setTitle(hintTitle)
                .setMessage(currentPuzzle.hint)
                .setPositiveButton("Got it!") { dialog, _ -> 
                    Log.d("OverlayLockActivity", "💡 Hint dialog dismissed - no unlock triggered")
                    dialog.dismiss() 
                }
                .setCancelable(true)
                .setOnDismissListener {
                    Log.d("OverlayLockActivity", "💡 Hint dialog dismissed via cancel - no unlock triggered")
                }
                .show()
        } else {
            Toast.makeText(this, "No hint available for this puzzle", Toast.LENGTH_SHORT).show()
        }
        
        Log.d("OverlayLockActivity", "💡 HINT FUNCTION COMPLETED - No unlock should have occurred")
    }
    
    private fun showEmergencyUnlockDialog() {
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val hasNativeSecurity = keyguardManager.isKeyguardSecure
        
        val message = if (hasNativeSecurity) {
            "Are you sure you want to bypass the chess puzzle? You will still need to authenticate with your device's native lockscreen (PIN/pattern/biometric). This bypass will be logged for security."
        } else {
            "Are you sure you want to bypass the chess puzzle? Warning: No native device security is configured, so this will completely unlock your device. This bypass will be logged for security."
        }
        
        AlertDialog.Builder(this)
            .setTitle("🚨 Emergency Unlock")
            .setMessage(message)
            .setPositiveButton("Yes, Proceed") { _, _ ->
                attemptBiometricUnlock()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                emergencyClickCount = 0
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun attemptBiometricUnlock() {
        try {
            val biometricPrompt = BiometricPrompt(this, 
                ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        
                        // Authorize unlock for emergency bypass
                        isUnlockAuthorized = true
                        unlockReason = "Emergency biometric unlock"
                        
                        logEmergencyUnlock("Biometric authentication successful")
                        unlockDevice(true)
                        Log.d("OverlayLockActivity", "✓ Biometric emergency unlock successful")
                    }
                    
                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Toast.makeText(this@OverlayLockActivity, 
                            "❌ Authentication failed - please try again", Toast.LENGTH_SHORT).show()
                        emergencyClickCount = 0
                        Log.w("OverlayLockActivity", "Biometric authentication failed")
                    }
                    
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        when (errorCode) {
                            BiometricPrompt.ERROR_HW_NOT_PRESENT -> {
                                Toast.makeText(this@OverlayLockActivity, 
                                    "No biometric hardware available - falling back to system lockscreen", 
                                    Toast.LENGTH_LONG).show()
                            }
                            BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                                Toast.makeText(this@OverlayLockActivity, 
                                    "No biometric credentials enrolled - falling back to system lockscreen", 
                                    Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(this@OverlayLockActivity, 
                                    "Biometric error: $errString - falling back to system lockscreen", 
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                        Log.w("OverlayLockActivity", "Biometric error $errorCode: $errString")
                        // Fallback to system lockscreen
                        showSystemLockscreen()
                    }
                }
            )
            
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("🔐 Emergency Unlock")
                .setSubtitle("Authenticate to bypass chess puzzle")
                .setDescription("This will be logged for security purposes")
                .setNegativeButtonText("Cancel")
                .build()
                
            biometricPrompt.authenticate(promptInfo)
            
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "Error setting up biometric authentication: ${e.message}")
            Toast.makeText(this, "Biometric setup failed - falling back to system lockscreen", Toast.LENGTH_LONG).show()
            showSystemLockscreen()
        }
    }
    
    private fun showSystemLockscreen() {
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        
        Log.i("OverlayLockActivity", "🚨 Emergency unlock - checking native lockscreen availability")
        
        if (keyguardManager.isKeyguardSecure) {
            // Native lockscreen is available and secure
            Toast.makeText(this, "🚨 Emergency bypass granted. Transitioning to native lockscreen...", Toast.LENGTH_SHORT).show()
            logEmergencyUnlock("Emergency unlock - transitioning to native lockscreen")
            
            // Finish ChessLock and let system lockscreen take over
            lifecycleScope.launch {
                delay(1000) // Brief delay to show message
                finish() // User will see native PIN/pattern/biometric screen
            }
        } else {
            // No native lockscreen configured - offer complete unlock
            AlertDialog.Builder(this)
                .setTitle("� Emergency Unlock Available")
                .setMessage("No native lockscreen (PIN/pattern/password) is configured. Emergency unlock will completely unlock the device.")
                .setPositiveButton("Complete Unlock") { _, _ ->
                    logEmergencyUnlock("Emergency unlock - no native security configured")
                    isUnlockAuthorized = true
                    unlockReason = "Emergency unlock - no native security"
                    unlockDevice(true)
                }
                .setNeutralButton("Setup Security First") { _, _ ->
                    // Option to configure native lock before unlocking
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                        startActivity(intent)
                        Toast.makeText(this@OverlayLockActivity, "Please set up device security, then try emergency unlock again", Toast.LENGTH_LONG).show()
                        emergencyClickCount = 0 // Reset counter
                    } catch (_: Exception) {
                        Toast.makeText(this@OverlayLockActivity, "Cannot open settings", Toast.LENGTH_SHORT).show()
                        isUnlockAuthorized = true
                        unlockReason = "Emergency unlock - settings failed"
                        unlockDevice(true)
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    emergencyClickCount = 0
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        }
    }
    
    private fun onPuzzleSolved() {
        Log.i("OverlayLockActivity", "🔓 PUZZLE SOLVED - Starting unlock sequence...")
        Log.w("OverlayLockActivity", "📍 onPuzzleSolved stack trace:", Exception("onPuzzleSolved called"))
        
        // Authorize unlock for checkmate victory
        isUnlockAuthorized = true
        unlockReason = "Checkmate victory"
        
        // Stop timer and get solve time
        val solveTimeSeconds = stopTimer()
        Log.d("OverlayLockActivity", "⏱️ Puzzle solved in ${solveTimeSeconds}s")
        
        // Save stats and puzzle completion
        val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
        prefs.edit {
            putString("last_solved_puzzle_id", currentPuzzle.id)
        }
        Log.d("OverlayLockActivity", "💾 Stats saved for puzzle: ${currentPuzzle.id}")
        
        // Update statistics
        StatsManager.recordGame(true, solveTimeSeconds.toInt())
        Log.d("OverlayLockActivity", "📊 Game statistics updated")
            
        Log.i("OverlayLockActivity", "🎉 Triggering unlock success animation...")
        showUnlockSuccess()
    }
    
    private fun showUnlockSuccess() {
        Toast.makeText(this, "🎉 Chess puzzle solved! Device unlocked!", Toast.LENGTH_LONG).show()
        
        // Optional: Show success animation
        boardView.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(200)
            .withEndAction {
                boardView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .withEndAction {
                        // Check if native lock should be activated after ChessLock
                        handleSuccessfulUnlock()
                    }
            }
    }
    
    private fun logEmergencyUnlock(reason: String) {
        val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
        val currentCount = prefs.getInt("emergency_unlock_count", 0)
        prefs.edit {
            putInt("emergency_unlock_count", currentCount + 1)
            putLong("last_emergency_unlock", System.currentTimeMillis())
            putString("last_emergency_reason", reason)
        }
    }
    
    private fun handleSuccessfulUnlock() {
        val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
        
        // Deactivate ChessLock mode first
        prefs.edit { putBoolean("chess_lock_active", false) }
        
        val nativeLockEnabled = prefs.getBoolean("nativeLock", false)
        
        Log.i("OverlayLockActivity", "🔐 Processing successful unlock - Native lock enabled: $nativeLockEnabled")
        
        if (nativeLockEnabled) {
            // User wants native lock integration - check if it's available
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            if (keyguardManager.isKeyguardSecure) {
                // Native lock is properly configured, transition to it
                Toast.makeText(this, "🏆 Chess puzzle solved! Transitioning to native lockscreen...", Toast.LENGTH_SHORT).show()
                logSuccessfulUnlock("Checkmate victory - transitioning to native lock")
                
                // Delay slightly to show the message, then finish to reveal native lock
                lifecycleScope.launch {
                    delay(1500)
                    finish() // This will reveal the native lockscreen (PIN/pattern/biometric)
                }
            } else {
                // Native lock setting enabled but no system security configured
                AlertDialog.Builder(this)
                    .setTitle("⚠️ Native Lock Not Configured")
                    .setMessage("You enabled native lock integration, but no system PIN/pattern/password is set. The device will unlock completely.")
                    .setPositiveButton("Continue Unlock") { _, _ ->
                        logSuccessfulUnlock("Checkmate victory - native lock not configured")
                        isUnlockAuthorized = true
                        unlockReason = "Checkmate victory - native lock not configured"
                        unlockDevice()
                    }
                    .setNeutralButton("Setup Security") { _, _ ->
                        // Open system security settings to configure native lock
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                            startActivity(intent)
                            Toast.makeText(this@OverlayLockActivity, "Please set up PIN/pattern/password, then restart ChessLock", Toast.LENGTH_LONG).show()
                        } catch (_: Exception) {
                            Toast.makeText(this@OverlayLockActivity, "Cannot open settings", Toast.LENGTH_SHORT).show()
                        }
                        isUnlockAuthorized = true
                        unlockReason = "Checkmate victory - going to security settings"
                        unlockDevice()
                    }
                    .setCancelable(false)
                    .show()
            }
        } else {
            // Native lock disabled - direct unlock after chess victory
            Toast.makeText(this, "🏆 Chess puzzle solved! Device unlocked!", Toast.LENGTH_SHORT).show()
            logSuccessfulUnlock("Checkmate victory - direct unlock")
            isUnlockAuthorized = true
            unlockReason = "Checkmate victory - direct unlock"
            unlockDevice()
        }
    }
    
    private fun unlockDevice(isEmergencyUnlock: Boolean = false) {
        Log.w("OverlayLockActivity", "🚨 UNLOCK DEVICE CALLED - Emergency: $isEmergencyUnlock")
        Log.w("OverlayLockActivity", "📍 unlockDevice stack trace:", Exception("unlockDevice called"))
        
        // 🛡️ SECURITY CHECK: Only allow authorized unlocks
        if (!isUnlockAuthorized) {
            Log.e("OverlayLockActivity", "🚫 UNAUTHORIZED UNLOCK ATTEMPT BLOCKED!")
            Log.e("OverlayLockActivity", "🚫 Unlock was not authorized through proper channels")
            Toast.makeText(this, "🚫 Unauthorized unlock attempt blocked", Toast.LENGTH_LONG).show()
            return
        }
        
        Log.i("OverlayLockActivity", "✅ Unlock authorized: $unlockReason")
        
        // Record loss if emergency unlock was used
        if (isEmergencyUnlock) {
            val timeSpent = stopTimer()
            StatsManager.recordGame(false, timeSpent.toInt())
        }
        
        // Save stats and deactivate ChessLock
        val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
        val currentSolvedCount = prefs.getInt("puzzles_solved", 0)
        prefs.edit {
            putInt("puzzles_solved", if (isEmergencyUnlock) currentSolvedCount else currentSolvedCount + 1)
            putLong("last_unlock_time", System.currentTimeMillis())
            putBoolean("chess_lock_active", false) // Deactivate ChessLock
        }
            
        // Complete unlock - dismiss lockscreen entirely
        finish()
    }
    
    private fun logSuccessfulUnlock(method: String) {
        val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
        val currentCount = prefs.getInt("successful_unlocks", 0)
        prefs.edit {
            putInt("successful_unlocks", currentCount + 1)
            putLong("last_successful_unlock", System.currentTimeMillis())
            putString("last_unlock_method", method)
        }
    }
    
    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isPreviewMode) {
                    // In preview mode, allow back button to exit
                    finish()
                } else {
                    // In actual lock mode, prevent back button from dismissing lockscreen
                    Toast.makeText(this@OverlayLockActivity, "🔒 Solve the chess puzzle to unlock", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
    
    override fun onPause() {
        super.onPause()
        // Prevent activity from being paused/hidden while ChessLock is active
        val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
        val isChessLockActive = prefs.getBoolean("chess_lock_active", false)
        if (isChessLockActive) {
            moveTaskToBack(false) // Don't allow hiding
        }
    }
    
    private fun startPuzzleTimer() {
        puzzleStartTime = System.currentTimeMillis()
        updateTimer()
    }
    
    private fun updateTimer() {
        if (puzzleStartTime > 0) {
            val elapsed = (System.currentTimeMillis() - puzzleStartTime) / 1000
            val minutes = elapsed / 60
            val seconds = elapsed % 60
            timerDisplay.text = getString(R.string.timer_format, minutes.toInt(), seconds.toInt())
            
            // Continue updating every second
            timerDisplay.postDelayed({ updateTimer() }, 1000)
        }
    }
    
    private fun stopTimer(): Long {
        val elapsed = if (puzzleStartTime > 0) {
            (System.currentTimeMillis() - puzzleStartTime) / 1000
        } else 0L
        puzzleStartTime = 0L
        return elapsed
    }
    
    private fun createMinimalInterface() {
        // Create a simple emergency interface if normal layout fails
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(getColor(android.R.color.black))
            
            addView(TextView(this@OverlayLockActivity).apply {
                text = getString(R.string.emergency_mode_title)
                textSize = 18f
                setTextColor(getColor(android.R.color.white))
                gravity = Gravity.CENTER
                setPadding(16, 16, 16, 16)
            })
            
            addView(Button(this@OverlayLockActivity).apply {
                text = getString(R.string.emergency_unlock_button)
                setOnClickListener { 
                    finish()
                }
                setPadding(32, 16, 32, 16)
            })
        })
    }
    
    /**
     * ENHANCED CALL HANDLING SYSTEM
     * Ensures ChessLock doesn't interfere with phone calls
     */
    
    private var callStateReceiver: BroadcastReceiver? = null
    
    private fun isInCall(): Boolean {
        return try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val callState = telephonyManager.callState
            val inCall = callState != TelephonyManager.CALL_STATE_IDLE
            
            // Also check shared preferences for call state
            val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
            val deviceInCall = prefs.getBoolean("device_in_call", false)
            
            val result = inCall || deviceInCall
            Log.d("OverlayLockActivity", "📞 Call state check - TelephonyManager: $inCall, Prefs: $deviceInCall, Result: $result")
            result
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "Error checking call state: ${e.message}")
            // Fallback to shared preferences
            val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
            prefs.getBoolean("device_in_call", false)
        }
    }
    
    private fun setupCallMonitoring() {
        if (isPreviewMode) return // No call monitoring in preview mode
        
        try {
            // Set up broadcast receiver for call state changes
            callStateReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    intent?.let {
                        when (it.action) {
                            "android.intent.action.PHONE_STATE" -> {
                                val state = it.getStringExtra("state")
                                handleCallStateChange(state)
                            }
                            "android.intent.action.NEW_OUTGOING_CALL" -> {
                                Log.d("OverlayLockActivity", "📞 Outgoing call detected - closing ChessLock")
                                handleIncomingCall()
                            }
                        }
                    }
                }
            }
            
            // Register receiver for call state monitoring
            val filter = IntentFilter().apply {
                addAction("android.intent.action.PHONE_STATE")
                addAction("android.intent.action.NEW_OUTGOING_CALL")
            }
            registerReceiver(callStateReceiver, filter)
            
            Log.d("OverlayLockActivity", "📞 Call monitoring set up successfully")
            
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "Failed to setup call monitoring: ${e.message}")
        }
    }
    
    private fun handleCallStateChange(state: String?) {
        Log.d("OverlayLockActivity", "📞 Call state changed to: $state")
        
        when (state) {
            "RINGING" -> {
                Log.d("OverlayLockActivity", "📞 Incoming call - ChessLock should step aside")
                handleIncomingCall()
            }
            "OFFHOOK" -> {
                Log.d("OverlayLockActivity", "📞 Call answered - ChessLock stepping aside")
                handleIncomingCall()
            }
            "IDLE" -> {
                Log.d("OverlayLockActivity", "📞 Call ended - ChessLock can resume normally")
                // Don't auto-restart ChessLock here, let normal wake logic handle it
            }
        }
    }
    
    private fun handleIncomingCall() {
        try {
            // Immediately close ChessLock to allow call interface
            Log.d("OverlayLockActivity", "📞 Gracefully closing ChessLock for call")
            
            // Update preferences to indicate call state
            val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
            prefs.edit().putBoolean("device_in_call", true).apply()
            
            // Show brief message (optional)
            Toast.makeText(this, "📞 ChessLock paused for call", Toast.LENGTH_SHORT).show()
            
            // Close ChessLock gracefully
            finish()
            
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "Error handling incoming call: ${e.message}")
            // Force close as fallback
            finish()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Unregister call state receiver
        try {
            callStateReceiver?.let { receiver ->
                unregisterReceiver(receiver)
                Log.d("OverlayLockActivity", "📞 Call monitoring receiver unregistered")
            }
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "Error unregistering call receiver: ${e.message}")
        }
        
        // Clean up timer if running
        try {
            chessTimerView?.stopTimer()
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "Error stopping timer: ${e.message}")
        }
    }
    
    /**
     * ENHANCED NATIVE LOCK PREVENTION
     * Prevents native fingerprint/PIN from overriding ChessLock unexpectedly
     */
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        
        if (!isPreviewMode) {
            if (hasFocus) {
                Log.d("OverlayLockActivity", "🔒 ChessLock gained focus - reinforcing override")
                reinforceOverride()
            } else {
                Log.d("OverlayLockActivity", "🔒 ChessLock lost focus - checking for native lock interference")
                handleFocusLoss()
            }
        }
    }
    
    private fun reinforceOverride() {
        try {
            // Modern approach for maintaining lockscreen override (minSdk=28 guarantees availability)
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            
            // Ensure we stay on top
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
            
            // Re-hide system UI
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.apply {
                    hide(WindowInsets.Type.systemBars())
                    systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
            
        } catch (e: Exception) {
            Log.e("OverlayLockActivity", "Error reinforcing override: ${e.message}")
        }
    }
    
    private fun handleFocusLoss() {
        // Check if focus loss is due to a call
        if (isInCall()) {
            Log.d("OverlayLockActivity", "📞 Focus lost due to call - this is expected")
            return
        }
        
        // If not in call but lost focus, try to regain it (prevent native lock interference)
        lifecycleScope.launch {
            delay(100) // Brief delay to avoid conflicts
            
            if (!isFinishing && !isInCall()) {
                try {
                    Log.d("OverlayLockActivity", "🔒 Attempting to regain focus from potential native lock interference")
                    
                    // Bring ChessLock back to front
                    val intent = Intent(this@OverlayLockActivity, OverlayLockActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                        )
                        putExtra("focus_recovery", true)
                    }
                    startActivity(intent)
                    
                } catch (e: Exception) {
                    Log.e("OverlayLockActivity", "Error recovering focus: ${e.message}")
                }
            }
        }
    }
}
