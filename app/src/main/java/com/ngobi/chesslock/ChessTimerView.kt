package com.ngobi.chesslock

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import kotlin.math.*

/**
 * Professional Chess Timer Widget for ChessLock
 * Features:
 * - Starts counting when user makes first move against AI
 * - Visual countdown with time pressure indicators
 * - Responsive design for all screen sizes
 * - Smooth animations and professional appearance
 */
class ChessTimerView @JvmOverloads constructor(
    context: Context, 
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Timer state
    private var startTime = 0L
    private var isRunning = false
    private var currentTime = 0L
    private var maxTime = 300000L // 5 minutes default
    
    // Visual components
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Animation
    private val pulseAnimator = ValueAnimator.ofFloat(1.0f, 1.1f).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
    }
    
    // Callbacks
    var onTimerStart: (() -> Unit)? = null
    var onTimerUpdate: ((timeInSeconds: Long) -> Unit)? = null
    var onTimeWarning: ((remainingSeconds: Long) -> Unit)? = null
    
    // Colors
    private val normalColor = "#4CAF50".toColorInt()
    private val warningColor = "#FF9800".toColorInt()
    private val criticalColor = "#F44336".toColorInt()
    private val backgroundColor = "#1E1E1E".toColorInt()
    
    init {
        setupPaints()
        setupAnimations()
    }
    
    private fun setupPaints() {
        // Timer text
        textPaint.apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 48f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        
        // Background
        bgPaint.apply {
            color = backgroundColor
            style = Paint.Style.FILL
        }
        
        // Progress arc
        progressPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 8f
            strokeCap = Paint.Cap.ROUND
        }
    }
    
    private fun setupAnimations() {
        pulseAnimator.addUpdateListener { animator ->
            val scale = animator.animatedValue as Float
            scaleX = scale
            scaleY = scale
        }
    }
    
    /**
     * Start the timer when user makes first chess move
     */
    fun startTimer() {
        if (!isRunning) {
            startTime = System.currentTimeMillis()
            isRunning = true
            onTimerStart?.invoke()
            updateTimer()
            Log.d("ChessTimer", "⏱️ Timer started!")
        }
    }
    
    /**
     * Stop the timer and return elapsed time in seconds
     */
    fun stopTimer(): Long {
        if (isRunning) {
            isRunning = false
            val elapsed = (System.currentTimeMillis() - startTime) / 1000
            pulseAnimator.cancel()
            scaleX = 1.0f
            scaleY = 1.0f
            Log.d("ChessTimer", "⏱️ Timer stopped - Elapsed: ${elapsed}s")
            return elapsed
        }
        return 0L
    }
    
    /**
     * Reset timer to initial state
     */
    fun resetTimer() {
        isRunning = false
        startTime = 0L
        currentTime = 0L
        pulseAnimator.cancel()
        scaleX = 1.0f
        scaleY = 1.0f
        invalidate()
    }
    
    /**
     * Set maximum time for countdown mode
     */
    fun setMaxTime(timeInMillis: Long) {
        maxTime = timeInMillis
    }
    
    private fun updateTimer() {
        if (isRunning) {
            currentTime = System.currentTimeMillis() - startTime
            
            // Check for time warnings
            val remainingMs = maxTime - currentTime
            val remainingSeconds = remainingMs / 1000
            
            when {
                remainingSeconds <= 10 && remainingSeconds > 0 -> {
                    if (!pulseAnimator.isRunning) {
                        pulseAnimator.start()
                    }
                    onTimeWarning?.invoke(remainingSeconds)
                }
                remainingSeconds <= 0 -> {
                    // Time's up!
                    stopTimer()
                    return
                }
            }
            
            onTimerUpdate?.invoke(currentTime / 1000)
            invalidate()
            
            // Schedule next update
            Handler(Looper.getMainLooper()).postDelayed({ updateTimer() }, 100)
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 3f
        
        // Draw background circle
        bgPaint.alpha = 100
        canvas.drawCircle(centerX, centerY, radius, bgPaint)
        
        // Draw progress arc if timer is running
        if (isRunning && maxTime > 0) {
            val progress = currentTime.toFloat() / maxTime.toFloat()
            val angle = 360f * progress
            
            // Choose color based on time remaining
            val remainingMs = maxTime - currentTime
            progressPaint.color = when {
                remainingMs <= 10000 -> criticalColor  // Last 10 seconds
                remainingMs <= 30000 -> warningColor   // Last 30 seconds
                else -> normalColor
            }
            
            val rect = RectF(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
            )
            
            canvas.drawArc(rect, -90f, angle, false, progressPaint)
        }
        
        // Draw timer text
        val timeText = formatTime(currentTime)
        
        // Adjust text size based on view size
        textPaint.textSize = min(width, height) * 0.15f
        
        // Choose text color based on time state
        textPaint.color = when {
            !isRunning -> Color.WHITE
            currentTime > maxTime -> criticalColor
            maxTime - currentTime <= 10000 -> criticalColor
            maxTime - currentTime <= 30000 -> warningColor
            else -> normalColor
        }
        
        canvas.drawText(timeText, centerX, centerY + textPaint.textSize / 3f, textPaint)
        
        // Draw status indicator
        val statusText = when {
            !isRunning && currentTime == 0L -> "Ready"
            !isRunning && currentTime > 0L -> "Finished"
            isRunning -> "Playing"
            else -> ""
        }
        
        if (statusText.isNotEmpty()) {
            textPaint.textSize = min(width, height) * 0.08f
            textPaint.color = Color.GRAY
            canvas.drawText(statusText, centerX, centerY + radius + textPaint.textSize * 2f, textPaint)
        }
    }
    
    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = (150 * resources.displayMetrics.density).toInt()
        
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        
        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredSize, widthSize)
            else -> desiredSize
        }
        
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredSize, heightSize)
            else -> desiredSize
        }
        
        // Make it square
        val size = min(width, height)
        setMeasuredDimension(size, size)
    }
}