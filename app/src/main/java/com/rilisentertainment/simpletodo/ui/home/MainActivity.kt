package com.rilisentertainment.simpletodo.ui.home

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rilisentertainment.simpletodo.R
import com.rilisentertainment.simpletodo.databinding.ActivityMainBinding
import com.rilisentertainment.simpletodo.domain.TodoInfo
import com.rilisentertainment.simpletodo.domain.TodoList
import com.rilisentertainment.simpletodo.ui.settings.SettingsActivity
import com.rilisentertainment.simpletodo.ui.settings.adapter.SettingsModel
import com.rilisentertainment.simpletodo.ui.todo.TodoListViewModel
import com.rilisentertainment.simpletodo.ui.todo.TodoViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

@Suppress("DEPRECATION")
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "registry")

        const val TODOS_STORE = "todos_store"
        const val TODOS_LIST = "todos_list"
        const val REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1
        const val REQUEST_CODE_READ_EXTERNAL_STORAGE = 2
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_WRITE_EXTERNAL_STORAGE
            )
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_READ_EXTERNAL_STORAGE
            )
        }

        CoroutineScope(Dispatchers.IO).launch {
            val lang = DataManager(this@MainActivity).getStrings("language")

            val locale = Locale(lang)
            Locale.setDefault(locale)
            val configuration = Configuration(this@MainActivity.resources.configuration)
            configuration.setLocale(locale)
            this@MainActivity.resources.updateConfiguration(
                configuration,
                this@MainActivity.resources.displayMetrics
            )
        }

        initUI()
    }

    private fun initUI() {
        initListeners()
    }

    private fun openFloatingSideMenu(view: View) {
        val popupMenu = PopupMenu(this, view, Gravity.NO_GRAVITY, 0, R.style.Popup_Menu)
        popupMenu.inflate(R.menu.floating_side_menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuSettings -> {
                    openSettings()
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun initListeners() {
        binding.ivFloatingSideMenu.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)

            openFloatingSideMenu(it)
        }
    }

    @Suppress("DEPRECATION")
    class DataManager(context: Context) {
        private val dataStore = context.dataStore
        private val todosViewModel: TodoViewModel by lazy {
            ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
                .create(TodoViewModel::class.java)
        }

        private val todosListViewModel: TodoListViewModel by lazy {
            ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
                .create(TodoListViewModel::class.java)
        }

        suspend fun saveSwitches(key: String, value: Boolean) {
            dataStore.edit { preferences ->
                preferences[booleanPreferencesKey(key)] = value
            }
        }

        suspend fun saveStrings(key: String, value: String) {
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey(key)] = value
            }
        }

        suspend fun getStrings(key: String): String {
            val preferencesKey = stringPreferencesKey(key)
            val preferences = dataStore.data.first()
            return preferences[preferencesKey] ?: ""
        }

        suspend fun saveTodosList(todosList: MutableList<TodoInfo>) {
            val json = Gson().toJson(todosList)
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey(TODOS_STORE)] = json
            }
        }

        suspend fun saveCurrentTodosList(todosList: MutableList<TodoList>) {
            val json = Gson().toJson(todosList)
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey(TODOS_LIST)] = json
            }
        }

        suspend fun getTodosListFromDataStore(): MutableList<TodoInfo> {
            val todosListKey = stringPreferencesKey(TODOS_STORE)
            val jsonString = dataStore.data.first()[todosListKey]
            val typeToken = object : TypeToken<MutableList<TodoInfo>>() {}.type
            return Gson().fromJson(jsonString, typeToken) ?: todosViewModel.getTodosList()
        }

        suspend fun getCurrentListsFromDataStore(): MutableList<TodoList> {
            val todosListKey = stringPreferencesKey(TODOS_LIST)
            val jsonString = dataStore.data.first()[todosListKey]
            val typeToken = object : TypeToken<MutableList<TodoList>>() {}.type
            return Gson().fromJson(jsonString, typeToken) ?: todosListViewModel.getTodosList()
        }

        fun getSettings(): Flow<SettingsModel> {
            return dataStore.data.map { preferences ->
                SettingsModel(
                    vibration = preferences[booleanPreferencesKey(SettingsActivity.SWITCH_VIBRATE)]
                        ?: true,
                    theme = preferences[stringPreferencesKey(SettingsActivity.THEME)]
                        ?: "System default",
                    language = preferences[stringPreferencesKey(SettingsActivity.LANGUAGE)]
                        ?: "en"
                )
            }
        }

        suspend fun setLocale(context: Context, languageCode: String, key: String, value: String) {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(locale)
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)

            dataStore.edit { preferences ->
                preferences[stringPreferencesKey(key)] = value
            }
        }

        fun restartApp(activity: Activity) {
            val intent = Intent(activity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.finish()
            activity.startActivity(intent)
        }
    }
}