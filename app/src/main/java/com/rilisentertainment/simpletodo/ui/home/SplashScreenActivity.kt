package com.rilisentertainment.simpletodo.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rilisentertainment.simpletodo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.Locale

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {
    private var firstTimeFlag = true
    private var themeSettingsStore: String = "System default"
    private var languageSettingsStore: String = "en"
    private var currentLanguage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        splashScreen.setKeepOnScreenCondition { true }

        currentLanguage = Locale.getDefault().language

        CoroutineScope(Dispatchers.IO).launch {
            MainActivity.DataManager(this@SplashScreenActivity).getSettings()
                .filter { firstTimeFlag }
                .collect { settingsModel ->
                    runOnUiThread {
                        themeSettingsStore = settingsModel.theme
                        when (themeSettingsStore) {
                            "System default" -> darkModeAuto()
                            "Dark" -> darkModeOn()
                            "Light" -> darkModeOff()
                        }

                        languageSettingsStore = settingsModel.language
                        CoroutineScope(Dispatchers.IO).launch {
                            when (languageSettingsStore) {
                                "en" -> MainActivity.DataManager(this@SplashScreenActivity)
                                    .setLocale(
                                        this@SplashScreenActivity,
                                        languageSettingsStore,
                                        "language",
                                        "en"
                                    )

                                "es" -> MainActivity.DataManager(this@SplashScreenActivity)
                                    .setLocale(
                                        this@SplashScreenActivity,
                                        languageSettingsStore,
                                        "language",
                                        "es"
                                    )

                                else -> MainActivity.DataManager(this@SplashScreenActivity)
                                    .setLocale(
                                        this@SplashScreenActivity,
                                        languageSettingsStore,
                                        "language",
                                        currentLanguage
                                    )
                            }
                        }

                        firstTimeFlag = !firstTimeFlag
                    }
                }
        }

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun darkModeOn() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        delegate.applyDayNight()
    }

    private fun darkModeOff() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        delegate.applyDayNight()
    }

    private fun darkModeAuto() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        delegate.applyDayNight()
    }
}