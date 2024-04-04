package com.rilisentertainment.simpletodo.domain

sealed class TodosFilter {
    data object All : TodosFilter()
    data object Pending : TodosFilter()
    data object Completed : TodosFilter()
}