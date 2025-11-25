package com.example.chesslock

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlin.math.max
import kotlin.math.min

/**
 * Professional Chess Lockscreen with Full Rules, AI, and Beautiful UI
 */
class ProfessionalChessLockscreen(context: Context) : View(context) {
    
    private val engine = ChessEngine()
    private val ai = ChessAI(engine)
    private val prefs: SharedPreferences = context.getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
    
    // Game state
    private var selectedSquare: ChessEngine.Position? = null
    private var validMoves = listOf<ChessEngine.Position>()
    private var isPlayerTurn = true
    private var gameMode = GameMode.PUZZLE // Default to puzzle mode
    private var aiDifficulty = ChessAI.Difficulty.MEDIUM
    private var playerColor = ChessEngine.Color.WHITE
    private var dialogShown = false // Prevent dialog from showing multiple times
    
    enum class GameMode { PUZZLE, VS_AI, PRACTICE }
    
    // UI dimensions
    private var squareSize = 0f
    private var boardOffset = 0f
    private var boardStartY = 0f
    
    // Colors (customizable from settings)
    private var lightSquareColor = Color.parseColor(prefs.getString("lightSquare", "#F0D9B5") ?: "#F0D9B5")
    private var darkSquareColor = Color.parseColor(prefs.getString("darkSquare", "#B58863") ?: "#B58863")
    private var selectedColor = Color.parseColor("#7CAF50")
    private var validMoveColor = Color.parseColor("#88AAFF00")
    private var checkColor = Color.parseColor("#FFCC0000")
    private var backgroundColor = Color.parseColor("#1E1E1E")
    
    private val paint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    
    private val shadowPaint = Paint().apply {
        isAntiAlias = true
        setShadowLayer(8f, 0f, 4f, Color.BLACK)
    }
    
    // Chess piece Unicode symbols
    private val pieceSymbols = mapOf(
        "wk" to "♔", "wq" to "♕", "wr" to "♖", "wb" to "♗", "wn" to "♘", "wp" to "♙",
        "bk" to "♚", "bq" to "♛", "br" to "♜", "bb" to "♝", "bn" to "♞", "bp" to "♟"
    )
    
    // Buttons
    private var emergencyButtonRect = RectF()
    private var newPuzzleButtonRect = RectF()
    private var hintButtonRect = RectF()
    
    // Puzzle mode state
    private var currentPuzzle: ChessPuzzles.Puzzle? = null
    private var puzzleMoveCount = 0
    private var puzzleSolved = false
    private var playerMoveIndex = 0 // Tracks position in puzzle solution
    
    init {
        // Load preferences FIRST - this sets gameMode
        loadPreferences()// Setup initial board based on game mode
        when(gameMode) {
            GameMode.PUZZLE -> loadRandomPuzzle()
            GameMode.VS_AI -> {
                engine.reset()
                if (playerColor == ChessEngine.Color.BLACK) {
                    makeAIMove()
                }
            }
            GameMode.PRACTICE -> engine.reset()
        }
    }
    
    private fun loadRandomPuzzle() {
        // Check if user selected a specific puzzle
        val selectedPuzzleId = prefs.getInt("selectedPuzzleId", -1)
        
        currentPuzzle = if (selectedPuzzleId == -1) {
            // Random mode: use difficulty filter
            val puzzleDifficulty = when(prefs.getString("puzzleDifficulty", "RANDOM")) {
                "EASY" -> ChessPuzzles.Difficulty.EASY
                "MEDIUM" -> ChessPuzzles.Difficulty.MEDIUM
                "HARD" -> ChessPuzzles.Difficulty.HARD
                else -> null
            }
            ChessPuzzles.getRandomPuzzle(puzzleDifficulty)
        } else {
            // User selected specific puzzle
            ChessPuzzles.allPuzzles.getOrNull(selectedPuzzleId - 1)
        }
        
        currentPuzzle?.let {
            engine.loadFromFEN(it.fen)
            puzzleMoveCount = 0
            puzzleSolved = false
            playerMoveIndex = 0
            hintShown = false
            
            // Respect player color choice
            // If player chose black but puzzle is white to move (or vice versa), let AI make first move
            val playAsWhite = prefs.getBoolean("playAsWhite", true)
            val puzzlePlayerColor = if (playAsWhite) ChessEngine.Color.WHITE else ChessEngine.Color.BLACK
            
            if (engine.currentPlayer != puzzlePlayerColor) {
                // Wrong color to move - AI makes first move to flip turn
                Handler(Looper.getMainLooper()).postDelayed({ makePuzzleAIMove() }, 500)
            }
        }
    }
    
