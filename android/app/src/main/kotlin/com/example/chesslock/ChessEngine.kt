package com.example.chesslock

import kotlin.math.abs

/**
 * Professional Chess Engine with Full Rules Validation
 * Supports: All piece movements, castling, en passant, pawn promotion, check/checkmate detection
 */
class ChessEngine {
    
    data class Position(val row: Int, val col: Int) {
        fun isValid() = row in 0..7 && col in 0..7
        operator fun plus(other: Position) = Position(row + other.row, col + other.col)
    }
    
    data class Move(val from: Position, val to: Position, val promotion: String? = null)
    
    enum class PieceType { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING }
    enum class Color { WHITE, BLACK }
    
    data class Piece(val type: PieceType, val color: Color) {
        fun toCode() = when(color) {
            Color.WHITE -> "w" + type.name[0].lowercaseChar()
            Color.BLACK -> "b" + type.name[0].lowercaseChar()
        }
        
        companion object {
            fun fromCode(code: String): Piece? {
                if (code.length != 2) return null
                val color = if (code[0] == 'w') Color.WHITE else Color.BLACK
                val type = when(code[1]) {
                    'p' -> PieceType.PAWN
                    'n' -> PieceType.KNIGHT
                    'b' -> PieceType.BISHOP
                    'r' -> PieceType.ROOK
                    'q' -> PieceType.QUEEN
                    'k' -> PieceType.KING
                    else -> return null
                }
                return Piece(type, color)
            }
        }
    }
    
    var board = Array(8) { row -> Array(8) { col -> initialPosition(row, col) } }
    var currentPlayer = Color.WHITE
    var moveHistory = mutableListOf<Move>()
    
    // Castling rights
    var whiteCanCastleKingside = true
    var whiteCanCastleQueenside = true
    var blackCanCastleKingside = true
    var blackCanCastleQueenside = true
    
    // En passant
    var enPassantTarget: Position? = null
    
    private fun initialPosition(row: Int, col: Int): Piece? {
        return when(row) {
            0 -> when(col) {
                0, 7 -> Piece(PieceType.ROOK, Color.BLACK)
                1, 6 -> Piece(PieceType.KNIGHT, Color.BLACK)
                2, 5 -> Piece(PieceType.BISHOP, Color.BLACK)
                3 -> Piece(PieceType.QUEEN, Color.BLACK)
                4 -> Piece(PieceType.KING, Color.BLACK)
                else -> null
            }
            1 -> Piece(PieceType.PAWN, Color.BLACK)
            6 -> Piece(PieceType.PAWN, Color.WHITE)
            7 -> when(col) {
                0, 7 -> Piece(PieceType.ROOK, Color.WHITE)
                1, 6 -> Piece(PieceType.KNIGHT, Color.WHITE)
                2, 5 -> Piece(PieceType.BISHOP, Color.WHITE)
                3 -> Piece(PieceType.QUEEN, Color.WHITE)
                4 -> Piece(PieceType.KING, Color.WHITE)
                else -> null
            }
            else -> null
        }
    }
    
    fun reset() {
        board = Array(8) { row -> Array(8) { col -> initialPosition(row, col) } }
        currentPlayer = Color.WHITE
        moveHistory.clear()
        whiteCanCastleKingside = true
        whiteCanCastleQueenside = true
        blackCanCastleKingside = true
        blackCanCastleQueenside = true
        enPassantTarget = null
    }
    
    /**
     * Load a position from FEN string
     */
    fun loadFromFEN(fen: String) {
        val fenData = ChessPuzzles.parseFEN(fen)
        board = ChessPuzzles.fenToBoard(fenData.position)
        currentPlayer = fenData.activeColor
        ChessPuzzles.applyCastlingRights(this, fenData.castling)
        
        // Parse en passant
        enPassantTarget = if (fenData.enPassant != "-") {
            val file = fenData.enPassant[0] - 'a'
            val rank = 8 - (fenData.enPassant[1] - '0')
            Position(rank, file)
        } else null
        
        moveHistory.clear()
    }
    
    /**
     * Load a puzzle by ID
     */
    fun loadPuzzle(puzzleId: Int): ChessPuzzles.Puzzle? {
        val puzzle = ChessPuzzles.getPuzzleById(puzzleId)
        puzzle?.let {
            loadFromFEN(it.fen)
        }
        return puzzle
    }
    
    /**
     * Check if a move is legal according to chess rules
     */
    fun isLegalMove(move: Move): Boolean {
        val piece = board[move.from.row][move.from.col] ?: return false
        
        // Must move your own piece
        if (piece.color != currentPlayer) return false
        
        // Can't capture own piece
        val targetPiece = board[move.to.row][move.to.col]
        if (targetPiece?.color == currentPlayer) return false
        
        // Check piece-specific movement rules
        if (!isPieceMoveValid(piece, move)) return false
        
        // Can't move into check
        if (wouldBeInCheck(move)) return false
        
        return true
    }
    
