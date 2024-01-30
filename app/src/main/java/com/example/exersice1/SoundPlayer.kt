package com.example.exersice1

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity


class SoundPlayer(private val context: AppCompatActivity, private val bank: List<Int>) {
    fun play(id: Int) {
        val mediaPlayer = MediaPlayer.create(context, bank[id])
        mediaPlayer.setOnCompletionListener {
            it.reset()
            it.release()
        }
        mediaPlayer.start()
    }
}

class SequencePlayer(
    private val context: AppCompatActivity,
    private val bank: List<Int>,
    private val interval: Long = 500
) {
    private var finish: (() -> Unit) = {}
    private var onEachAction: (id: Int, length: Int) -> Unit = { _: Int, _: Int -> }
    private var queue: MutableList<Int> = mutableListOf()
    private val mPlayer: MediaPlayer = MediaPlayer()
    fun play(
        sequence: List<Int>,
        initialDelay: Long = 0,
        onEachSoundAction: (id: Int, length: Int) -> Unit = { _: Int, _: Int -> },
        finishAction: () -> Unit = {},
    ) {
        finish = finishAction
        onEachAction = onEachSoundAction
        queue = sequence.toMutableList()
        playNext(initialDelay)
    }

    private fun playNext(delay: Long) {
        if (queue.isEmpty()) {
            finish()
            finish = {}
            onEachAction = { _: Int, _: Int -> }
            return
        }
        mPlayer.reset()
        val id = queue.removeFirst()
        val afd = context.resources.openRawResourceFd(bank[id])
        mPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.declaredLength)
        mPlayer.prepare()
        mPlayer.setOnCompletionListener {
            playNext(interval)
        }
        Handler(Looper.getMainLooper()).postDelayed ({
            onEachAction(id, mPlayer.duration)
            mPlayer.start()
        }, delay)
    }
}