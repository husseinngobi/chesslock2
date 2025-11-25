package com.ngobi.chesslock

import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.toColorInt
import androidx.preference.PreferenceManager
import kotlin.math.*

class ChessBoardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // Dynamic color scheme using BoardTheme
    // Note: Chess pieces remain white/black regardless of board theme
    private var currentTheme = BoardTheme.getTheme("classic")
    private val prefs: SharedPreferences = context.getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
    
    // Sound manager for chess move effects
    private val soundManager = ChessSoundManager.getInstance(context)
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Responsive sizing properties
    private var boardSize = 0f
    private var squareSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f
    
    private var board: Array<Array<Char>> = Array(8) { Array(8) { ' ' } }
    private var selectedSquare: Pair<Int, Int>? = null
    private var lastMoveFrom: Pair<Int, Int>? = null
    private var lastMoveTo: Pair<Int, Int>? = null
    private var validMoves = mutableSetOf<Pair<Int, Int>>()

    private var targetFEN: String? = null
    var onPuzzleSolved: (() -> Unit)? = null
    var onMoveAttempted: ((from: Pair<Int, Int>, to: Pair<Int, Int>) -> Boolean)? = null
    var onMoveExecuted: ((from: Pair<Int, Int>, to: Pair<Int, Int>) -> Unit)? = null // Called after successful move
    var onFirstMoveAttempted: (() -> Unit)? = null // Timer activation callback
    
    private var isFirstMoveAttempted = false

    // AI system for interactive play
    private val chessAI = ChessAI()
    private var isPlayerTurn = true
    private var playerIsWhite = true
    
    private val moveHistory = mutableListOf<Array<Array<Char>>>()
    private var animationProgress = 0f
    private val handler = Handler(Looper.getMainLooper())
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 400 // Slightly longer for smoother feel
        interpolator = android.view.animation.AccelerateDecelerateInterpolator() // Professional easing
        addUpdateListener { valueAnimator ->
            animationProgress = valueAnimator.animatedValue as Float
            invalidate()
        }
    }

    init {
        contentDescription = "Interactive chess board for puzzle solving"
        isFocusable = true
        isClickable = true
        
        // Load current theme from preferences
        updateTheme()
        
        // Setup text paint for coordinates
        textPaint.textSize = 24f
        textPaint.color = Color.BLACK
        textPaint.textAlign = Paint.Align.CENTER
        
        // Setup shadow paint for pieces
        shadowPaint.color = Color.BLACK
        shadowPaint.alpha = 50
    }

    /**
     * Updates the board theme from preferences
     * Note: This affects both board colors and piece styles
     */
    fun updateTheme() {
        // Use "board_style" for board theme if available, otherwise use "piece_style"
        val boardStyle = prefs.getString("board_style", "classic") ?: "classic"
        val pieceStyle = prefs.getString("piece_style", "traditional") ?: "traditional"
        
        // Use board_style for theme, but log both for debugging
        currentTheme = BoardTheme.getTheme(boardStyle)
        
        // Force a complete redraw to refresh both theme and pieces
        requestLayout()
        invalidate()
        
        Log.d("ChessBoardView", "Theme updated - Board: $boardStyle, Pieces: $pieceStyle")
    }

    fun setFEN(fen: String, solutionFEN: String? = null) {
        board = FenParser.parse(fen)
        targetFEN = solutionFEN
        moveHistory.clear()
        selectedSquare = null
        lastMoveFrom = null
        lastMoveTo = null
        validMoves.clear()
        isFirstMoveAttempted = false // Reset for new puzzle
        invalidate()
    }

    fun getCurrentFEN(): String = FenParser.toFEN(board)

    fun undoLastMove() {
        if (moveHistory.isNotEmpty()) {
            board = moveHistory.removeAt(moveHistory.lastIndex)
            selectedSquare = null
            lastMoveFrom = null
            lastMoveTo = null
            validMoves.clear()
            playUndoAnimation()
        }
    }
    
    private fun playUndoAnimation() {
        animator.start()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Ensure the board is always square and fits properly on screen
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        
        val size = when {
            widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY -> {
                min(widthSize, heightSize)
            }
            widthMode == MeasureSpec.EXACTLY -> widthSize
            heightMode == MeasureSpec.EXACTLY -> heightSize
            else -> min(widthSize, heightSize)
        }
        
        setMeasuredDimension(size, size)
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateBoardDimensions(w, h)
    }
    
    private fun calculateBoardDimensions(width: Int, height: Int) {
        // Calculate the maximum size that fits in the available space
        boardSize = min(width, height).toFloat()
        squareSize = boardSize / 8f
        
        // Center the board if the view is not square
        offsetX = (width - boardSize) / 2f
        offsetY = (height - boardSize) / 2f
        
        // Setup text paint with appropriate size
        textPaint.apply {
            color = "#8B4513".toColorInt()
            textSize = squareSize * 0.2f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        
        // Setup shadow paint for pieces
        shadowPaint.apply {
            color = "#40000000".toColorInt()
            maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Skip drawing if dimensions not calculated yet
        if (squareSize == 0f) return

        val borderSize = squareSize * 0.05f

        // Draw board background with elegant border using current theme
        paint.color = currentTheme.darkSquareColor
        canvas.drawRect(offsetX, offsetY, offsetX + boardSize, offsetY + boardSize, paint)

        // Draw squares with enhanced styling using current theme
        for (row in 0..7) {
            for (col in 0..7) {
                // 🎯 BOARD ORIENTATION: Flip board coordinates for black player
                val displayRow = if (playerIsWhite) row else 7 - row
                val displayCol = if (playerIsWhite) col else 7 - col
                
                val isLight = (row + col) % 2 == 0
                paint.color = if (isLight) currentTheme.lightSquareColor else currentTheme.darkSquareColor
                
                val left = offsetX + displayCol * squareSize + borderSize
                val top = offsetY + displayRow * squareSize + borderSize
                val right = offsetX + (displayCol + 1) * squareSize - borderSize
                val bottom = offsetY + (displayRow + 1) * squareSize - borderSize
                
                canvas.drawRect(left, top, right, bottom, paint)

                // Draw coordinate labels on edges
                if (displayRow == (if (playerIsWhite) 7 else 0)) { // Bottom row - files (a-h)
                    textPaint.color = if (isLight) currentTheme.darkSquareColor else currentTheme.lightSquareColor
                    textPaint.alpha = 180
                    val file = if (playerIsWhite) ('a' + col).toString() else ('h' - col).toString()
                    canvas.drawText(file, left + squareSize/2 - borderSize, bottom - 8f, textPaint)
                }
                if (displayCol == (if (playerIsWhite) 0 else 7)) { // Left column - ranks (1-8)
                    textPaint.color = if (isLight) currentTheme.darkSquareColor else currentTheme.lightSquareColor
                    textPaint.alpha = 180
                    val rank = if (playerIsWhite) (8 - row).toString() else (row + 1).toString()
                    canvas.drawText(rank, left + 8f, top + squareSize/2 - borderSize, textPaint)
                }

                val piece = board[row][col]
                if (piece != ' ') {
                    drawPieceWithShadow(canvas, piece, displayCol, displayRow)
                }
            }
        }

        // Highlight last move using current theme (with board orientation)
        lastMoveFrom?.let { (fromRow, fromCol) ->
            val displayRow = if (playerIsWhite) fromRow else 7 - fromRow
            val displayCol = if (playerIsWhite) fromCol else 7 - fromCol
            drawSquareHighlight(canvas, displayCol, displayRow, currentTheme.highlightColor, 0.3f)
        }
        lastMoveTo?.let { (toRow, toCol) ->
            val displayRow = if (playerIsWhite) toRow else 7 - toRow
            val displayCol = if (playerIsWhite) toCol else 7 - toCol
            drawSquareHighlight(canvas, displayCol, displayRow, currentTheme.highlightColor, 0.5f)
        }

        // 🎯 USER-ONLY HIGHLIGHTS: Only show valid moves during player's turn
        if (isPlayerTurn) {
            validMoves.forEach { (row, col) ->
                val displayRow = if (playerIsWhite) row else 7 - row
                val displayCol = if (playerIsWhite) col else 7 - col
                drawSquareHighlight(canvas, displayCol, displayRow, currentTheme.possibleMoveColor, 0.4f)
                // Draw small circle for possible moves
                paint.color = currentTheme.possibleMoveColor
                paint.alpha = 150
                val centerX = offsetX + displayCol * squareSize + squareSize / 2
                val centerY = offsetY + displayRow * squareSize + squareSize / 2
                canvas.drawCircle(centerX, centerY, squareSize * 0.15f, paint)
            }
        }

        // 🎯 USER-ONLY SELECTION: Only highlight selected square during player's turn
        if (isPlayerTurn) {
            selectedSquare?.let { (row, col) ->
                // Only show selection for player's own pieces
                if (isPieceOwnedByPlayer(row, col)) {
                    val displayRow = if (playerIsWhite) row else 7 - row
                    val displayCol = if (playerIsWhite) col else 7 - col
                    val alpha = (128 + 127 * sin(animationProgress * 2 * PI)).toInt()
                    drawSquareHighlight(canvas, displayCol, displayRow, currentTheme.highlightColor, alpha / 255f)
                    
                    // Draw selection border
                    paint.color = currentTheme.highlightColor
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 6f
                    paint.alpha = 255
                    canvas.drawRoundRect(
                        offsetX + displayCol * squareSize + 3f,
                        offsetY + displayRow * squareSize + 3f,
                        offsetX + (displayCol + 1) * squareSize - 3f,
                        offsetY + (displayRow + 1) * squareSize - 3f,
                        8f, 8f, paint
                    )
                    paint.style = Paint.Style.FILL
                }
            }
        }
    }
    
    private fun drawSquareHighlight(canvas: Canvas, col: Int, row: Int, color: Int, alpha: Float) {
        paint.color = color
        paint.alpha = (255 * alpha).toInt()
        canvas.drawRect(
            offsetX + col * squareSize,
            offsetY + row * squareSize,
            offsetX + (col + 1) * squareSize,
            offsetY + (row + 1) * squareSize,
            paint
        )
        paint.alpha = 255
    }

    private fun drawPieceWithShadow(canvas: Canvas, piece: Char, col: Int, row: Int) {
        val drawableId = PieceManager.getDrawableId(piece, context)
        if (drawableId == 0) return

        val drawable = AppCompatResources.getDrawable(context, drawableId) ?: return
        val padding = squareSize * 0.1f
        val shadowOffset = 3f
        
        // Draw shadow
        drawable.setBounds(
            (offsetX + col * squareSize + padding + shadowOffset).toInt(),
            (offsetY + row * squareSize + padding + shadowOffset).toInt(),
            (offsetX + (col + 1) * squareSize - padding + shadowOffset).toInt(),
            (offsetY + (row + 1) * squareSize - padding + shadowOffset).toInt()
        )
        drawable.colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP)
        drawable.alpha = 30
        drawable.draw(canvas)
        
        // Draw piece
        drawable.setBounds(
            (offsetX + col * squareSize + padding).toInt(),
            (offsetY + row * squareSize + padding).toInt(),
            (offsetX + (col + 1) * squareSize - padding).toInt(),
            (offsetY + (row + 1) * squareSize - padding).toInt()
        )
        drawable.colorFilter = null
        drawable.alpha = 255
        drawable.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (squareSize == 0f) return false
        
        // Adjust touch coordinates for board offset
        val adjustedX = event.x - offsetX
        val adjustedY = event.y - offsetY
        
        // Check if touch is within the board bounds
        if (adjustedX < 0 || adjustedY < 0 || adjustedX >= boardSize || adjustedY >= boardSize) {
            return false
        }
        
        var col = (adjustedX / squareSize).toInt().coerceIn(0, 7)
        var row = (adjustedY / squareSize).toInt().coerceIn(0, 7)
        
        // 🎯 BOARD ORIENTATION: Convert touch coordinates for black player
        if (!playerIsWhite) {
            col = 7 - col
            row = 7 - row
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val tapped = Pair(row, col)
                handleSquareTouch(tapped)
                performClick()
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                return true
            }
        }

        return super.onTouchEvent(event)
    }
    
    private fun handleSquareTouch(tapped: Pair<Int, Int>) {
        val (row, col) = tapped
        
        // 🚫 TURN ENFORCEMENT: Only allow interaction during player's turn
        if (!isPlayerTurn) {
            Log.d("ChessBoardView", "🚫 Blocked touch - it's AI's turn (no highlights shown)")
            return
        }
        
        when {
            selectedSquare == null -> {
                // Select a piece if there's one on the tapped square AND it belongs to the player
                if (board[row][col] != ' ' && isPieceOwnedByPlayer(row, col)) {
                    selectedSquare = tapped
                    validMoves.clear()
                    generateValidMoves(tapped)
                    if (!animator.isRunning) animator.start()
                    soundManager.playSound(ChessSoundManager.SoundType.PIECE_SELECT)
                    invalidate()
                } else if (board[row][col] != ' ') {
                    // Player tried to select opponent's piece
                    Log.d("ChessBoardView", "Blocked selection - not player's piece")
                    soundManager.playSound(ChessSoundManager.SoundType.ILLEGAL_MOVE)
                }
            }
            
            selectedSquare == tapped -> {
                // Deselect if tapping the same square
                selectedSquare = null
                validMoves.clear()
                invalidate()
            }
            
            board[row][col] != ' ' && isSameColor(selectedSquare!!, tapped) && isPieceOwnedByPlayer(row, col) -> {
                // Select different piece of same color (only if it belongs to player)
                selectedSquare = tapped
                validMoves.clear()
                generateValidMoves(tapped)
                if (!animator.isRunning) animator.start()
                soundManager.playSound(ChessSoundManager.SoundType.PIECE_SELECT)
                invalidate()
            }
            
            else -> {
                // Attempt to move (only allowed if moving player's piece)
                val (fromRow, fromCol) = selectedSquare!!
                if (isPieceOwnedByPlayer(fromRow, fromCol)) {
                    attemptMove(selectedSquare!!, tapped)
                } else {
                    Log.d("ChessBoardView", "Blocked move - trying to move opponent's piece")
                    soundManager.playSound(ChessSoundManager.SoundType.ILLEGAL_MOVE)
                    selectedSquare = null
                    validMoves.clear()
                    invalidate()
                }
            }
        }
    }
    
    private fun attemptMove(from: Pair<Int, Int>, to: Pair<Int, Int>) {
        val (fromRow, fromCol) = from
        val (toRow, toCol) = to
        
        // Trigger first move callback for timer activation
        if (!isFirstMoveAttempted) {
            isFirstMoveAttempted = true
            onFirstMoveAttempted?.invoke()
        }
        
        // Check if move is valid for this puzzle
        val moveAllowed = onMoveAttempted?.invoke(from, to) ?: isBasicLegalMove(from, to)
        
        if (moveAllowed) {
            // Save state for undo
            moveHistory.add(board.map { it.copyOf() }.toTypedArray())
            
            // Determine move type for sound effects
            val capturedPiece = board[toRow][toCol]
            val movingPiece = board[fromRow][fromCol]
            
            // Execute move
            val piece = board[fromRow][fromCol]
            board[fromRow][fromCol] = ' '
            board[toRow][toCol] = piece
            
            // 🎯 NOTIFY THAT MOVE WAS EXECUTED - Important for engine synchronization
            onMoveExecuted?.invoke(from, to)
            
            // Play appropriate sound effect
            when {
                capturedPiece != ' ' -> {
                    soundManager.playSound(ChessSoundManager.SoundType.CAPTURE)
                }
                isCastlingMove(movingPiece, from, to) -> {
                    soundManager.playSound(ChessSoundManager.SoundType.CASTLE)
                }
                else -> {
                    soundManager.playSound(ChessSoundManager.SoundType.MOVE)
                }
            }
            
            // Update UI state
            lastMoveFrom = from
            lastMoveTo = to
            selectedSquare = null
            validMoves.clear()
            
            if (!animator.isRunning) animator.start()
            invalidate()

            // Check if puzzle is solved
            val currentFEN = getCurrentFEN()
            if (targetFEN != null && currentFEN == targetFEN) {
                // Puzzle solved! Provide satisfying feedback
                soundManager.playSound(ChessSoundManager.SoundType.PUZZLE_SOLVED)
                
                // Enhanced victory animation sequence
                animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(200)
                    .setInterpolator(android.view.animation.OvershootInterpolator())
                    .withEndAction {
                        animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(200)
                            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                            .withEndAction {
                                onPuzzleSolved?.invoke()
                            }
                    }
                
                Log.d("ChessBoardView", "🎉 Puzzle solved! Target FEN reached.")
            } else {
                // Check if AI should respond (either free play or lockscreen AI mode)
                val aiEnabled = prefs.getBoolean("ai_enabled_lockscreen", true)
                if (targetFEN == null || aiEnabled) {
                    isPlayerTurn = false
                    
                    // 🚫 TURN ENFORCEMENT: Clear any player selections when AI's turn starts
                    selectedSquare = null
                    validMoves.clear()
                    
                    // Stop selection animation during AI's turn
                    if (animator.isRunning) {
                        animator.cancel()
                    }
                    
                    invalidate()
                    
                    triggerAIMove()
                }
            }
        } else {
            // Invalid move - clear selection and play error sound
            soundManager.playSound(ChessSoundManager.SoundType.ILLEGAL_MOVE)
            selectedSquare = null
            validMoves.clear()
            invalidate()
            
            // Provide haptic feedback for invalid move
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
    
    private fun generateValidMoves(from: Pair<Int, Int>) {
        val (row, col) = from
        val piece = board[row][col].lowercaseChar()
        
        // Generate basic valid moves based on piece type
        when (piece) {
            'p' -> generatePawnMoves(from)
            'r' -> generateRookMoves(from)
            'n' -> generateKnightMoves(from)
            'b' -> generateBishopMoves(from)
            'q' -> generateQueenMoves(from)
            'k' -> generateKingMoves(from)
        }
    }
    
    private fun generatePawnMoves(from: Pair<Int, Int>) {
        val (row, col) = from
        val piece = board[row][col]
        val isWhite = piece.isUpperCase()
        val direction = if (isWhite) -1 else 1
        val startRow = if (isWhite) 6 else 1
        
        // Forward moves
        if (isValidSquare(row + direction, col) && board[row + direction][col] == ' ') {
            validMoves.add(Pair(row + direction, col))
            
            // Two squares forward from starting position
            if (row == startRow && board[row + 2 * direction][col] == ' ') {
                validMoves.add(Pair(row + 2 * direction, col))
            }
        }
        
        // Captures
        for (dc in listOf(-1, 1)) {
            val newRow = row + direction
            val newCol = col + dc
            if (isValidSquare(newRow, newCol) && 
                board[newRow][newCol] != ' ' && 
                board[newRow][newCol].isUpperCase() != isWhite) {
                validMoves.add(Pair(newRow, newCol))
            }
        }
    }
    
    private fun generateRookMoves(from: Pair<Int, Int>) {
        val directions = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
        generateSlidingMoves(from, directions)
    }
    
    private fun generateBishopMoves(from: Pair<Int, Int>) {
        val directions = listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
        generateSlidingMoves(from, directions)
    }
    
    private fun generateQueenMoves(from: Pair<Int, Int>) {
        generateRookMoves(from)
        generateBishopMoves(from)
    }
    
    private fun generateKnightMoves(from: Pair<Int, Int>) {
        val (row, col) = from
        val moves = listOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        
        for ((dr, dc) in moves) {
            val newRow = row + dr
            val newCol = col + dc
            if (isValidSquare(newRow, newCol) && canMoveTo(from, Pair(newRow, newCol))) {
                validMoves.add(Pair(newRow, newCol))
            }
        }
    }
    
    private fun generateKingMoves(from: Pair<Int, Int>) {
        val (row, col) = from
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val newRow = row + dr
                val newCol = col + dc
                if (isValidSquare(newRow, newCol) && canMoveTo(from, Pair(newRow, newCol))) {
                    validMoves.add(Pair(newRow, newCol))
                }
            }
        }
    }
    
    private fun generateSlidingMoves(from: Pair<Int, Int>, directions: List<Pair<Int, Int>>) {
        val (row, col) = from
        
        for ((dr, dc) in directions) {
            var newRow = row + dr
            var newCol = col + dc
            
            while (isValidSquare(newRow, newCol)) {
                if (board[newRow][newCol] == ' ') {
                    validMoves.add(Pair(newRow, newCol))
                } else {
                    // Can capture opponent piece
                    if (canMoveTo(from, Pair(newRow, newCol))) {
                        validMoves.add(Pair(newRow, newCol))
                    }
                    break // Can't move further in this direction
                }
                newRow += dr
                newCol += dc
            }
        }
    }
    
    private fun isValidSquare(row: Int, col: Int): Boolean = 
        row in 0..7 && col in 0..7
    
    private fun isCastlingMove(piece: Char, from: Pair<Int, Int>, to: Pair<Int, Int>): Boolean {
        // Check if this is a king moving more than one square (castling indicator)
        val (fromRow, fromCol) = from
        val (toRow, toCol) = to
        
        return (piece.lowercaseChar() == 'k') && 
               (fromRow == toRow) && 
               (abs(toCol - fromCol) > 1)
    }
    
    private fun canMoveTo(from: Pair<Int, Int>, to: Pair<Int, Int>): Boolean {
        val (fromRow, fromCol) = from
        val (toRow, toCol) = to
        val fromPiece = board[fromRow][fromCol]
        val toPiece = board[toRow][toCol]
        
        return toPiece == ' ' || fromPiece.isUpperCase() != toPiece.isUpperCase()
    }
    
    private fun isSameColor(pos1: Pair<Int, Int>, pos2: Pair<Int, Int>): Boolean {
        val piece1 = board[pos1.first][pos1.second]
        val piece2 = board[pos2.first][pos2.second]
        return piece1.isUpperCase() == piece2.isUpperCase()
    }
    
    /**
     * 🚫 TURN ENFORCEMENT: Check if piece belongs to the current player
     */
    private fun isPieceOwnedByPlayer(row: Int, col: Int): Boolean {
        val piece = board[row][col]
        if (piece == ' ') return false
        
        val pieceIsWhite = piece.isUpperCase()
        return pieceIsWhite == playerIsWhite
    }
    
    private fun triggerAIMove() {
        val difficulty = prefs.getString("difficulty", "medium") ?: "medium"
        
        // Add natural thinking delay for more professional feel
        val thinkingDelay = when (difficulty) {
            "easy" -> 300L   // Quick response for easy puzzles
            "medium" -> 600L // Moderate thinking time
            "hard" -> 900L   // Longer consideration for hard puzzles
            else -> 600L
        }
        
        handler.postDelayed({
            chessAI.generateMove(board, !playerIsWhite) { aiMove ->
                aiMove?.let { move ->
                    // Execute AI move with enhanced feedback
                    board[move.to.first][move.to.second] = move.piece
                    board[move.from.first][move.from.second] = ' '
                    
                    // Update visual state
                    lastMoveFrom = move.from
                    lastMoveTo = move.to
                    
                    // Play appropriate sound with better audio feedback
                    when {
                        move.capturedPiece != ' ' -> {
                            soundManager.playSound(ChessSoundManager.SoundType.CAPTURE)
                        }
                        move.piece.lowercase() == "k" -> {
                            soundManager.playSound(ChessSoundManager.SoundType.MOVE) // Special king move sound
                        }
                        else -> {
                            soundManager.playSound(ChessSoundManager.SoundType.MOVE)
                        }
                    }
                    
                    if (!animator.isRunning) animator.start()
                    invalidate()
                    
                    // Return turn to player
                    isPlayerTurn = true
                    
                    Log.d("ChessBoardView", "AI move completed: ${move.from} -> ${move.to}")
                }
            }
        }, thinkingDelay)
    }
    
    /**
     * Update player color setting - AI automatically plays opposite color
     */
    fun updatePlayerColor(isWhite: Boolean) {
        playerIsWhite = isWhite
        
        // Force redraw to reflect board orientation change
        invalidate()
        
        // If it's currently AI's turn and color changed, trigger AI move with new color
        if (!isPlayerTurn) {
            triggerAIMove()
        }
        
        Log.d("ChessBoardView", "Player color updated to: ${if (isWhite) "white" else "black"}, board orientation updated")
    }
    
    /**
     * Get the current player color
     * @return true if player is White, false if player is Black
     */
    fun isPlayerWhite(): Boolean {
        return playerIsWhite
    }
    
    /**
     * Update piece style and refresh display
     */
    fun updatePieceStyle() {
        // Re-read preferences and force redraw with new piece style
        val pieceStyle = prefs.getString("piece_style", "traditional") ?: "traditional"
        val boardStyle = prefs.getString("board_style", "classic") ?: "classic"
        
        // Update theme if needed
        currentTheme = BoardTheme.getTheme(boardStyle)
        
        // Force redraw with new settings
        invalidate()
        Log.d("ChessBoardView", "Piece style updated - Pieces: $pieceStyle, Board: $boardStyle")
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun isBasicLegalMove(from: Pair<Int, Int>, to: Pair<Int, Int>): Boolean {
        // Basic validation - check if move is in valid moves list
        // The 'from' parameter validates the source square is selected
        return selectedSquare == from && validMoves.contains(to)
    }
}