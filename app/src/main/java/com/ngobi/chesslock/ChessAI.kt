package com.ngobi.chesslock

import kotlinx.coroutines.*
import kotlin.random.Random

/**
 * Simple chess AI for ChessLock puzzles
 * Provides computer responses to make puzzles interactive
 */
class ChessAI {
    
    data class Move(
        val from: Pair<Int, Int>,
        val to: Pair<Int, Int>,
        val piece: Char,
        val capturedPiece: Char = ' '
    )
    
    /**
     * Generate AI response move with enhanced defensive strategy
     */
    fun generateMove(
        board: Array<Array<Char>>,
        isWhiteToMove: Boolean,
        onMoveGenerated: (Move?) -> Unit
    ) {
        // Use coroutine for AI thinking delay (realistic feel)
        CoroutineScope(Dispatchers.Main).launch {
            // Consistent thinking time for strategic play
            val thinkingTime = Random.nextLong(800, 1500)  // Thoughtful consideration
            delay(thinkingTime)
            
            val aiMove = findBestMove(board, isWhiteToMove)
            onMoveGenerated(aiMove)
        }
    }
    
    private fun findBestMove(board: Array<Array<Char>>, isWhiteToMove: Boolean): Move? {
        val validMoves = getAllValidMoves(board, isWhiteToMove)
        
        if (validMoves.isEmpty()) return null
        
        android.util.Log.d("ChessAI", "🤖 AI analyzing ${validMoves.size} possible moves with defensive strategy")
        
        // 🛡️ ENHANCED DEFENSIVE AI - Now plays defensively first, then strategically
        return findDefensiveStrategicMove(validMoves, board, isWhiteToMove)
    }
    
    private fun findDefensiveStrategicMove(validMoves: List<Move>, board: Array<Array<Char>>, isWhiteToMove: Boolean): Move? {
        // 🛡️ PRIORITY 1: DEFENSIVE MOVES
        val defensiveMoves = findDefensiveMoves(validMoves, board, isWhiteToMove)
        val safeCaptures = findSafeCaptures(validMoves, board, isWhiteToMove)
        val blockingMoves = findBlockingMoves(validMoves, board, isWhiteToMove)
        val escapeMoves = findEscapeMoves(validMoves, board, isWhiteToMove)
        
        // 🎯 PRIORITY 2: STRATEGIC MOVES
        val checks = findCheckingMoves(validMoves, board, isWhiteToMove)
        val threats = findThreateningMoves(validMoves, board, isWhiteToMove)
        val positional = findPositionalMoves(validMoves, board, isWhiteToMove)
        
        android.util.Log.d("ChessAI", "🛡️ Defensive analysis: ${defensiveMoves.size} defensive, ${safeCaptures.size} safe captures")
        android.util.Log.d("ChessAI", "⚔️ Strategic analysis: ${checks.size} checks, ${threats.size} threats")
        
        // 🧠 BALANCED CHESS.COM STYLE MOVE SELECTION
        // Mix of tactical awareness (attacks) and strategic safety (defense)
        return when {
            // 1. CRITICAL DEFENSE - Must address immediate threats
            defensiveMoves.isNotEmpty() && isInDanger(board, isWhiteToMove) -> {
                android.util.Log.d("ChessAI", "🛡️ Critical defense required")
                defensiveMoves.maxByOrNull { evaluateDefensiveValue(it, board) }
            }
            
            // 2. TACTICAL OPPORTUNITIES - Take advantage of opponent mistakes
            safeCaptures.isNotEmpty() && safeCaptures.any { getPieceValue(it.capturedPiece) >= 3 } -> {
                android.util.Log.d("ChessAI", "💰 Valuable capture opportunity")
                safeCaptures.maxByOrNull { getPieceValue(it.capturedPiece) }
            }
            
            // 3. ATTACKING CHANCES - Checks and direct threats (Chess.com style)
            checks.isNotEmpty() && isSafeToAttack(board, isWhiteToMove) -> {
                android.util.Log.d("ChessAI", "⚔️ Safe attacking check")
                checks.first()
            }
            
            // 4. ESCAPE ENDANGERED PIECES - Don't lose material
            escapeMoves.isNotEmpty() -> {
                android.util.Log.d("ChessAI", "🏃 Saving endangered piece")
                escapeMoves.maxByOrNull { getPieceValue(board[it.from.first][it.from.second]) }
            }
            
            // 5. BALANCED STRATEGY - Mix captures and threats (50/50 chance)
            (safeCaptures.isNotEmpty() || threats.isNotEmpty()) && Random.nextFloat() < 0.5f -> {
                if (safeCaptures.isNotEmpty() && Random.nextFloat() < 0.6f) {
                    android.util.Log.d("ChessAI", "💰 Balanced capture")
                    safeCaptures.maxByOrNull { getPieceValue(it.capturedPiece) }
                } else if (threats.isNotEmpty()) {
                    android.util.Log.d("ChessAI", "🎯 Balanced threat")
                    threats.maxByOrNull { evaluateThreatenValue(it, board) }
                } else {
                    safeCaptures.firstOrNull()
                }
            }
            
            // 6. DEFENSIVE CONSOLIDATION - Block threats, improve position
            blockingMoves.isNotEmpty() -> {
                android.util.Log.d("ChessAI", "🚧 Blocking opponent threat")
                blockingMoves.first()
            }
            
            // 7. POSITIONAL PLAY - Improve piece coordination
            positional.isNotEmpty() -> {
                android.util.Log.d("ChessAI", "📍 Positional improvement")
                positional.first()
            }
            
            // 8. SAFE DEVELOPMENT - Any remaining good move
            else -> {
                android.util.Log.d("ChessAI", "🎲 Safe development move")
                validMoves.randomOrNull()
            }
        }
    }
    
