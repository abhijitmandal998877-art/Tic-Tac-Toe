package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GamePreferences
import com.example.logic.AiEngine
import com.example.logic.GameEngine
import com.example.model.Difficulty
import com.example.model.GameStatus
import com.example.model.Player
import com.example.util.DeveloperDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppNameStringResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Tic Tac Toe", appName)
    }

    @Test
    fun testGameEngineRowWin() {
        // [X, X, X, null, O, O, null, null, null]
        val board = listOf(
            Player.X, Player.X, Player.X,
            null, Player.O, Player.O,
            null, null, null
        )
        val winnerPair = GameEngine.checkWinner(board)
        assertNotNull(winnerPair)
        assertEquals(Player.X, winnerPair?.first)
        assertEquals(listOf(0, 1, 2), winnerPair?.second)
    }

    @Test
    fun testGameEngineDiagonalWin() {
        // [O, null, null, null, O, null, null, null, O]
        val board = listOf(
            Player.O, null, null,
            null, Player.O, null,
            null, null, Player.O
        )
        val winnerPair = GameEngine.checkWinner(board)
        assertNotNull(winnerPair)
        assertEquals(Player.O, winnerPair?.first)
        assertEquals(listOf(0, 4, 8), winnerPair?.second)
    }

    @Test
    fun testGameEngineDraw() {
        // [X, O, X, X, O, O, O, X, X] -> No 3-in-a-row, board full
        val board = listOf(
            Player.X, Player.O, Player.X,
            Player.X, Player.O, Player.O,
            Player.O, Player.X, Player.X
        )
        val status = GameEngine.evaluateGameStatus(board, Player.O)
        assertTrue(status is GameStatus.Draw)
    }

    @Test
    fun testAiWinningMove() {
        // AI is O. Board: O, O, null -> AI should take index 2 to win!
        val board = listOf(
            Player.O, Player.O, null,
            Player.X, Player.X, null,
            null, null, null
        )
        val move = AiEngine.determineMove(board, Difficulty.HARD, aiPlayer = Player.O)
        assertEquals(2, move)
    }

    @Test
    fun testAiBlockingMove() {
        // Human is X. Human has index 0 and 1 -> AI (O) MUST block at index 2!
        val board = listOf(
            Player.X, Player.X, null,
            Player.O, null, null,
            null, null, null
        )
        val move = AiEngine.determineMove(board, Difficulty.HARD, aiPlayer = Player.O)
        assertEquals(2, move)
    }

    @Test
    fun testAiNeverSelectsOccupiedCell() {
        val board = listOf(
            Player.X, Player.O, Player.X,
            Player.O, Player.X, null,
            Player.O, null, null
        )
        val move = AiEngine.determineMove(board, Difficulty.HARD, aiPlayer = Player.O)
        assertNotNull(move)
        assertTrue(move in listOf(5, 7, 8))
        assertTrue(board[move!!] == null)
    }

    @Test
    fun testDeveloperDetectorTenTaps() {
        val detector = DeveloperDetector(requiredTaps = 10, tapTimeoutMs = 1500L)
        var easterEggTriggered = false
        var normalTaps = 0

        // 9 taps -> all normal
        repeat(9) {
            detector.recordTap(
                onEasterEggTriggered = { easterEggTriggered = true },
                onNormalTap = { normalTaps++ }
            )
        }
        assertEquals(9, normalTaps)
        assertEquals(false, easterEggTriggered)

        // 10th tap -> easter egg!
        detector.recordTap(
            onEasterEggTriggered = { easterEggTriggered = true },
            onNormalTap = { normalTaps++ }
        )
        assertEquals(true, easterEggTriggered)
    }

    @Test
    fun testGamePreferencesPersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context)
        prefs.resetScore()

        val initialScore = prefs.getScore()
        assertEquals(0, initialScore.xWins)
        assertEquals(0, initialScore.oWins)
        assertEquals(0, initialScore.draws)

        prefs.incrementXWin()
        prefs.incrementXWin()
        prefs.incrementOWin()
        prefs.incrementDraw()

        val updated = prefs.getScore()
        assertEquals(2, updated.xWins)
        assertEquals(1, updated.oWins)
        assertEquals(1, updated.draws)

        prefs.resetScore()
        val resetScore = prefs.getScore()
        assertEquals(0, resetScore.xWins)
        assertEquals(0, resetScore.oWins)
        assertEquals(0, resetScore.draws)
    }

    @Test
    fun testOpenDeveloperSectionNavigation() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.viewmodel.GameViewModel(app)
        assertEquals(com.example.model.Screen.HOME, viewModel.uiState.value.currentScreen)

        viewModel.openDeveloperSection()
        assertEquals(com.example.model.Screen.DEVELOPER_WEBVIEW, viewModel.uiState.value.currentScreen)
    }
}
