package com.rilisentertainment.simpletodo.ui.todo.settings

import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.rilisentertainment.simpletodo.R
import com.rilisentertainment.simpletodo.data.usecase.VibrationUtil
import com.rilisentertainment.simpletodo.databinding.ActivityTodoSettingsBinding

class TodoSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTodoSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTodoSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()
    }

    private fun initUI() {
        initListeners()
    }

    private fun initListeners() {
        binding.ivSettingsBack.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)

            onBackPressedDispatcher.onBackPressed()
        }

        binding.llSettingsTodoBackup.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)
            VibrationUtil.vibrate1(this)
        }

        binding.llSettingsTodoRestore.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)
            VibrationUtil.vibrate1(this)
        }
    }
}