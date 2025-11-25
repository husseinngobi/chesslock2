package com.ngobi.chesslock

import android.util.Log
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import com.github.bhlangonijr.chesslib.move.MoveList

/**
 * 🧠 COMPREHENSIVE CHESS RULES ENGINE
 * 
 * Enforces ALL chess rules including:
 * ✅ Move validation (piece movement, captures, special moves)
 * ✅ Check/Checkmate detection
 * ✅ Stalemate detection
 * ✅ Draw conditions (50-move rule, insufficient material, repetition)
 * ✅ Game state management
 * ✅ FEN notation support
 * 
 * This ensures ChessLock never allows illegal moves and properly
 * recognizes when the game should end.
 */
class ChessEngine {
    
    // Core chess board state
    private val board: Board = Board()
    
    // Game state tracking
    private var gameStarted = false
    private var gameEnded = false
    private var gameResult: GameResult = GameResult.IN_PROGRESS
    private var lastMoveTime = System.currentTimeMillis()
    
    // Move history for analysis
    private val moveHistory = mutableListOf<Move>()
    
    // Listeners for game events
    private var onGameEndListener: ((GameResult) -> Unit)? = null
    private var onMoveListener: ((MoveResult) -> Unit)? = null
    private var onCheckListener: ((Side) -> Unit)? = null
    
    init {
        resetGame()
        Log.d("ChessEngine", "🧠 Chess rules engine initialized")
    }
    
    /**
     * 🎯 GAME RESULTS ENUM
     * Comprehensive game outcome definitions
     */
    enum class GameResult(val description: String, val isWin: Boolean, val winner: Side?) {
        IN_PROGRESS("Game in progress", false, null),
        WHITE_WINS_CHECKMATE("White wins by checkmate", true, Side.WHITE),
        BLACK_WINS_CHECKMATE("Black wins by checkmate", true, Side.BLACK),
        STALEMATE("Draw by stalemate", false, null),
        DRAW_50_MOVE_RULE("Draw by 50-move rule", false, null),
        DRAW_INSUFFICIENT_MATERIAL("Draw by insufficient material", false, null),
        DRAW_THREEFOLD_REPETITION("Draw by threefold repetition", false, null),
        WHITE_WINS_RESIGNATION("White wins by resignation", true, Side.WHITE),
        BLACK_WINS_RESIGNATION("Black wins by resignation", true, Side.BLACK),
        WHITE_WINS_TIMEOUT("White wins on time", true, Side.WHITE),
        BLACK_WINS_TIMEOUT("Black wins on time", true, Side.BLACK)
    }
    
    /**
     * 📋 MOVE RESULT DATA CLASS
     * Contains comprehensive information about move attempts
     */
    data class MoveResult(
        val success: Boolean,
        val move: Move?,
        val gameResult: GameResult,
        val isCheck: Boolean,
        val isCapture: Boolean,
        val isPromotion: Boolean,
        val isCastling: Boolean,
        val isEnPassant: Boolean,
        val errorMessage: String?,
        val boardFEN: String,
        val legalMoves: List<Move>
    )
    
    /**
     * 🚀 CORE MOVE EXECUTION
     * Validates and executes moves with comprehensive rule checking
     */
    fun makeMove(fromSquare: String, toSquare: String, promotionPiece: String? = null): MoveResult {
        if (gameEnded) {
            return createMoveResult(
                success = false,
                move = null,
                errorMessage = "Game has already ended: ${gameResult.description}"
            )
        }
        
        return try {
            val from = parseSquare(fromSquare)
            val to = parseSquare(toSquare)
            
            if (from == null || to == null) {
                return createMoveResult(
                    success = false,
                    move = null,
                    errorMessage = "Invalid square notation: $fromSquare -> $toSquare"
                )
            }
            
            // Find the move in legal moves
            val legalMoves = board.legalMoves()
            val candidateMove = findMove(legalMoves.toList(), from, to, promotionPiece)
            
            if (candidateMove == null) {
                Log.w("ChessEngine", "🚫 Illegal move attempted: $fromSquare -> $toSquare")
                return createMoveResult(
                    success = false,
                    move = null,
                    errorMessage = "Illegal move: ${fromSquare.uppercase()} to ${toSquare.uppercase()}"
                )
            }
            
            // Execute the move
            board.doMove(candidateMove)
            moveHistory.add(candidateMove)
            lastMoveTime = System.currentTimeMillis()
            
            Log.d("ChessEngine", "✅ Move executed: $fromSquare -> $toSquare")
            
            // Analyze the resulting position
            val newGameResult = analyzePosition()
            
            // Update game state
            if (newGameResult != GameResult.IN_PROGRESS) {
                gameEnded = true
                gameResult = newGameResult
                Log.i("ChessEngine", "🏁 Game ended: ${newGameResult.description}")
                onGameEndListener?.invoke(newGameResult)
            }
            
            // Check for check
            val isCheck = board.isKingAttacked()
            if (isCheck && newGameResult == GameResult.IN_PROGRESS) {
                Log.i("ChessEngine", "⚠️ ${board.sideToMove} king is in check!")
                onCheckListener?.invoke(board.sideToMove)
            }
            
            val result = createMoveResult(
                success = true,
                move = candidateMove,
                errorMessage = null
            )
            
            onMoveListener?.invoke(result)
            return result
            
        } catch (e: Exception) {
            Log.e("ChessEngine", "Error executing move: ${e.message}")
            return createMoveResult(
                success = false,
                move = null,
                errorMessage = "Move execution failed: ${e.message}"
            )
        }
    }
    
