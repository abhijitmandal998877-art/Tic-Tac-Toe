package com.example.util

class DeveloperDetector(
    private val requiredTaps: Int = 10,
    private val tapTimeoutMs: Long = 1500L
) {
    private var tapCount = 0
    private var lastTapTime = 0L

    fun recordTap(
        onEasterEggTriggered: () -> Unit,
        onNormalTap: () -> Unit
    ) {
        val now = System.currentTimeMillis()
        if (now - lastTapTime > tapTimeoutMs) {
            tapCount = 1
        } else {
            tapCount++
        }
        lastTapTime = now

        if (tapCount >= requiredTaps) {
            tapCount = 0
            onEasterEggTriggered()
        } else {
            onNormalTap()
        }
    }

    fun reset() {
        tapCount = 0
        lastTapTime = 0L
    }
}
