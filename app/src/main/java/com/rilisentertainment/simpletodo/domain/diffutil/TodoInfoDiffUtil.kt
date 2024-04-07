package com.rilisentertainment.simpletodo.domain.diffutil

import androidx.recyclerview.widget.DiffUtil
import com.rilisentertainment.simpletodo.domain.TodoInfo

class TodoInfoDiffUtil(
    private val newList: List<TodoInfo>,
    private val oldList: List<TodoInfo>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return newList[newItemPosition].desc != oldList[oldItemPosition].desc
    }
}