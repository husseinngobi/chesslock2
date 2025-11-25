package com.example.chesslock

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

/**
 * Pure Native Android Chess Lockscreen
 * Renders chess board and handles touch directly in Kotlin
 * No Flutter - guaranteed to work!
 */
class NativeChessLockscreen(context: Context) : View(context) {
    
    // Chess board state (8x8 grid)
    // "wp" = white pawn, "bn" = black knight, etc.
    private var board = arrayOf(
        arrayOf("br", "bn", "bb", "bq", "bk", "bb", "bn", "br"),
        arrayOf("bp", "bp", "bp", "bp", "bp", "bp", "bp", "bp"),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("wp", "wp", "wp", "wp", "wp", "wp", "wp", "wp"),
        arrayOf("wr", "wn", "wb", "wq", "wk", "wb", "wn", "wr")
    )
    
    private var selectedSquare: Pair<Int, Int>? = null
    private var squareSize = 0f
    private var boardOffset = 0f
    
    // Colors
    private val lightSquareColor = Color.parseColor("#F0D9B5")
    private val darkSquareColor = Color.parseColor("#B58863")
    private val selectedColor = Color.parseColor("#AAFF00")
    
    private val paint = Paint().apply {
        isAntiAlias = true
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }
    
    // Chess piece Unicode characters
    private val pieceSymbols = mapOf(
        "wk" to "♔", "wq" to "♕", "wr" to "♖", "wb" to "♗", "wn" to "♘", "wp" to "♙",
        "bk" to "♚", "bq" to "♛", "br" to "♜", "bb" to "♝", "bn" to "♞", "bp" to "♟"
    )
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Calculate square size and centering
        val size = min(w, h) * 0.9f
        squareSize = size / 8
        boardOffset = (min(w, h) - size) / 2
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw background
        canvas.drawColor(Color.parseColor("#2C3E50"))
        
        // Draw title
        paint.color = Color.WHITE
        paint.textSize = 60f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Chess Lock", width / 2f, 100f, paint)
        
        paint.textSize = 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Solve the puzzle to unlock", width / 2f, 150f, paint)
        
        // Draw chess board
        drawChessBoard(canvas)
        
        // Draw instructions
        paint.textSize = 25f
        canvas.drawText("Tap pieces to move", width / 2f, height - 100f, paint)
    }
    
    private fun drawChessBoard(canvas: Canvas) {
        val startY = 200f + boardOffset
        
        for (row in 0..7) {
            for (col in 0..7) {
                val isLight = (row + col) % 2 == 0
                val isSelected = selectedSquare?.let { it.first == row && it.second == col } ?: false
                
                // Draw square background
                paint.color = when {
                    isSelected -> selectedColor
                    isLight -> lightSquareColor
                    else -> darkSquareColor
                }
                
                val left = boardOffset + col * squareSize
                val top = startY + row * squareSize
                canvas.drawRect(left, top, left + squareSize, top + squareSize, paint)
                
                // Draw piece if present
                val piece = board[row][col]
                if (piece.isNotEmpty()) {
                    paint.color = if (piece.startsWith("w")) Color.WHITE else Color.BLACK
                    paint.textSize = squareSize * 0.7f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    
                    val symbol = pieceSymbols[piece] ?: piece
                    canvas.drawText(
                        symbol,
                        left + squareSize / 2,
                        top + squareSize / 2 + paint.textSize / 3,
                        paint
                    )
                }
            }
        }
        
        // Draw board border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        paint.color = Color.parseColor("#8B4513")
        canvas.drawRect(
            boardOffset, startY,
            boardOffset + squareSize * 8, startY + squareSize * 8,
            paint
        )
        paint.style = Paint.Style.FILL
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val startY = 200f + boardOffset
            val x = event.x
            val y = event.y
            
            // Check if touch is on the board
            if (x >= boardOffset && x <= boardOffset + squareSize * 8 &&
                y >= startY && y <= startY + squareSize * 8) {
                
                val col = ((x - boardOffset) / squareSize).toInt()
                val row = ((y - startY) / squareSize).toInt()
                
                handleSquareTap(row, col)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun handleSquareTap(row: Int, col: Int) {if (selectedSquare == null) {
            // Select piece if present
            if (board[row][col].isNotEmpty()) {
                selectedSquare = Pair(row, col)
                invalidate() // Redraw to show selection
            }
        } else {
            // Move piece
            val (fromRow, fromCol) = selectedSquare!!
            val piece = board[fromRow][fromCol]
            
            // Simple move (no validation yet - just testing touch)
            board[row][col] = piece
            board[fromRow][fromCol] = ""
            
            selectedSquare = null
            invalidate() // Redrawto ($row,$col)")
        }
    }
    
    fun resetBoard() {
        board = arrayOf(
            arrayOf("br", "bn", "bb", "bq", "bk", "bb", "bn", "br"),
            arrayOf("bp", "bp", "bp", "bp", "bp", "bp", "bp", "bp"),
            arrayOf("", "", "", "", "", "", "", ""),
            arrayOf("", "", "", "", "", "", "", ""),
            arrayOf("", "", "", "", "", "", "", ""),
            arrayOf("", "", "", "", "", "", "", ""),
            arrayOf("wp", "wp", "wp", "wp", "wp", "wp", "wp", "wp"),
            arrayOf("wr", "wn", "wb", "wq", "wk", "wb", "wn", "wr")
        )
        selectedSquare = null
        invalidate()
    }
}
