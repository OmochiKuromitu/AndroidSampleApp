package com.example.androidsampleapp.ui.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidsampleapp.R
import com.example.androidsampleapp.core.mvi.CollectEffect
import com.example.androidsampleapp.ui.theme.dimensions

/**
 * 無操作が続いたときに出す全画面表示。下部バーは出さない。
 *
 * 解除は画面下端の帯を上にスワイプしたときだけ。触れただけでは解除しないので、
 * 拭き掃除や誤接触で操作画面に戻らない。
 */
@Composable
fun SleepScreen(
    onWake: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SleepViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            SleepEffect.Wake -> onWake()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.timeText,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
            )
            Text(
                text = state.dateText,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.7f),
            )
        }

        UnlockArea(
            progress = state.unlockProgress,
            onProgress = { viewModel.dispatch(SleepIntent.UnlockDragged(it)) },
            onCancel = { viewModel.dispatch(SleepIntent.UnlockCancelled) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * 画面下端の解除エリア。ここで始まった上方向のドラッグだけを解除操作として扱う。
 * 帯の高さと必要な移動量は [com.example.androidsampleapp.ui.theme.Dimensions] にある。
 */
@Composable
private fun UnlockArea(
    progress: Float,
    onProgress: (Float) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val areaHeight = MaterialTheme.dimensions.unlockAreaHeight
    val unlockDistance = MaterialTheme.dimensions.unlockDistance
    val unlockDistancePx = with(LocalDensity.current) { unlockDistance.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(areaHeight)
            .pointerInput(unlockDistancePx) {
                var draggedUpPx = 0f
                detectVerticalDragGestures(
                    onDragStart = { draggedUpPx = 0f },
                    onDragEnd = { onCancel() },
                    onDragCancel = { onCancel() },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        // dragAmount は下方向が正。上方向を正に読み替える。
                        // 押し戻せば進み具合も戻るので、途中でやめられる。
                        draggedUpPx = (draggedUpPx - dragAmount).coerceAtLeast(0f)
                        onProgress(draggedUpPx / unlockDistancePx)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        UnlockHint(progress = progress, travel = unlockDistance)
    }
}

/** 指の動きに合わせて少し持ち上がり、濃くなる。反応していることを見せるためだけの表示。 */
@Composable
private fun UnlockHint(progress: Float, travel: Dp) {
    Column(
        modifier = Modifier
            .offset(y = -(travel * progress * HINT_FOLLOW_RATIO))
            .alpha(HINT_MIN_ALPHA + (1f - HINT_MIN_ALPHA) * progress),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spaceSmall),
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = null,
            tint = Color.White,
        )
        Text(
            text = stringResource(R.string.sleep_unlock_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
    }
}

/** ヒントは指の移動量そのままではなく、控えめに追従させる。 */
private const val HINT_FOLLOW_RATIO = 0.3f
private const val HINT_MIN_ALPHA = 0.35f
