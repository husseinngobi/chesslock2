package com.ngobi.chesslock

import androidx.core.graphics.toColorInt

/**
 * Manages board color themes separate from chess piece colors
 * Chess pieces remain white/black regardless of board theme
 */
object BoardTheme {
    
    data class Theme(
        val lightSquareColor: Int,
        val darkSquareColor: Int,
        val highlightColor: Int,
        val possibleMoveColor: Int
    )
    
    private val themes = mapOf(
        "professional" to Theme(
            lightSquareColor = "#f0d9b5".toColorInt(), // Professional tournament standard
            darkSquareColor = "#b58863".toColorInt(),
            highlightColor = "#ffdd44".toColorInt(),  // Clear but elegant highlight
            possibleMoveColor = "#88cc88".toColorInt() // Subtle green for moves
        ),
        "tournament" to Theme(
            lightSquareColor = "#eeeed2".toColorInt(), // Chess.com tournament style
            darkSquareColor = "#769656".toColorInt(),
            highlightColor = "#f6f669".toColorInt(),
            possibleMoveColor = "#7fb3d3".toColorInt() // Professional blue
        ),
        "luxury" to Theme(
            lightSquareColor = "#f5f5dc".toColorInt(), // Beige luxury
            darkSquareColor = "#8b7355".toColorInt(), // Rich brown
            highlightColor = "#ffd700".toColorInt(),  // Gold accent
            possibleMoveColor = "#b0e0e6".toColorInt() // Powder blue
        ),
        "executive" to Theme(
            lightSquareColor = "#f8f8ff".toColorInt(), // Ghost white
            darkSquareColor = "#708090".toColorInt(), // Slate gray
            highlightColor = "#ff6b35".toColorInt(),  // Orange accent
            possibleMoveColor = "#4a90e2".toColorInt() // Corporate blue
        ),
        "classic" to Theme(
            lightSquareColor = "#f0d9b5".toColorInt(),
            darkSquareColor = "#b58863".toColorInt(),
            highlightColor = "#ffff99".toColorInt(),
            possibleMoveColor = "#99ff99".toColorInt()
        ),
        "dark_mode" to Theme(
            lightSquareColor = "#3c3c3c".toColorInt(), // Professional dark
            darkSquareColor = "#1e1e1e".toColorInt(), // Deep black
            highlightColor = "#ff4757".toColorInt(),  // Vibrant red
            possibleMoveColor = "#2ed573".toColorInt() // Clean green
        ),
        "wood_premium" to Theme(
            lightSquareColor = "#deb887".toColorInt(), // Burlywood
            darkSquareColor = "#8b4513".toColorInt(), // Saddle brown
            highlightColor = "#ffd700".toColorInt(),  // Gold
            possibleMoveColor = "#90ee90".toColorInt() // Light green
        ),
        "marble_elite" to Theme(
            lightSquareColor = "#f5f5dc".toColorInt(), // Beige
            darkSquareColor = "#696969".toColorInt(), // Dim gray
            highlightColor = "#ff7f50".toColorInt(),  // Coral
            possibleMoveColor = "#87ceeb".toColorInt() // Sky blue
        ),
        "neon" to Theme(
            lightSquareColor = "#1a1a2e".toColorInt(),
            darkSquareColor = "#0f0f23".toColorInt(),
            highlightColor = "#ff00ff".toColorInt(),
            possibleMoveColor = "#00ffff".toColorInt()
        ),
        "minimal_pro" to Theme(
            lightSquareColor = "#ffffff".toColorInt(), // Pure white
            darkSquareColor = "#e0e0e0".toColorInt(), // Light gray
            highlightColor = "#007acc".toColorInt(),  // Professional blue
            possibleMoveColor = "#28a745".toColorInt() // Success green
        )
    )
    
    /**
     * Gets the theme colors for the specified theme name
     * Defaults to professional tournament theme
     */
    fun getTheme(themeName: String): Theme {
        return themes[themeName] ?: themes["professional"]!!
    }
    
    /**
     * Gets all available theme names
     */
    fun getAvailableThemes(): List<String> {
        return themes.keys.toList()
    }
}
