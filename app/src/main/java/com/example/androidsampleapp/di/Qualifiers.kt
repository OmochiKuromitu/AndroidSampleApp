package com.example.androidsampleapp.di

import javax.inject.Qualifier

/**
 * アプリと同じ寿命を持つ CoroutineScope。Service や常駐処理で使う。
 *
 * メインスレッドで動く。使っている常駐処理（IdleTimer、MissedCallManager）は UI のイベントと
 * 同じ状態を触るので、別スレッドで動かすと読み書きが食い違う。重い処理はここで直接せず、
 * 呼ぶ側（リポジトリなど）が withContext で切り替える。
 *
 * どのスレッドで動かすかはここで決め、使う側に `launch(Dispatchers.Main)` と書かない。
 * テストで `backgroundScope` を渡したときに仮想時間が効かなくなるため。
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