    /**
     * 🔍 MOVE VALIDATION ONLY
     * Validates move without executing it - perfect for UI validation
     */
    fun isValidMove(fromSquare: String, toSquare: String, promotionPiece: String? = null): Boolean {
        if (gameEnded) {
            return false
        }
        
        return try {
            val from = parseSquare(fromSquare)
            val to = parseSquare(toSquare)
            
            if (from == null || to == null) {
                return false
            }
            
            // Find the move in legal moves without executing it
            val legalMoves = board.legalMoves()
            val candidateMove = findMove(legalMoves.toList(), from, to, promotionPiece)
            
            return candidateMove != null
            
        } catch (e: Exception) {
            Log.e("ChessEngine", "Error validating move: ${e.message}")
            return false
        }
    }
    
    /**
     * 🎓 EDUCATIONAL MOVE VALIDATION
     * Provides detailed explanation of why a move is illegal
     */
    fun validateMoveWithExplanation(fromSquare: String, toSquare: String, promotionPiece: String? = null): MoveValidationResult {
        if (gameEnded) {
            return MoveValidationResult(
                isValid = false,
                reason = "Game has ended",
                educationalTip = "The game is over. Start a new game to continue playing."
            )
        }
        
        try {
            val from = parseSquare(fromSquare)
            val to = parseSquare(toSquare)
            
            if (from == null || to == null) {
                return MoveValidationResult(
                    isValid = false,
                    reason = "Invalid square notation",
                    educationalTip = "Chess squares are named with a letter (a-h) and number (1-8), like 'e4' or 'a1'."
                )
            }
            
            // Check if there's a piece on the from square
            val piece = board.getPiece(from)
            if (piece == null) {
                return MoveValidationResult(
                    isValid = false,
                    reason = "No piece on starting square",
                    educationalTip = "You can only move pieces that exist on the board. Select a square that contains your piece."
                )
            }
            
            // Check if it's the right player's turn
            if ((piece.pieceSide == Side.WHITE && !board.sideToMove.equals(Side.WHITE)) ||
                (piece.pieceSide == Side.BLACK && !board.sideToMove.equals(Side.BLACK))) {
                val wrongSide = if (piece.pieceSide == Side.WHITE) "white" else "black"
                val correctSide = if (board.sideToMove == Side.WHITE) "white" else "black"
                return MoveValidationResult(
                    isValid = false,
                    reason = "Wrong player's turn",
                    educationalTip = "You selected a $wrongSide piece, but it's $correctSide's turn to move."
                )
            }
            
            // Get all legal moves and check if this move exists
            val legalMoves = board.legalMoves()
            val candidateMove = findMove(legalMoves.toList(), from, to, promotionPiece)
            
            if (candidateMove != null) {
                // Move is legal - provide positive feedback
                val moveType = analyzeMoveType(candidateMove)
                return MoveValidationResult(
                    isValid = true,
                    reason = "Legal move",
                    educationalTip = moveType
                )
            } else {
                // Move is illegal - provide specific reason
                return explainWhyMoveIsIllegal(from, to, piece, legalMoves.toList())
            }
            
        } catch (e: Exception) {
            Log.e("ChessEngine", "Error validating move with explanation: ${e.message}")
            return MoveValidationResult(
                isValid = false,
                reason = "Validation error",
                educationalTip = "An error occurred while checking this move. Please try again."
            )
        }
    }
    
    /**
     * 🎓 MOVE VALIDATION RESULT WITH EDUCATIONAL FEEDBACK
     */
    data class MoveValidationResult(
        val isValid: Boolean,
        val reason: String,
        val educationalTip: String
    )
    
