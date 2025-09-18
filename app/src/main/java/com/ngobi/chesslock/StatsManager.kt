package com.ngobi.chesslock

object StatsManager {
    var stats = ChessStats()

    fun recordGame(won: Boolean, timeTaken: Int) {
        stats.gamesPlayed++
        stats.totalUnlocks++

        if (won) {
            stats.gamesWon++
            stats.streak++
            if (stats.bestTime == 0 || timeTaken < stats.bestTime) {
                stats.bestTime = timeTaken
            }

            stats.averageTime = if (stats.gamesWon > 0) {
                ((stats.averageTime * (stats.gamesWon - 1)) + timeTaken) / stats.gamesWon
            } else {
                timeTaken
            }
        } else {
            stats.streak = 0
        }
    }

    fun resetStats() {
        stats = ChessStats()
    }
}