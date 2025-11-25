package com.example.chesslock

/**
 * Chess Puzzle Database - 10 Legendary Puzzles
 * Each puzzle includes FEN position, solution, and metadata
 */
object ChessPuzzles {
    
    data class Puzzle(
        val id: Int,
        val name: String,
        val theme: String,
        val difficulty: Difficulty,
        val fen: String,
        val description: String,
        val solution: List<String>, // List of moves in algebraic notation
        val mateIn: Int = 0 // 0 for tactical puzzles, N for mate in N
    )
    
    enum class Difficulty { EASY, MEDIUM, HARD }
    
    val allPuzzles = listOf(
        // 1. Back Rank Mate (Easy - Mate in 1)
        Puzzle(
            id = 1,
            name = "Back Rank Mate",
            theme = "King trapped behind pawns",
            difficulty = Difficulty.EASY,
            fen = "6k1/5ppp/8/8/8/8/5PPP/R5K1 w - - 0 1",
            description = "White to move. Deliver checkmate on the back rank!",
            solution = listOf("Ra8#"),
            mateIn = 1
        ),
        
        // 2. Smothered Mate (Medium - Mate in 2)
        Puzzle(
            id = 2,
            name = "Smothered Mate",
            theme = "Knight checkmates king trapped by own pieces",
            difficulty = Difficulty.MEDIUM,
            fen = "5rk1/5ppp/8/8/8/8/5PPP/4RNK1 w - - 0 1",
            description = "Sacrifice the rook, then deliver smothered mate with knight!",
            solution = listOf("Re8+", "Rxe8", "Nf7#"),
            mateIn = 2
        ),
        
        // 3. Opera Mate (Medium - Mate in 2)
        Puzzle(
            id = 3,
            name = "Opera Mate",
            theme = "Rook and bishops coordinate for mate",
            difficulty = Difficulty.MEDIUM,
            fen = "r4rk1/ppp2ppp/2n5/3q4/3P4/2NB4/PPP2PPP/R2Q1RK1 w - - 0 1",
            description = "Morphy's famous combination! Find the forced mate.",
            solution = listOf("Qxd5+", "Nxd5", "Re8#"),
            mateIn = 2
        ),
        
        // 4. Legal's Mate (Easy)
        Puzzle(
            id = 4,
            name = "Legal's Mate",
            theme = "Queen sacrifice leads to checkmate",
            difficulty = Difficulty.EASY,
            fen = "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 0 1",
            description = "Sacrifice your queen for a beautiful checkmate!",
            solution = listOf("Qxf7+", "Kxf7", "Bc4+"),
            mateIn = 2
        ),
        
        // 5. Arabian Mate (Medium - Mate in 1)
        Puzzle(
            id = 5,
            name = "Arabian Mate",
            theme = "Knight and rook deliver mate in corner",
            difficulty = Difficulty.MEDIUM,
            fen = "5rkN/8/6R1/8/8/8/8/6K1 w - - 0 1",
            description = "Knight controls escape squares, rook delivers mate!",
            solution = listOf("Rg8#"),
            mateIn = 1
        ),
        
        // 6. Anastasia's Mate (Medium)
        Puzzle(
            id = 6,
            name = "Anastasia's Mate",
            theme = "Knight and rook trap king on edge",
            difficulty = Difficulty.MEDIUM,
            fen = "2kr4/ppp5/8/3N4/8/8/PPP5/1K1R4 w - - 0 1",
            description = "Use knight to cut off escape, rook delivers mate!",
            solution = listOf("Rd8#"),
            mateIn = 1
        ),
        
        // 7. Boden's Mate (Hard)
        Puzzle(
            id = 7,
            name = "Boden's Mate",
            theme = "Two bishops deliver criss-cross checkmate",
            difficulty = Difficulty.HARD,
            fen = "r1b1kb1r/pppp1ppp/5n2/4p3/2B1P3/2N5/PPPP1qPP/R1BQ1RK1 b kq - 0 1",
            description = "Black to move. Use both bishops for a stunning mate!",
            solution = listOf("Qxc2", "Bxf7+", "Kh8", "Bg6#"),
            mateIn = 3
        ),
        
        // 8. Greek Gift Sacrifice (Medium)
        Puzzle(
            id = 8,
            name = "Greek Gift",
            theme = "Bishop sacrifice on h7 leads to attack",
            difficulty = Difficulty.MEDIUM,
            fen = "r1bq1rk1/ppp2ppp/2n2n2/3p4/2BP4/2N2N2/PPP2PPP/R1BQ1RK1 w - - 0 1",
            description = "Sacrifice the bishop on h7, then bring queen and knight!",
            solution = listOf("Bxh7+", "Kxh7", "Ng5+", "Kg8", "Qh5"),
            mateIn = 3
        ),
        
        // 9. Zugzwang (Hard)
        Puzzle(
            id = 9,
            name = "Zugzwang",
            theme = "Any move opponent makes loses",
            difficulty = Difficulty.HARD,
            fen = "8/8/8/8/4k3/8/3K4/4Q3 w - - 0 1",
            description = "Find the move that forces black into zugzwang!",
            solution = listOf("Qe2+", "Kf5", "Qf3+"),
            mateIn = 2
        ),
        
        // 10. Damiano's Mate (Easy - Mate in 2)
        Puzzle(
            id = 10,
            name = "Damiano's Mate",
            theme = "Queen sacrifice leads to pawn mate",
            difficulty = Difficulty.EASY,
            fen = "6k1/5p1p/6p1/8/8/6Q1/8/6K1 w - - 0 1",
            description = "Sacrifice the queen for an elegant pawn checkmate!",
            solution = listOf("Qg7+", "fxg7", "h7#"),
            mateIn = 2
        )
    )
    
