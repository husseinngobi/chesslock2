package com.ngobi.chesslock

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

class LockAdminReceiver : DeviceAdminReceiver() {
    
    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, LockAdminReceiver::class.java)
        }
        
        fun isAdminActive(context: Context): Boolean {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            return devicePolicyManager.isAdminActive(getComponentName(context))
        }
        
        fun lockDevice(context: Context): Boolean {
            if (!isAdminActive(context)) return false
            
            try {
                val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                devicePolicyManager.lockNow()
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
        
        fun requestAdminPermission(context: Context) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getComponentName(context))
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                    "ChessLock needs device administrator permission to lock your screen securely when you activate chess lock mode.")
            }
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        
        Toast.makeText(
            context, 
            "🔐 ChessLock security enabled! Your device can now be locked with chess puzzles.", 
            Toast.LENGTH_LONG
        ).show()
        
        // Save admin status
        val prefs = context.getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("admin_enabled", true)
            .putLong("admin_enabled_time", System.currentTimeMillis())
            .apply()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        
        Toast.makeText(
            context, 
            "⚠️ ChessLock security disabled. Chess lock features may not work properly.", 
            Toast.LENGTH_LONG
        ).show()
        
        // Update admin status
        val prefs = context.getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("admin_enabled", false)
            .putLong("admin_disabled_time", System.currentTimeMillis())
            .apply()
    }
    
    override fun onPasswordChanged(context: Context, intent: Intent, user: android.os.UserHandle) {
        super.onPasswordChanged(context, intent, user)
        
        // Notify user that password change might affect chess lock
        Toast.makeText(
            context,
            "🔑 Device password changed. ChessLock emergency unlock updated.",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    override fun onPasswordFailed(context: Context, intent: Intent, user: android.os.UserHandle) {
        super.onPasswordFailed(context, intent, user)
        
        // Log failed password attempts
        val prefs = context.getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
        val currentFailures = prefs.getInt("password_failures", 0)
        prefs.edit()
            .putInt("password_failures", currentFailures + 1)
            .putLong("last_password_failure", System.currentTimeMillis())
            .apply()
    }
    
    override fun onPasswordSucceeded(context: Context, intent: Intent, user: android.os.UserHandle) {
        super.onPasswordSucceeded(context, intent, user)
        
        // Reset password failure count on successful authentication
        val prefs = context.getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("password_failures", 0)
            .putLong("last_password_success", System.currentTimeMillis())
            .apply()
    }
}