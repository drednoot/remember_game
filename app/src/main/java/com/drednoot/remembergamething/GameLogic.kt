package com.drednoot.remembergamething

import kotlin.random.Random
import kotlin.random.nextInt

object GameLogic {
    private const val MAX_INDEX: Int = 3
    private var correctSequence: MutableList<Int> = mutableListOf(Random.nextInt(0..MAX_INDEX))
    private var currentPress: Int = 0
    var level: Int = 1
        private set

    var highscore: Int = 0
        private set

    var freePlay: Boolean = false
    fun inflateHighscore(fromOutside: Int) {
        if (freePlay) return
        highscore = fromOutside
    }
    fun press(id: Int): PressResult? {
        if (freePlay) return null
        if (id !in 0..MAX_INDEX) return null
        var pressAccepted = false
        var levelIncreased = false
        var highscoreUpdated = false
        if (id == correctSequence[currentPress]) {
            if (currentPress + 1 == level) {
                levelIncreased = true
                pressAccepted = true
                highscoreUpdated = increaseLevel()
                currentPress = 0
            } else {
                pressAccepted = true
                ++currentPress
            }
        } else {
            currentPress = 0
        }

        return PressResult(pressAccepted, levelIncreased, highscoreUpdated)
    }

    fun getSequence(): List<Int> {
        if (freePlay) return listOf()
        return correctSequence
    }

    private fun increaseLevel(): Boolean {
        ++level
        correctSequence.add(Random.nextInt(0..MAX_INDEX))
        return if (level > highscore) {
            highscore = level
            true
        } else {
            false
        }
    }

    fun restart() {
        correctSequence = mutableListOf(Random.nextInt(0..MAX_INDEX))
        currentPress = 0
        level = 1
        freePlay = false
    }
}

data class PressResult(
    val pressAccepted: Boolean,
    val levelIncreased: Boolean,
    val highscoreUpdated: Boolean,
)