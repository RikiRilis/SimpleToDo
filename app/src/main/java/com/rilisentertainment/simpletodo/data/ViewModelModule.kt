package com.rilisentertainment.simpletodo.data

import com.rilisentertainment.simpletodo.ui.todo.TodoListViewModel
import com.rilisentertainment.simpletodo.ui.todo.TodoViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ViewModelModule {
    @Provides
    @Singleton
    fun provideTodoViewModel(): TodoViewModel {
        return TodoViewModel()
    }

    @Provides
    @Singleton
    fun provideTodoListViewModel(): TodoListViewModel {
        return TodoListViewModel()
    }
}