package com.example.chesslock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefs = getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
        
        // Create scrollable layout
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#1E1E1E"))
        }
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        
        // Header
        layout.addView(TextView(this).apply {
            text = "♔ ChessLock Settings ♔"
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        })
        
        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2
            )
            setBackgroundColor(Color.parseColor("#444444"))
        })
        
        // Setup Section - NEW: No Accessibility Service needed!
        layout.addView(createSectionTitle("🔐 ChessLock Control"))
        
        // Enable/Disable ChessLock Toggle
        val isEnabled = prefs.getBoolean("chesslockEnabled", true)
        
        val enableButton = Button(this).apply {
            text = if (isEnabled) "✅ ChessLock ENABLED" else "❌ ChessLock DISABLED"
            setBackgroundColor(if (isEnabled) Color.parseColor("#4CAF50") else Color.parseColor("#FF5722"))
            setTextColor(Color.WHITE)
            setPadding(40, 40, 40, 40)
            textSize = 20f
            setOnClickListener {
                val newState = !prefs.getBoolean("chesslockEnabled", true)
                prefs.edit().putBoolean("chesslockEnabled", newState).apply()
                
                if (newState) {
                    // Enable ChessLock - start service
                    LockscreenService.start(this@MainActivity)
                    text = "✅ ChessLock ENABLED"
                    setBackgroundColor(Color.parseColor("#4CAF50"))
                    Toast.makeText(this@MainActivity, "✅ ChessLock enabled! Lock your phone to test.", Toast.LENGTH_LONG).show()
                } else {
                    // Disable ChessLock - stop service
                    LockscreenService.stop(this@MainActivity)
                    text = "❌ ChessLock DISABLED"
                    setBackgroundColor(Color.parseColor("#FF5722"))
                    Toast.makeText(this@MainActivity, "❌ ChessLock disabled", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        layout.addView(enableButton)
        
        layout.addView(TextView(this).apply {
            text = if (isEnabled) {
                "🎉 ChessLock is active! Lock your phone to see the chess lockscreen."
            } else {
                "⚠️ ChessLock is disabled. Tap the button above to enable."
            }
            textSize = 16f
            setTextColor(Color.parseColor("#AAAAAA"))
            setPadding(20, 20, 20, 10)
        })
        
        // Permission Setup
        layout.addView(createSectionTitle("⚙️ Required Permissions"))
        
        layout.addView(Button(this).apply {
            text = "1. Enable Accessibility Service"
            setBackgroundColor(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            setPadding(30, 30, 30, 30)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        })
        
        layout.addView(Button(this).apply {
            text = "2. Enable Draw Over Other Apps"
            setBackgroundColor(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            setPadding(30, 30, 30, 30)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 15 }
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        })
        
        layout.addView(TextView(this).apply {
            text = "⚠️ TECNO USERS: If you see 'Restricted setting' error,\nuse ADB method (see instructions file)"
            textSize = 14f
            setTextColor(Color.parseColor("#FF9800"))
            setPadding(20, 15, 20, 10)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        
        // Game Mode Section
        layout.addView(createSectionTitle("Game Mode"))
        
        val gameModeGroup = RadioGroup(this).apply {
            setPadding(20, 10, 20, 20)
        }
        
        val currentMode = prefs.getString("gameMode", "PUZZLE") ?: "PUZZLE"
        
        val puzzleMode = RadioButton(this).apply {
            id = View.generateViewId() // CRITICAL: Unique ID for radio button
            text = "🎯 Puzzle Mode (Solve to unlock)"
            setTextColor(Color.WHITE)
            textSize = 18f
            isChecked = currentMode == "PUZZLE"
        }
        
        val vsAIMode = RadioButton(this).apply {
            id = View.generateViewId() // CRITICAL: Unique ID for radio button
            text = "🤖 VS AI (Defeat AI to unlock)"
            setTextColor(Color.WHITE)
            textSize = 18f
            isChecked = currentMode == "VS_AI"
        }
        
        val practiceMode = RadioButton(this).apply {
            id = View.generateViewId() // CRITICAL: Unique ID for radio button
            text = "♟️ Practice Mode (Just play)"
            setTextColor(Color.WHITE)
            textSize = 18f
            isChecked = currentMode == "PRACTICE"
        }
        
        gameModeGroup.addView(puzzleMode)
        gameModeGroup.addView(vsAIMode)
        gameModeGroup.addView(practiceMode)
        
        gameModeGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when(checkedId) {
                puzzleMode.id -> "PUZZLE"
                vsAIMode.id -> "VS_AI"
                else -> "PRACTICE"
            }
            prefs.edit().putString("gameMode", mode).apply()
            Toast.makeText(this, "Game mode: $mode", Toast.LENGTH_SHORT).show()
        }
        
        layout.addView(gameModeGroup)
        
        // Puzzle Difficulty Section
        layout.addView(createSectionTitle("Puzzle Difficulty"))
        
        val puzzleDifficultySpinner = Spinner(this).apply {
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.parseColor("#2C2C2C"))
        }
        
        val puzzleDifficulties = arrayOf("RANDOM", "EASY", "MEDIUM", "HARD")
        puzzleDifficultySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, puzzleDifficulties).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        val currentPuzzleDifficulty = prefs.getString("puzzleDifficulty", "RANDOM") ?: "RANDOM"
        puzzleDifficultySpinner.setSelection(puzzleDifficulties.indexOf(currentPuzzleDifficulty))
        
        puzzleDifficultySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putString("puzzleDifficulty", puzzleDifficulties[position]).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        layout.addView(puzzleDifficultySpinner)
        
        // Puzzle Browser Section
        layout.addView(createSectionTitle("🧩 Puzzle Browser"))
        
        layout.addView(TextView(this).apply {
            text = "Choose a specific puzzle or let the app pick randomly"
            textSize = 14f
            setTextColor(Color.parseColor("#AAAAAA"))
            setPadding(20, 5, 20, 15)
        })
        
        // Puzzle info display (define before spinner listener uses it)
        val puzzleInfoText = TextView(this).apply {
            id = View.generateViewId()
            textSize = 14f
            setTextColor(Color.parseColor("#FFD700"))
            setPadding(20, 15, 20, 10)
            setBackgroundColor(Color.parseColor("#2C2C2C"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { 
                topMargin = 10
                setMargins(20, 10, 20, 0)
            }
        }
        
        // Helper function to update puzzle info display
        fun updatePuzzleInfo() {
            val selectedPuzzleId = prefs.getInt("selectedPuzzleId", -1)
            if (selectedPuzzleId == -1) {
                puzzleInfoText.text = "📝 Mode: RANDOM\nThe app will pick a random puzzle each time."
            } else {
                val puzzle = ChessPuzzles.allPuzzles[selectedPuzzleId - 1]
                puzzleInfoText.text = """
                    📝 ${puzzle.name}
                    🎯 Theme: ${puzzle.theme}
                    ⭐ Difficulty: ${puzzle.difficulty.name}
                    ${if (puzzle.mateIn > 0) "♔ Mate in ${puzzle.mateIn}" else ""}
                    💡 ${puzzle.description}
                """.trimIndent()
            }
        }
        
        val puzzleSelectorSpinner = Spinner(this).apply {
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.parseColor("#2C2C2C"))
        }
        
        val puzzleNames = mutableListOf("RANDOM (Any Puzzle)")
        puzzleNames.addAll(ChessPuzzles.allPuzzles.map { "${it.id}. ${it.name} (${it.difficulty.name})" })
        
        puzzleSelectorSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, puzzleNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        val selectedPuzzleId = prefs.getInt("selectedPuzzleId", -1)
        if (selectedPuzzleId == -1) {
            puzzleSelectorSpinner.setSelection(0) // RANDOM
        } else {
            puzzleSelectorSpinner.setSelection(selectedPuzzleId) // Specific puzzle
        }
        
        puzzleSelectorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    prefs.edit().putInt("selectedPuzzleId", -1).apply()
                } else {
                    prefs.edit().putInt("selectedPuzzleId", position).apply()
                }
                updatePuzzleInfo()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        layout.addView(puzzleSelectorSpinner)
        layout.addView(puzzleInfoText)
        
        // Update info based on current selection
        updatePuzzleInfo()
        
        // AI Difficulty Section
        layout.addView(createSectionTitle("AI Difficulty (VS AI Mode)"))
        
        val difficultySpinner = Spinner(this).apply {
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.parseColor("#2C2C2C"))
        }
        
        val difficulties = arrayOf("EASY", "MEDIUM", "HARD")
        difficultySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficulties).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        val currentDifficulty = prefs.getString("aiDifficulty", "MEDIUM") ?: "MEDIUM"
        difficultySpinner.setSelection(difficulties.indexOf(currentDifficulty))
        
        difficultySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putString("aiDifficulty", difficulties[position]).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        layout.addView(difficultySpinner)
        
        // Player Color Section
        layout.addView(createSectionTitle("Player Color (VS AI)"))
        
        val colorGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            setPadding(20, 10, 20, 20)
        }
        
        val playAsWhite = prefs.getBoolean("playAsWhite", true)
        
        val whiteButton = RadioButton(this).apply {
            id = View.generateViewId() // Generate unique ID
            text = "⚪ White"
            setTextColor(Color.WHITE)
            textSize = 18f
            isChecked = playAsWhite
        }
        
        val blackButton = RadioButton(this).apply {
            id = View.generateViewId() // Generate unique ID
            text = "⚫ Black"
            setTextColor(Color.WHITE)
            textSize = 18f
            isChecked = !playAsWhite
        }
        
        colorGroup.addView(whiteButton)
        colorGroup.addView(blackButton)
        
        colorGroup.setOnCheckedChangeListener { _, checkedId ->
            val isWhite = checkedId == whiteButton.id
            prefs.edit().putBoolean("playAsWhite", isWhite).apply()
        }
        
        layout.addView(colorGroup)
        
        // Board Appearance Section
        layout.addView(createSectionTitle("Board Appearance"))
        
        layout.addView(createColorPicker("Light Squares", "lightSquare", "#F0D9B5"))
        layout.addView(createColorPicker("Dark Squares", "darkSquare", "#B58863"))
        
        // How It Works Section
        layout.addView(createSectionTitle("How It Works"))
        
        layout.addView(TextView(this).apply {
            text = """
                📱 Setup Instructions:
                1. Enable "Draw Over Other Apps" permission (above)
                2. Tap "ChessLock ENABLED" button to activate
                3. Lock your phone to test - ChessLock will appear!
                
                🔐 Lockscreen Behavior:
                • Respects your native lock (PIN/Pattern/Fingerprint)
                • After you unlock, ChessLock appears immediately
                • Solve puzzle OR defeat AI to unlock phone
                • Emergency unlock bypasses chess
                
                ♟️ In Puzzle Mode:
                • Tap "New Puzzle" to switch puzzles
                • Tap "Hint" for help
                • Only YOUR pieces can move
                
                🤖 In VS AI Mode:
                • Full chess game with all 32 pieces
                • AI moves automatically after you
                • Checkmate AI to unlock
                
                🛡️ Security Features:
                • Back/Home buttons disabled during lock
                • No way to bypass without solving
                • Emergency unlock always available
            """.trimIndent()
            textSize = 16f
            setTextColor(Color.parseColor("#CCCCCC"))
            setPadding(20, 10, 20, 20)
            setLineSpacing(5f, 1f)
        })
        
        // Stats Section with expandable content
        layout.addView(createSectionTitle("📊 Statistics"))
        
        val statsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE // Hidden by default
        }
        
        val successfulUnlocks = prefs.getInt("successfulUnlocks", 0)
        val emergencyUnlocks = prefs.getInt("emergencyUnlocks", 0)
        val totalGames = prefs.getInt("totalGamesPlayed", 0)
        
        statsContainer.addView(createStatCard("✅ Successful Unlocks", successfulUnlocks))
        statsContainer.addView(createStatCard("🚨 Emergency Unlocks", emergencyUnlocks))
        statsContainer.addView(createStatCard("🎮 Total Games", totalGames))
        
        statsContainer.addView(Button(this).apply {
            text = "🔄 Reset Statistics"
            setBackgroundColor(Color.parseColor("#FF5722"))
            setTextColor(Color.WHITE)
            setPadding(30, 30, 30, 30)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 15; setMargins(20, 15, 20, 0) }
            setOnClickListener {
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Reset Statistics?")
                    .setMessage("This will reset all your stats to zero.")
                    .setPositiveButton("Reset") { _, _ ->
                        prefs.edit()
                            .putInt("successfulUnlocks", 0)
                            .putInt("emergencyUnlocks", 0)
                            .putInt("totalGamesPlayed", 0)
                            .apply()
                        Toast.makeText(this@MainActivity, "Statistics reset!", Toast.LENGTH_SHORT).show()
                        recreate()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        })
        
        layout.addView(createExpandableButton("📊 View Statistics", statsContainer))
        layout.addView(statsContainer)
        
        // Donation Section with expandable content
        layout.addView(createSectionTitle("💰 Support Development"))
        
        val donationContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE // Hidden by default
        }
        
        donationContainer.addView(TextView(this).apply {
            text = "Love ChessLock? Support the developer!"
            textSize = 16f
            setTextColor(Color.parseColor("#AAAAAA"))
            setPadding(20, 10, 20, 20)
        })
        
        donationContainer.addView(createDonationButton(
            "💳 PayPal Donation",
            "https://www.paypal.me/ngobihussein",
            "#0070BA"
        ))
        
        donationContainer.addView(TextView(this).apply {
            text = "🪙 Crypto Donations"
            textSize = 18f
            setTextColor(Color.parseColor("#FFD700"))
            setPadding(20, 20, 20, 10)
        })
        
        donationContainer.addView(createCryptoCard(
            "USDT (TRC20)",
            "TW8wYx1qnhNtLQ8XEgK4xMxoBDKjgokWuW"
        ))
        
        donationContainer.addView(createCryptoCard(
            "Bitcoin (BTC)",
            "1DfcK3s5k6GtTgeZua1xWR7VzfaBZoHY2g"
        ))
        
        donationContainer.addView(createCryptoCard(
            "Ethereum (ERC20)",
            "0x8085bc20c835be02402857ecc8a1cebcb061f7ed"
        ))
        
        layout.addView(createExpandableButton("💰 View Donation Options", donationContainer))
        layout.addView(donationContainer)
        
        layout.addView(TextView(this).apply {
            text = """
                
                Made with ❤️ for chess lovers
                Version 1.0.0
            """.trimIndent()
            textSize = 14f
            setTextColor(Color.parseColor("#888888"))
            setPadding(20, 20, 20, 40)
            gravity = Gravity.CENTER
        })
        
        scrollView.addView(layout)
        setContentView(scrollView)
    }
    
    private fun createSectionTitle(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 24f
            setTextColor(Color.parseColor("#4CAF50"))
            setPadding(0, 40, 0, 15)
        }
    }
    
    private fun createExpandableButton(text: String, container: LinearLayout): Button {
        return Button(this).apply {
            this.text = text
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setPadding(30, 30, 30, 30)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 10 }
            setOnClickListener {
                if (container.visibility == View.GONE) {
                    container.visibility = View.VISIBLE
                    this.text = text.replace("View", "Hide")
                } else {
                    container.visibility = View.GONE
                    this.text = text.replace("Hide", "View")
                }
            }
        }
    }
    
    private fun createStatCard(label: String, value: Int): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#2C2C2C"))
            setPadding(30, 25, 30, 25)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { 
                topMargin = 10
                bottomMargin = 5
                setMargins(20, 10, 20, 5)
            }
        }
        
        card.addView(TextView(this).apply {
            text = label
            textSize = 18f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        })
        
        card.addView(TextView(this).apply {
            text = value.toString()
            textSize = 24f
            setTextColor(Color.parseColor("#4CAF50"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        
        return card
    }
    
    private fun createDonationButton(text: String, link: String, color: String): Button {
        return Button(this).apply {
            this.text = text
            setBackgroundColor(Color.parseColor(color))
            setTextColor(Color.WHITE)
            setPadding(30, 30, 30, 30)
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Donation Link", link)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this@MainActivity, "✅ Link copied: $link", Toast.LENGTH_LONG).show()
                
                // Also try to open in browser
                try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(link))
                    startActivity(intent)
                } catch (e: Exception) {
                    // Browser open failed, link already copied
                }
            }
        }
    }
    
    private fun createCryptoCard(label: String, address: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#2C2C2C"))
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { 
                topMargin = 10
                bottomMargin = 10
            }
        }
        
        // Label
        card.addView(TextView(this).apply {
            text = label
            textSize = 16f
            setTextColor(Color.parseColor("#FFD700"))
            setPadding(0, 0, 0, 10)
        })
        
        // Address
        card.addView(TextView(this).apply {
            text = address
            textSize = 12f
            setTextColor(Color.parseColor("#AAAAAA"))
            setPadding(0, 0, 0, 10)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        
        // Copy button
        card.addView(Button(this).apply {
            text = "📋 Copy Address"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setPadding(20, 15, 20, 15)
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText(label, address)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this@MainActivity, "✅ $label address copied!", Toast.LENGTH_SHORT).show()
            }
        })
        
        return card
    }
    
    private fun createColorPicker(label: String, prefKey: String, defaultColor: String): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 15, 20, 15)
        }
        
        container.addView(TextView(this).apply {
            text = label
            textSize = 18f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        
        val colors = arrayOf("#F0D9B5", "#E8D7B5", "#D8C7A5", "#B58863", "#A07853", "#8B6843", "#7CAF50", "#5A8C3A")
        val spinner = Spinner(this).apply {
            setBackgroundColor(Color.parseColor("#2C2C2C"))
            setPadding(15, 15, 15, 15)
        }
        
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colors).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        val currentColor = prefs.getString(prefKey, defaultColor) ?: defaultColor
        spinner.setSelection(colors.indexOf(currentColor))
        
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putString(prefKey, colors[position]).apply()
                Toast.makeText(this@MainActivity, "$label updated", Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        container.addView(spinner)
        return container
    }
}
