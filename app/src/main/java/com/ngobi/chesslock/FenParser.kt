package com.ngobi.chesslock

object FenParser {

    fun parse(fen: String): Array<Array<Char>> {
        val board = Array(8) { Array(8) { ' ' } }
        val rows = fen.split(" ")[0].split("/")

        for (r in 0..7) {
            var col = 0
            for (char in rows[r]) {
                if (char.isDigit()) {
                    col += char.toString().toInt()
                } else {
                    board[r][col] = char
                    col++
                }
            }
        }
        return board
    }

    fun toFEN(board: Array<Array<Char>>): String {
        return board.joinToString("/") { row ->
            var emptyCount = 0
            buildString {
                for (cell in row) {
                    if (cell == ' ') {
                        emptyCount++
                    } else {
                        if (emptyCount > 0) {
                            append(emptyCount)
                            emptyCount = 0
                        }
                        append(cell)
                    }
                }
                if (emptyCount > 0) append(emptyCount)
            }
        }
    }
}