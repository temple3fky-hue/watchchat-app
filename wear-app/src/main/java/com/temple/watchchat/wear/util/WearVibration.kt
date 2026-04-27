package com.temple.watchchat.wear.util

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

object WearVibration {
    fun tap(context: Context) {
        vibrate(
            context = context,
            milliseconds = 35L,
            amplitude = 90,
        )
    }

    fun success(context: Context) {
        vibrate(
            context = context,
            milliseconds = 70L,
            amplitude = 140,
        )
    }

    fun incomingMessage(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0L, 80L, 80L, 120L),
                intArrayOf(0, 160, 0, 220),
                -1,
            ),
        )
    }

    fun error(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0L, 60L, 70L, 90L),
                intArrayOf(0, 120, 0, 180),
                -1,
            ),
        )
    }

    private fun vibrate(
        context: Context,
        milliseconds: Long,
        amplitude: Int,
    ) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        vibrator.vibrate(
            VibrationEffect.createOneShot(
                milliseconds,
                amplitude.coerceIn(1, 255),
            ),
        )
    }
}
