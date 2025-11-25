package com.ngobi.chesslock

import android.content.Context
import androidx.preference.PreferenceManager

object PieceManager {
    
    fun getDrawableId(piece: Char, context: Context? = null): Int {
        // Get piece style from preferences if context is available
        val pieceStyle = if (context != null) {
            val prefs = context.getSharedPreferences("ChessLockPrefs", Context.MODE_PRIVATE)
            prefs.getString("piece_style", "traditional") ?: "traditional"
        } else {
            "traditional" // Default fallback
        }
        
        return when (pieceStyle) {
            "minimalist" -> getMinimalistPieceId(piece)
            else -> getTraditionalPieceId(piece)
        }
    }
    
    private fun getTraditionalPieceId(piece: Char): Int {
        return when (piece) {
            'K' -> R.drawable.w_king
            'Q' -> R.drawable.w_queen
            'R' -> R.drawable.w_rook
            'B' -> R.drawable.w_bishop
            'N' -> R.drawable.w_knight
            'P' -> R.drawable.w_pawn
            'k' -> R.drawable.b_king
            'q' -> R.drawable.b_queen
            'r' -> R.drawable.b_rook
            'b' -> R.drawable.b_bishop
            'n' -> R.drawable.b_knight
            'p' -> R.drawable.b_pawn
            else -> 0
        }
    }
    
    private fun getMinimalistPieceId(piece: Char): Int {
        // For now, use the same pieces but we could add minimalist versions later
        // In a full implementation, these would be separate drawable resources
        return when (piece) {
            'K' -> R.drawable.w_king  // Would be R.drawable.w_king_minimal
            'Q' -> R.drawable.w_queen // Would be R.drawable.w_queen_minimal
            'R' -> R.drawable.w_rook  // Would be R.drawable.w_rook_minimal
            'B' -> R.drawable.w_bishop // Would be R.drawable.w_bishop_minimal
            'N' -> R.drawable.w_knight // Would be R.drawable.w_knight_minimal
            'P' -> R.drawable.w_pawn   // Would be R.drawable.w_pawn_minimal
            'k' -> R.drawable.b_king   // Would be R.drawable.b_king_minimal
            'q' -> R.drawable.b_queen  // Would be R.drawable.b_queen_minimal
            'r' -> R.drawable.b_rook   // Would be R.drawable.b_rook_minimal
            'b' -> R.drawable.b_bishop // Would be R.drawable.b_bishop_minimal
            'n' -> R.drawable.b_knight // Would be R.drawable.b_knight_minimal
            'p' -> R.drawable.b_pawn   // Would be R.drawable.b_pawn_minimal
            else -> 0
        }
    }
    
    /**
     * Gets all available piece styles
     */
    fun getAvailableStyles(): List<String> {
        return listOf("traditional", "minimalist")
    }
}