package com.rilisentertainment.simpletodo.ui.todo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rilisentertainment.simpletodo.domain.TodoInfo
import com.rilisentertainment.simpletodo.ui.home.MainActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoViewModel @Inject constructor() : ViewModel() {
    private var _todos = MutableStateFlow<List<TodoInfo>>(emptyList())
    val todos: StateFlow<List<TodoInfo>> = _todos

    var todosList: List<TodoInfo> = listOf()

    fun addTodo(todoInfo: TodoInfo = TodoInfo()): List<TodoInfo> {
        if (todosList.isNotEmpty() && todosList[0].timestamp == "Default") {
            todosList = todosList.minusElement(todosList[0])
        }

        todosList = todosList.plus(todoInfo)
        _todos.value = todosList
        return todosList
    }

    fun updateAllList(list: List<TodoInfo>) {
        todosList = list
        _todos.value = todosList
    }

    fun editList(desc: String, pos: Int) {
        todosList[pos].desc = desc
        _todos.value = todosList
    }

    fun checkTask(pos: Int) {
        todosList[pos].done = !todosList[pos].done
        _todos.value = todosList
    }

    fun getList(): List<TodoInfo> {
        return todosList
    }

    fun saveTodosToDataStore(context: Context) {
        viewModelScope.launch {
            MainActivity.DataManager(context).saveTodosList(getList())
        }
    }
}