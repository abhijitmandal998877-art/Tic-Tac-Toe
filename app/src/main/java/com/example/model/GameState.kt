package com.example.model

enum class Player(val symbol: String) {
    X("X"),
    O("O");

    fun opponent(): Player = if (this == X) O else X
}

enum class Difficulty(val label: String, val description: String) {
    EASY("Easy", "Random moves for casual play"),
    MEDIUM("Medium", "Balanced strategy with tactical awareness"),
    HARD("Hard", "Unbeatable minimax algorithm")
}

sealed class GameMode {
    object TwoPlayer : GameMode()
    data class VsAi(val difficulty: Difficulty) : GameMode()
}

sealed class GameStatus {
    data class InProgress(val currentTurn: Player, val isAiThinking: Boolean = false) : GameStatus()
    data class Victory(val winner: Player, val winningLine: List<Int>) : GameStatus()
    object Draw : GameStatus()
}

data class Score(
    val xWins: Int = 0,
    val oWins: Int = 0,
    val draws: Int = 0
)

enum class Screen {
    HOME,
    GAME,
    SETTINGS,
    HOW_TO_PLAY,
    DEVELOPER_WEBVIEW
}
