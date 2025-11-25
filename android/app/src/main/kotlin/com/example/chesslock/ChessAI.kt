package com.example.chesslock

import kotlin.random.Random

/**
 * Chess AI with multiple difficulty levels
 * Easy: Random legal moves
 * Medium: Material evaluation + simple tactics
 * Hard: Material evaluation + positional play + deeper search
 */
class ChessAI(private val engine: ChessEngine) {
    
    enum class Difficulty { EASY, MEDIUM, HARD }
    
    private val pieceValues = mapOf(
        ChessEngine.PieceType.PAWN to 100,
        ChessEngine.PieceType.KNIGHT to 320,
        ChessEngine.PieceType.BISHOP to 330,
        ChessEngine.PieceType.ROOK to 500,
        ChessEngine.PieceType.QUEEN to 900,
        ChessEngine.PieceType.KING to 20000
    )
    
    // Positional bonuses for pawns (encourage center control and advancement)
    private val pawnSquareTable = arrayOf(
        intArrayOf(0,  0,  0,  0,  0,  0,  0,  0),
        intArrayOf(50, 50, 50, 50, 50, 50, 50, 50),
        intArrayOf(10, 10, 20, 30, 30, 20, 10, 10),
        intArrayOf(5,  5, 10, 25, 25, 10,  5,  5),
        intArrayOf(0,  0,  0, 20, 20,  0,  0,  0),
        intArrayOf(5, -5,-10,  0,  0,-10, -5,  5),
        intArrayOf(5, 10, 10,-20,-20, 10, 10,  5),
        intArrayOf(0,  0,  0,  0,  0,  0,  0,  0)
    )
    
    // Knight square table (encourage center control)
    private val knightSquareTable = arrayOf(
        intArrayOf(-50,-40,-30,-30,-30,-30,-40,-50),
        intArrayOf(-40,-20,  0,  0,  0,  0,-20,-40),
        intArrayOf(-30,  0, 10, 15, 15, 10,  0,-30),
        intArrayOf(-30,  5, 15, 20, 20, 15,  5,-30),
        intArrayOf(-30,  0, 15, 20, 20, 15,  0,-30),
        intArrayOf(-30,  5, 10, 15, 15, 10,  5,-30),
        intArrayOf(-40,-20,  0,  5,  5,  0,-20,-40),
        intArrayOf(-50,-40,-30,-30,-30,-30,-40,-50)
    )
    
    fun getBestMove(difficulty: Difficulty, aiColor: ChessEngine.Color): ChessEngine.Move? {
        val legalMoves = engine.getAllLegalMoves(aiColor)
        if (legalMoves.isEmpty()) return null
        
        return when(difficulty) {
            Difficulty.EASY -> getRandomMove(legalMoves)
            Difficulty.MEDIUM -> getMediumMove(legalMoves, aiColor)
            Difficulty.HARD -> getHardMove(legalMoves, aiColor)
        }
    }
    
    private fun getRandomMove(moves: List<ChessEngine.Move>): ChessEngine.Move {
        return moves[Random.nextInt(moves.size)]
    }
    
