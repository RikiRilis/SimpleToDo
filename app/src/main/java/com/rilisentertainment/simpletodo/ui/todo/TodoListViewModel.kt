package com.rilisentertainment.simpletodo.ui.todo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rilisentertainment.simpletodo.domain.TodoList
import com.rilisentertainment.simpletodo.ui.home.MainActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoListViewModel @Inject constructor(): ViewModel() {
    private var _list = MutableStateFlow<List<TodoList>>(emptyList())
    val list: StateFlow<List<TodoList>> = _list

    private var mutableList: MutableList<TodoList> = ArrayList()

    init {
        _list.value = addList()
    }

    fun addList(todoList: TodoList = TodoList()): MutableList<TodoList> {
        mutableList.add(todoList)
        _list.value = mutableList
        return mutableList
    }

    fun getTodosList(): MutableList<TodoList> {
        return mutableList
    }

    fun updateAllList(list: MutableList<TodoList>) {
        mutableList = list
        _list.value = mutableList
    }

    fun saveLists(context: Context) {
        viewModelScope.launch {
            MainActivity.DataManager(context).saveCurrentTodosList(getTodosList())
        }
    }
}