    private fun isPieceMoveValid(piece: Piece, move: Move): Boolean {
        return when(piece.type) {
            PieceType.PAWN -> isValidPawnMove(move, piece.color)
            PieceType.KNIGHT -> isValidKnightMove(move)
            PieceType.BISHOP -> isValidBishopMove(move)
            PieceType.ROOK -> isValidRookMove(move)
            PieceType.QUEEN -> isValidQueenMove(move)
            PieceType.KING -> isValidKingMove(move, piece.color)
        }
    }
    
    private fun isValidPawnMove(move: Move, color: Color): Boolean {
        val direction = if (color == Color.WHITE) -1 else 1
        val startRow = if (color == Color.WHITE) 6 else 1
        val rowDiff = move.to.row - move.from.row
        val colDiff = abs(move.to.col - move.from.col)
        
        // Forward one square
        if (colDiff == 0 && rowDiff == direction) {
            return board[move.to.row][move.to.col] == null
        }
        
        // Forward two squares from starting position
        if (colDiff == 0 && rowDiff == 2 * direction && move.from.row == startRow) {
            val middleRow = move.from.row + direction
            return board[middleRow][move.from.col] == null && board[move.to.row][move.to.col] == null
        }
        
        // Capture diagonally
        if (colDiff == 1 && rowDiff == direction) {
            val targetPiece = board[move.to.row][move.to.col]
            if (targetPiece != null && targetPiece.color != color) return true
            
            // En passant
            if (move.to == enPassantTarget) return true
        }
        
        return false
    }
    
