package com.rilisentertainment.simpletodo.ui.todo.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rilisentertainment.simpletodo.R
import com.rilisentertainment.simpletodo.domain.TodoList

class TodoListAdapter(
    private var list: List<TodoList> = emptyList(),
    private val onItemRemove: (TodoList) -> Unit,
    private val onItemSelected: (String) -> Unit,
) : RecyclerView.Adapter<TodoListViewHolder>() {
    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<TodoList>) {
        list = newList

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoListViewHolder {
        return TodoListViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_todo_list, parent, false)
        )
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: TodoListViewHolder, position: Int) {
        holder.render(list[position], onItemRemove, onItemSelected)
    }

}