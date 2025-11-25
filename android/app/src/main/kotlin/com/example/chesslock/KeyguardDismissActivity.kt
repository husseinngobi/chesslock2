package com.example.chesslock

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager

/**
 * Transparent helper Activity to dismiss the keyguard safely
 * and notify the AccessibilityService afterward.
 */
class KeyguardDismissActivity : Activity() {

    companion object {
        const val ACTION_DISMISS_KEYGUARD = "com.example.chesslock.DISMISS_KEYGUARD"

        fun launch(context: Context) {
            val intent = Intent(context, KeyguardDismissActivity::class.java).apply {
                action = ACTION_DISMISS_KEYGUARD
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No lockscreen flags — this is critical
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {

                    override fun onDismissSucceeded() {postNotifyAndFinish()
                    }

                    override fun onDismissError() {
                        android.util.Log.e("ChessLock", "❌ Keyguard dismiss error")
                        postNotifyAndFinish()
                    }

                    override fun onDismissCancelled() {
                        android.util.Log.w("ChessLock", "⚠️ Keyguard dismiss cancelled")
                        postNotifyAndFinish()
                    }
                }
            )
        } else {
            postNotifyAndFinish()
        }
    }

    private fun postNotifyAndFinish() {
        // Wait for system to fully exit keyguard mode
        Handler(Looper.getMainLooper()).postDelayed({

            val intent = Intent(ChessLockAccessibilityService.ACTION_DISPLAY_OVERLAY).apply {
                setPackage(packageName)
            }

            sendBroadcast(intent)
            finish()

        }, 120) // 120ms is optimal for Android 10–14
    }
}
