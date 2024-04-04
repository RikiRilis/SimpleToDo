package com.rilisentertainment.simpletodo.ui.settings

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.rilisentertainment.simpletodo.data.usecase.VibrationUtil
import com.rilisentertainment.simpletodo.R
import com.rilisentertainment.simpletodo.databinding.ActivitySettingsBinding
import com.rilisentertainment.simpletodo.ui.home.MainActivity
import com.rilisentertainment.simpletodo.ui.todo.settings.TodoSettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    private var firstTimeFlag = true
    private var currentLanguage: String = "en"

    companion object {
        const val SWITCH_VIBRATE = "switch_vibrate"
        const val THEME = "theme"
        const val LANGUAGE = "language"
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentLanguage = resources.configuration.locales[0].language

        CoroutineScope(Dispatchers.IO).launch {
            MainActivity.DataManager(binding.ivSettingsBack.context).getSettings()
                .filter { firstTimeFlag }.collect { settingsModel ->
                    runOnUiThread {
                        binding.sSettingsVibration.isChecked = settingsModel.vibration

                        when (settingsModel.theme) {
                            "Dark" -> binding.tvSettingsDescTheme.text =
                                this@SettingsActivity.getString(R.string.dark_light_mode_desc_dark)

                            "Light" -> binding.tvSettingsDescTheme.text =
                                this@SettingsActivity.getString(R.string.dark_light_mode_desc_light)

                            else -> binding.tvSettingsDescTheme.text =
                                this@SettingsActivity.getString(R.string.dark_light_mode_desc_system)
                        }

                        when (settingsModel.language) {
                            "en" -> binding.tvSettingsDescLanguage.text = "English"
                            "es" -> binding.tvSettingsDescLanguage.text = "Español"
                        }

                        firstTimeFlag = !firstTimeFlag
                    }
                }
        }

        initUI()
    }

    private fun initUI() {
        initListeners()
    }

    private fun openPrivacy() {
        val url = "https://privacy.rilisentertainment.xyz"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun showThemeDialog(context: Context) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_settings_theme)
        dialog.window!!.setBackgroundDrawableResource(R.color.transparent)
        dialog.window!!.setElevation(1F)
        dialog.window!!.setDimAmount(0.4F)
        dialog.window!!.decorView.scaleX = 0.5F
        dialog.window!!.decorView.scaleY = 0.5F
        dialog.window!!.decorView
            .animate()
            .setDuration(200)
            .scaleX(1F)
            .scaleY(1F)
            .start()

        val okBtn: TextView = dialog.findViewById(R.id.tvDialogThemeOK)
        val cancelBtn: TextView = dialog.findViewById(R.id.tvDialogThemeCancel)
        val options: RadioGroup = dialog.findViewById(R.id.rgThemes)
        val rbSystem: RadioButton = dialog.findViewById(R.id.rbThemeSystem)
        val rbDark: RadioButton = dialog.findViewById(R.id.rbThemeDark)
        val rbLight: RadioButton = dialog.findViewById(R.id.rbThemeLight)

        val themeState: String = when (binding.tvSettingsDescTheme.text.toString()) {
            "Predeterminado del sistema" -> "System default"
            "Oscuro" -> "Dark"
            "Claro" -> "Light"
            else -> binding.tvSettingsDescTheme.text.toString()
        }

        when (themeState) {
            "System default" -> rbSystem.isChecked = true
            "Dark" -> rbDark.isChecked = true
            else -> rbLight.isChecked = true
        }

        rbSystem.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)
        }

        rbDark.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)
        }

        rbLight.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)
        }

        cancelBtn.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)
            dialog.hide()
        }

        okBtn.setOnClickListener {
            val selectedId = options.checkedRadioButtonId
            val selected: RadioButton = options.findViewById(selectedId)

            when (val text: String = selected.contentDescription.toString()) {
                "System default" -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        MainActivity.DataManager(context).saveStrings(THEME, text)
                    }
                    binding.tvSettingsDescTheme.text = this.getString(
                        R.string.dark_light_mode_desc_system
                    )
                    darkModeAuto()
                }

                "Dark" -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        MainActivity.DataManager(context).saveStrings(THEME, text)
                    }
                    binding.tvSettingsDescTheme.text = this.getString(
                        R.string.dark_light_mode_desc_dark
                    )
                    darkModeOn()
                }

                else -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        MainActivity.DataManager(context).saveStrings(THEME, text)
                    }
                    binding.tvSettingsDescTheme.text = this.getString(
                        R.string.dark_light_mode_desc_light
                    )
                    darkModeOff()
                }
            }

            val animation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)
            dialog.hide()
        }

        dialog.show()
    }

    private fun openLanguageDialog(context: Context) {
        val dialog = BottomSheetDialog(context)
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_bottom_language_settings)
        dialog.window!!.setElevation(1F)
        dialog.window!!.setDimAmount(0.4F)
        dialog.behavior.peekHeight = 800
        dialog.behavior.state = BottomSheetBehavior.STATE_COLLAPSED

        val rbEn: RadioButton = dialog.findViewById(R.id.rbLangEnglish)!!
        val rbEs: RadioButton = dialog.findViewById(R.id.rbLangSpanish)!!
        val ivCloseBtn: LinearLayout = dialog.findViewById(R.id.ivLanguagesDialogClose)!!

        when (currentLanguage) {
            "en" -> rbEn.isChecked = true
            "es" -> rbEs.isChecked = true
        }

        rbEn.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                MainActivity.DataManager(context).setLocale(
                    context,
                    it.contentDescription.toString(),
                    LANGUAGE,
                    "en"
                )
            }

            dialog.dismiss()
            dialog.hide()
            MainActivity.DataManager(context).restartApp(this)
        }

        rbEs.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                MainActivity.DataManager(context).setLocale(
                    context,
                    it.contentDescription.toString(),
                    LANGUAGE,
                    "es"
                )
            }

            dialog.dismiss()
            dialog.hide()
            MainActivity.DataManager(context).restartApp(this)
        }

        ivCloseBtn.setOnClickListener {
            dialog.dismiss()
            dialog.hide()
        }

        dialog.show()
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.window!!.setBackgroundDrawableResource(R.color.transparent)
        dialog.window!!.attributes.windowAnimations = R.style.Bottom_Sheet_Dialog_Anim
        dialog.window!!.setGravity(Gravity.BOTTOM)
    }

    private fun openTodoSettings() {
        val intent = Intent(this, TodoSettingsActivity::class.java)
        startActivity(intent)
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

    private fun initListeners() {
        val context: Context = binding.ivSettingsBack.context

        binding.ivSettingsBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            val animation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)
        }

        binding.llSettingsMode.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)
            VibrationUtil.vibrate1(context)

            showThemeDialog(context)
        }

        binding.llSettingsLanguage.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)
            VibrationUtil.vibrate1(context)

            openLanguageDialog(context)
        }

        binding.llSettingsVibration.setOnClickListener {
            binding.sSettingsVibration.isChecked = !binding.sSettingsVibration.isChecked

            val animation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)
        }

        binding.sSettingsVibration.setOnCheckedChangeListener { _, value ->
            CoroutineScope(Dispatchers.IO).launch {
                MainActivity.DataManager(context).saveSwitches(SWITCH_VIBRATE, value)
            }
        }

        binding.llSettingsPrivacy.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)
            VibrationUtil.vibrate1(context)

            openPrivacy()
        }

        binding.llSettingsTodo.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)
            VibrationUtil.vibrate1(context)

            openTodoSettings()
        }
    }
}