package com.rilisentertainment.simpletodo.ui.todo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rilisentertainment.simpletodo.R
import com.rilisentertainment.simpletodo.domain.TodoInfo
import com.rilisentertainment.simpletodo.domain.diffutil.TodoInfoDiffUtil

class TodoAdapter(
    private var todosList: List<TodoInfo> = emptyList(),
    private val onItemRemove: (TodoInfo) -> Unit,
    private val onItemChecked: (TodoInfo) -> Unit,
    private val onLongItemSelected: (TodoInfo) -> Unit
) : RecyclerView.Adapter<TodoViewHolder>() {
    fun updateList(list: List<TodoInfo>) {
        val listDiffUtil = TodoInfoDiffUtil(list, todosList)
        val result = DiffUtil.calculateDiff(listDiffUtil)
        todosList = list
        result.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        return TodoViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_todo_model, parent, false)
        )
    }

    override fun getItemCount(): Int = todosList.size

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.render(todosList[position], onItemRemove, onItemChecked, onLongItemSelected)
    }
}