    // 🛡️ NEW DEFENSIVE MOVE DETECTION
    private fun findDefensiveMoves(validMoves: List<Move>, board: Array<Array<Char>>, isWhiteToMove: Boolean): List<Move> {
        val defensiveMoves = mutableListOf<Move>()
        
        for (move in validMoves) {
            // Check if this move defends against immediate threats
            if (isDefensiveMove(move, board, isWhiteToMove)) {
                defensiveMoves.add(move)
            }
        }
        
        return defensiveMoves
    }
    
    private fun findSafeCaptures(validMoves: List<Move>, board: Array<Array<Char>>, isWhiteToMove: Boolean): List<Move> {
        return validMoves.filter { move ->
            move.capturedPiece != ' ' && isSafeMove(move, board, isWhiteToMove)
        }
    }
    
    private fun findBlockingMoves(validMoves: List<Move>, board: Array<Array<Char>>, isWhiteToMove: Boolean): List<Move> {
        val blockingMoves = mutableListOf<Move>()
        
        for (move in validMoves) {
            if (blocksOpponentThreat(move, board, isWhiteToMove)) {
                blockingMoves.add(move)
            }
        }
        
        return blockingMoves
    }
    
    private fun findEscapeMoves(validMoves: List<Move>, board: Array<Array<Char>>, isWhiteToMove: Boolean): List<Move> {
        val escapeMoves = mutableListOf<Move>()
        
        for (move in validMoves) {
            if (movesPieceToSafety(move, board, isWhiteToMove)) {
                escapeMoves.add(move)
            }
        }
        
        return escapeMoves
    }
    
    // 🧠 DEFENSIVE EVALUATION FUNCTIONS
    private fun isDefensiveMove(move: Move, board: Array<Array<Char>>, isWhiteToMove: Boolean): Boolean {
        // Move defends the king, protects valuable pieces, or prevents checkmate
        val (fromRow, fromCol) = move.from
        val (toRow, toCol) = move.to
        
        // Simplified defensive check - moving to defend king area or protect valuable pieces
        return isNearKing(toRow, toCol, board, isWhiteToMove) || 
               protectsValuablePiece(move, board, isWhiteToMove)
    }
    
