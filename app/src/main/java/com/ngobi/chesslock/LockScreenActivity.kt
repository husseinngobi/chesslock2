package com.ngobi.chesslock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LockScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_screen)

        val prefs = getSharedPreferences("ChessLockPrefs", MODE_PRIVATE)
        val difficulty = prefs.getString("difficulty", "medium") ?: "medium"
        val nativeLockEnabled = prefs.getBoolean("nativeLock", false)

        val puzzle = PuzzleManager.getRandomChessPuzzle()

        val promptView = findViewById<TextView>(R.id.puzzlePrompt)
        val boardView = findViewById<ChessBoardView>(R.id.chessBoard)
        val emergencyButton = findViewById<Button>(R.id.emergencyUnlockButton)

        promptView.text = puzzle.prompt
        boardView.setFEN(puzzle.fen, puzzle.solutionFEN)

        boardView.onPuzzleSolved = {
            if (nativeLockEnabled) {
                val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val adminComponent = ComponentName(this, LockAdminReceiver::class.java)

                if (dpm.isAdminActive(adminComponent)) {
                    Toast.makeText(this, "Puzzle solved! Unlocking...", Toast.LENGTH_SHORT).show()
                    finish() // Dismiss lock screen
                } else {
                    Toast.makeText(this, "Admin not active. Please enable in settings.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Puzzle solved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        emergencyButton.setOnClickListener {
            Toast.makeText(this, "Emergency unlock triggered", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}