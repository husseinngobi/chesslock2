package com.ngobi.chesslock

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class StatsActivity : AppCompatActivity() {

    private lateinit var stats: ChessStats

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        stats = StatsManager.stats

        findViewById<TextView>(R.id.gamesPlayedText).text = stats.gamesPlayed.toString()
        findViewById<TextView>(R.id.winRateText).text = "${calculateWinRate()}%"
        findViewById<TextView>(R.id.streakText).text = stats.streak.toString()
        findViewById<TextView>(R.id.totalUnlocksText).text = stats.totalUnlocks.toString()
        findViewById<TextView>(R.id.avgTimeText).text = formatTime(stats.averageTime)
        findViewById<TextView>(R.id.bestTimeText).text = formatTime(stats.bestTime)

        findViewById<Button>(R.id.resetStatsButton).setOnClickListener {
            StatsManager.resetStats()
            recreate()
        }
    }

    private fun calculateWinRate(): Int {
        return if (stats.gamesPlayed > 0) {
            (stats.gamesWon * 100) / stats.gamesPlayed
        } else 0
    }

    private fun formatTime(seconds: Int): String {
        return if (seconds == 0) "--"
        else if (seconds < 60) "$seconds s"
        else "${seconds / 60}m ${seconds % 60}s"
    }
}