    private fun isSafeMove(move: Move, board: Array<Array<Char>>, isWhiteToMove: Boolean): Boolean {
        // Check if the destination square is safe (not under attack by opponent)
        val (toRow, toCol) = move.to
        return !isSquareUnderAttack(toRow, toCol, board, !isWhiteToMove)
    }
    
    private fun evaluateDefensiveValue(move: Move, board: Array<Array<Char>>): Int {
        var value = 0
        
        // Higher value for moves that protect the king
        if (move.piece.lowercaseChar() == 'k') value += 100
        
        // Value for protecting high-value pieces
        if (move.capturedPiece != ' ') {
            value += getPieceValue(move.capturedPiece) * 2 // Double value for defensive captures
        }
        
        return value
    }
    
    // 🛡️ ADDITIONAL DEFENSIVE HELPER FUNCTIONS
    private fun blocksOpponentThreat(move: Move, board: Array<Array<Char>>, isWhiteToMove: Boolean): Boolean {
        // Simplified threat blocking - check if move blocks an attack line to our king
        val (toRow, toCol) = move.to
        return isOnDefensiveLine(toRow, toCol, board, isWhiteToMove)
    }
    
    private fun movesPieceToSafety(move: Move, board: Array<Array<Char>>, isWhiteToMove: Boolean): Boolean {
        val (fromRow, fromCol) = move.from
        val (toRow, toCol) = move.to
        
        // Check if piece is currently under attack and moving to safety
        val pieceUnderAttack = isSquareUnderAttack(fromRow, fromCol, board, !isWhiteToMove)
        val movesToSafety = !isSquareUnderAttack(toRow, toCol, board, !isWhiteToMove)
        
        return pieceUnderAttack && movesToSafety
    }
    
