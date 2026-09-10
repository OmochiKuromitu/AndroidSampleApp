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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.model.NoticeCategory
import com.example.androidsampleapp.domain.model.NoticeDestination
import com.example.androidsampleapp.ui.common.NoticeList
import com.example.androidsampleapp.ui.common.PreviewSurface
import com.example.androidsampleapp.ui.common.PanelPreview
import com.example.androidsampleapp.ui.theme.dimensions

/**
 * 無操作が続いたときに出す全画面表示。下部バーは出さない。
 *
 * 解除は画面下端の帯を上にスワイプしたときだけ。触れただけでは解除しないので、
 * 拭き掃除や誤接触で操作画面に戻らない。
 * ただし通知をタップした場合は、意図した操作とみなして復帰と遷移をまとめて行う。
 */
@Composable
fun SleepScreen(
    onWake: () -> Unit,
    onOpenDestination: (NoticeDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SleepViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            SleepEffect.Wake -> onWake()
            is SleepEffect.OpenDestination -> onOpenDestination(effect.destination)
        }
    }

    SleepContent(state = state, onIntent = viewModel::dispatch, modifier = modifier)
}

@Composable
private fun SleepContent(
    state: SleepState,
    onIntent: (SleepIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = MaterialTheme.dimensions

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = dimensions.sleepClockTop,
                    start = dimensions.spaceLarge,
                    end = dimensions.spaceLarge,
                    // 解除エリアに隠れてタップできない行が出ないよう、下を空けておく。
                    bottom = dimensions.unlockAreaHeight,
                ),
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

            NoticeList(
                notices = state.notices,
                onNoticeClick = { onIntent(SleepIntent.NoticeClicked(it)) },
                onClearClick = { onIntent(SleepIntent.ClearNoticesClicked) },
                contentColor = Color.White,
                modifier = Modifier.padding(top = dimensions.spaceLarge),
            )
        }

        UnlockArea(
            progress = state.unlockProgress,
            onProgress = { onIntent(SleepIntent.UnlockDragged(it)) },
            onCancel = { onIntent(SleepIntent.UnlockCancelled) },
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
    // 帯が低いので横並びにする。指に追従して帯の外へはみ出すが、親は切り取らない。
    Row(
        modifier = Modifier
            .offset(y = -(travel * progress * HINT_FOLLOW_RATIO))
            .alpha(HINT_MIN_ALPHA + (1f - HINT_MIN_ALPHA) * progress),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spaceSmall),
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

private val previewNotices = listOf(
    Notice("1", NoticeCategory.CALL, "玄関からの呼び出しに応答がありませんでした", NoticeDestination.TOP),
    Notice("2", NoticeCategory.ALERT, "フィルターの清掃時期です", NoticeDestination.AIRCON),
    Notice("3", NoticeCategory.AIRCON, "リビングの設定温度を 26.0 度に変更しました", NoticeDestination.AIRCON),
    Notice("4", NoticeCategory.INFO, "システムを起動しました", NoticeDestination.TOP),
)

@PanelPreview
@Composable
private fun SleepContentPreview() {
    PreviewSurface {
        SleepContent(
            state = SleepState(
                timeText = "21:47",
                dateText = "9月10日 (水)",
                notices = previewNotices,
            ),
            onIntent = {},
        )
    }
}

@PanelPreview
@Composable
private fun SleepContentEmptyPreview() {
    PreviewSurface {
        SleepContent(
            state = SleepState(timeText = "21:47", dateText = "9月10日 (水)"),
            onIntent = {},
        )
    }
}

@PanelPreview
@Composable
private fun SleepContentSwipingPreview() {
    PreviewSurface {
        SleepContent(
            state = SleepState(
                timeText = "21:47",
                dateText = "9月10日 (水)",
                notices = previewNotices,
                unlockProgress = 0.7f,
            ),
            onIntent = {},
        )
    }
}
