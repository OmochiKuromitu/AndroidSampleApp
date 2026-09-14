package com.example.androidsampleapp.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.androidsampleapp.R
import com.example.androidsampleapp.domain.model.Notice
import com.example.androidsampleapp.domain.model.NoticeCategory
import com.example.androidsampleapp.ui.theme.NoticeCardBackground
import com.example.androidsampleapp.ui.theme.NoticeCardContent
import com.example.androidsampleapp.ui.theme.NoticeCardSubContent
import com.example.androidsampleapp.ui.theme.TagAircon
import com.example.androidsampleapp.ui.theme.TagAlert
import com.example.androidsampleapp.ui.theme.TagCall
import com.example.androidsampleapp.ui.theme.TagInfo
import com.example.androidsampleapp.ui.theme.dimensions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 受け取った通知の一覧。消去ボタンは一覧の右上に小さく置く。
 *
 * 1 件ずつ白いカードで出す。警報は他より先に見せたいので上にまとめ、
 * それ以外との間に線を引く。グループの中の順番は受け取った順のまま。
 *
 * 行をタップすると、その通知が持つ飛び先を [onNoticeClick] に返す。
 * どこへ飛ぶかは通知自身が知っているので、この一覧は分類と遷移先の対応を持たない。
 *
 * スリープ画面のような暗い背景にも置くため、カードの外の文字色と線の色は
 * [contentColor] で受け取る。カードの中はテーマに関係なく白地に濃い文字。
 *
 * 読み込み中と失敗の出し分けもここで持つ。スリープ画面と連絡先画面で同じにするため。
 */
@Composable
fun NoticeList(
    notices: List<Notice>,
    onNoticeClick: (Notice) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    loadFailed: Boolean = false,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    // 取得済みの一覧があるなら、読み込み中でも失敗してもそれを出したままにする。
    if (notices.isEmpty() && isLoading) {
        LoadingBox(modifier = modifier)
        return
    }
    if (notices.isEmpty() && loadFailed) {
        CenteredMessage(
            message = stringResource(R.string.notice_load_failed),
            modifier = modifier,
            color = contentColor.copy(alpha = 0.6f),
        )
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onClearClick, enabled = notices.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.notice_clear),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        if (notices.isEmpty()) {
            Text(
                text = stringResource(R.string.notice_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.dimensions.spaceLarge),
                textAlign = TextAlign.Center,
            )
            return@Column
        }

        val (alerts, others) = notices.partition { it.category == NoticeCategory.ALERT }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spaceSmall),
        ) {
            items(alerts, key = { it.id }) { notice ->
                NoticeCard(notice = notice, onClick = { onNoticeClick(notice) })
            }
            // 片方しか無いときに線だけ浮かないよう、両方あるときだけ引く。
            if (alerts.isNotEmpty() && others.isNotEmpty()) {
                item(key = ALERT_DIVIDER_KEY) {
                    HorizontalDivider(
                        color = contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.padding(
                            horizontal = MaterialTheme.dimensions.spaceMedium,
                            vertical = MaterialTheme.dimensions.spaceLarge,
                        ),
                    )
                }
            }
            items(others, key = { it.id }) { notice ->
                NoticeCard(notice = notice, onClick = { onNoticeClick(notice) })
            }
        }
    }
}

/**
 * 通知 1 件分のカード。
 *
 * ```
 * [種別] タイトル          時刻
 * 詳細
 * ```
 */
@Composable
private fun NoticeCard(
    notice: Notice,
    onClick: () -> Unit,
) {
    val dimensions = MaterialTheme.dimensions
    // minSdk 24 なので java.time は使わない。SimpleDateFormat はスレッドをまたがないここだけで使う。
    val timeFormat = remember { SimpleDateFormat(OCCURRED_AT_PATTERN, Locale.getDefault()) }

    Surface(
        color = NoticeCardBackground,
        contentColor = NoticeCardContent,
        shape = RoundedCornerShape(dimensions.noticeCardCorner),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = dimensions.noticeCardPaddingHorizontal,
                vertical = dimensions.spaceSmall,
            ),
            verticalArrangement = Arrangement.spacedBy(dimensions.spaceSmall),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions.spaceSmall),
            ) {
                NoticeTag(category = notice.category)
                Text(
                    text = notice.title.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // タイトルが長くても時刻を押し出さないよう、残りの幅だけを使う。
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = timeFormat.format(Date(notice.occurredAt)),
                    style = MaterialTheme.typography.labelLarge,
                    color = NoticeCardSubContent,
                    maxLines = 1,
                )
            }
            Text(
                text = notice.message,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 分類のタグ。幅を揃えて、タイトルの開始位置がカードごとにずれないようにする。 */
@Composable
private fun NoticeTag(category: NoticeCategory) {
    Surface(
        color = category.tagColor(),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.width(TAG_WIDTH),
    ) {
        Text(
            text = stringResource(category.labelRes()),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(vertical = 2.dp),
        )
    }
}

private fun NoticeCategory.tagColor(): Color = when (this) {
    NoticeCategory.CALL -> TagCall
    NoticeCategory.AIRCON -> TagAircon
    NoticeCategory.ALERT -> TagAlert
    NoticeCategory.INFO -> TagInfo
}

private val TAG_WIDTH = 80.dp

private const val OCCURRED_AT_PATTERN = "M月d日 HH:mm"

/** 通知の id と重ならない、区切り線用の key。 */
private const val ALERT_DIVIDER_KEY = "alert-divider"