    private fun getMediumMove(moves: List<ChessEngine.Move>, aiColor: ChessEngine.Color): ChessEngine.Move {
        // Evaluate each move based on material gain
        var bestMove = moves[0]
        var bestScore = Int.MIN_VALUE
        
        for (move in moves) {
            val score = evaluateMoveSimple(move, aiColor)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        
        return bestMove
    }
    
    private fun getHardMove(moves: List<ChessEngine.Move>, aiColor: ChessEngine.Color): ChessEngine.Move {
        // Use minimax with alpha-beta pruning (depth 2)
        var bestMove = moves[0]
        var bestScore = Int.MIN_VALUE
        
        for (move in moves) {
            val score = minimax(move, 2, Int.MIN_VALUE, Int.MAX_VALUE, false, aiColor)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        
        return bestMove
    }
    
    private fun evaluateMoveSimple(move: ChessEngine.Move, aiColor: ChessEngine.Color): Int {
        val targetPiece = engine.board[move.to.row][move.to.col]
        val movingPiece = engine.board[move.from.row][move.from.col] ?: return 0
        
        var score = 0
        
        // Capture value
        if (targetPiece != null && targetPiece.color != aiColor) {
            score += pieceValues[targetPiece.type] ?: 0
        }
        
        // Center control bonus
        val centerDistance = kotlin.math.abs(move.to.row - 3.5) + kotlin.math.abs(move.to.col - 3.5)
        score += (7 - centerDistance).toInt() * 10
        
        // Check if move puts opponent in check
        if (simulateAndCheckOpponentInCheck(move, aiColor)) {
            score += 50
        }
        
        return score
    }
    
    private fun minimax(move: ChessEngine.Move, depth: Int, alpha: Int, beta: Int, 
                       maximizingPlayer: Boolean, aiColor: ChessEngine.Color): Int {
        // Make the move temporarily
        val piece = engine.board[move.from.row][move.from.col]
        val captured = engine.board[move.to.row][move.to.col]
        val oldPlayer = engine.currentPlayer
        
        engine.board[move.to.row][move.to.col] = piece
        engine.board[move.from.row][move.from.col] = null
        engine.currentPlayer = if (maximizingPlayer) 
            (if (aiColor == ChessEngine.Color.WHITE) ChessEngine.Color.BLACK else ChessEngine.Color.WHITE)
        else aiColor
        
        val score = if (depth == 0) {
            evaluateBoard(aiColor)
        } else {
            val moves = engine.getAllLegalMoves(engine.currentPlayer)
            if (moves.isEmpty()) {
                if (engine.isInCheck(engine.currentPlayer)) -10000 // Checkmate
                else 0 // Stalemate
            } else {
                var value = if (maximizingPlayer) Int.MIN_VALUE else Int.MAX_VALUE
                var currentAlpha = alpha
                var currentBeta = beta
                
                for (nextMove in moves) {
                    val eval = minimax(nextMove, depth - 1, currentAlpha, currentBeta, !maximizingPlayer, aiColor)
                    if (maximizingPlayer) {
                        value = maxOf(value, eval)
                        currentAlpha = maxOf(currentAlpha, eval)
                    } else {
                        value = minOf(value, eval)
                        currentBeta = minOf(currentBeta, eval)
                    }
                    if (currentBeta <= currentAlpha) break // Alpha-beta pruning
                }
                value
            }
        }
        
        // Undo the move
        engine.board[move.from.row][move.from.col] = piece
        engine.board[move.to.row][move.to.col] = captured
        engine.currentPlayer = oldPlayer
        
        return score
    }
    
    private fun evaluateBoard(aiColor: ChessEngine.Color): Int {
        var score = 0
        
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = engine.board[row][col] ?: continue
                val pieceValue = pieceValues[piece.type] ?: 0
                
                // Add positional bonus
                val positionalBonus = when(piece.type) {
                    ChessEngine.PieceType.PAWN -> {
                        val table = if (piece.color == ChessEngine.Color.WHITE) pawnSquareTable else pawnSquareTable.reversedArray()
                        table[row][col]
                    }
                    ChessEngine.PieceType.KNIGHT -> {
                        val table = if (piece.color == ChessEngine.Color.WHITE) knightSquareTable else knightSquareTable.reversedArray()
                        table[row][col]
                    }
                    else -> 0
                }
                
                val totalValue = pieceValue + positionalBonus
                
                if (piece.color == aiColor) {
                    score += totalValue
                } else {
                    score -= totalValue
                }
            }
        }
        
        // Bonus for mobility (number of legal moves)
        score += engine.getAllLegalMoves(aiColor).size * 10
        score -= engine.getAllLegalMoves(
            if (aiColor == ChessEngine.Color.WHITE) ChessEngine.Color.BLACK else ChessEngine.Color.WHITE
        ).size * 10
        
        return score
    }
    
    private fun simulateAndCheckOpponentInCheck(move: ChessEngine.Move, aiColor: ChessEngine.Color): Boolean {
        val piece = engine.board[move.from.row][move.from.col]
        val captured = engine.board[move.to.row][move.to.col]
        
        engine.board[move.to.row][move.to.col] = piece
        engine.board[move.from.row][move.from.col] = null
        
        val opponentColor = if (aiColor == ChessEngine.Color.WHITE) ChessEngine.Color.BLACK else ChessEngine.Color.WHITE
        val inCheck = engine.isInCheck(opponentColor)
        
        // Undo
        engine.board[move.from.row][move.from.col] = piece
        engine.board[move.to.row][move.to.col] = captured
        
        return inCheck
    }
}
