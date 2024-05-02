package com.rilisentertainment.simpletodo.ui.todo.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.rilisentertainment.simpletodo.R
import com.rilisentertainment.simpletodo.data.usecase.VibrationUtil
import com.rilisentertainment.simpletodo.databinding.ItemTodoModelBinding
import com.rilisentertainment.simpletodo.domain.TodoInfo

class TodoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val binding = ItemTodoModelBinding.bind(view)
    val context: Context = binding.tvTodoDesc.context

    @SuppressLint("SetTextI18n")
    fun render(
        todosInfo: TodoInfo,
        onItemRemove: (TodoInfo) -> Unit,
        onItemChecked: (TodoInfo) -> Unit,
        onLongItemSelected: (TodoInfo) -> Unit,
    ) {
        val checkTextColor = ContextCompat.getColor(context, R.color.main_txt11)
        val noCheckTextColor = ContextCompat.getColor(context, R.color.main_txt4)

        binding.tvTodoDesc.text = todosInfo.desc
        binding.tvTodoTimestamp.text =
            "${context.getString(R.string.reg_created_timestamp)} ${todosInfo.timestamp}"
        if (todosInfo.done) {
            binding.ivTodoCompletedImage.visibility = View.VISIBLE
            binding.tvTodoDesc.paintFlags =
                binding.tvTodoDesc.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.tvTodoDesc.setTextColor(checkTextColor)
        } else {
            binding.ivTodoCompletedImage.visibility = View.GONE
            binding.tvTodoDesc.paintFlags =
                binding.tvTodoDesc.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            binding.tvTodoDesc.setTextColor(noCheckTextColor)
        }

        binding.llCheckedZone.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
            it.startAnimation(animation)

            onItemChecked(todosInfo)
            VibrationUtil.vibrate1(context)
        }

        binding.llCheckedZone.setOnLongClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
            it.startAnimation(animation)

            if (!todosInfo.done) {
                onLongItemSelected(todosInfo)
                VibrationUtil.vibrate1(context)
            }

            true
        }

        binding.ivTodoDelete.setOnClickListener {
            val animation: Animation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
            it.startAnimation(animation)

            onItemRemove(todosInfo)
            VibrationUtil.vibrate1(context)
        }
    }
}