    /**
     * Get a random puzzle by difficulty
     */
    fun getRandomPuzzle(difficulty: Difficulty? = null): Puzzle {
        val filtered = if (difficulty != null) {
            allPuzzles.filter { it.difficulty == difficulty }
        } else {
            allPuzzles
        }
        return filtered.random()
    }
    
    /**
     * Get puzzle by ID
     */
    fun getPuzzleById(id: Int): Puzzle? {
        return allPuzzles.find { it.id == id }
    }
    
    /**
     * Parse FEN string and set up board
     * FEN format: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
     * Parts: position, activeColor, castling, enPassant, halfmove, fullmove
     */
    fun parseFEN(fen: String): FENPosition {
        val parts = fen.split(" ")
        val position = parts[0]
        val activeColor = if (parts.getOrNull(1) == "b") ChessEngine.Color.BLACK else ChessEngine.Color.WHITE
        val castling = parts.getOrNull(2) ?: "KQkq"
        val enPassant = parts.getOrNull(3) ?: "-"
        
        return FENPosition(position, activeColor, castling, enPassant)
    }
    
    data class FENPosition(
        val position: String,
        val activeColor: ChessEngine.Color,
        val castling: String,
        val enPassant: String
    )
    
    /**
     * Convert FEN position string to board array
     */
    fun fenToBoard(fenPosition: String): Array<Array<ChessEngine.Piece?>> {
        val board = Array(8) { Array<ChessEngine.Piece?>(8) { null } }
        val ranks = fenPosition.split("/")
        
        for ((rankIndex, rankString) in ranks.withIndex()) {
            var fileIndex = 0
            for (char in rankString) {
                when {
                    char.isDigit() -> {
                        // Empty squares
                        fileIndex += char.toString().toInt()
                    }
                    else -> {
                        // Piece
                        val color = if (char.isUpperCase()) ChessEngine.Color.WHITE else ChessEngine.Color.BLACK
                        val type = when (char.lowercaseChar()) {
                            'p' -> ChessEngine.PieceType.PAWN
                            'n' -> ChessEngine.PieceType.KNIGHT
                            'b' -> ChessEngine.PieceType.BISHOP
                            'r' -> ChessEngine.PieceType.ROOK
                            'q' -> ChessEngine.PieceType.QUEEN
                            'k' -> ChessEngine.PieceType.KING
                            else -> null
                        }
                        if (type != null) {
                            board[rankIndex][fileIndex] = ChessEngine.Piece(type, color)
                        }
                        fileIndex++
                    }
                }
            }
        }
        
        return board
    }
    
    /**
     * Apply castling rights from FEN
     */
    fun applyCastlingRights(engine: ChessEngine, castling: String) {
        engine.whiteCanCastleKingside = castling.contains('K')
        engine.whiteCanCastleQueenside = castling.contains('Q')
        engine.blackCanCastleKingside = castling.contains('k')
        engine.blackCanCastleQueenside = castling.contains('q')
    }
}