    /**
     * 🔍 ANALYZE MOVE TYPE FOR EDUCATIONAL FEEDBACK
     */
    private fun analyzeMoveType(move: Move): String {
        val moveStr = move.toString()
        return when {
            moveStr.contains("O-O-O") -> "Queenside castling - A special move to safeguard your king!"
            moveStr.contains("O-O") -> "Kingside castling - Moving your king to safety!"
            moveStr.contains("x") -> "Capture move - You're taking an opponent's piece!"
            moveStr.contains("=") -> "Pawn promotion - Your pawn reaches the end and becomes a stronger piece!"
            moveStr.contains("+") -> "Check - You're attacking the opponent's king!"
            moveStr.contains("#") -> "Checkmate - Game winning move!"
            move.from.file != move.to.file && board.getPiece(move.from)?.pieceType.toString().contains("PAWN") && board.getPiece(move.to) == null -> "En passant capture - A special pawn capture!"
            else -> "Standard move - Following proper chess piece movement rules."
        }
    }
    
    /**
     * 🚫 EXPLAIN WHY MOVE IS ILLEGAL
     */
    private fun explainWhyMoveIsIllegal(from: Square, to: Square, piece: com.github.bhlangonijr.chesslib.Piece, legalMoves: List<Move>): MoveValidationResult {
        val pieceType = piece.pieceType.toString().lowercase()
        val pieceColor = if (piece.pieceSide == Side.WHITE) "white" else "black"
        
        // Check if piece can move to any square (is it pinned?)
        val pieceLegalMoves = legalMoves.filter { it.from == from }
        if (pieceLegalMoves.isEmpty()) {
            return MoveValidationResult(
                isValid = false,
                reason = "Piece cannot move",
                educationalTip = "This $pieceColor $pieceType cannot move because it would expose your king to check (pinned piece)."
            )
        }
        
        // Check if the destination square is reachable by this piece type
        val pieceCanReachSquare = canPieceReachSquare(piece.pieceType, from, to)
        if (!pieceCanReachSquare) {
            return MoveValidationResult(
                isValid = false,
                reason = "Invalid piece movement",
                educationalTip = getPieceMovementRule(piece.pieceType)
            )
        }
        
        // Check if path is blocked
        if (isPathBlocked(from, to, piece.pieceType)) {
            return MoveValidationResult(
                isValid = false,
                reason = "Path blocked",
                educationalTip = "The path between $from and $to is blocked by other pieces. Most pieces cannot jump over other pieces (except knights)."
            )
        }
        
        // Check if trying to capture own piece
        val targetPiece = board.getPiece(to)
        if (targetPiece != null && targetPiece.pieceSide == piece.pieceSide) {
            return MoveValidationResult(
                isValid = false,
                reason = "Cannot capture own piece",
                educationalTip = "You cannot capture your own pieces. You can only capture opponent's pieces."
            )
        }
        
        // Check if move would leave king in check
        return MoveValidationResult(
            isValid = false,
            reason = "Would expose king to check",
            educationalTip = "This move would leave your king in check, which is not allowed. You must protect your king."
        )
    }
    
    /**
     * 📚 GET PIECE MOVEMENT RULES EXPLANATION
     */
    private fun getPieceMovementRule(pieceType: com.github.bhlangonijr.chesslib.PieceType): String {
        return when (pieceType) {
            com.github.bhlangonijr.chesslib.PieceType.PAWN -> "Pawns move forward one square, or two squares from their starting position. They capture diagonally."
            com.github.bhlangonijr.chesslib.PieceType.ROOK -> "Rooks move horizontally or vertically any number of squares."
            com.github.bhlangonijr.chesslib.PieceType.KNIGHT -> "Knights move in an L-shape: 2 squares in one direction and 1 square perpendicular."
            com.github.bhlangonijr.chesslib.PieceType.BISHOP -> "Bishops move diagonally any number of squares."
            com.github.bhlangonijr.chesslib.PieceType.QUEEN -> "Queens move horizontally, vertically, or diagonally any number of squares."
            com.github.bhlangonijr.chesslib.PieceType.KING -> "Kings move one square in any direction (horizontally, vertically, or diagonally)."
            else -> "Each piece has specific movement rules in chess."
        }
    }
    
