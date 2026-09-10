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
import com.example.androidsampleapp.ui.theme.TagAircon
import com.example.androidsampleapp.ui.theme.TagAlert
import com.example.androidsampleapp.ui.theme.TagCall
import com.example.androidsampleapp.ui.theme.TagInfo
import com.example.androidsampleapp.ui.theme.dimensions

/**
 * 受け取った通知の一覧。消去ボタンは一覧の右上に小さく置く。
 *
 * 行をタップすると、その通知が持つ飛び先を [onNoticeClick] に返す。
 * どこへ飛ぶかは通知自身が知っているので、この一覧は分類と遷移先の対応を持たない。
 *
 * スリープ画面のような暗い背景にも置くため、文字色は [contentColor] で受け取る。
 */
@Composable
fun NoticeList(
    notices: List<Notice>,
    onNoticeClick: (Notice) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
) {
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

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(notices, key = { it.id }) { notice ->
                NoticeRow(
                    notice = notice,
                    onClick = { onNoticeClick(notice) },
                    contentColor = contentColor,
                )
                HorizontalDivider(color = contentColor.copy(alpha = 0.15f))
            }
        }
    }
}

@Composable
private fun NoticeRow(
    notice: Notice,
    onClick: () -> Unit,
    contentColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = MaterialTheme.dimensions.spaceMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spaceMedium),
    ) {
        NoticeTag(category = notice.category)
        Text(
            text = notice.message,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 分類のタグ。幅を揃えて、本文の開始位置が行ごとにずれないようにする。 */
@Composable
private fun NoticeTag(category: NoticeCategory) {
    Surface(
        color = category.tagColor(),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.width(TAG_WIDTH),
    ) {
        Text(
            text = category.label,
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
