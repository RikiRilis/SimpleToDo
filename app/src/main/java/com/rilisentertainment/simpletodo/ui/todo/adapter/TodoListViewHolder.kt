package com.rilisentertainment.simpletodo.ui.todo.adapter

import android.content.Context
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.rilisentertainment.simpletodo.data.usecase.VibrationUtil
import com.rilisentertainment.simpletodo.R
import com.rilisentertainment.simpletodo.databinding.ItemTodoListBinding
import com.rilisentertainment.simpletodo.domain.TodoList

class TodoListViewHolder(view: View): RecyclerView.ViewHolder(view) {
    private val binding = ItemTodoListBinding.bind(view)
    val context: Context = binding.tvTodoList.context

    fun render(
        todoList: TodoList,
        onItemRemove: (TodoList) -> Unit,
        onItemSelected: (String) -> Unit,
        ) {
        binding.tvTodoList.text = todoList.title

        binding.parent.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
            it.startAnimation(animation)

            onItemSelected(todoList.title)
            VibrationUtil.vibrate1(context)
        }

        binding.ivTodoListDelete.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
            it.startAnimation(animation)

            onItemRemove(todoList)
            VibrationUtil.vibrate1(context)
        }
    }
}