    /**
     * 🎯 CHECK IF PIECE CAN THEORETICALLY REACH SQUARE
     */
    private fun canPieceReachSquare(pieceType: com.github.bhlangonijr.chesslib.PieceType, from: Square, to: Square): Boolean {
        val fromFile = from.file.ordinal
        val fromRank = from.rank.ordinal
        val toFile = to.file.ordinal
        val toRank = to.rank.ordinal
        
        val fileDiff = Math.abs(toFile - fromFile)
        val rankDiff = Math.abs(toRank - fromRank)
        
        return when (pieceType) {
            com.github.bhlangonijr.chesslib.PieceType.PAWN -> {
                // Simplified - actual pawn logic is more complex
                fileDiff <= 1 && rankDiff <= 2
            }
            com.github.bhlangonijr.chesslib.PieceType.ROOK -> {
                fileDiff == 0 || rankDiff == 0
            }
            com.github.bhlangonijr.chesslib.PieceType.KNIGHT -> {
                (fileDiff == 2 && rankDiff == 1) || (fileDiff == 1 && rankDiff == 2)
            }
            com.github.bhlangonijr.chesslib.PieceType.BISHOP -> {
                fileDiff == rankDiff
            }
            com.github.bhlangonijr.chesslib.PieceType.QUEEN -> {
                fileDiff == 0 || rankDiff == 0 || fileDiff == rankDiff
            }
            com.github.bhlangonijr.chesslib.PieceType.KING -> {
                fileDiff <= 1 && rankDiff <= 1
            }
            else -> false
        }
    }
    
    /**
     * 🚧 CHECK IF PATH IS BLOCKED
     */
    private fun isPathBlocked(from: Square, to: Square, pieceType: com.github.bhlangonijr.chesslib.PieceType): Boolean {
        // Knights can jump, so path is never blocked for them
        if (pieceType == com.github.bhlangonijr.chesslib.PieceType.KNIGHT) {
            return false
        }
        
        // For other pieces, check if any squares between from and to are occupied
        // This is a simplified check - the chess library handles the detailed logic
        return false // Let the chess library handle the complex path checking
    }
    
    /**
     * 🔍 POSITION ANALYSIS
     * Determines if the game should end and why
     */
    private fun analyzePosition(): GameResult {
        val legalMoves = board.legalMoves()
        val isInCheck = board.isKingAttacked()
        
        // Check for checkmate or stalemate
        if (legalMoves.isEmpty()) {
            return if (isInCheck) {
                // Checkmate - the side NOT to move wins
                if (board.sideToMove == Side.WHITE) {
                    GameResult.BLACK_WINS_CHECKMATE
                } else {
                    GameResult.WHITE_WINS_CHECKMATE
                }
            } else {
                // Stalemate
                GameResult.STALEMATE
            }
        }
        
        // Check for draw conditions
        if (board.halfMoveCounter >= 100) { // 50-move rule (100 half-moves)
            return GameResult.DRAW_50_MOVE_RULE
        }
        
        if (hasInsufficientMaterial()) {
            return GameResult.DRAW_INSUFFICIENT_MATERIAL
        }
        
        if (hasThreefoldRepetition()) {
            return GameResult.DRAW_THREEFOLD_REPETITION
        }
        
        return GameResult.IN_PROGRESS
    }
    
    /**
     * 🧩 INSUFFICIENT MATERIAL CHECK
     * Determines if neither side can force checkmate
     */
    private fun hasInsufficientMaterial(): Boolean {
        try {
            // Count pieces on the board manually since pieceLocation API may not be available
            var whitePawns = 0
            var whiteKnights = 0
            var whiteBishops = 0
            var whiteRooks = 0
            var whiteQueens = 0
            
            var blackPawns = 0
            var blackKnights = 0
            var blackBishops = 0
            var blackRooks = 0
            var blackQueens = 0
            
            // Scan all squares for pieces
            for (square in Square.values()) {
                val piece = board.getPiece(square)
                if (piece != null) {
                    when (piece.pieceType) {
                        com.github.bhlangonijr.chesslib.PieceType.PAWN -> {
                            if (piece.pieceSide == Side.WHITE) whitePawns++ else blackPawns++
                        }
                        com.github.bhlangonijr.chesslib.PieceType.KNIGHT -> {
                            if (piece.pieceSide == Side.WHITE) whiteKnights++ else blackKnights++
                        }
                        com.github.bhlangonijr.chesslib.PieceType.BISHOP -> {
                            if (piece.pieceSide == Side.WHITE) whiteBishops++ else blackBishops++
                        }
                        com.github.bhlangonijr.chesslib.PieceType.ROOK -> {
                            if (piece.pieceSide == Side.WHITE) whiteRooks++ else blackRooks++
                        }
                        com.github.bhlangonijr.chesslib.PieceType.QUEEN -> {
                            if (piece.pieceSide == Side.WHITE) whiteQueens++ else blackQueens++
                        }
                        else -> {} // King doesn't count for material
                    }
                }
            }
            
            val whiteMaterial = whitePawns + whiteKnights * 3 + whiteBishops * 3 + whiteRooks * 5 + whiteQueens * 9
            val blackMaterial = blackPawns + blackKnights * 3 + blackBishops * 3 + blackRooks * 5 + blackQueens * 9
            
            Log.d("ChessEngine", "🧩 Material check - White: $whiteMaterial, Black: $blackMaterial")
            
            // Known insufficient material combinations:
            // King vs King
            if (whiteMaterial == 0 && blackMaterial == 0) {
                Log.d("ChessEngine", "🧩 Insufficient material: King vs King")
                return true
            }
            
            // King + Knight vs King or King + Bishop vs King
            if ((whiteMaterial == 3 && blackMaterial == 0) ||
                (blackMaterial == 3 && whiteMaterial == 0)) {
                Log.d("ChessEngine", "🧩 Insufficient material: King + minor piece vs King")
                return true
            }
            
            // King + Knight vs King + Knight (very rare but possible draw)
            if (whiteKnights == 1 && whiteMaterial == 3 &&
                blackKnights == 1 && blackMaterial == 3) {
                Log.d("ChessEngine", "🧩 Insufficient material: King + Knight vs King + Knight")
                return true
            }
            
            return false
            
        } catch (e: Exception) {
            Log.e("ChessEngine", "Error checking insufficient material: ${e.message}")
            return false
        }
    }
    
