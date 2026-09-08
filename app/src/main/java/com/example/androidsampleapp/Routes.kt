package com.example.androidsampleapp

/** ホームタブの中のルート。タブ内遷移はこのグラフに閉じる。 */
object HomeRoutes {
    const val LIST = "home/list"
    const val DETAIL = "home/detail/{taskId}"
    const val ARG_TASK_ID = "taskId"

    fun detail(taskId: String): String = "home/detail/$taskId"
}
