package com.rilisentertainment.simpletodo.domain

import java.util.UUID

data class TodoInfo(
    var desc: String = "New To-Do",
    var timestamp: String = "Default",
    var done: Boolean = false,
    val list: String = "Default",
    val id: String = UUID.randomUUID().toString()
)
