package com.rilisentertainment.simpletodo.ui.todo.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rilisentertainment.simpletodo.R
import com.rilisentertainment.simpletodo.data.usecase.VibrationUtil
import com.rilisentertainment.simpletodo.databinding.ActivityTodoSettingsBinding
import com.rilisentertainment.simpletodo.domain.TodoInfo
import com.rilisentertainment.simpletodo.domain.TodoList
import com.rilisentertainment.simpletodo.ui.todo.TodoListViewModel
import com.rilisentertainment.simpletodo.ui.todo.TodoViewModel
import com.rilisentertainment.simpletodo.ui.todo.adapter.TodoListAdapter
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@Suppress("DEPRECATION")
class TodoSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTodoSettingsBinding
    private lateinit var todoListAdapter: TodoListAdapter

    companion object {
        const val REQUEST_CODE_PICK_FILE = 100
        const val REQUEST_CODE_PICK_JSON = 111
    }

    private val todoViewModel by viewModels<TodoViewModel>()
    private val todoListViewModel by viewModels<TodoListViewModel>()

    private var listToSave: MutableList<TodoInfo> = mutableListOf()

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

                    listRestored(newList)

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

    private fun listRestored(list: MutableList<TodoInfo>) {
        Toast.makeText(this, "a", Toast.LENGTH_SHORT).show()
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
            "${getFilteredList()[0].list}[${dateFormat.format(currentDate)}].json"
        )
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE)
    }

    private fun writeJsonToFile(outputStream: java.io.OutputStream) {
        try {
            val json = Gson().toJson(getFilteredList())
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

    private fun initUIListState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                todoListViewModel.list.collect {
                    todoListAdapter.updateList(it)
                }
            }
        }
    }

    private fun showBackupDialog() {
        val dialog = BottomSheetDialog(this)
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_bottom_save_lists)
        dialog.behavior.peekHeight = 800
        dialog.behavior.state = BottomSheetBehavior.STATE_COLLAPSED

        val closeBtn: LinearLayout = dialog.findViewById(R.id.ivBottomDialogClose)!!
        val rvTodoLists: RecyclerView = dialog.findViewById(R.id.rvTodoLists)!!

        todoListAdapter = TodoListAdapter(
            onItemSelected = {
                listSelected(it)
            },

            onItemRemove = {
                Toast.makeText(
                    this,
                    this.getString(R.string.list_backup_warn),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        initUIListState()

        rvTodoLists.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = todoListAdapter
        }

        closeBtn.setOnClickListener {
            val animation: Animation =
                AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)

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

    @SuppressLint("SetTextI18n")
    private fun listSelected(list: String) {
        listToSave.clear()
        todoViewModel.getTodosList().forEach { item ->
            if (item.list == list) {
                listToSave.add(item)
            }
        }

        if (listToSave.isNotEmpty()) backupTodosList()
        else Toast.makeText(
            this,
            this.getString(R.string.list_backup_warn_2),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun getFilteredList(): MutableList<TodoInfo> {
        return listToSave
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

            showBackupDialog()
        }

        binding.llSettingsTodoRestore.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            it.startAnimation(animation)
            VibrationUtil.vibrate1(this)

            restoreTodosList()
        }
    }
}