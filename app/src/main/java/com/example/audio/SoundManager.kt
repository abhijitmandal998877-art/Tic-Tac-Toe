package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SoundManager {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var musicJob: Job? = null

    @Volatile
    var isSoundEnabled: Boolean = true

    @Volatile
    var isMusicEnabled: Boolean = true
        set(value) {
            field = value
            if (value) {
                startMusic()
            } else {
                stopMusic()
            }
        }

    fun playClick() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                // Short crisp click (880Hz, 35ms)
                val buffer = generateTone(
                    startFreq = 880.0,
                    endFreq = 700.0,
                    durationMs = 35,
                    volume = 0.5f
                )
                playPcmBuffer(buffer)
            } catch (_: Exception) { }
        }
    }

    fun playXMove() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                // Energetic cyber chirp (580Hz -> 920Hz, 70ms)
                val buffer = generateTone(
                    startFreq = 580.0,
                    endFreq = 920.0,
                    durationMs = 70,
                    volume = 0.65f
                )
                playPcmBuffer(buffer)
            } catch (_: Exception) { }
        }
    }

    fun playOMove() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                // Smooth resonant chime (harmonic 440Hz -> 350Hz, 85ms)
                val buffer = generateHarmonicTone(
                    baseFreq = 440.0,
                    harmonicFreq = 660.0,
                    durationMs = 85,
                    volume = 0.6f
                )
                playPcmBuffer(buffer)
            } catch (_: Exception) { }
        }
    }

    fun playAiMove() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                // Sci-fi thought pulse (720Hz -> 860Hz, 60ms)
                val buffer = generateTone(
                    startFreq = 720.0,
                    endFreq = 860.0,
                    durationMs = 60,
                    volume = 0.5f
                )
                playPcmBuffer(buffer)
            } catch (_: Exception) { }
        }
    }

    fun playWin() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                // Ascending 4-tone victory arpeggio: C5 (523Hz), E5 (659Hz), G5 (784Hz), C6 (1046Hz)
                val notes = listOf(523.25, 659.25, 783.99, 1046.50)
                val noteDuration = 100
                val totalSamples = ((noteDuration * notes.size) * 44100) / 1000
                val fullBuffer = ShortArray(totalSamples)

                var offset = 0
                for (freq in notes) {
                    val noteBuffer = generateTone(
                        startFreq = freq,
                        endFreq = freq,
                        durationMs = noteDuration,
                        volume = 0.7f
                    )
                    System.arraycopy(noteBuffer, 0, fullBuffer, offset, noteBuffer.size)
                    offset += noteBuffer.size
                }

                playPcmBuffer(fullBuffer)
            } catch (_: Exception) { }
        }
    }

    fun playDraw() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                // Descending gentle chord (440Hz -> 330Hz, 180ms)
                val buffer = generateTone(
                    startFreq = 440.0,
                    endFreq = 330.0,
                    durationMs = 180,
                    volume = 0.55f
                )
                playPcmBuffer(buffer)
            } catch (_: Exception) { }
        }
    }

    fun startMusic() {
        if (!isMusicEnabled || musicJob?.isActive == true) return
        musicJob = scope.launch {
            try {
                // Gentle relaxing chill ambient synth progression (pentatonic melody sequence)
                val progression = listOf(
                    Pair(261.63, 329.63), // C4 + E4
                    Pair(293.66, 349.23), // D4 + F4
                    Pair(329.63, 392.00), // E4 + G4
                    Pair(392.00, 440.00), // G4 + A4
                    Pair(329.63, 392.00), // E4 + G4
                    Pair(293.66, 349.23)  // D4 + F4
                )

                while (isActive && isMusicEnabled) {
                    for (chord in progression) {
                        if (!isActive || !isMusicEnabled) break
                        val buffer = generateHarmonicTone(
                            baseFreq = chord.first,
                            harmonicFreq = chord.second,
                            durationMs = 380,
                            volume = 0.12f
                        )
                        playPcmBuffer(buffer)
                        delay(400)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    private fun playPcmBuffer(buffer: ShortArray) {
        var track: AudioTrack? = null
        try {
            val sampleRate = 44100
            val minSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minSize, buffer.size * 2)

            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()

            // Wait until played then release
            val durationMs = (buffer.size * 1000L) / sampleRate
            Thread.sleep(durationMs + 20)
        } catch (_: Exception) {
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) { }
        }
    }

    private fun generateTone(
        startFreq: Double,
        endFreq: Double,
        durationMs: Int,
        volume: Float
    ): ShortArray {
        val sampleRate = 44100
        val sampleCount = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(sampleCount)
        var phase = 0.0

        for (i in 0 until sampleCount) {
            val progress = i.toDouble() / sampleCount
            val freq = startFreq + (endFreq - startFreq) * progress

            // Envelope: fast linear attack (10%), exponential decay (90%)
            val envelope = if (progress < 0.1) {
                progress / 0.1
            } else {
                (1.0 - progress) * (1.0 - progress)
            }

            val sample = (sin(phase) * envelope * volume * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

            phase += 2.0 * PI * freq / sampleRate
            if (phase > 2.0 * PI) phase -= 2.0 * PI
        }
        return buffer
    }

    private fun generateHarmonicTone(
        baseFreq: Double,
        harmonicFreq: Double,
        durationMs: Int,
        volume: Float
    ): ShortArray {
        val sampleRate = 44100
        val sampleCount = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(sampleCount)
        var phase1 = 0.0
        var phase2 = 0.0

        for (i in 0 until sampleCount) {
            val progress = i.toDouble() / sampleCount
            val envelope = if (progress < 0.15) {
                progress / 0.15
            } else {
                1.0 - (progress - 0.15) / 0.85
            }

            val s1 = sin(phase1) * 0.65
            val s2 = sin(phase2) * 0.35
            val sample = ((s1 + s2) * envelope * volume * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

            phase1 += 2.0 * PI * baseFreq / sampleRate
            if (phase1 > 2.0 * PI) phase1 -= 2.0 * PI

            phase2 += 2.0 * PI * harmonicFreq / sampleRate
            if (phase2 > 2.0 * PI) phase2 -= 2.0 * PI
        }
        return buffer
    }
}
