package com.example.exersice1

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.res.getResourceIdOrThrow
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.example.exersice1.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sounds: List<Int>
    private lateinit var buttons: List<Button>
    private lateinit var soundPlayer: SoundPlayer
    private lateinit var sequencePlayer: SequencePlayer
    private lateinit var buttonDefaultColorStateList: List<ColorStateList>
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val highscore: Preferences.Key<Int> = intPreferencesKey("highscore")

    // ---------------------- init -------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        inflateLateinits()
        addButtonFunctionality()
        addRestartButtonFunctionality()
        playCurrentLevelSequence()
        GameLogic.inflateHighscore(loadHighscore())
        updateTitle()
    }

    private fun inflateLateinits() {
        buttons = listOf(
            binding.button11,
            binding.button12,
            binding.button13,
            binding.button14,
        )

        val soundsTArray = resources.obtainTypedArray(R.array.sounds)
        val tempSounds = mutableListOf<Int>()
        for (i in 0 until soundsTArray.length()) {
            tempSounds.add(soundsTArray.getResourceIdOrThrow(i))
        }
        soundsTArray.recycle()
        sounds = tempSounds.toList()

        soundPlayer = SoundPlayer(this, sounds)
        val interval = resources.getInteger(R.integer.sound_interval).toLong()
        sequencePlayer = SequencePlayer(this, sounds, interval)

        val tempCSL = mutableListOf<ColorStateList>()
        for (btn in buttons) {
            tempCSL.add(btn.backgroundTintList!!)
        }
        buttonDefaultColorStateList = tempCSL
    }

    private fun addButtonFunctionality() {
        val preSequenceDelay = resources.getInteger(R.integer.pre_sequence_delay).toLong()
        for ((i, btn) in buttons.withIndex()) {
            btn.setOnClickListener {
                soundPlayer.play(i)
                val score = GameLogic.level
                val result = GameLogic.press(i)
                if (result.levelIncreased) {
                    updateTitle()
                    flashBackground(ContextCompat.getColor(this, R.color.green))
                    if (result.highscoreUpdated) uploadHighscore(GameLogic.highscore)
                    playCurrentLevelSequence(preSequenceDelay)
                } else if (!result.pressAccepted) {
                    flashBackground(ContextCompat.getColor(this, R.color.red))
                    showRestartScreen(score)
                    GameLogic.restart()
                }
            }
        }
    }
    private fun addRestartButtonFunctionality() {
        binding.restartScreen.button.setOnClickListener {
            binding.restartScreen.endOverlay.visibility = View.GONE
            updateTitle()
            playCurrentLevelSequence()
        }
    }

    // ---------------------- gameplay -----------------------

    private fun showRestartScreen(score: Int) {
        binding.restartScreen.endOverlay.visibility = View.VISIBLE
        binding.restartScreen.restartScore.text = getString(R.string.score, score)
    }

    private fun playCurrentLevelSequence(initialDelay: Long = 0) {
        setButtonsState(false)
        val seq = GameLogic.getSequence()
        sequencePlayer.play(
            seq,
            initialDelay,
            { id: Int, length: Int ->
                flashButtonBackground(id, length)
            },
            {
                setButtonsState(true)
        })
    }

    // ---------------------- load highscore -----------------------
    private fun loadHighscore(): Int {
        val result: Int
        runBlocking(Dispatchers.IO) {
            result = dataStore.data.map { preferences ->
                preferences[highscore] ?: 0
            }.first()
        }
        return result
    }

    private fun uploadHighscore(value: Int) {
        runBlocking(Dispatchers.IO) {
            dataStore.edit { settings ->
                settings[highscore] = value
            }
        }
    }

    // ---------------------- animation -----------------------

    private fun flashViewBackground(
        @ColorInt from: Int,
        @ColorInt to: Int,
        view: View,
        time: Long,
        isTint: Boolean = false,
        onEnd: () -> Unit = {},
    ) {
        val anim = ValueAnimator.ofArgb(
            from,
            to,
        )
        anim.duration = time
        anim.interpolator = LinearOutSlowInInterpolator()
        if (isTint) {
            anim.addUpdateListener {
                view.backgroundTintList = ColorStateList(
                    arrayOf(
                        intArrayOf(-android.R.attr.state_enabled),
                        intArrayOf(android.R.attr.state_enabled)
                    ),
                    intArrayOf(
                        it.animatedValue as Int,
                        it.animatedValue as Int,
                    )
                )
            }
        } else {
            anim.addUpdateListener {
                view.setBackgroundColor(it.animatedValue as Int)
            }
        }
        anim.doOnEnd {
            onEnd()
        }
        anim.start()
    }

    private fun flashBackground(@ColorInt color: Int) {
        val colorTypedValue = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, colorTypedValue, true)
        val time = resources.getInteger(R.integer.flash_background_time).toLong()
        flashViewBackground(color, colorTypedValue.data, binding.mainLayout, time)
    }

    private fun flashButtonBackground(id: Int, length: Int) {
        val colorTypedValue = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, colorTypedValue, true)
        val from = colorTypedValue.data
        theme.resolveAttribute(com.google.android.material.R.attr.buttonTint, colorTypedValue, true)
        val to = colorTypedValue.data
        val maxTime = resources.getInteger(R.integer.flash_button_time).toLong()
        flashViewBackground(from, to, buttons[id], if (length > maxTime) maxTime else length.toLong(), true) {
            buttons[id].backgroundTintList = buttonDefaultColorStateList[id]
        }
    }

    // ---------------------- utility -------------------------

    private fun setButtonsState(value: Boolean) {
        for (btn in buttons) {
            btn.isEnabled = value
        }
    }

    private fun updateTitle() {
        supportActionBar?.title = "${getString(R.string.level)} ${GameLogic.level} ${getString(R.string.highscore)} ${GameLogic.highscore}"
    }
}