    private fun resetBoard() {
        // Reset to starting position for VS_AI or PRACTICE mode
        engine.reset()
        selectedSquare = null
        val playAsWhite = prefs.getBoolean("playAsWhite", true)
        isPlayerTurn = if (gameMode == GameMode.VS_AI) playAsWhite else true
        puzzleSolved = false
        
        // If playing as black in VS_AI mode, let AI make first move
        if (gameMode == GameMode.VS_AI && !playAsWhite) {
            Handler(Looper.getMainLooper()).postDelayed({ makeAIMove() }, 500)
        }
    }
    
    private fun loadPreferences() {
        gameMode = GameMode.valueOf(prefs.getString("gameMode", "PUZZLE") ?: "PUZZLE")
        aiDifficulty = ChessAI.Difficulty.valueOf(prefs.getString("aiDifficulty", "MEDIUM") ?: "MEDIUM")
        playerColor = if (prefs.getBoolean("playAsWhite", true)) ChessEngine.Color.WHITE else ChessEngine.Color.BLACK
        
        lightSquareColor = Color.parseColor(prefs.getString("lightSquare", "#F0D9B5") ?: "#F0D9B5")
        darkSquareColor = Color.parseColor(prefs.getString("darkSquare", "#B58863") ?: "#B58863")
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // IMPROVED RESPONSIVE LAYOUT - Better proportions
        // Calculate optimal spacing based on screen height
        val headerHeight = h * 0.14f // 14% for header
        val emergencyBtnHeight = h * 0.10f // 10% for emergency button
        val puzzleBtnHeight = h * 0.085f // 8.5% for puzzle buttons
        val buttonMargin = h * 0.025f // 2.5% margin between buttons
        val statusHeight = h * 0.07f // 7% for status text
        val bottomPadding = h * 0.03f // 3% padding at bottom
        
        // ALWAYS calculate space for puzzle buttons (they're conditionally drawn)
        val buttonAreaHeight = emergencyBtnHeight + puzzleBtnHeight + buttonMargin + bottomPadding
        
        // Available space for chess board
        val availableHeight = h - headerHeight - buttonAreaHeight - statusHeight - (h * 0.03f)
        val availableWidth = w * 0.92f // 92% of width
        
        // Board size: fit in available space
        val size = min(availableWidth, availableHeight)
        squareSize = size / 8
        boardOffset = (w - size) / 2
        boardStartY = headerHeight + (h * 0.02f)
        
        // Button dimensions
        val btnWidth = w * 0.85f // 85% of screen width (wider buttons!)
        val btnMarginHorizontal = w * 0.03f
        
        // Emergency button at bottom (visible, but not oversized)
        val emergencyLeft = (w - btnWidth) / 2
        val emergencyTop = h - emergencyBtnHeight - bottomPadding
        emergencyButtonRect = RectF(
            emergencyLeft, 
            emergencyTop, 
            emergencyLeft + btnWidth, 
            emergencyTop + emergencyBtnHeight
        )
        
        // Puzzle buttons (above emergency button)
        val puzzleBtnWidth = (btnWidth - btnMarginHorizontal) / 2
        val puzzleBtnTop = emergencyTop - puzzleBtnHeight - buttonMargin
        
        newPuzzleButtonRect = RectF(
            emergencyLeft,
            puzzleBtnTop,
            emergencyLeft + puzzleBtnWidth,
            puzzleBtnTop + puzzleBtnHeight
        )
        
        hintButtonRect = RectF(
            emergencyLeft + puzzleBtnWidth + btnMarginHorizontal,
            puzzleBtnTop,
            emergencyLeft + btnWidth,
            puzzleBtnTop + puzzleBtnHeight
        )}
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)// Background with gradient
        val gradient = LinearGradient(0f, 0f, 0f, height.toFloat(), backgroundColor, 
            Color.parseColor("#0D0D0D"), Shader.TileMode.CLAMP)
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        
        // Draw header
        drawHeader(canvas)
        
        // Draw chess board with shadow
        drawBoardShadow(canvas)
        drawChessBoard(canvas)
        
        // Draw status
        drawStatus(canvas)
        
        // Draw buttons
        if (gameMode == GameMode.PUZZLE) {
            drawPuzzleButtons(canvas)
        }
        drawEmergencyButton(canvas)
    }
    
    private fun drawHeader(canvas: Canvas) {
        // BIGGER TEXT SIZES for better readability
        val titleSize = min(height * 0.05f, 64f) // Bigger title
        val subtitleSize = min(height * 0.032f, 32f) // 50% bigger subtitle
        val descSize = min(height * 0.028f, 28f) // 40% bigger description
        
        val titleY = height * 0.06f // 6% from top
        val subtitleY = height * 0.09f // 9% from top
        val descY = height * 0.12f // 12% from top
        
        // Title
        paint.color = Color.WHITE
        paint.textSize = titleSize
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        shadowPaint.color = Color.WHITE
        shadowPaint.textSize = titleSize
        shadowPaint.typeface = paint.typeface
        shadowPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("♔ ChessLock ♔", width / 2f, titleY, shadowPaint)
        
        // Mode badge - CLEAR indication of current mode
        paint.textSize = subtitleSize * 0.8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val modeText = when(gameMode) {
            GameMode.PUZZLE -> "🧩 PUZZLE MODE"
            GameMode.VS_AI -> "🤖 VS AI MODE"
            GameMode.PRACTICE -> "♟️ PRACTICE MODE"
        }
        val modeColor = when(gameMode) {
            GameMode.PUZZLE -> Color.parseColor("#FFD700")
            GameMode.VS_AI -> Color.parseColor("#FF5722")
            GameMode.PRACTICE -> Color.parseColor("#4CAF50")
        }
        paint.color = modeColor
        canvas.drawText(modeText, width / 2f, subtitleY, paint)
        
        // Subtitle
        paint.textSize = subtitleSize * 0.85f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#AAAAAA")
        val subtitle = when(gameMode) {
            GameMode.PUZZLE -> currentPuzzle?.let { 
                "${it.name} (${it.difficulty.name})"
            } ?: "Loading puzzle..."
            GameMode.VS_AI -> {
                val yourColor = if (playerColor == ChessEngine.Color.WHITE) "White" else "Black"
                "You: $yourColor • AI: ${aiDifficulty.name}"
            }
            GameMode.PRACTICE -> "Practice mode • Both sides"
        }
        canvas.drawText(subtitle, width / 2f, subtitleY + subtitleSize, paint)
        
        // Puzzle description
        if (gameMode == GameMode.PUZZLE && currentPuzzle != null) {
            paint.textSize = descSize
            paint.color = Color.parseColor("#FFD700")
            canvas.drawText(currentPuzzle!!.description, width / 2f, descY, paint)
        }
    }
    
    private fun drawBoardShadow(canvas: Canvas) {
        paint.color = Color.parseColor("#40000000")
        val shadowOffset = 12f
        canvas.drawRect(
            boardOffset + shadowOffset, boardStartY + shadowOffset,
            boardOffset + squareSize * 8 + shadowOffset, boardStartY + squareSize * 8 + shadowOffset,
            paint
        )
    }
    
    private fun drawChessBoard(canvas: Canvas) {
        // Find king in check
        val whiteKingInCheck = engine.isInCheck(ChessEngine.Color.WHITE)
        val blackKingInCheck = engine.isInCheck(ChessEngine.Color.BLACK)
        
        // Determine board orientation based on player color
        val playAsWhite = prefs.getBoolean("playAsWhite", true)
        val flipBoard = !playAsWhite // Flip board when playing as black
        
        for (row in 0..7) {
            for (col in 0..7) {
                // Flip coordinates if playing as black
                val displayRow = if (flipBoard) 7 - row else row
                val displayCol = if (flipBoard) 7 - col else col
                
                val isLight = (displayRow + displayCol) % 2 == 0
                val pos = ChessEngine.Position(displayRow, displayCol)
                val isSelected = pos == selectedSquare
                val isValidMove = validMoves.contains(pos)
                
                val piece = engine.board[displayRow][displayCol]
                val isKingInCheck = piece?.type == ChessEngine.PieceType.KING && 
                    ((piece.color == ChessEngine.Color.WHITE && whiteKingInCheck) ||
                     (piece.color == ChessEngine.Color.BLACK && blackKingInCheck))
                
                // Draw square
                paint.color = when {
                    isKingInCheck -> checkColor
                    isSelected -> selectedColor
                    isLight -> lightSquareColor
                    else -> darkSquareColor
                }
                
                val left = boardOffset + col * squareSize
                val top = boardStartY + row * squareSize
                canvas.drawRect(left, top, left + squareSize, top + squareSize, paint)
                
                // Draw valid move indicator
                if (isValidMove) {
                    paint.color = validMoveColor
                    val radius = if (piece != null) squareSize * 0.45f else squareSize * 0.15f
                    canvas.drawCircle(left + squareSize / 2, top + squareSize / 2, radius, paint)
                }
                
                // Draw coordinate labels (responsive size)
                val labelSize = max(squareSize * 0.18f, 12f) // Min 12sp, scales with board
                if (col == 0) {
                    paint.color = if (isLight) darkSquareColor else lightSquareColor
                    paint.textSize = labelSize
                    paint.textAlign = Paint.Align.LEFT
                    val rankLabel = if (flipBoard) (row + 1).toString() else (8 - row).toString()
                    canvas.drawText(rankLabel, left + squareSize * 0.05f, top + labelSize + squareSize * 0.05f, paint)
                    paint.textAlign = Paint.Align.CENTER
                }
                if (row == 7) {
                    val fileLabel = if (flipBoard) ('h' - col).toString() else ('a' + col).toString()
                    paint.color = if (isLight) darkSquareColor else lightSquareColor
                    paint.textSize = labelSize
                    canvas.drawText(('a' + col).toString(), left + squareSize - labelSize, top + squareSize - squareSize * 0.05f, paint)
                }
                
                // Draw piece
                if (piece != null) {
                    val pieceCode = when(piece.color) {
                        ChessEngine.Color.WHITE -> "w"
                        ChessEngine.Color.BLACK -> "b"
                    } + when(piece.type) {
                        ChessEngine.PieceType.KING -> "k"
                        ChessEngine.PieceType.QUEEN -> "q"
                        ChessEngine.PieceType.ROOK -> "r"
                        ChessEngine.PieceType.BISHOP -> "b"
                        ChessEngine.PieceType.KNIGHT -> "n"
                        ChessEngine.PieceType.PAWN -> "p"
                    }
                    
                    val symbol = pieceSymbols[pieceCode] ?: pieceCode
                    paint.color = if (piece.color == ChessEngine.Color.WHITE) Color.WHITE else Color.BLACK
                    paint.textSize = squareSize * 0.75f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    
                    // Shadow for pieces
                    shadowPaint.color = if (piece.color == ChessEngine.Color.WHITE) Color.WHITE else Color.BLACK
                    shadowPaint.textSize = paint.textSize
                    shadowPaint.typeface = paint.typeface
                    shadowPaint.textAlign = Paint.Align.CENTER
                    
                    canvas.drawText(
                        symbol,
                        left + squareSize / 2,
                        top + squareSize / 2 + paint.textSize / 3,
                        shadowPaint
                    )
                }
            }
        }
        
        // Board border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        paint.color = Color.parseColor("#8B4513")
        canvas.drawRect(
            boardOffset, boardStartY,
            boardOffset + squareSize * 8, boardStartY + squareSize * 8,
            paint
        )
        paint.style = Paint.Style.FILL
    }
    
    private fun drawStatus(canvas: Canvas) {
        // RESPONSIVE STATUS TEXT - Positioned relative to board
        val statusY = boardStartY + squareSize * 8 + height * 0.04f
        val normalSize = min(height * 0.024f, 24f)
        val largeSize = min(height * 0.035f, 35f)
        
        paint.textSize = normalSize
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        when {
            puzzleSolved -> {
                paint.color = Color.parseColor("#4CAF50")
                paint.textSize = largeSize
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("🎉 Puzzle Solved!", width / 2f, statusY, paint)
                // Victory dialog handles unlock - don't auto-unlock here
            }
            engine.isCheckmate() -> {
                paint.color = Color.parseColor("#4CAF50")
                paint.textSize = largeSize
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                
                if (gameMode == GameMode.PUZZLE) {
                    puzzleSolved = true
                    canvas.drawText("✓ Mate! Puzzle Solved!", width / 2f, statusY, paint)
                } else {
                    canvas.drawText("✓ Checkmate! Victory!", width / 2f, statusY, paint)
                    // Show victory dialog for VS_AI and PRACTICE modes
                    if (gameMode == GameMode.VS_AI || gameMode == GameMode.PRACTICE) {
                        postDelayed({ showVictoryDialog() }, 500)
                    }
                }
            }
            engine.isStalemate() -> {
                paint.color = Color.parseColor("#FFA500")
                paint.textSize = normalSize
                if (gameMode == GameMode.PUZZLE) {
                    canvas.drawText("Stalemate - Puzzle failed", width / 2f, statusY, paint)
                } else {
                    canvas.drawText("Stalemate - Try again", width / 2f, statusY, paint)
                }
            }
            engine.isInCheck(engine.currentPlayer) -> {
                paint.color = Color.parseColor("#FF5252")
                paint.textSize = normalSize
                val player = if (engine.currentPlayer == ChessEngine.Color.WHITE) "White" else "Black"
                canvas.drawText("⚠ $player is in Check!", width / 2f, statusY, paint)
            }
            else -> {
                paint.color = Color.parseColor("#CCCCCC")
                paint.textSize = normalSize
                val player = if (engine.currentPlayer == ChessEngine.Color.WHITE) "White" else "Black"
                val status = when(gameMode) {
                    GameMode.PUZZLE -> {
                        currentPuzzle?.let {
                            if (it.mateIn > 0) "Find mate in ${it.mateIn}!" else "Find the winning move!"
                        } ?: "Your turn ($player)"
                    }
                    GameMode.VS_AI -> if (isPlayerTurn) "Your turn ($player)" else "AI is thinking..."
                    GameMode.PRACTICE -> "Your turn ($player)"
                }
                canvas.drawText(status, width / 2f, statusY, paint)
            }
        }
    }
    
    private fun drawPuzzleButtons(canvas: Canvas) {
        val buttonTextSize = min(height * 0.03f, 30f) // Bigger button text
        val cornerRadius = 20f // Rounder corners
        
        // New Puzzle button (blue)
        val newPuzzleGradient = LinearGradient(
            newPuzzleButtonRect.left, newPuzzleButtonRect.top,
            newPuzzleButtonRect.right, newPuzzleButtonRect.bottom,
            Color.parseColor("#1976D2"), Color.parseColor("#2196F3"),
            Shader.TileMode.CLAMP
        )
        paint.shader = newPuzzleGradient
        canvas.drawRoundRect(newPuzzleButtonRect, cornerRadius, cornerRadius, paint)
        paint.shader = null
        
        paint.color = Color.WHITE
        paint.textSize = buttonTextSize
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(
            "🔄 New Puzzle",
            newPuzzleButtonRect.centerX(),
            newPuzzleButtonRect.centerY() + buttonTextSize * 0.35f,
            paint
        )
        
        // Hint button (green)
        val hintGradient = LinearGradient(
            hintButtonRect.left, hintButtonRect.top,
            hintButtonRect.right, hintButtonRect.bottom,
            Color.parseColor("#388E3C"), Color.parseColor("#4CAF50"),
            Shader.TileMode.CLAMP
        )
        paint.shader = hintGradient
        canvas.drawRoundRect(hintButtonRect, cornerRadius, cornerRadius, paint)
        paint.shader = null
        
        paint.color = Color.WHITE
        paint.textSize = buttonTextSize
        canvas.drawText(
            "💡 Hint",
            hintButtonRect.centerX(),
            hintButtonRect.centerY() + buttonTextSize * 0.35f,
            paint
        )
    }
    
    private fun drawEmergencyButton(canvas: Canvas) {
        // Button background with gradient
        val gradient = LinearGradient(
            emergencyButtonRect.left, emergencyButtonRect.top,
            emergencyButtonRect.right, emergencyButtonRect.bottom,
            Color.parseColor("#C62828"), Color.parseColor("#D32F2F"),
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRoundRect(emergencyButtonRect, 25f, 25f, paint)
        paint.shader = null
        
        // Button border (thicker, more visible)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        paint.color = Color.parseColor("#FFCDD2")
        canvas.drawRoundRect(emergencyButtonRect, 25f, 25f, paint)
        paint.style = Paint.Style.FILL
        
        // Button text (BIGGER for visibility)
        val buttonTextSize = min(height * 0.038f, 38f)
        paint.color = Color.WHITE
        paint.textSize = buttonTextSize
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(
            "🚨 EMERGENCY UNLOCK",
            emergencyButtonRect.centerX(),
            emergencyButtonRect.centerY() + buttonTextSize * 0.35f,
            paint
        )
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y
            
            // CRITICAL: HUGE touch area for emergency button (60px padding!)
            val emergencyPadding = 60f
            val expandedEmergency = RectF(
                emergencyButtonRect.left - emergencyPadding,
                emergencyButtonRect.top - emergencyPadding,
                emergencyButtonRect.right + emergencyPadding,
                emergencyButtonRect.bottom + emergencyPadding
            )
            val inEmergency = expandedEmergency.contains(x, y)
            
            // PRIORITY 1: Emergency button with confirmation
            if (inEmergency) {
                showEmergencyUnlockDialog()
                performClick()
                return true
            }
            
            // Check New Puzzle button (BEFORE board check)
            if (gameMode == GameMode.PUZZLE && newPuzzleButtonRect.contains(x, y)) {loadRandomPuzzle()
                invalidate()
                performClick()
                return true
            }
            
            // Check Hint button (BEFORE board check)
            if (gameMode == GameMode.PUZZLE && hintButtonRect.contains(x, y)) {showHint()
                performClick()
                return true
            }
            
            // Check board touch (LAST - lowest priority)
            val onBoard = x >= boardOffset && x <= boardOffset + squareSize * 8 &&
                          y >= boardStartY && y <= boardStartY + squareSize * 8
            
            if (onBoard) {
                if (isPlayerTurn) {
                    var col = ((x - boardOffset) / squareSize).toInt()
                    var row = ((y - boardStartY) / squareSize).toInt()
                    
                    // Flip coordinates if playing as black
                    val playAsWhite = prefs.getBoolean("playAsWhite", true)
                    if (!playAsWhite) {
                        col = 7 - col
                        row = 7 - row
                    }
                    
                    handleSquareTap(ChessEngine.Position(row, col))
                } else {
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
    
    private fun handleSquareTap(pos: ChessEngine.Position) {
        val piece = engine.board[pos.row][pos.col]
        
        // CRITICAL FIX: Prevent moving opponent's pieces (except in practice mode)
        // In VS_AI mode: Only allow moving YOUR color
        // In PUZZLE mode: Respect player color choice (white or black)
        // In PRACTICE mode: Allow moving BOTH sides (null = no restriction)
        val allowedColor = when(gameMode) {
            GameMode.VS_AI -> playerColor // Only your color
            GameMode.PUZZLE -> playerColor // Respect player color choice in puzzles
            GameMode.PRACTICE -> null // NO restriction - move any piece!
        }
        
        if (selectedSquare == null) {
            // Select piece - check color restriction (null means no restriction)
            if (piece != null && (allowedColor == null || piece.color == allowedColor)) {
                selectedSquare = pos
                validMoves = engine.getLegalMovesForPiece(pos).map { it.to }
                invalidate()
            } else if (piece == null) {
            } else {
                // Trying to select wrong color - give feedback
                invalidate()
            }
        } else {
            // Try to move
            val move = ChessEngine.Move(selectedSquare!!, pos)
            
            if (engine.isLegalMove(move)) {
                engine.makeMove(move)
                selectedSquare = null
                validMoves = emptyList()
                
                // Track puzzle progress
                if (gameMode == GameMode.PUZZLE) {
                    puzzleMoveCount++
                    playerMoveIndex++
                    
                    // Check if puzzle is solved (checkmate achieved)
                    if (engine.isCheckmate()) {
                        puzzleSolved = true// Show victory dialog with options
                        showVictoryDialog()
                    } else {
                        // AI plays opponent's move in puzzle mode
                        isPlayerTurn = false
                        invalidate()
                        Handler(Looper.getMainLooper()).postDelayed({ makePuzzleAIMove() }, 600)
                    }
                }
                
                invalidate()
                
                // Check game end
                if (engine.isCheckmate() || engine.isStalemate()) {
                    showVictoryDialog()
                    return
                }
                
                // AI turn in VS_AI mode
                if (gameMode == GameMode.VS_AI && engine.currentPlayer != playerColor) {
                    isPlayerTurn = false
                    invalidate()
                    Handler(Looper.getMainLooper()).postDelayed({ makeAIMove() }, 800)
                }
            } else {
                // Deselect or select new piece - ONLY YOUR COLOR
                if (piece != null && piece.color == allowedColor) {
                    selectedSquare = pos
                    validMoves = engine.getLegalMovesForPiece(pos).map { it.to }
                } else {
                    selectedSquare = null
                    validMoves = emptyList()
                }
                invalidate()
            }
        }
    }
    
    private fun ChessEngine.getLegalMovesForPiece(from: ChessEngine.Position): List<ChessEngine.Move> {
        val moves = mutableListOf<ChessEngine.Move>()
        for (row in 0..7) {
            for (col in 0..7) {
                val move = ChessEngine.Move(from, ChessEngine.Position(row, col))
                if (isLegalMove(move)) {
                    moves.add(move)
                }
            }
        }
        return moves
    }
    
    private fun makeAIMove() {
        val aiMove = ai.getBestMove(aiDifficulty, engine.currentPlayer)
        if (aiMove != null) {
            engine.makeMove(aiMove)
            isPlayerTurn = true
            invalidate()
        }
    }
    
    private fun makePuzzleAIMove() {
        // In puzzle mode, AI makes best move as opponent
        val aiMove = ai.getBestMove(ChessAI.Difficulty.MEDIUM, engine.currentPlayer)
        if (aiMove != null) {
            engine.makeMove(aiMove)
            playerMoveIndex++// Check if puzzle completed after AI move
            if (engine.isCheckmate()) {
                // Player failed to deliver checkmate
                Toast.makeText(context, "❌ Try again! Load a new puzzle.", Toast.LENGTH_SHORT).show()
            }
            
            isPlayerTurn = true
            invalidate()
        } else {
            android.util.Log.e("ChessLock", "⚠️ No AI move available in puzzle!")
            isPlayerTurn = true
            invalidate()
        }
    }
    
    private var hintShown = false
    private var hintMove: ChessEngine.Move? = null
    
    private fun showHint() {
        currentPuzzle?.let { puzzle ->
            if (puzzle.solution.isNotEmpty()) {
                // Show current move from solution as hint
                val hintMoveIndex = minOf(playerMoveIndex, puzzle.solution.size - 1)
                val hintMoveStr = puzzle.solution[hintMoveIndex]
                
                val hintMessage = if (!hintShown) {
                    "💡 Hint: Try move $hintMoveStr"
                } else {
                    "💡 Remember: $hintMoveStr"
                }
                
                Toast.makeText(context, hintMessage, Toast.LENGTH_LONG).show()
                hintShown = true
                invalidate()
            } else {
                Toast.makeText(context, "No hint available for this puzzle", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(context, "No puzzle loaded", Toast.LENGTH_SHORT).show()
    }
    
    private var victoryDialog: android.app.AlertDialog? = null
    
    private fun showVictoryDialog() {
        // Prevent showing dialog multiple times
        if (dialogShown) return
        dialogShown = true
        
        // Dismiss any existing dialog first
        victoryDialog?.dismiss()
        
        // Create victory dialog using Handler to post to main thread
        Handler(Looper.getMainLooper()).post {
            try {
                val dialogBuilder = android.app.AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert)
                
                // Determine title and message based on game state
                val isCheckmate = engine.isCheckmate()
                val isStalemate = engine.isStalemate()
                
                val title = when {
                    isCheckmate -> "🎉 Checkmate!"
                    isStalemate -> "🤝 Stalemate"
                    else -> "🎉 Game Over"
                }
                
                val message = when {
                    isCheckmate && gameMode == GameMode.PUZZLE -> "Puzzle solved! Great job!"
                    isCheckmate && gameMode == GameMode.VS_AI -> "Checkmate! You won!"
                    isCheckmate && gameMode == GameMode.PRACTICE -> "Checkmate achieved!"
                    isStalemate -> "Draw! No legal moves available. Try again?"
                    else -> "Game ended"
                }
                
                dialogBuilder.setTitle(title)
                dialogBuilder.setMessage(message)
                
                // Show UNLOCK button only for checkmate (victory), not for stalemate (draw)
                if (isCheckmate) {
                    dialogBuilder.setPositiveButton("🔓 UNLOCK PHONE") { _, _ ->
                        dialogShown = false // Reset flag
                        victoryDialog?.dismiss()
                        victoryDialog = null
                        incrementStat("successfulUnlocks")
                        incrementStat("totalGamesPlayed")
                        unlockDevice()
                    }
                }
                
                // New Game button - always available (STAYS on lockscreen)
                dialogBuilder.setNegativeButton("🆕 NEW GAME") { _, _ ->
                    dialogShown = false // Reset flag
                    victoryDialog?.dismiss()
                    victoryDialog = null
                    incrementStat("totalGamesPlayed")
                    
                    // Start new game - STAY on lockscreen (DO NOT unlock)
                    when(gameMode) {
                        GameMode.PUZZLE -> loadRandomPuzzle()
                        GameMode.VS_AI -> resetBoard()
                        GameMode.PRACTICE -> resetBoard()
                    }
                    invalidate()
                }
                
                dialogBuilder.setCancelable(false) // Must choose an option
                victoryDialog = dialogBuilder.create()
                
                // CRITICAL: Proper window setup for overlay dialog
                victoryDialog?.window?.apply {
                    setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                    clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                    addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                
                victoryDialog?.show()
            } catch (e: Exception) {
                android.util.Log.e("ChessLock", "❌ Victory dialog failed: ${e.message}")
                e.printStackTrace()
                // Show toast but don't auto-unlock - keep lockscreen active
                Toast.makeText(context, "❌ Dialog error - use Emergency button", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showEmergencyUnlockDialog() {// Set bypass flag (no timer - lasts until next lock event)
        val success = prefs.edit().putBoolean("emergencyBypassActive", true).commit() // Use commit() for immediate writeToast.makeText(context, "🔓 Emergency Bypass - Unlocked Until Next Lock", Toast.LENGTH_LONG).show()
        incrementStat("emergencyUnlocks")
        unlockDevice()
    }
    
    private fun unlockDevice() {try {
            // Broadcast to close overlay
            val intent = android.content.Intent(ChessLockAccessibilityService.ACTION_HIDE_OVERLAY)
            intent.setPackage(context.packageName) // Ensure broadcast stays within our app
            context.sendBroadcast(intent)// Additional verification
            val verifyBypass = prefs.getLong("emergencyBypassUntil", 0L)} catch (e: Exception) {
            android.util.Log.e("ChessLock", "❌ Unlock failed: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun incrementStat(statName: String) {
        val current = prefs.getInt(statName, 0)
        prefs.edit().putInt(statName, current + 1).apply()}
    
    fun resetGame() {
        selectedSquare = null
        validMoves = emptyList()
        isPlayerTurn = true
        puzzleSolved = false
        puzzleMoveCount = 0
        playerMoveIndex = 0
        hintShown = false
        
        loadPreferences()
        
        // Setup board based on game mode
        when(gameMode) {
            GameMode.PUZZLE -> loadRandomPuzzle()
            GameMode.VS_AI -> {
                engine.reset()
                if (playerColor == ChessEngine.Color.BLACK) {
                    makeAIMove()
                }
            }
            GameMode.PRACTICE -> engine.reset()
        }
        
        invalidate()
    }
}
