package com.example.logic

import com.example.model.Difficulty
import com.example.model.Player
import kotlin.random.Random

object AiEngine {

    fun determineMove(
        board: List<Player?>,
        difficulty: Difficulty,
        aiPlayer: Player = Player.O
    ): Int? {
        val availableMoves = GameEngine.getAvailableMoves(board)
        if (availableMoves.isEmpty()) return null

        return when (difficulty) {
            Difficulty.EASY -> getEasyMove(availableMoves)
            Difficulty.MEDIUM -> getMediumMove(board, availableMoves, aiPlayer)
            Difficulty.HARD -> getHardMove(board, availableMoves, aiPlayer)
        }
    }

    private fun getEasyMove(availableMoves: List<Int>): Int {
        return availableMoves.random()
    }

    private fun getMediumMove(
        board: List<Player?>,
        availableMoves: List<Int>,
        aiPlayer: Player
    ): Int {
        val humanPlayer = aiPlayer.opponent()

        // 1. Can AI win immediately in one move?
        for (move in availableMoves) {
            val simulated = board.toMutableList().also { it[move] = aiPlayer }
            if (GameEngine.checkWinner(simulated)?.first == aiPlayer) {
                return move
            }
        }

        // 2. Can human win next move? 85% chance to block!
        for (move in availableMoves) {
            val simulated = board.toMutableList().also { it[move] = humanPlayer }
            if (GameEngine.checkWinner(simulated)?.first == humanPlayer) {
                if (Random.nextFloat() < 0.85f) {
                    return move
                }
            }
        }

        // 3. Take center (cell 4) if open with 50% probability
        if (4 in availableMoves && Random.nextFloat() < 0.50f) {
            return 4
        }

        // 4. Take corners if open with 40% probability
        val corners = listOf(0, 2, 6, 8).filter { it in availableMoves }
        if (corners.isNotEmpty() && Random.nextFloat() < 0.40f) {
            return corners.random()
        }

        // 5. Fallback to random valid move
        return availableMoves.random()
    }

    private fun getHardMove(
        board: List<Player?>,
        availableMoves: List<Int>,
        aiPlayer: Player
    ): Int {
        val humanPlayer = aiPlayer.opponent()

        // If board is empty, pick center or a random corner for fast start
        if (availableMoves.size == 9) {
            val startingMoves = listOf(4, 0, 2, 6, 8)
            return startingMoves.random()
        }

        var bestScore = Int.MIN_VALUE
        val bestMoves = mutableListOf<Int>()

        for (move in availableMoves) {
            val simulated = board.toMutableList().also { it[move] = aiPlayer }
            val score = minimax(
                board = simulated,
                depth = 0,
                isMaximizing = false,
                aiPlayer = aiPlayer,
                humanPlayer = humanPlayer,
                alpha = Int.MIN_VALUE,
                beta = Int.MAX_VALUE
            )

            if (score > bestScore) {
                bestScore = score
                bestMoves.clear()
                bestMoves.add(move)
            } else if (score == bestScore) {
                bestMoves.add(move)
            }
        }

        return bestMoves.randomOrNull() ?: availableMoves.first()
    }

    private fun minimax(
        board: MutableList<Player?>,
        depth: Int,
        isMaximizing: Boolean,
        aiPlayer: Player,
        humanPlayer: Player,
        alpha: Int,
        beta: Int
    ): Int {
        val winnerPair = GameEngine.checkWinner(board)
        if (winnerPair != null) {
            return if (winnerPair.first == aiPlayer) {
                10 - depth
            } else {
                depth - 10
            }
        }

        val available = GameEngine.getAvailableMoves(board)
        if (available.isEmpty()) {
            return 0 // Draw
        }

        var currentAlpha = alpha
        var currentBeta = beta

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in available) {
                board[move] = aiPlayer
                val evaluation = minimax(
                    board = board,
                    depth = depth + 1,
                    isMaximizing = false,
                    aiPlayer = aiPlayer,
                    humanPlayer = humanPlayer,
                    alpha = currentAlpha,
                    beta = currentBeta
                )
                board[move] = null
                maxEval = maxOf(maxEval, evaluation)
                currentAlpha = maxOf(currentAlpha, evaluation)
                if (currentBeta <= currentAlpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in available) {
                board[move] = humanPlayer
                val evaluation = minimax(
                    board = board,
                    depth = depth + 1,
                    isMaximizing = true,
                    aiPlayer = aiPlayer,
                    humanPlayer = humanPlayer,
                    alpha = currentAlpha,
                    beta = currentBeta
                )
                board[move] = null
                minEval = minOf(minEval, evaluation)
                currentBeta = minOf(currentBeta, evaluation)
                if (currentBeta <= currentAlpha) break
            }
            return minEval
        }
    }
}
