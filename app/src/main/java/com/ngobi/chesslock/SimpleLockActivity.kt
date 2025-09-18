package com.ngobi.chesslock

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SimpleLockActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val isPreviewMode = intent.getBooleanExtra("preview_mode", false)
        
        try {
            createSimpleLockInterface(isPreviewMode)
        } catch (e: Exception) {
            createUltimateFallback(isPreviewMode)
        }
    }
    
    private fun createSimpleLockInterface(isPreview: Boolean) {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 48)
        }
        
        // Status
        layout.addView(TextView(this).apply {
            text = if (isPreview) "👁️ PREVIEW MODE" else "🔒 CHESSLOCK ACTIVE"
            textSize = 16f
            setTextColor(if (isPreview) Color.YELLOW else Color.RED)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 24)
        })
        
        // Title
        layout.addView(TextView(this).apply {
            text = "♛ ChessLock"
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 16)
        })
        
        // Time
        val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        layout.addView(TextView(this).apply {
            text = currentTime
            textSize = 48f
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 8)
        })
        
        // Date
        val currentDate = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault()).format(java.util.Date())
        layout.addView(TextView(this).apply {
            text = currentDate
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 32)
        })
        
        // Chess puzzle placeholder
        layout.addView(TextView(this).apply {
            text = "♚ ♛ ♜ ♝ ♞ ♟\n♔ ♕ ♖ ♗ ♘ ♙"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.DKGRAY)
        })
        
        // Puzzle description
        layout.addView(TextView(this).apply {
            text = """
                Chess Puzzle Challenge
                
                Solve the puzzle to unlock your device.
                
                Note: This is a simplified interface.
                The full chess board will be available
                once technical issues are resolved.
            """.trimIndent()
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = android.view.Gravity.CENTER
            setPadding(16, 16, 16, 24)
        })
        
        // Unlock button (simulated)
        layout.addView(Button(this).apply {
            text = "🔓 Simulate Puzzle Solved"
            setOnClickListener {
                Toast.makeText(this@SimpleLockActivity, "Puzzle completed! Device unlocked.", Toast.LENGTH_SHORT).show()
                finish()
            }
            setPadding(16, 16, 16, 16)
        })
        
        // Emergency button
        layout.addView(Button(this).apply {
            text = if (isPreview) "Close Preview" else "Emergency Exit"
            setOnClickListener { 
                finish()
            }
            setPadding(16, 16, 16, 16)
        })
        
        scrollView.addView(layout)
        setContentView(scrollView)
    }
    
    private fun createUltimateFallback(isPreview: Boolean) {
        val button = Button(this).apply {
            text = if (isPreview) "CLOSE PREVIEW" else "EMERGENCY EXIT"
            setOnClickListener { finish() }
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
            textSize = 18f
        }
        setContentView(button)
    }
}
