package com.drednoot.remembergamething

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.view.MenuProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
class GameFragment : Fragment(R.layout.game) {
    private lateinit var sounds: List<Int>
    private lateinit var buttons: List<Button>
    private lateinit var soundPlayer: SoundPlayer
    private lateinit var sequencePlayer: SequencePlayer
    private lateinit var buttonDefaultColorStateList: List<ColorStateList>
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var highscore: Preferences.Key<Int>
    private lateinit var menuProvider: GameMenuProvider
    var isStopped: Boolean = false
        set(value) {
            if (value) {
                sequencePlayer.stop()
            }
            field = value
        }

    private var isMenuVisible = true
        set(value) {
            if (isMenuVisible != value) {
                field = value
                activity?.invalidateOptionsMenu()
            }
        }

    // ---------------------- init -------------------------

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inflateLateinits()
        addButtonFunctionality()
        addRestartButtonFunctionality()
        GameLogic.inflateHighscore(loadHighscore())
        activity?.addMenuProvider(menuProvider)
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            if (GameLogic.freePlay) {
                setIsMenuVisible(true)
                restartGame()
            } else {
                isMenuVisible = false
                GameLogic.freePlay = false
                (activity as AppCompatActivity).supportActionBar?.hide()
                isStopped = true
                findNavController().popBackStack()
            }
        }
        (activity as AppCompatActivity).supportActionBar?.show()
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        isStopped = false
        restartGame()
    }

    override fun onDestroyView() {
        activity?.removeMenuProvider(menuProvider)
        super.onDestroyView()
    }
    private fun inflateLateinits() {
        buttons = listOf(
            requireView().findViewById(R.id.button11),
            requireView().findViewById(R.id.button12),
            requireView().findViewById(R.id.button13),
            requireView().findViewById(R.id.button14),
        )

        val soundsTArray = resources.obtainTypedArray(R.array.sounds)
        val tempSounds = mutableListOf<Int>()
        for (i in 0 until soundsTArray.length()) {
            tempSounds.add(soundsTArray.getResourceIdOrThrow(i))
        }
        soundsTArray.recycle()
        sounds = tempSounds.toList()

        soundPlayer = SoundPlayer(requireContext(), sounds)
        val interval = resources.getInteger(R.integer.sound_interval).toLong()
        sequencePlayer = SequencePlayer(requireContext(), sounds, interval)

        val tempCSL = mutableListOf<ColorStateList>()
        for (btn in buttons) {
            tempCSL.add(btn.backgroundTintList!!)
        }
        buttonDefaultColorStateList = tempCSL

        dataStore = requireContext().dataStore
        highscore = intPreferencesKey("highscore")

        menuProvider = GameMenuProvider(this)
    }

    private fun addButtonFunctionality() {
        val preSequenceDelay = resources.getInteger(R.integer.pre_sequence_delay).toLong()
        for ((i, btn) in buttons.withIndex()) {
            btn.setOnClickListener {
                soundPlayer.play(i)
                val score = GameLogic.level
                val result = GameLogic.press(i) ?: return@setOnClickListener
                if (result.levelIncreased) {
                    updateTitle()
                    flashBackground(ContextCompat.getColor(requireContext(), R.color.green))
                    if (result.highscoreUpdated) uploadHighscore(GameLogic.highscore)
                    playCurrentLevelSequence(preSequenceDelay)
                } else if (!result.pressAccepted) {
                    flashBackground(ContextCompat.getColor(requireContext(), R.color.red))
                    showRestartScreen(score)
                    GameLogic.restart()
                }
            }
        }
    }
    private fun addRestartButtonFunctionality() {
        requireView().findViewById<Button>(R.id.restartButton).setOnClickListener {
            restartGame()
        }
    }

    // ---------------------- gameplay -----------------------

    private fun showRestartScreen(score: Int) {
        view?.findViewById<ConstraintLayout>(R.id.restartScreen)?.visibility = View.VISIBLE
        view?.findViewById<TextView>(R.id.restartScore)?.text = requireActivity().getString(R.string.score, score)
        isMenuVisible = false
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
    fun restartGame() {
        view?.findViewById<ConstraintLayout>(R.id.restartScreen)?.visibility = View.GONE
        GameLogic.restart()
        updateTitle()
        playCurrentLevelSequence()
    }

    fun startFreePlay() {
        if (!GameLogic.freePlay) {
            GameLogic.freePlay = true
            updateTitle()
            isMenuVisible = false
        }
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
        requireActivity().theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, colorTypedValue, true)
        val time = resources.getInteger(R.integer.flash_background_time).toLong()
        flashViewBackground(color, colorTypedValue.data, requireView().findViewById(R.id.gameLayout), time)
    }

    private fun flashButtonBackground(id: Int, length: Int) {
        val colorTypedValue = TypedValue()
        activity?.theme?.resolveAttribute(com.google.android.material.R.attr.colorPrimary, colorTypedValue, true)
        val from = colorTypedValue.data
        activity?.theme?.resolveAttribute(com.google.android.material.R.attr.buttonTint, colorTypedValue, true)
        val to = colorTypedValue.data
        val maxTime = context?.resources?.getInteger(R.integer.flash_button_time)?.toLong()
        if (maxTime != null) {
            flashViewBackground(
                from,
                to,
                buttons[id],
                if (length > maxTime) maxTime else length.toLong(),
                true
            ) {
                buttons[id].backgroundTintList = buttonDefaultColorStateList[id]
            }
        }
    }

    // ---------------------- utility -------------------------

    private fun setButtonsState(value: Boolean) {
        for (btn in buttons) {
            btn.isEnabled = value
        }
        isMenuVisible = value
    }

    private fun updateTitle() {
        if (GameLogic.freePlay) {
            (activity as AppCompatActivity?)?.supportActionBar?.title = requireActivity().getString(R.string.free_play_title)
        } else {
            (activity as AppCompatActivity?)?.supportActionBar?.title = requireActivity().getString(R.string.normal_mode_title, GameLogic.level, GameLogic.highscore)
        }
    }

    fun setIsMenuVisible(value: Boolean) { isMenuVisible = value }
    fun getIsMenuVisible(): Boolean { return isMenuVisible }
}
class GameMenuProvider(private val gameFragment: GameFragment) : MenuProvider {
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (gameFragment.getIsMenuVisible()) {
            menuInflater.inflate(R.menu.game_menu, menu)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
        R.id.newGameMenuEntry -> {
            gameFragment.restartGame()
            true
        }

        R.id.freePlayMenuEntry -> {
            gameFragment.startFreePlay()
            true
        }

        android.R.id.home -> {
            if (GameLogic.freePlay) {
                gameFragment.setIsMenuVisible(true)
                gameFragment.restartGame()
            } else {
                gameFragment.isStopped = true
                gameFragment.findNavController().popBackStack()
            }
            true
        }

        else -> false
    }
}