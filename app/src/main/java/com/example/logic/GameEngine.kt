package com.example.logic

import com.example.model.GameStatus
import com.example.model.Player

object GameEngine {
    val WINNING_COMBINATIONS = listOf(
        listOf(0, 1, 2), // Row 0
        listOf(3, 4, 5), // Row 1
        listOf(6, 7, 8), // Row 2
        listOf(0, 3, 6), // Col 0
        listOf(1, 4, 7), // Col 1
        listOf(2, 5, 8), // Col 2
        listOf(0, 4, 8), // Main diagonal
        listOf(2, 4, 6)  // Anti-diagonal
    )

    fun checkWinner(board: List<Player?>): Pair<Player, List<Int>>? {
        for (combo in WINNING_COMBINATIONS) {
            val (a, b, c) = combo
            val playerA = board[a]
            if (playerA != null && playerA == board[b] && playerA == board[c]) {
                return Pair(playerA, combo)
            }
        }
        return null
    }

    fun isBoardFull(board: List<Player?>): Boolean {
        return board.none { it == null }
    }

    fun evaluateGameStatus(board: List<Player?>, currentTurn: Player): GameStatus {
        val winnerResult = checkWinner(board)
        if (winnerResult != null) {
            return GameStatus.Victory(winnerResult.first, winnerResult.second)
        }
        if (isBoardFull(board)) {
            return GameStatus.Draw
        }
        return GameStatus.InProgress(currentTurn)
    }

    fun getAvailableMoves(board: List<Player?>): List<Int> {
        return board.indices.filter { board[it] == null }
    }
}
