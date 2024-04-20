package com.rilisentertainment.simpletodo.data.usecase

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import com.rilisentertainment.simpletodo.ui.home.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
object VibrationUtil {
    private var vibrationState: Boolean = true

    fun vibrate1(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibrationEffect = VibrationEffect.createOneShot(3, VibrationEffect.DEFAULT_AMPLITUDE)

        CoroutineScope(Dispatchers.IO).launch {
            MainActivity.DataManager(context).getSettings().collect {
                vibrationState = it.vibration
            }
        }

        if (vibrationState) {
            vibrator.vibrate(vibrationEffect)
        }
    }

    fun vibrate2(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibrationEffect = VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)

        CoroutineScope(Dispatchers.IO).launch {
            MainActivity.DataManager(context).getSettings().collect {
                vibrationState = it.vibration
            }
        }

        if (vibrationState) {
            vibrator.vibrate(vibrationEffect)
        }
    }
}
