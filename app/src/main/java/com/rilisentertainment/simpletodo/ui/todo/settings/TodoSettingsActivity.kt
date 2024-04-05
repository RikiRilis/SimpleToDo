package com.rilisentertainment.simpletodo.ui.todo.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rilisentertainment.simpletodo.R
import com.rilisentertainment.simpletodo.data.usecase.VibrationUtil
import com.rilisentertainment.simpletodo.databinding.ActivityTodoSettingsBinding
import com.rilisentertainment.simpletodo.domain.TodoInfo
import com.rilisentertainment.simpletodo.domain.TodoList
import com.rilisentertainment.simpletodo.ui.home.MainActivity
import com.rilisentertainment.simpletodo.ui.todo.TodoListViewModel
import com.rilisentertainment.simpletodo.ui.todo.TodoViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@Suppress("DEPRECATION")
@AndroidEntryPoint
class TodoSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTodoSettingsBinding

    companion object {
        const val REQUEST_CODE_PICK_FILE = 100
        const val REQUEST_CODE_PICK_JSON = 111
        const val CURRENT_LIST = "current_list"
    }

    private val todoViewModel by viewModels<TodoViewModel>()
    private val todoListViewModel by viewModels<TodoListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTodoSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()
    }

    private fun initUI() {
        initListeners()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val outputStream = contentResolver.openOutputStream(uri)
                outputStream?.use { stream ->
                    writeJsonToFile(stream)
                }
            }
        } else if (requestCode == REQUEST_CODE_PICK_JSON && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val context = this
                    val json = inputStream.bufferedReader().use { it.readText() }
                    val typeToken = object : TypeToken<MutableList<TodoInfo>>() {}.type
                    val newList = Gson().fromJson(
                        json, typeToken
                    ) ?: todoViewModel.getTodosList()

                    listRestoredCheck(newList)

                    Toast.makeText(
                        context,
                        context.getString(R.string.list_restored),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            return
        }
    }

    private fun listRestoredCheck(list: MutableList<TodoInfo>) {
        if (list[0].type == "TodoList" && list.all { it.type.isNotEmpty() }) {
            restoreList(list)
        } else {
            Toast.makeText(
                this,
                this.getString(
                    R.string.list_restore_warn
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun restoreList(list: MutableList<TodoInfo>) {
        val newTodoListInfo: MutableList<TodoList> = todoListViewModel.getTodosList()
        todoViewModel.updateAllList(list)
        todoViewModel.getTodosList().forEach { item ->
            if (
                !todoListViewModel.getTodosList().any { it.title == item.list } &&
                !newTodoListInfo.any { it.title == item.list }
            ) {
                newTodoListInfo.add(TodoList(item.list))
            }
        }
        todoListViewModel.updateAllList(newTodoListInfo)
        todoViewModel.saveTodosToDataStore(this)
        todoListViewModel.saveLists(this)
        CoroutineScope(Dispatchers.IO).launch {
            MainActivity.DataManager(this@TodoSettingsActivity).saveStrings(
                CURRENT_LIST, todoViewModel.getTodosList()[0].list
            )
        }
    }

    private fun restoreTodosList() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, REQUEST_CODE_PICK_JSON)
    }

    @SuppressLint("SimpleDateFormat")
    private fun backupTodosList() {
        val dateFormat = SimpleDateFormat("d/M/yyyy - HH:mm")
        val currentDate = Date()
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/json"
        intent.putExtra(
            Intent.EXTRA_TITLE,
            "todoLists_backup_[${dateFormat.format(currentDate)}].json"
        )
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE)
    }

    private fun writeJsonToFile(outputStream: java.io.OutputStream) {
        try {
            val json = Gson().toJson(todoViewModel.getTodosList())
            outputStream.use { stream ->
                val file = File(outputStream.toString())
                if (file.exists()) {
                    file.delete()
                }
                stream.write(json.toByteArray())
            }
            Toast.makeText(this, this.getString(R.string.backup_success), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, this.getString(R.string.backup_error), Toast.LENGTH_SHORT).show()
        }
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

            if (todoViewModel.getTodosList().isEmpty()) {
                Toast.makeText(
                    this,
                    this.getString(R.string.list_backup_warn),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                backupTodosList()
            }
        }

        binding.llSettingsTodoRestore.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)
            VibrationUtil.vibrate1(this)

            restoreTodosList()
        }
    }
}