    /**
     *  THREEFOLD REPETITION CHECK
     * Simplified version - checks for exact position repetition
     */
    private fun hasThreefoldRepetition(): Boolean {
        if (moveHistory.size < 8) return false // Need at least 4 moves each side
        
        val currentFEN = board.fen.split(" ").take(3).joinToString(" ") // Position only
        var repetitions = 1
        
        // Check last positions for repetitions
        val tempBoard = Board()
        tempBoard.loadFromFen(getStartingFEN())
        
        for (i in moveHistory.indices) {
            tempBoard.doMove(moveHistory[i])
            val fen = tempBoard.fen.split(" ").take(3).joinToString(" ")
            if (fen == currentFEN) {
                repetitions++
                if (repetitions >= 3) return true
            }
        }
        
        return false
    }
    
    /**
     * 🔍 MOVE FINDING UTILITY
     * Finds the correct Move object from legal moves list
     */
    private fun findMove(legalMoves: List<Move>, from: Square, to: Square, promotionPiece: String?): Move? {
        for (move in legalMoves) {
            if (move.from == from && move.to == to) {
                // Handle promotion
                if (promotionPiece != null && move.promotion.pieceType.name != "NONE") {
                    val expectedPromotion = when (promotionPiece.uppercase()) {
                        "Q" -> "QUEEN"
                        "R" -> "ROOK"
                        "B" -> "BISHOP"
                        "N" -> "KNIGHT"
                        else -> null
                    }
                    if (expectedPromotion != null && 
                        move.promotion.pieceType.name.contains(expectedPromotion)) {
                        return move
                    }
                } else if (promotionPiece == null && move.promotion.pieceType.name == "NONE") {
                    return move
                }
            }
        }
        return null
    }
    
    /**
     * 📝 MOVE RESULT CREATION
     * Creates comprehensive MoveResult objects
     */
    private fun createMoveResult(success: Boolean, move: Move?, errorMessage: String?): MoveResult {
        val legalMoves = if (!gameEnded) board.legalMoves().toList() else emptyList()
        
        return MoveResult(
            success = success,
            move = move,
            gameResult = gameResult,
            isCheck = board.isKingAttacked(),
            isCapture = move?.let { 
                // Simple capture detection - could be enhanced
                false // For now, simplify this
            } ?: false,
            isPromotion = move?.let { it.promotion.pieceType.name != "NONE" } ?: false,
            isCastling = move?.let { 
                val piece = board.getPiece(move.to)
                piece.pieceType.name.contains("KING") && kotlin.math.abs(move.from.ordinal - move.to.ordinal) == 2
            } ?: false,
            isEnPassant = move?.let { 
                // Simple en passant detection
                false // For now, simplify this
            } ?: false,
            errorMessage = errorMessage,
            boardFEN = board.fen,
            legalMoves = legalMoves
        )
    }
    
    /**
     * 🎮 GAME MANAGEMENT METHODS
     */
    
    fun resetGame() {
        board.loadFromFen(getStartingFEN())
        gameStarted = false
        gameEnded = false
        gameResult = GameResult.IN_PROGRESS
        moveHistory.clear()
        lastMoveTime = System.currentTimeMillis()
        Log.d("ChessEngine", "🔄 Game reset to starting position")
    }
    