    private fun isValidKnightMove(move: Move): Boolean {
        val rowDiff = abs(move.to.row - move.from.row)
        val colDiff = abs(move.to.col - move.from.col)
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2)
    }
    
    private fun isValidBishopMove(move: Move): Boolean {
        val rowDiff = abs(move.to.row - move.from.row)
        val colDiff = abs(move.to.col - move.from.col)
        if (rowDiff != colDiff) return false
        return isPathClear(move)
    }
    
    private fun isValidRookMove(move: Move): Boolean {
        if (move.from.row != move.to.row && move.from.col != move.to.col) return false
        return isPathClear(move)
    }
    
    private fun isValidQueenMove(move: Move): Boolean {
        return (isValidBishopMove(move) || isValidRookMove(move))
    }
    
    private fun isValidKingMove(move: Move, color: Color): Boolean {
        val rowDiff = abs(move.to.row - move.from.row)
        val colDiff = abs(move.to.col - move.from.col)
        
        // Normal king move (one square)
        if (rowDiff <= 1 && colDiff <= 1) return true
        
        // Castling
        if (rowDiff == 0 && colDiff == 2) {
            return isValidCastling(move, color)
        }
        
        return false
    }
    
    private fun isValidCastling(move: Move, color: Color): Boolean {
        val row = if (color == Color.WHITE) 7 else 0
        if (move.from.row != row || move.from.col != 4) return false
        
        // Check castling rights
        val kingside = move.to.col == 6
        val hasRight = when {
            color == Color.WHITE && kingside -> whiteCanCastleKingside
            color == Color.WHITE && !kingside -> whiteCanCastleQueenside
            color == Color.BLACK && kingside -> blackCanCastleKingside
            else -> blackCanCastleQueenside
        }
        if (!hasRight) return false
        
        // Check path is clear
        val colRange = if (kingside) 5..6 else 1..3
        for (col in colRange) {
            if (board[row][col] != null) return false
        }
        
        // Check king is not in check and doesn't pass through check
        if (isPositionUnderAttack(Position(row, 4), color)) return false
        if (isPositionUnderAttack(Position(row, if (kingside) 5 else 3), color)) return false
        
        return true
    }
    
    private fun isPathClear(move: Move): Boolean {
        val rowStep = when {
            move.to.row > move.from.row -> 1
            move.to.row < move.from.row -> -1
            else -> 0
        }
        val colStep = when {
            move.to.col > move.from.col -> 1
            move.to.col < move.from.col -> -1
            else -> 0
        }
        
        var pos = Position(move.from.row + rowStep, move.from.col + colStep)
        while (pos != move.to) {
            if (board[pos.row][pos.col] != null) return false
            pos = Position(pos.row + rowStep, pos.col + colStep)
        }
        return true
    }
    
    private fun wouldBeInCheck(move: Move): Boolean {
        // Make temporary move
        val piece = board[move.from.row][move.from.col]
        val captured = board[move.to.row][move.to.col]
        board[move.to.row][move.to.col] = piece
        board[move.from.row][move.from.col] = null
        
        // Find king position
        val kingPos = findKing(currentPlayer)
        val inCheck = kingPos?.let { isPositionUnderAttack(it, currentPlayer) } ?: false
        
        // Undo move
        board[move.from.row][move.from.col] = piece
        board[move.to.row][move.to.col] = captured
        
        return inCheck
    }
    
    private fun findKing(color: Color): Position? {
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board[row][col]
                if (piece?.type == PieceType.KING && piece.color == color) {
                    return Position(row, col)
                }
            }
        }
        return null
    }
    
    fun isInCheck(color: Color): Boolean {
        val kingPos = findKing(color) ?: return false
        return isPositionUnderAttack(kingPos, color)
    }
    
    private fun isPositionUnderAttack(pos: Position, byColor: Color): Boolean {
        val attackerColor = if (byColor == Color.WHITE) Color.BLACK else Color.WHITE
        
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board[row][col]
                if (piece?.color == attackerColor) {
                    val move = Move(Position(row, col), pos)
                    if (isPieceMoveValid(piece, move)) {
                        return true
                    }
                }
            }
        }
        return false
    }
    
    /**
     * Execute a move (assumes it's already validated)
     */
    fun makeMove(move: Move): Boolean {
        if (!isLegalMove(move)) return false
        
        val piece = board[move.from.row][move.from.col]!!
        
        // Handle castling
        if (piece.type == PieceType.KING && abs(move.to.col - move.from.col) == 2) {
            val kingside = move.to.col == 6
            val rookFromCol = if (kingside) 7 else 0
            val rookToCol = if (kingside) 5 else 3
            val row = move.from.row
            
            board[row][rookToCol] = board[row][rookFromCol]
            board[row][rookFromCol] = null
        }
        
        // Handle en passant capture
        if (piece.type == PieceType.PAWN && move.to == enPassantTarget) {
            val captureRow = move.from.row
            board[captureRow][move.to.col] = null
        }
        
        // Set en passant target for next turn
        enPassantTarget = if (piece.type == PieceType.PAWN && abs(move.to.row - move.from.row) == 2) {
            Position((move.from.row + move.to.row) / 2, move.from.col)
        } else null
        
        // Move the piece
        board[move.to.row][move.to.col] = piece
        board[move.from.row][move.from.col] = null
        
        // Handle pawn promotion
        if (piece.type == PieceType.PAWN && (move.to.row == 0 || move.to.row == 7)) {
            val promotionType = when(move.promotion) {
                "q" -> PieceType.QUEEN
                "r" -> PieceType.ROOK
                "b" -> PieceType.BISHOP
                "n" -> PieceType.KNIGHT
                else -> PieceType.QUEEN
            }
            board[move.to.row][move.to.col] = Piece(promotionType, piece.color)
        }
        
        // Update castling rights
        if (piece.type == PieceType.KING) {
            if (piece.color == Color.WHITE) {
                whiteCanCastleKingside = false
                whiteCanCastleQueenside = false
            } else {
                blackCanCastleKingside = false
                blackCanCastleQueenside = false
            }
        }
        if (piece.type == PieceType.ROOK) {
            when {
                move.from == Position(7, 0) -> whiteCanCastleQueenside = false
                move.from == Position(7, 7) -> whiteCanCastleKingside = false
                move.from == Position(0, 0) -> blackCanCastleQueenside = false
                move.from == Position(0, 7) -> blackCanCastleKingside = false
            }
        }
        
        moveHistory.add(move)
        currentPlayer = if (currentPlayer == Color.WHITE) Color.BLACK else Color.WHITE
        return true
    }
    
    fun getAllLegalMoves(color: Color): List<Move> {
        val moves = mutableListOf<Move>()
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board[row][col]
                if (piece?.color == color) {
                    moves.addAll(getLegalMovesForPiece(Position(row, col)))
                }
            }
        }
        return moves
    }
    
    private fun getLegalMovesForPiece(from: Position): List<Move> {
        val moves = mutableListOf<Move>()
        for (row in 0..7) {
            for (col in 0..7) {
                val move = Move(from, Position(row, col))
                if (isLegalMove(move)) {
                    moves.add(move)
                }
            }
        }
        return moves
    }
    
    fun isCheckmate(): Boolean {
        return isInCheck(currentPlayer) && getAllLegalMoves(currentPlayer).isEmpty()
    }
    
    fun isStalemate(): Boolean {
        return !isInCheck(currentPlayer) && getAllLegalMoves(currentPlayer).isEmpty()
    }
}
