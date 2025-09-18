package com.ngobi.chesslock

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class DebugLockActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            createDebugInterface()
        } catch (e: Exception) {
            createUltimateFallback()
        }
    }
    
    private fun createDebugInterface() {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Title
        layout.addView(TextView(this).apply {
            text = "ChessLock Debug Mode"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 32)
        })
        
        // Status info
        val isPreview = intent.getBooleanExtra("preview_mode", false)
        layout.addView(TextView(this).apply {
            text = if (isPreview) "Mode: PREVIEW (Safe Testing)" else "Mode: ACTUAL LOCKSCREEN"
            textSize = 16f
            setTextColor(if (isPreview) Color.GREEN else Color.YELLOW)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 16)
        })
        
        // App info
        layout.addView(TextView(this).apply {
            text = """
                Debug Information:
                • Android Version: ${android.os.Build.VERSION.RELEASE}
                • Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                • App Version: 1.0.0
                
                This debug interface appears because the main chess interface failed to load.
                This helps us identify what's causing crashes.
            """.trimIndent()
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, 24)
        })
        
        // Test button
        layout.addView(Button(this).apply {
            text = "Test Basic Functionality"
            setOnClickListener {
                Toast.makeText(this@DebugLockActivity, "✅ Basic functionality works!", Toast.LENGTH_SHORT).show()
            }
            setPadding(16, 16, 16, 16)
        })
        
        // Exit button
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
    
    private fun createUltimateFallback() {
        val button = Button(this).apply {
            text = "EMERGENCY EXIT"
            setOnClickListener { finish() }
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
        }
        setContentView(button)
    }
}
