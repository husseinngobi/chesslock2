package com.ngobi.chesslock

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment

/**
 * Enhanced setup dialog with step-by-step guidance for ChessLock permissions
 * Provides clear instructions for different Android versions and manufacturers
 */
class SetupGuideDialog : DialogFragment() {

    companion object {
        fun newInstance(): SetupGuideDialog {
            return SetupGuideDialog()
        }
    }

    private lateinit var strategy: LockStrategyManager.LockStrategy
    private var currentStep = 0
    private val maxSteps = 2

    private lateinit var stepIndicator: TextView
    private lateinit var stepTitle: TextView
    private lateinit var stepDescription: TextView
    private lateinit var stepIcon: ImageView
    private lateinit var nextButton: Button
    private lateinit var skipButton: Button
    private lateinit var backButton: Button

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_setup_guide, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        strategy = LockStrategyManager.getBestLockStrategy(requireContext())
        setupClickListeners()
        showCurrentStep()
    }

    private fun initializeViews(view: View) {
        stepIndicator = view.findViewById(R.id.stepIndicator)
        stepTitle = view.findViewById(R.id.stepTitle)
        stepDescription = view.findViewById(R.id.stepDescription)
        stepIcon = view.findViewById(R.id.stepIcon)
        nextButton = view.findViewById(R.id.nextButton)
        skipButton = view.findViewById(R.id.skipButton)
        backButton = view.findViewById(R.id.backButton)
    }

    private fun setupClickListeners() {
        nextButton.setOnClickListener { handleNextStep() }
        skipButton.setOnClickListener { handleSkip() }
        backButton.setOnClickListener { handleBack() }
        
        view?.findViewById<Button>(R.id.closeButton)?.setOnClickListener {
            dismiss()
        }
    }

    private fun showCurrentStep() {
        stepIndicator.text = "Step ${currentStep + 1} of $maxSteps"
        
        when (currentStep) {
            0 -> showOverlayPermissionStep()
            1 -> showStrategyPermissionStep()
        }
        
        // Update button states
        backButton.isEnabled = currentStep > 0
        backButton.alpha = if (currentStep > 0) 1.0f else 0.5f
        
        when {
            currentStep == maxSteps - 1 -> {
                nextButton.text = "Complete Setup"
            }
            else -> {
                nextButton.text = "Next"
            }
        }
    }

    private fun showOverlayPermissionStep() {
        stepTitle.text = "🔥 Overlay Permission"
        stepDescription.text = """
            ChessLock needs permission to display over other apps.
            
            This allows the chess puzzle to appear on top of your lock screen.
            
            ✅ What happens next:
            • Android Settings will open
            • Find "ChessLock" in the list
            • Toggle "Allow display over other apps"
            • Return to this setup
        """.trimIndent()
        
        stepIcon.setImageResource(R.drawable.ic_chess_queen)
        stepIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.difficulty_easy))
        
        nextButton.text = if (Settings.canDrawOverlays(requireContext())) "✅ Already Granted - Next" else "Grant Permission"
    }

    private fun showStrategyPermissionStep() {
        when (strategy) {
            LockStrategyManager.LockStrategy.ACCESSIBILITY_SERVICE -> showAccessibilityStep()
            LockStrategyManager.LockStrategy.DEVICE_ADMIN -> showDeviceAdminStep()
            LockStrategyManager.LockStrategy.HUAWEI_EMUI -> showHuaweiStep()
            LockStrategyManager.LockStrategy.OVERLAY_ONLY -> showOverlayOnlyStep()
        }
    }

    private fun showAccessibilityStep() {
        stepTitle.text = "🛡️ Accessibility Service (Android 9+)"
        stepDescription.text = """
            Enable ChessLock Accessibility Service for the best experience.
            
            🎯 Benefits:
            • Keeps fingerprint unlock working
            • More reliable lock activation
            • Better system integration
            
            📱 Setup steps:
            • Android Settings > Accessibility will open
            • Find "ChessLock" in the Downloaded apps section
            • Toggle it ON
            • Confirm the permission dialog
        """.trimIndent()
        
        stepIcon.setImageResource(R.drawable.ic_chess_queen)
        stepIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.difficulty_medium))
        
        nextButton.text = if (LockStrategyManager.isAccessibilityServiceEnabled(requireContext())) 
            "✅ Already Enabled - Complete" else "Enable Accessibility Service"
    }

    private fun showDeviceAdminStep() {
        stepTitle.text = "🔐 Device Administrator"
        stepDescription.text = """
            Grant Device Administrator permission for secure locking.
            
            📋 What this enables:
            • Secure system-level screen locking
            • Integration with Android security
            • Reliable lock activation
            
            ⚙️ Setup steps:
            • Device Admin settings will open
            • Find "ChessLock" in the list
            • Check the box to activate
            • Tap "Activate"
            
            Note: On newer devices, fingerprint unlock may not work with this method.
        """.trimIndent()
        
        stepIcon.setImageResource(R.drawable.ic_chess_queen)
        stepIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.difficulty_hard))
        
        nextButton.text = if (LockStrategyManager.isDeviceAdminEnabled(requireContext())) 
            "✅ Already Granted - Complete" else "Grant Device Admin"
    }

    private fun showHuaweiStep() {
        stepTitle.text = "📱 Huawei EMUI Ready"
        stepDescription.text = """
            Great! Your Huawei device supports direct lock activation.
            
            🎉 What's special:
            • No additional permissions needed
            • Uses Huawei's built-in lock system
            • Maintains fingerprint unlock
            • Optimized for EMUI
            
            Your ChessLock setup is almost complete!
        """.trimIndent()
        
        stepIcon.setImageResource(R.drawable.ic_chess_queen)
        stepIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.difficulty_easy))
        
        nextButton.text = "✅ Setup Complete"
    }

    private fun showOverlayOnlyStep() {
        stepTitle.text = "📱 Overlay Mode"
        stepDescription.text = """
            ChessLock will use overlay-only mode.
            
            ℹ️ What this means:
            • Chess puzzle appears over existing lock screen
            • Limited system integration
            • Still provides chess-based security
            
            💡 Tip: For better functionality, consider updating to a newer Android version or check if your device supports accessibility services.
        """.trimIndent()
        
        stepIcon.setImageResource(R.drawable.ic_chess_queen)
        stepIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.difficulty_medium))
        
        nextButton.text = "✅ Setup Complete"
    }

    private fun handleNextStep() {
        when (currentStep) {
            0 -> {
                if (!Settings.canDrawOverlays(requireContext())) {
                    requestOverlayPermission()
                } else {
                    currentStep++
                    showCurrentStep()
                }
            }
            1 -> {
                when (strategy) {
                    LockStrategyManager.LockStrategy.ACCESSIBILITY_SERVICE -> {
                        if (!LockStrategyManager.isAccessibilityServiceEnabled(requireContext())) {
                            LockStrategyManager.requestAccessibilityServicePermission(requireContext())
                        } else {
                            completeSetup()
                        }
                    }
                    LockStrategyManager.LockStrategy.DEVICE_ADMIN -> {
                        if (!LockStrategyManager.isDeviceAdminEnabled(requireContext())) {
                            LockStrategyManager.requestDeviceAdminPermission(requireContext())
                        } else {
                            completeSetup()
                        }
                    }
                    else -> {
                        completeSetup()
                    }
                }
            }
        }
    }

    private fun handleBack() {
        if (currentStep > 0) {
            currentStep--
            showCurrentStep()
        }
    }

    private fun handleSkip() {
        if (currentStep < maxSteps - 1) {
            currentStep++
            showCurrentStep()
        } else {
            dismiss()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:${requireContext().packageName}")
        }
        startActivity(intent)
    }

    private fun completeSetup() {
        // Refresh main activity status
        (activity as? MainActivity)?.runOnUiThread {
            (activity as? MainActivity)?.updateStatusDisplay()
        }
        dismiss()
    }

    override fun onResume() {
        super.onResume()
        
        // Check if permissions were granted while dialog was in background
        val overlayGranted = Settings.canDrawOverlays(requireContext())
        val strategyGranted = when (strategy) {
            LockStrategyManager.LockStrategy.ACCESSIBILITY_SERVICE -> 
                LockStrategyManager.isAccessibilityServiceEnabled(requireContext())
            LockStrategyManager.LockStrategy.DEVICE_ADMIN -> 
                LockStrategyManager.isDeviceAdminEnabled(requireContext())
            else -> true
        }
        
        // Auto-advance if current step permission was granted
        when (currentStep) {
            0 -> {
                if (overlayGranted) {
                    currentStep++
                    showCurrentStep()
                }
            }
            1 -> {
                if (strategyGranted) {
                    completeSetup()
                }
            }
        }
    }
}