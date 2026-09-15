package com.example.androidsampleapp.di

import javax.inject.Qualifier

/**
 * アプリと同じ寿命を持つ CoroutineScope。メインスレッド以外（Dispatchers.Default）で動く。
 * Service や常駐処理で使う。
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * アプリと同じ寿命を持ち、メインスレッドで動く CoroutineScope。
 *
 * UI のイベントや受信（どちらもメインスレッド）と同じ状態を触る常駐処理で使う。
 * [ApplicationScope] で動かすと、状態の読み書きがスレッドをまたいで食い違う。
 *
 * どのスレッドで動かすかはここで決め、使う側に `launch(Dispatchers.Main)` と書かない。
 * テストで `backgroundScope` を渡したときに仮想時間が効かなくなるため。
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainThreadScope
