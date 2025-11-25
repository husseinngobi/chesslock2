package com.ngobi.chesslock

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.random.Random

class LockScreenService : Service() {
    
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ChessLockChannel"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        showLockScreen()
        return START_STICKY // Restart if killed
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ChessLock Active",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "ChessLock is actively protecting your device"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ChessLock Active")
            .setContentText("Device is protected with chess puzzles")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    private fun showLockScreen() {
        try {
            // Create overlay view with proper container
            val layoutInflater = LayoutInflater.from(this)
            val container = android.widget.FrameLayout(this)
            overlayView = layoutInflater.inflate(R.layout.activity_lock_screen_responsive, container, false)
            
            // Setup window parameters for system overlay
            val params = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                
                // Modern Android 14 compatible flags for overlay
                flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }
            
            // Add view to window manager
            windowManager?.addView(overlayView, params)
            
            // Setup the chess puzzle interface
            setupChessInterface()
            
        } catch (e: Exception) {
            android.util.Log.e("LockScreenService", "Failed to show lockscreen: ${e.message}")
            stopSelf()
        }
    }
    
    private fun setupChessInterface() {
        overlayView?.let { view ->
            // Find views and setup chess puzzle
            val boardView = view.findViewById<ChessBoardView>(R.id.chessBoard)
            val promptView = view.findViewById<TextView>(R.id.puzzlePrompt)
            val emergencyButton = view.findViewById<android.widget.Button>(R.id.emergencyUnlockButton)
            
            // Load puzzle
            val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
            val difficulty = prefs.getString("difficulty", "medium") ?: "medium"
            val playerColor = prefs.getString("player_color", "white") ?: "white"
            
            // Handle random color selection
            val actualPlayerColor = if (playerColor == "random") {
                if (Random.nextBoolean()) "white" else "black"
            } else {
                playerColor
            }
            
            val puzzle = PuzzleManager.getPuzzleForColor(difficulty, actualPlayerColor, "")
            
            promptView?.text = puzzle.prompt
            boardView?.apply {
                // Update player color (AI automatically plays opposite)
                updatePlayerColor(actualPlayerColor == "white")
                setFEN(puzzle.fen, puzzle.solutionFEN)
                updateTheme()
                
                onPuzzleSolved = {
                    // Puzzle solved - remove lock
                    hideLockScreen()
                    stopSelf()
                }
            }
            
            // Emergency unlock
            emergencyButton?.setOnClickListener {
                hideLockScreen()
                stopSelf()
            }
        }
    }
    
    private fun hideLockScreen() {
        try {
            overlayView?.let { view ->
                windowManager?.removeView(view)
                overlayView = null
            }
        } catch (e: Exception) {
            android.util.Log.e("LockScreenService", "Failed to hide lockscreen: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        hideLockScreen()
        super.onDestroy()
    }
}
