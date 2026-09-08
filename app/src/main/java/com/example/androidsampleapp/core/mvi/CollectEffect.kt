package com.example.androidsampleapp.core.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Effect は STARTED の間だけ受け取る。
 * バックグラウンドで画面遷移が走ったりスナックバーを取りこぼしたりするのを防ぐため。
 */
@Composable
fun <E : UiEffect> CollectEffect(
    effect: Flow<E>,
    onEffect: suspend (E) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(effect, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            effect.collect { onEffect(it) }
        }
    }
}
