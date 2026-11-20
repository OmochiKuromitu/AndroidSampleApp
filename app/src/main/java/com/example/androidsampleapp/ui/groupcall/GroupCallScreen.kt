package com.example.androidsampleapp.ui.groupcall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.androidsampleapp.R
import com.example.androidsampleapp.domain.model.GroupCallMember
import com.example.androidsampleapp.ui.common.PanelButton
import com.example.androidsampleapp.ui.common.PanelPreview
import com.example.androidsampleapp.ui.common.PreviewSurface
import com.example.androidsampleapp.ui.theme.CallActiveGreen
import com.example.androidsampleapp.ui.theme.dimensions

/**
 * グループ通話の画面の表示。State を描き、操作をコールバックで返すだけ。
 *
 * 上から、通話中のラベル（固定）、参加しているメンバーの一覧（ここだけスクロールする）、
 * マイク音量・スピーカー・ミュート、通話終了のボタン。
 *
 * 入口は [GroupCallRoute]。
 */
@Composable
fun GroupCallScreen(
    state: GroupCallState,
    onMicVolumeChange: (Float) -> Unit,
    onSpeakerClick: () -> Unit,
    onMuteClick: () -> Unit,
    onEndCallClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = MaterialTheme.dimensions

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensions.spaceLarge),
        verticalArrangement = Arrangement.spacedBy(dimensions.spaceMedium),
    ) {
        GroupCallLabel(memberCount = state.memberCount)

        // メンバーが多いときは、ここだけスクロールする。ラベルと下の操作は動かない。
        MemberList(
            members = state.members,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )

        CallControls(
            micVolume = state.micVolume,
            isSpeakerOn = state.isSpeakerOn,
            isMuted = state.isMuted,
            onMicVolumeChange = onMicVolumeChange,
            onSpeakerClick = onSpeakerClick,
            onMuteClick = onMuteClick,
        )

        PanelButton(
            text = stringResource(R.string.group_call_end),
            onClick = onEndCallClick,
            containerColor = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** いちばん上の緑のラベル。「グループ通話中」と人数。 */
@Composable
private fun GroupCallLabel(memberCount: Int) {
    val dimensions = MaterialTheme.dimensions
    Surface(
        color = CallActiveGreen,
        contentColor = Color.White,
        shape = RoundedCornerShape(dimensions.cardCorner),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = dimensions.spaceLarge, vertical = dimensions.spaceMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.group_call_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.group_call_member_count, memberCount),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@Composable
private fun MemberList(
    members: List<GroupCallMember>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(members, key = { it.id }) { member ->
            MemberRow(member = member)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

/** メンバー 1 人分の行。名前を大きく、その下に端末の種類。 */
@Composable
private fun MemberRow(member: GroupCallMember) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.dimensions.spaceMedium),
    ) {
        Text(
            text = member.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = member.device,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * マイク音量・スピーカー・ミュート。
 *
 * 画面を仮置きした段階なので、操作すると画面の中で値が変わるだけで、通話にはまだ反映しない。
 */
@Composable
private fun CallControls(
    micVolume: Float,
    isSpeakerOn: Boolean,
    isMuted: Boolean,
    onMicVolumeChange: (Float) -> Unit,
    onSpeakerClick: () -> Unit,
    onMuteClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spaceMedium),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.group_call_mic_volume),
                style = MaterialTheme.typography.labelLarge,
            )
            Slider(value = micVolume, onValueChange = onMicVolumeChange)
        }
        ControlToggle(
            label = stringResource(R.string.group_call_speaker),
            checked = isSpeakerOn,
            icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
            onClick = onSpeakerClick,
        )
        ControlToggle(
            label = stringResource(R.string.group_call_mute),
            checked = isMuted,
            icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
            onClick = onMuteClick,
        )
    }
}

/** 上にラベル、下にアイコンのボタン。オンのときはアイコンの色を変える。 */
@Composable
private fun ControlToggle(
    label: String,
    checked: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        IconToggleButton(
            checked = checked,
            onCheckedChange = { onClick() },
            modifier = Modifier.size(MaterialTheme.dimensions.minTouchTarget),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

private val previewMembers = listOf(
    GroupCallMember(id = "1", name = "1号車運転台", device = "ハンドセット"),
    GroupCallMember(id = "2", name = "添乗員端末2", device = "車両連絡"),
)

@PanelPreview
@Composable
private fun GroupCallScreenPreview() {
    PreviewSurface {
        GroupCallScreen(
            state = GroupCallState(members = previewMembers),
            onMicVolumeChange = {},
            onSpeakerClick = {},
            onMuteClick = {},
            onEndCallClick = {},
        )
    }
}

@PanelPreview
@Composable
private fun GroupCallScreenManyMembersPreview() {
    PreviewSurface {
        GroupCallScreen(
            // 一覧が画面に収まらず、スクロールになるとき。
            state = GroupCallState(
                members = (1..12).map { GroupCallMember(id = "$it", name = "${it}号車運転台", device = "ハンドセット") },
            ),
            onMicVolumeChange = {},
            onSpeakerClick = {},
            onMuteClick = {},
            onEndCallClick = {},
        )
    }
}

@PanelPreview
@Composable
private fun GroupCallScreenMutedPreview() {
    PreviewSurface {
        GroupCallScreen(
            state = GroupCallState(members = previewMembers, micVolume = 0.2f, isSpeakerOn = true, isMuted = true),
            onMicVolumeChange = {},
            onSpeakerClick = {},
            onMuteClick = {},
            onEndCallClick = {},
        )
    }
}
