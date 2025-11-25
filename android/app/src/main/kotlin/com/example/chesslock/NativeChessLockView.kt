package com.example.chesslock

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

/**
 * Pure Native Android Chess Lockscreen View
 * No Flutter - guaranteed touch handling
 */
class NativeChessLockView(context: Context, private val onUnlock: () -> Unit) : View(context) {
    
    private val TAG = "NativeChessLock"
    
    // Paint objects
    private val lightSquarePaint = Paint().apply {
        color = Color.parseColor("#F0D9B5")
        style = Paint.Style.FILL
    }
    
    private val darkSquarePaint = Paint().apply {
        color = Color.parseColor("#B58863")
        style = Paint.Style.FILL
    }
    
    private val selectedSquarePaint = Paint().apply {
        color = Color.parseColor("#80FFFF00")
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private val piecePaint = Paint().apply {
        textSize = 80f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    
    // Chess board state (simplified mate in 1 puzzle)
    private var board = arrayOf(
        arrayOf("♜", "♞", "♝", "♛", "♚", "♝", "♞", "♜"),
        arrayOf("♟", "♟", "♟", "♟", "", "♟", "♟", "♟"),
        arrayOf("", "", "", "", "♟", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "♙", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("♙", "♙", "♙", "♙", "", "♙", "♙", "♙"),
        arrayOf("♖", "♘", "♗", "♕", "♔", "♗", "♘", "♖")
    )
    
    private var selectedRow: Int? = null
    private var selectedCol: Int? = null
    private var squareSize = 0f
    private var boardOffset = 0f
    
    init {
        setBackgroundColor(Color.parseColor("#1A1A2E"))
        Log.d(TAG, "✅ Native chess view created")
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = min(w, h) * 0.9f
        squareSize = size / 8f
        boardOffset = (w - size) / 2f
        Log.d(TAG, "Board size: $size, square: $squareSize, offset: $boardOffset")
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw title
        canvas.drawText("Solve Chess Puzzle to Unlock", width / 2f, 100f, textPaint)
        canvas.drawText("Tap pieces to move", width / 2f, 180f, textPaint.apply { textSize = 40f })
        textPaint.textSize = 60f
        
        // Draw chess board
        for (row in 0..7) {
            for (col in 0..7) {
                val left = boardOffset + col * squareSize
                val top = 250f + row * squareSize
                val right = left + squareSize
                val bottom = top + squareSize
                
                // Draw square
                val isLight = (row + col) % 2 == 0
                val paint = when {
                    selectedRow == row && selectedCol == col -> selectedSquarePaint
                    isLight -> lightSquarePaint
                    else -> darkSquarePaint
                }
                canvas.drawRect(left, top, right, bottom, paint)
                
                // Draw piece
                val piece = board[row][col]
                if (piece.isNotEmpty()) {
                    piecePaint.color = if (piece in "♔♕♖♗♘♙") Color.WHITE else Color.BLACK
                    canvas.drawText(
                        piece,
                        left + squareSize / 2f,
                        top + squareSize / 2f + 30f,
                        piecePaint
                    )
                }
            }
        }
        
        // Draw hint
        canvas.drawText("Hint: Move White Queen to checkmate!", width / 2f, height - 100f, 
            textPaint.apply { textSize = 35f; color = Color.YELLOW })
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y - 250f
            
            if (x >= boardOffset && y >= 0) {
                val col = ((x - boardOffset) / squareSize).toInt()
                val row = (y / squareSize).toInt()
                
                if (col in 0..7 && row in 0..7) {
                    handleSquareTap(row, col)
                    Log.d(TAG, "✅ NATIVE TOUCH WORKS! Tapped: row=$row, col=$col")
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun handleSquareTap(row: Int, col: Int) {
        if (selectedRow == null) {
            // Select piece if it's white
            val piece = board[row][col]
            if (piece.isNotEmpty() && piece in "♔♕♖♗♘♙") {
                selectedRow = row
                selectedCol = col
                Log.d(TAG, "Selected: $piece at ($row, $col)")
                invalidate()
            }
        } else {
            // Move piece
            val fromRow = selectedRow!!
            val fromCol = selectedCol!!
            val piece = board[fromRow][fromCol]
            
            // Simple move validation (for demo)
            if (row != fromRow || col != fromCol) {
                board[row][col] = piece
                board[fromRow][fromCol] = ""
                Log.d(TAG, "Moved $piece from ($fromRow,$fromCol) to ($row,$col)")
                
                // Check if it's mate (simplified - queen takes king position as example)
                if (piece == "♕" && row == 0 && col == 4) {
                    Log.d(TAG, "🎉 CHECKMATE! Unlocking...")
                    postDelayed({ onUnlock() }, 500)
                }
            }
            
            selectedRow = null
            selectedCol = null
            invalidate()
        }
    }
    
    fun resetPuzzle() {
        board = arrayOf(
            arrayOf("♜", "♞", "♝", "♛", "♚", "♝", "♞", "♜"),
            arrayOf("♟", "♟", "♟", "♟", "", "♟", "♟", "♟"),
            arrayOf("", "", "", "", "♟", "", "", ""),
            arrayOf("", "", "", "", "", "", "", ""),
            arrayOf("", "", "", "", "♙", "", "", ""),
            arrayOf("", "", "", "", "", "", "", ""),
            arrayOf("♙", "♙", "♙", "♙", "", "♙", "♙", "♙"),
            arrayOf("♖", "♘", "♗", "♕", "♔", "♗", "♘", "♖")
        )
        selectedRow = null
        selectedCol = null
        invalidate()
        Log.d(TAG, "♟️ Puzzle reset")
    }
}
