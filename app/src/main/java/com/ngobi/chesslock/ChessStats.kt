package com.ngobi.chesslock

data class ChessStats(
    var gamesPlayed: Int = 0,
    var gamesWon: Int = 0,
    var streak: Int = 0,
    var totalUnlocks: Int = 0,
    var averageTime: Int = 0, // in seconds
    var bestTime: Int = 0     // in seconds
)