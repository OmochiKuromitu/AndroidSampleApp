package com.example.androidsampleapp.ui.main

import com.example.androidsampleapp.core.mvi.UiEffect

/**
 * この画面が出す一回きりの出来事は、今のところ無い。
 *
 * 以前はここにタブ遷移の命令が並んでいたが、遷移は AppNavigation の担当になった。
 * 枠（ヘッダーと下部バー）に一回きりの出来事が生まれたら、ここに足す。
 */
sealed interface MainEffect : UiEffect
