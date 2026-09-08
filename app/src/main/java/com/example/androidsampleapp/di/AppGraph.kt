package com.example.androidsampleapp.di

import com.example.androidsampleapp.data.TaskRepository

/**
 * 手書きの依存グラフ。規模が大きくなったら Hilt に置き換える想定で、
 * 生成箇所をここ 1 か所に閉じ込めておく。
 */
object AppGraph {
    val taskRepository: TaskRepository by lazy { TaskRepository() }
}