    fun loadPosition(fen: String): Boolean {
        return try {
            board.loadFromFen(fen)
            gameStarted = true
            gameEnded = false
            gameResult = GameResult.IN_PROGRESS
            moveHistory.clear()
            Log.d("ChessEngine", "📋 Position loaded from FEN: $fen")
            true
        } catch (e: Exception) {
            Log.e("ChessEngine", "Failed to load FEN: ${e.message}")
            false
        }
    }
    
    fun undoMove(): Boolean {
        return try {
            if (moveHistory.isNotEmpty()) {
                board.undoMove()
                moveHistory.removeLastOrNull()
                gameEnded = false
                gameResult = GameResult.IN_PROGRESS
                Log.d("ChessEngine", "↩️ Move undone")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("ChessEngine", "Failed to undo move: ${e.message}")
            false
        }
    }
    
    /**
     * 📊 GAME STATE QUERIES
     */
    
    fun isGameEnded(): Boolean = gameEnded
    fun getGameResult(): GameResult = gameResult
    fun isInCheck(): Boolean = board.isKingAttacked()
    fun getCurrentPlayer(): Side = board.sideToMove
    fun getBoardFEN(): String = board.fen
    fun getLegalMoves(): List<Move> = board.legalMoves().toList()
    fun getMoveHistory(): List<Move> = moveHistory.toList()
    fun getLastMoveTime(): Long = lastMoveTime
    
    /**
     * 🎯 ENHANCED GAME STATE CHECKS
     */
    
    fun getCurrentTurn(): String = if (board.sideToMove == Side.WHITE) "WHITE" else "BLACK"
    fun isCheck(): Boolean = board.isKingAttacked()
    
    fun isCheckmate(): Boolean {
        return board.legalMoves().isEmpty() && board.isKingAttacked()
    }
    
    fun isStalemate(): Boolean {
        return board.legalMoves().isEmpty() && !board.isKingAttacked()
    }
    
    fun isDraw(): Boolean {
        return gameResult in listOf(
            GameResult.STALEMATE,
            GameResult.DRAW_50_MOVE_RULE,
            GameResult.DRAW_INSUFFICIENT_MATERIAL,
            GameResult.DRAW_THREEFOLD_REPETITION
        )
    }
    
    fun canPlayerWin(side: Side): Boolean {
        // Simple material assessment - simplified for now
        return true // Assume player can win unless proven otherwise
    }
    
    /**
     * 🎧 EVENT LISTENERS
     */
    
    fun setOnGameEndListener(listener: (GameResult) -> Unit) {
        onGameEndListener = listener
    }
    
    fun setOnMoveListener(listener: (MoveResult) -> Unit) {
        onMoveListener = listener
    }
    
    fun setOnCheckListener(listener: (Side) -> Unit) {
        onCheckListener = listener
    }
    
    /**
     * 🔧 UTILITY METHODS
     */
    
    /**
     * Get the current player's turn
     * @return true if it's White's turn, false if it's Black's turn
     */
    fun isWhiteTurn(): Boolean {
        return board.sideToMove == Side.WHITE
    }
    
    /**
     * Get the current side to move
     */
    fun getCurrentSide(): Side {
        return board.sideToMove
    }
    
    private fun parseSquare(square: String): Square? {
        return try {
            Square.valueOf(square.uppercase())
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getStartingFEN(): String {
        return "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    }
    
    /**
     * 📈 DEBUGGING AND ANALYSIS
     */
    
    fun printBoard() {
        Log.d("ChessEngine", "📋 Current board position:\n${board}")
    }
    
    fun getPositionAnalysis(): String {
        val analysis = StringBuilder()
        analysis.appendLine("🔍 Position Analysis:")
        analysis.appendLine("Current player: ${board.sideToMove}")
        analysis.appendLine("In check: ${board.isKingAttacked()}")
        analysis.appendLine("Legal moves: ${board.legalMoves().size}")
        analysis.appendLine("Half-move clock: ${board.halfMoveCounter}")
        analysis.appendLine("Full-move number: ${board.moveCounter}")
        analysis.appendLine("Game result: ${gameResult.description}")
        analysis.appendLine("FEN: ${board.fen}")
        
        return analysis.toString()
    }
    
    /**
     * 📚 COMPREHENSIVE CHESS RULES REFERENCE
     * Provides detailed explanation of all chess rules for educational purposes
     */
    fun getChessRulesReference(): String {
        return buildString {
            appendLine("♟️ COMPREHENSIVE CHESS RULES REFERENCE")
            appendLine("==================================================")
            appendLine()
            
            appendLine("🎯 OBJECTIVE:")
            appendLine("Checkmate the opponent's king. The king is in 'check' when attacked,")
            appendLine("and 'checkmate' when in check with no legal moves to escape.")
            appendLine()
            
            appendLine("👑 PIECE MOVEMENTS:")
            appendLine("• KING: One square in any direction")
            appendLine("• QUEEN: Any number of squares horizontally, vertically, or diagonally")
            appendLine("• ROOK: Any number of squares horizontally or vertically")
            appendLine("• BISHOP: Any number of squares diagonally")
            appendLine("• KNIGHT: L-shape: 2 squares in one direction, then 1 perpendicular")
            appendLine("• PAWN: Forward one square (two from starting position), captures diagonally")
            appendLine()
            
            appendLine("🎯 SPECIAL MOVES:")
            appendLine("• CASTLING: King and rook special move for safety")
            appendLine("  - King moves 2 squares toward rook, rook jumps over")
            appendLine("  - Requirements: Neither piece has moved, no pieces between them,")
            appendLine("    king not in check, and doesn't pass through check")
            appendLine()
            appendLine("• EN PASSANT: Special pawn capture")
            appendLine("  - When opponent pawn moves 2 squares, you can capture 'in passing'")
            appendLine("  - Must be done immediately on the next move")
            appendLine()
            appendLine("• PAWN PROMOTION: When pawn reaches end of board")
            appendLine("  - Must promote to Queen, Rook, Bishop, or Knight")
            appendLine("  - Usually choose Queen (most powerful piece)")
            appendLine()
            
            appendLine("⚖️ FUNDAMENTAL RULES:")
            appendLine("• You must move when it's your turn")
            appendLine("• Cannot move into check")
            appendLine("• Must get out of check if in check")
            appendLine("• Cannot move piece if it would expose your king to check")
            appendLine("• Cannot capture your own pieces")
            appendLine("• White always moves first")
            appendLine()
            
            appendLine("🏁 GAME ENDING CONDITIONS:")
            appendLine("• CHECKMATE: King in check with no legal moves (you lose)")
            appendLine("• STALEMATE: No legal moves but not in check (draw)")
            appendLine("• INSUFFICIENT MATERIAL: Neither side can checkmate (draw)")
            appendLine("• 50-MOVE RULE: 50 moves without pawn move or capture (draw)")
            appendLine("• THREEFOLD REPETITION: Same position 3 times (draw)")
            appendLine("• MUTUAL AGREEMENT: Both players agree to draw")
            appendLine()
            
            appendLine("💡 CHESS TIPS:")
            appendLine("• Control the center squares (e4, d4, e5, d5)")
            appendLine("• Develop pieces before attacking")
            appendLine("• Castle early for king safety")
            appendLine("• Don't move the same piece twice in opening")
            appendLine("• Think before you move - chess rewards careful planning")
        }
    }
    
    /**
     * 🎓 GET SPECIFIC RULE EXPLANATION
     */
    fun getRuleExplanation(ruleType: String): String {
        return when (ruleType.lowercase()) {
            "castling" -> """
                🏰 CASTLING RULES:
                • King moves 2 squares toward rook
                • Rook moves to square king crossed
                • Requirements:
                  - Neither king nor rook has moved
                  - No pieces between king and rook
                  - King not currently in check
                  - King doesn't pass through or land on attacked square
                • Notation: O-O (kingside), O-O-O (queenside)
            """.trimIndent()
            
            "en passant" -> """
                🎯 EN PASSANT RULES:
                • Special pawn capture move
                • Opponent pawn moves 2 squares from starting position
                • Your pawn is on 5th rank (white) or 4th rank (black)
                • Capture as if opponent pawn moved only 1 square
                • Must be done immediately on next move
                • Captured pawn is removed from board
            """.trimIndent()
            
            "promotion" -> """
                👑 PAWN PROMOTION RULES:
                • When pawn reaches opposite end of board
                • Must promote to Queen, Rook, Bishop, or Knight
                • Cannot remain a pawn or become a King
                • Usually promote to Queen (most powerful)
                • Can have multiple Queens on board
                • Promotion is mandatory, not optional
            """.trimIndent()
            
            "check" -> """
                ⚠️ CHECK RULES:
                • King is under attack by opponent piece
                • Must get out of check immediately
                • Three ways to escape:
                  1. Move king to safe square
                  2. Block the attack with another piece
                  3. Capture the attacking piece
                • Cannot make any move that leaves king in check
            """.trimIndent()
            
            "checkmate" -> """
                💀 CHECKMATE RULES:
                • King is in check AND has no legal moves to escape
                • Game ends immediately - attacking side wins
                • Cannot be blocked, captured, or escaped
                • Most valuable achievement in chess
            """.trimIndent()
            
            else -> "Rule type not recognized. Try: castling, en passant, promotion, check, checkmate"
        }
    }
    
    /**
     * 🤖 AI MOVE GENERATION
     * Generates best move for current player using simple evaluation
     */
    fun generateAIMove(): Move? {
        if (gameEnded) {
            Log.w("ChessEngine", "🤖 AI cannot move - game has ended")
            return null
        }
        
        val legalMoves = getLegalMoves()
        if (legalMoves.isEmpty()) {
            Log.w("ChessEngine", "🤖 AI has no legal moves available")
            return null
        }
        
        Log.d("ChessEngine", "🤖 AI analyzing ${legalMoves.size} possible moves...")
        
        return try {
            selectBestMove(legalMoves)
        } catch (e: Exception) {
            Log.e("ChessEngine", "🤖 AI move generation failed: ${e.message}")
            // Fallback to random move
            legalMoves.random()
        }
    }
    
    /**
     * 🧠 AI MOVE SELECTION LOGIC
     * Simple but effective move evaluation
     */
    private fun selectBestMove(legalMoves: List<Move>): Move {
        val currentSide = board.sideToMove
        Log.d("ChessEngine", "🤖 AI ($currentSide) selecting best move from ${legalMoves.size} options")
        
        // Priority-based move selection
        var bestMoves = mutableListOf<Move>()
        var bestScore = Int.MIN_VALUE
        
        for (move in legalMoves) {
            val score = evaluateMove(move)
            
            when {
                score > bestScore -> {
                    bestScore = score
                    bestMoves.clear()
                    bestMoves.add(move)
                }
                score == bestScore -> {
                    bestMoves.add(move)
                }
            }
        }
        
        val selectedMove = bestMoves.random()
        Log.d("ChessEngine", "🤖 AI selected move: ${selectedMove.from}-${selectedMove.to} (score: $bestScore)")
        
        return selectedMove
    }
    
    /**
     * 📊 MOVE EVALUATION FUNCTION
     * Scores moves based on chess principles
     */
    private fun evaluateMove(move: Move): Int {
        var score = 0
        
        // Save current position
        val originalFEN = board.fen
        
        try {
            // Try the move to see its effects
            board.doMove(move)
            
            // 1. Checkmate is highest priority
            if (board.isMated) {
                score += 10000
                Log.d("ChessEngine", "🤖 Move ${move.from}-${move.to}: CHECKMATE! (+10000)")
            }
            // 2. Check is very good
            else if (board.isKingAttacked()) {
                score += 500
                Log.d("ChessEngine", "🤖 Move ${move.from}-${move.to}: Check (+500)")
            }
            
            // 3. Captures are valuable (simplified detection)
            if (move.toString().contains("x")) {
                score += 300 // Generic capture value
                Log.d("ChessEngine", "🤖 Move ${move.from}-${move.to}: Capture! (+300)")
            }
            
            // 4. Center control is good
            if (move.to.toString().matches(Regex("[d-e][4-5]"))) {
                score += 50
                Log.d("ChessEngine", "🤖 Move ${move.from}-${move.to}: Center control (+50)")
            }
            
            // 5. Castle early
            if (move.toString().contains("O-O")) {
                score += 100
                Log.d("ChessEngine", "🤖 Move ${move.from}-${move.to}: Castling (+100)")
            }
            
            // 6. Develop pieces in opening (simplified)
            if (board.moveCounter <= 10) {
                score += 30
                Log.d("ChessEngine", "🤖 Move ${move.from}-${move.to}: Early development (+30)")
            }
            
            // Restore original position
            board.loadFromFen(originalFEN)
            
        } catch (e: Exception) {
            Log.e("ChessEngine", "🤖 Error evaluating move ${move.from}-${move.to}: ${e.message}")
            // Restore position and give neutral score
            try {
                board.loadFromFen(originalFEN)
            } catch (restoreError: Exception) {
                Log.e("ChessEngine", "🤖 Failed to restore board position: ${restoreError.message}")
            }
            score = 0
        }
        
        return score
    }
    
    /**
     * ♟️ PIECE VALUE TABLE
     */
    private fun getPieceValue(pieceType: String): Int {
        return when (pieceType.uppercase()) {
            "PAWN" -> 100
            "KNIGHT" -> 300
            "BISHOP" -> 300
            "ROOK" -> 500
            "QUEEN" -> 900
            "KING" -> 0 // King can't be captured
            else -> 50 // Unknown piece
        }
    }
}