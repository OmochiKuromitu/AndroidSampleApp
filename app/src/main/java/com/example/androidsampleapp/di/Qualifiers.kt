package com.example.androidsampleapp.di

import javax.inject.Qualifier

/** アプリと同じ寿命を持つ CoroutineScope。Service や常駐処理で使う。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
