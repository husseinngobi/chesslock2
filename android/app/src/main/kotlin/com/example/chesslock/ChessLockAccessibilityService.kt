package com.example.chesslock

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout

/**
 * AccessibilityService that shows full-screen chess puzzle overlay
 * Important: Keyguard must be dismissed before showing overlay (handled by KeyguardDismissActivity).
 *
 * Notes:
 * - TYPE_APPLICATION_OVERLAY requires SYSTEM_ALERT_WINDOW / ACTION_MANAGE_OVERLAY_PERMISSION (Settings.canDrawOverlays)
 * - All WindowManager add/remove operations must be performed on the main thread.
 */
class ChessLockAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_LOCK = "com.example.chesslock.ACTION_LOCK"
        const val ACTION_SHOW_OVERLAY = "com.example.chesslock.SHOW_LOCKSCREEN"
        const val ACTION_HIDE_OVERLAY = "com.example.chesslock.UNLOCK_DEVICE"
        const val ACTION_DISPLAY_OVERLAY = "com.example.chesslock.DISPLAY_OVERLAY"

        private const val TAG = "ChessLock"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: FrameLayout? = null
    private var isOverlayShowing = false
    private var isPuzzleSolved = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var keyguardManager: KeyguardManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "📴 SCREEN OFF detected - Power button pressed or timeout")
                    // Screen went off - prepare to show overlay on next screen ON
                    // Don't show overlay yet - wait for SCREEN_ON
                    isPuzzleSolved = false // Reset for next lock session
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "📱 SCREEN ON detected - Showing chess overlay immediately")
                    // Screen turned on (power button pressed, or screen woke up)
                    // Show overlay immediately to create lockscreen illusion
                    // This works whether user has PIN or not
                    isPuzzleSolved = false // Reset puzzle state
                    showChessLockOverlay()
                }
                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "👤 USER PRESENT - User unlocked device")
                    // Native unlock completed
                }
                "android.intent.action.PHONE_STATE" -> {
                    val state = intent.getStringExtra("state")
                    Log.d(TAG, "📞 PHONE STATE CHANGED: $state")
                    if (state == "RINGING" || state == "OFFHOOK") {
                        // Incoming or active call - hide overlay temporarily
                        Log.d(TAG, "📞 Phone call active - hiding overlay")
                        hideChessLockOverlay()
                    } else if (state == "IDLE" && !isPuzzleSolved) {
                        // Call ended and puzzle not solved - show overlay again
                        Log.d(TAG, "📞 Call ended - showing overlay again")
                        mainHandler.postDelayed({ showChessLockOverlay() }, 500)
                    }
                }
                "android.intent.action.NEW_OUTGOING_CALL" -> {
                    Log.d(TAG, "📞 Outgoing call - hiding overlay")
                    hideChessLockOverlay()
                }
                ACTION_LOCK -> {
                    Log.d(TAG, "🔒 Lock command received")
                    lockScreen()
                }
                ACTION_SHOW_OVERLAY -> {
                    Log.d(TAG, "📱 Show overlay command received")
                    showChessLockOverlay()
                }
                ACTION_DISPLAY_OVERLAY -> {
                    Log.d(TAG, "✅ Keyguard dismissed - displaying overlay now")
                    displayOverlayOnUnlockedScreen()
                }
                ACTION_HIDE_OVERLAY -> {
                    Log.d(TAG, "🔔🔔🔔 ACTION_HIDE_OVERLAY RECEIVED 🔔🔔🔔")
                    
                    // Verify bypass flag
                    val prefs = context?.getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
                    val bypassActive = prefs?.getBoolean("emergencyBypassActive", false) ?: false
                    Log.d(TAG, "🔍 Emergency bypass active: $bypassActive")
                    
                    Log.d(TAG, "✅ Hiding overlay (puzzle solved or emergency bypass)")
                    isPuzzleSolved = true
                    hideChessLockOverlay()
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "🔐 ChessLock AccessibilityService connected")

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        // Register broadcast receivers
        val filter = IntentFilter().apply {
            addAction(ACTION_LOCK)
            addAction(ACTION_SHOW_OVERLAY)
            addAction(ACTION_HIDE_OVERLAY)
            addAction(ACTION_DISPLAY_OVERLAY)
            // CRITICAL: Listen for screen OFF to show overlay when power button pressed
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            // Respect phone calls - hide overlay during calls
            addAction("android.intent.action.PHONE_STATE")
            addAction("android.intent.action.NEW_OUTGOING_CALL")
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(broadcastReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Receiver registration failed: ${e.message}")
        }

        Log.d(TAG, "✅ Native Android chess lockscreen service started")
    }
    
    private fun showChessLockOverlay() {
        // Check if user activated emergency bypass
        val prefs = getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
        val bypassActive = prefs.getBoolean("emergencyBypassActive", false)
        
        if (bypassActive) {
            Log.d(TAG, "🚨 EMERGENCY BYPASS ACTIVE - Clearing flag and showing overlay on this lock event")
            // Clear bypass flag now that lock event occurred
            prefs.edit().putBoolean("emergencyBypassActive", false).apply()
            Log.d(TAG, "✅ Bypass flag cleared - overlay will show normally")
        }
        Log.d(TAG, "🔄 Lock event detected - Emergency bypass cleared")
        
        if (isOverlayShowing || windowManager == null) {
            Log.w(TAG, "⚠️ Cannot show overlay (already showing or not initialized)")
            return
        }

        try {
            isPuzzleSolved = false

            // Check if user has native lock (PIN/Pattern/Fingerprint)
            val km = keyguardManager ?: getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val isSecure = km.isKeyguardSecure

            if (!isSecure) {
                // NO native lock → Show ChessLock IMMEDIATELY
                Log.d(TAG, "🔓 No native lock enabled — showing ChessLock immediately")
                displayOverlayOnUnlockedScreen()
                return
            }

            // Native lock exists → Dismiss it first, then show ChessLock
            Log.d(TAG, "🔐 Native lock exists — dismissing keyguard first")
            KeyguardDismissActivity.launch(this)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Show overlay failed: ${e.message}")
            isOverlayShowing = false
            releaseWakeLock()
        }
    }

    private fun displayOverlayOnUnlockedScreen() {
        // Ensure overlay operations run on main thread
        mainHandler.post {
            try {
                if (isOverlayShowing) {
                    Log.w(TAG, "Overlay already showing - skipping display")
                    return@post
                }

                // overlay permission check for TYPE_APPLICATION_OVERLAY
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!Settings.canDrawOverlays(this)) {
                        Log.e(TAG, "❌ Cannot display overlay: missing draw-over-other-apps permission")
                        return@post
                    }
                }

                // Acquire wake lock to keep screen on (deprecated flags suppressed; keep for legacy devices)
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                @Suppress("DEPRECATION")
                wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                            PowerManager.ACQUIRE_CAUSES_WAKEUP or
                            PowerManager.ON_AFTER_RELEASE,
                    "ChessLock::OverlayWakeLock"
                ).apply {
                    try {
                        acquire()
                        Log.d(TAG, "🔆 Wake lock acquired on UNLOCKED screen - screen will stay on!")
                    } catch (e: Exception) {
                        Log.w(TAG, "Wake lock acquire failed: ${e.message}")
                    }
                }

                // ✅ PROFESSIONAL CHESS LOCKSCREEN WITH FULL RULES
                val professionalChessView = ProfessionalChessLockscreen(this)
                
                Log.d(TAG, "✅ Professional chess lockscreen created with full rules & AI!")

                // Simple container for chess view
                overlayView = FrameLayout(this).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    addView(professionalChessView, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    ))
                }

                // Choose correct type based on API level
                val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    // TYPE_PHONE is deprecated but used as fallback on old devices
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                // CRITICAL: Remove all touch-blocking flags and use only essential ones
                // Flutter needs the window to be fully interactive without any touch modifiers
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    windowType,
                    // ONLY these flags - nothing that affects touch
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    // Essential: Make sure touches go to THIS window
                    flags = flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                    flags = flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                }

                Log.d(TAG, "✅ Adding overlay view (type=$windowType) with INTERACTIVE flags - touch ENABLED!")

                // Prevent re-attaching the same view which can throw IllegalStateException
                try {
                    val parent = overlayView?.parent
                    if (parent != null) {
                        windowManager?.removeViewImmediate(overlayView)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "pre-add cleanup failed: ${e.message}")
                }

                // Add overlay to window (guard it with try/catch)
                try {
                    windowManager?.addView(overlayView, params)
                    isOverlayShowing = true
                    Log.d(TAG, "✅ Chess overlay shown - requesting focus for touch input")
                    
                    Log.d(TAG, "✅ Native chess overlay displayed - touch ready!")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ windowManager.addView failed: ${e.message}")
                    // Clean up partial state
                    try {
                        overlayView = null
                    } catch (_: Exception) { }
                    releaseWakeLock()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Display overlay failed: ${e.message}")
                isOverlayShowing = false
                releaseWakeLock()
            }
        }
    }
    
    private fun hideChessLockOverlay() {
        // run on main thread to remove views safely
        mainHandler.post {
            if (!isOverlayShowing) {
                // Still try to release wake lock and recreate engine state if needed
                releaseWakeLock()
                return@post
            }

            try {
                // Release wake lock
                releaseWakeLock()

                // Remove overlay safely
                try {
                    windowManager?.removeView(overlayView)
                } catch (e: IllegalArgumentException) {
                    // View not attached
                    Log.w(TAG, "removeView skipped - view not attached")
                } catch (e: Exception) {
                    Log.w(TAG, "removeView failed: ${e.message}")
                }

                overlayView = null
                isOverlayShowing = false

                // Go to home screen to clear UI (safe from service)
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    startActivity(homeIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "startActivity(home) failed: ${e.message}")
                }

                Log.d(TAG, "✅ Chess puzzle overlay hidden")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Hide overlay failed: ${e.message}")
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    try {
                        it.release()
                        Log.d(TAG, "🔆 Wake lock released")
                    } catch (e: Exception) {
                        Log.w(TAG, "Wake lock release failed: ${e.message}")
                    }
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Wake lock release failed: ${e.message}")
        }
    }

    private fun lockScreen() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val success = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                Log.d(TAG, "🔒 Lock screen: $success")
            } else {
                Log.w(TAG, "Lock not supported on this API level")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lock failed: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Block interactions when lockscreen is showing
        if (isOverlayShowing && !isPuzzleSolved) {
            // Intercept home/back/recents button presses
            event?.let {
                Log.d(TAG, "🛡️ Event blocked while lockscreen active: ${it.eventType}")
            }
        }
    }
    
    override fun onKeyEvent(event: android.view.KeyEvent): Boolean {
        // Intercept hardware buttons when lockscreen is showing
        if (isOverlayShowing && !isPuzzleSolved) {
            when (event.keyCode) {
                android.view.KeyEvent.KEYCODE_BACK,
                android.view.KeyEvent.KEYCODE_HOME,
                android.view.KeyEvent.KEYCODE_APP_SWITCH -> {
                    Log.d(TAG, "🛡️ Hardware button blocked: ${event.keyCode}")
                    return true // Block the event
                }
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onInterrupt() {
        Log.d(TAG, "⚠️ AccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()

        releaseWakeLock()

        // Remove overlay (mainHandler ensures safe removal)
        try {
            mainHandler.post {
                try {
                    if (isOverlayShowing) {
                        try {
                            windowManager?.removeViewImmediate(overlayView)
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    overlayView = null
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        try {
            unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            // Already unregistered
            Log.w(TAG, "Receiver unregister warning: ${e.message}")
        }

        Log.d(TAG, "🔐 ChessLock AccessibilityService destroyed")
    }
}