    private fun isNearKing(row: Int, col: Int, board: Array<Array<Char>>, isWhiteToMove: Boolean): Boolean {
        // Find our king and check if the destination is near it
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece.lowercaseChar() == 'k' && isWhitePiece(piece) == isWhiteToMove) {
                    val distance = kotlin.math.abs(r - row) + kotlin.math.abs(c - col)
                    return distance <= 2 // Within 2 squares of king
                }
            }
        }
        return false
    }
    
    private fun protectsValuablePiece(move: Move, board: Array<Array<Char>>, isWhiteToMove: Boolean): Boolean {
        // Check if this move protects a queen or rook
        val (toRow, toCol) = move.to
        
        // Look for valuable pieces adjacent to destination
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val r = toRow + dr
                val c = toCol + dc
                if (r in 0..7 && c in 0..7) {
                    val piece = board[r][c]
                    if (piece != ' ' && isWhitePiece(piece) == isWhiteToMove) {
                        val pieceValue = getPieceValue(piece)
                        if (pieceValue >= 5) return true // Queen or Rook
                    }
                }
            }
        }
        return false
    }
    
    private fun isSquareUnderAttack(row: Int, col: Int, board: Array<Array<Char>>, byWhite: Boolean): Boolean {
        // Check if the given square is under attack by the specified color
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != ' ' && isWhitePiece(piece) == byWhite) {
                    val moves = getValidMovesForPiece(board, Pair(r, c))
                    if (moves.any { it.to.first == row && it.to.second == col }) {
                        return true
                    }
                }
            }
        }
        return false
    }
    
    private fun isOnDefensiveLine(row: Int, col: Int, board: Array<Array<Char>>, isWhiteToMove: Boolean): Boolean {
        // Simplified check - see if position blocks a line to our king
        // This is a basic implementation - could be enhanced with more sophisticated logic
        return isNearKing(row, col, board, isWhiteToMove)
    }
    
    // ⚖️ BALANCED CHESS.COM STYLE EVALUATION FUNCTIONS
    private fun isInDanger(board: Array<Array<Char>>, isWhiteToMove: Boolean): Boolean {
        // Check if our king is in check or under immediate threat
        val king = if (isWhiteToMove) 'K' else 'k'
        for (r in 0..7) {
            for (c in 0..7) {
                if (board[r][c] == king) {
                    return isSquareUnderAttack(r, c, board, !isWhiteToMove)
                }
            }
        }
        return false
    }
    
    private fun isSafeToAttack(board: Array<Array<Char>>, isWhiteToMove: Boolean): Boolean {
        // Check if it's safe to go on the offensive
        // Return true if king is safe and no immediate threats
        return !isInDanger(board, isWhiteToMove) && !hasHangingPieces(board, isWhiteToMove)
    }
    
    private fun hasHangingPieces(board: Array<Array<Char>>, isWhiteToMove: Boolean): Boolean {
        // Check if we have valuable pieces under attack
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != ' ' && isWhitePiece(piece) == isWhiteToMove) {
                    val pieceValue = getPieceValue(piece)
                    if (pieceValue >= 3 && isSquareUnderAttack(r, c, board, !isWhiteToMove)) {
                        return true // We have a valuable piece under attack
                    }
                }
            }
        }
        return false
    }
    
    private fun getAllValidMoves(board: Array<Array<Char>>, isWhiteToMove: Boolean): List<Move> {
        val moves = mutableListOf<Move>()
        
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board[row][col]
                if (piece == ' ' || isWhitePiece(piece) != isWhiteToMove) continue
                
                val pieceMoves = getValidMovesForPiece(board, Pair(row, col))
                moves.addAll(pieceMoves)
            }
        }
        
        return moves
    }
    
    private fun getValidMovesForPiece(board: Array<Array<Char>>, from: Pair<Int, Int>): List<Move> {
        val moves = mutableListOf<Move>()
        val (row, col) = from
        val piece = board[row][col].lowercaseChar()
        
        when (piece) {
            'p' -> moves.addAll(getPawnMoves(board, from))
            'r' -> moves.addAll(getRookMoves(board, from))
            'n' -> moves.addAll(getKnightMoves(board, from))
            'b' -> moves.addAll(getBishopMoves(board, from))
            'q' -> moves.addAll(getQueenMoves(board, from))
            'k' -> moves.addAll(getKingMoves(board, from))
        }
        
        return moves
    }
    
    private fun getPawnMoves(board: Array<Array<Char>>, from: Pair<Int, Int>): List<Move> {
        val moves = mutableListOf<Move>()
        val (row, col) = from
        val piece = board[row][col]
        val isWhite = isWhitePiece(piece)
        val direction = if (isWhite) -1 else 1
        val startRow = if (isWhite) 6 else 1
        
        // Forward move
        val newRow = row + direction
        if (newRow in 0..7 && board[newRow][col] == ' ') {
            moves.add(Move(from, Pair(newRow, col), piece))
            
            // Double move from starting position
            if (row == startRow && board[newRow + direction][col] == ' ') {
                moves.add(Move(from, Pair(newRow + direction, col), piece))
            }
        }
        
        // Captures
        for (colOffset in listOf(-1, 1)) {
            val newCol = col + colOffset
            if (newRow in 0..7 && newCol in 0..7) {
                val target = board[newRow][newCol]
                if (target != ' ' && isWhitePiece(target) != isWhite) {
                    moves.add(Move(from, Pair(newRow, newCol), piece, target))
                }
            }
        }
        
        return moves
    }
    
    private fun getRookMoves(board: Array<Array<Char>>, from: Pair<Int, Int>): List<Move> {
        return getSlidingMoves(board, from, listOf(
            Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
        ))
    }
    
    private fun getBishopMoves(board: Array<Array<Char>>, from: Pair<Int, Int>): List<Move> {
        return getSlidingMoves(board, from, listOf(
            Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)
        ))
    }
    
    private fun getQueenMoves(board: Array<Array<Char>>, from: Pair<Int, Int>): List<Move> {
        return getSlidingMoves(board, from, listOf(
            Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1),
            Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)
        ))
    }
    
    private fun getKnightMoves(board: Array<Array<Char>>, from: Pair<Int, Int>): List<Move> {
        val moves = mutableListOf<Move>()
        val (row, col) = from
        val piece = board[row][col]
        
        val knightMoves = listOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        
        for ((dr, dc) in knightMoves) {
            val newRow = row + dr
            val newCol = col + dc
            
            if (newRow in 0..7 && newCol in 0..7) {
                val target = board[newRow][newCol]
                if (target == ' ' || isWhitePiece(target) != isWhitePiece(piece)) {
                    moves.add(Move(from, Pair(newRow, newCol), piece, target))
                }
            }
        }
        
        return moves
    }
    
    private fun getKingMoves(board: Array<Array<Char>>, from: Pair<Int, Int>): List<Move> {
        val moves = mutableListOf<Move>()
        val (row, col) = from
        val piece = board[row][col]
        
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                
                val newRow = row + dr
                val newCol = col + dc
                
                if (newRow in 0..7 && newCol in 0..7) {
                    val target = board[newRow][newCol]
                    if (target == ' ' || isWhitePiece(target) != isWhitePiece(piece)) {
                        moves.add(Move(from, Pair(newRow, newCol), piece, target))
                    }
                }
            }
        }
        
        return moves
    }
    
    private fun getSlidingMoves(board: Array<Array<Char>>, from: Pair<Int, Int>, directions: List<Pair<Int, Int>>): List<Move> {
        val moves = mutableListOf<Move>()
        val (row, col) = from
        val piece = board[row][col]
        
        for ((dr, dc) in directions) {
            var newRow = row + dr
            var newCol = col + dc
            
            while (newRow in 0..7 && newCol in 0..7) {
                val target = board[newRow][newCol]
                
                if (target == ' ') {
                    moves.add(Move(from, Pair(newRow, newCol), piece))
                } else {
                    if (isWhitePiece(target) != isWhitePiece(piece)) {
                        moves.add(Move(from, Pair(newRow, newCol), piece, target))
                    }
                    break
                }
                
                newRow += dr
                newCol += dc
            }
        }
        
        return moves
    }
    
    private fun isWhitePiece(piece: Char): Boolean = piece.isUpperCase()
    
    private fun isInCheck(board: Array<Array<Char>>, isWhiteKing: Boolean): Boolean {
        // Find king position
        val kingChar = if (isWhiteKing) 'K' else 'k'
        var kingPos: Pair<Int, Int>? = null
        
        for (row in 0..7) {
            for (col in 0..7) {
                if (board[row][col] == kingChar) {
                    kingPos = Pair(row, col)
                    break
                }
            }
        }
        
        if (kingPos == null) return false
        
        // Check if any opponent piece attacks the king
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board[row][col]
                if (piece != ' ' && isWhitePiece(piece) != isWhiteKing) {
                    val moves = getValidMovesForPiece(board, Pair(row, col))
                    if (moves.any { it.to == kingPos }) {
                        return true
                    }
                }
            }
        }
        
        return false
    }
    
    private fun getPieceValue(piece: Char): Int {
        return when (piece.lowercaseChar()) {
            'p' -> 1
            'n', 'b' -> 3
            'r' -> 5
            'q' -> 9
            'k' -> 100
            else -> 0
        }
    }
    
    private fun findCheckingMoves(validMoves: List<Move>, board: Array<Array<Char>>, isWhiteToMove: Boolean): List<Move> {
        return validMoves.filter { move ->
            val testBoard = board.map { it.copyOf() }.toTypedArray()
            testBoard[move.to.first][move.to.second] = move.piece
            testBoard[move.from.first][move.from.second] = ' '
            isInCheck(testBoard, !isWhiteToMove)
        }
    }
    
    private fun findThreateningMoves(validMoves: List<Move>, board: Array<Array<Char>>, isWhiteToMove: Boolean): List<Move> {
        // Moves that threaten opponent pieces
        return validMoves.filter { move ->
            // Check if this move attacks enemy pieces
            getValidMovesForPiece(board, move.to).any { 
                board[it.to.first][it.to.second] != ' ' && 
                isWhitePiece(board[it.to.first][it.to.second]) != isWhiteToMove
            }
        }
    }
    
    private fun findPositionalMoves(validMoves: List<Move>, board: Array<Array<Char>>, isWhiteToMove: Boolean): List<Move> {
        // Moves that improve piece positioning (center control, development)
        return validMoves.filter { move ->
            val (fromRow, fromCol) = move.from
            val (toRow, toCol) = move.to
            val piece = board[fromRow][fromCol]
            
            // Center control bonus
            val centerDistance = kotlin.math.abs(3.5 - toRow) + kotlin.math.abs(3.5 - toCol)
            val centerControl = centerDistance < 4.0
            
            // Development bonus for pieces moving forward
            val development = if (isWhiteToMove) {
                toRow < fromRow // White moves up the board (decreasing row numbers)
            } else {
                toRow > fromRow // Black moves down the board (increasing row numbers)
            }
            
            // Piece-specific positional evaluation
            when (piece.lowercaseChar()) {
                'p' -> development // Pawns prioritize advancement
                'n', 'b' -> centerControl || development // Knights and bishops benefit from center and development
                'r', 'q' -> centerControl // Rooks and queens prioritize center control
                'k' -> false // King should avoid center in middle game
                else -> centerControl || development // Default: prefer center or development
            }
        }
    }
    
    private fun evaluateMove(move: Move, board: Array<Array<Char>>, isWhiteToMove: Boolean): Int {
        var score = 0
        
        // Material gain
        if (move.capturedPiece != ' ') {
            score += getPieceValue(move.capturedPiece) * 10
        }
        
        // Center control
        val (toRow, toCol) = move.to
        val centerDistance = kotlin.math.abs(3.5 - toRow) + kotlin.math.abs(3.5 - toCol)
        score += (8 - centerDistance.toInt())
        
        // Piece safety (avoid hanging pieces)
        val testBoard = board.map { it.copyOf() }.toTypedArray()
        testBoard[move.to.first][move.to.second] = move.piece
        testBoard[move.from.first][move.from.second] = ' '
        
        val enemyMoves = getAllValidMoves(testBoard, !isWhiteToMove)
        val isHanging = enemyMoves.any { it.to == move.to }
        if (isHanging) score -= getPieceValue(move.piece) * 5
        
        return score
    }
    
    /**
     * 💡 ENHANCED HELPER METHODS for Professional AI
     */
    private fun getPositionalMove(validMoves: List<Move>, board: Array<Array<Char>>): Move? {
        // Find moves that improve piece position (center control, development)
        return validMoves.filter { move ->
            val (toRow, toCol) = move.to
            // Prefer center squares and advanced positions
            val centerDistance = kotlin.math.abs(3.5 - toRow) + kotlin.math.abs(3.5 - toCol)
            centerDistance < 3.0
        }.randomOrNull()
    }
    
    private fun evaluateThreatenValue(move: Move, board: Array<Array<Char>>): Int {
        // Simple evaluation of how threatening a move is
        val (toRow, toCol) = move.to
        var score = 0
        
        // Center squares are more threatening
        val centerDistance = kotlin.math.abs(3.5 - toRow) + kotlin.math.abs(3.5 - toCol)
        score += (8 - centerDistance.toInt())
        
        // Pieces closer to enemy king are more threatening
        val enemyKingPos = findEnemyKing(board, move.piece.isUpperCase())
        enemyKingPos?.let { (kingRow, kingCol) ->
            val kingDistance = kotlin.math.abs(toRow - kingRow) + kotlin.math.abs(toCol - kingCol)
            score += (8 - kingDistance)
        }
        
        return score
    }
    
    private fun findEnemyKing(board: Array<Array<Char>>, isWhiteToMove: Boolean): Pair<Int, Int>? {
        val enemyKing = if (isWhiteToMove) 'k' else 'K'
        for (row in 0..7) {
            for (col in 0..7) {
                if (board[row][col] == enemyKing) {
                    return Pair(row, col)
                }
            }
        }
        return null
    }
}
