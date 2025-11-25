package com.ngobi.chesslock

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager

/**
 * Platform detection and lock strategy manager for ChessLock
 * Implements intelligent platform-specific locking based on Android version and manufacturer
 */
object LockStrategyManager {
    private const val TAG = "LockStrategyManager"
    
    // Huawei EMUI specific components (for older Huawei devices)
    private const val EMUI_LOCK_PKG = "com.android.systemui"
    private const val EMUI_LOCK_CLS = "com.huawei.keyguard.onekeylock.OneKeyLockActivity"
    
    enum class LockStrategy {
        ACCESSIBILITY_SERVICE,  // Android 9+ - maintains fingerprint
        DEVICE_ADMIN,          // Older Android versions
        HUAWEI_EMUI,          // Special Huawei handling
        OVERLAY_ONLY          // Fallback to overlay only
    }
    
    /**
     * Determine the best lock strategy for the current device
     */
    fun getBestLockStrategy(context: Context): LockStrategy {
        Log.d(TAG, "Determining lock strategy for device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.SDK_INT}")
        
        return when {
            // Android 9+ devices - prefer accessibility service for fingerprint support
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                Log.d(TAG, "Android 9+ detected, using Accessibility Service strategy")
                LockStrategy.ACCESSIBILITY_SERVICE
            }
            
            // Special handling for Huawei Nougat
            isHuaweiNougat() -> {
                Log.d(TAG, "Huawei Nougat detected, using EMUI strategy")
                LockStrategy.HUAWEI_EMUI
            }
            
            // Older Android versions - use device admin
            else -> {
                Log.d(TAG, "Older Android version detected, using Device Admin strategy")
                LockStrategy.DEVICE_ADMIN
            }
        }
    }
    
    /**
     * Execute the appropriate lock method based on strategy
     */
    fun executeLockStrategy(context: Context, strategy: LockStrategy): Boolean {
        return when (strategy) {
            LockStrategy.ACCESSIBILITY_SERVICE -> lockViaAccessibilityService(context)
            LockStrategy.DEVICE_ADMIN -> lockViaDeviceAdmin(context)
            LockStrategy.HUAWEI_EMUI -> lockViaHuaweiEmui(context)
            LockStrategy.OVERLAY_ONLY -> lockViaOverlayOnly(context)
        }
    }
    
    /**
     * Check if current device is Huawei running Nougat
     */
    private fun isHuaweiNougat(): Boolean {
        return Build.MANUFACTURER.equals("huawei", ignoreCase = true) &&
               Build.VERSION.SDK_INT == Build.VERSION_CODES.N
    }
    
    /**
     * Lock via Accessibility Service (Android 9+)
     */
    private fun lockViaAccessibilityService(context: Context): Boolean {
        return try {
            if (isAccessibilityServiceEnabled(context)) {
                val intent = Intent(ChessLockAccessibilityService.ACTION_LOCK).apply {
                    setClass(context, ChessLockAccessibilityService::class.java)
                }
                context.startService(intent)
                Log.d(TAG, "Accessibility service lock initiated")
                true
            } else {
                Log.w(TAG, "Accessibility service not enabled, requesting permission")
                requestAccessibilityServicePermission(context)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lock via accessibility service", e)
            false
        }
    }
    
    /**
     * Lock via Device Admin
     */
    private fun lockViaDeviceAdmin(context: Context): Boolean {
        return try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, LockAdminReceiver::class.java)
            
            if (devicePolicyManager.isAdminActive(adminComponent)) {
                devicePolicyManager.lockNow()
                // Launch chess overlay after locking
                launchChessOverlay(context)
                Log.d(TAG, "Device admin lock executed")
                true
            } else {
                Log.w(TAG, "Device admin not active, requesting permission")
                requestDeviceAdminPermission(context)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lock via device admin", e)
            false
        }
    }
    
    /**
     * Lock via Huawei EMUI system
     */
    private fun lockViaHuaweiEmui(context: Context): Boolean {
        return try {
            val intent = Intent().apply {
                setClassName(EMUI_LOCK_PKG, EMUI_LOCK_CLS)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            // Launch chess overlay after EMUI lock
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                launchChessOverlay(context)
            }, 300)
            Log.d(TAG, "Huawei EMUI lock executed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lock via Huawei EMUI", e)
            // Fallback to overlay only
            lockViaOverlayOnly(context)
        }
    }
    
    /**
     * Fallback: launch chess overlay only (no system lock)
     */
    private fun lockViaOverlayOnly(context: Context): Boolean {
        return try {
            launchChessOverlay(context)
            Log.d(TAG, "Overlay-only lock executed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch overlay", e)
            false
        }
    }
    
    /**
     * Launch the chess game overlay
     */
    private fun launchChessOverlay(context: Context) {
        val chessIntent = Intent(context, OverlayLockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or 
                     Intent.FLAG_ACTIVITY_CLEAR_TOP or
                     Intent.FLAG_ACTIVITY_SINGLE_TOP or
                     Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        context.startActivity(chessIntent)
    }
    
    /**
     * Check if accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        
        return enabledServices.any { serviceInfo ->
            val serviceInfo = serviceInfo.resolveInfo.serviceInfo
            serviceInfo.packageName == context.packageName && 
            serviceInfo.name == ChessLockAccessibilityService::class.java.name
        }
    }
    
    /**
     * Check if device admin is enabled
     */
    fun isDeviceAdminEnabled(context: Context): Boolean {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, LockAdminReceiver::class.java)
        return devicePolicyManager.isAdminActive(adminComponent)
    }
    
    /**
     * Request accessibility service permission
     */
    fun requestAccessibilityServicePermission(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * Request device admin permission
     */
    fun requestDeviceAdminPermission(context: Context) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            val adminComponent = ComponentName(context, LockAdminReceiver::class.java)
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                context.getString(R.string.admin_permission_desc))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * Get setup status message for current strategy
     */
    fun getSetupStatusMessage(context: Context): String {
        val strategy = getBestLockStrategy(context)
        
        return when (strategy) {
            LockStrategy.ACCESSIBILITY_SERVICE -> {
                if (isAccessibilityServiceEnabled(context)) {
                    context.getString(R.string.status_ready)
                } else {
                    "Accessibility Service required for Android 9+ compatibility"
                }
            }
            LockStrategy.DEVICE_ADMIN -> {
                if (isDeviceAdminEnabled(context)) {
                    context.getString(R.string.status_ready)
                } else {
                    "Device Administrator permission required"
                }
            }
            LockStrategy.HUAWEI_EMUI -> {
                "Ready for Huawei EMUI lock"
            }
            LockStrategy.OVERLAY_ONLY -> {
                "Overlay-only mode (limited functionality)"
            }
        }
    }
}