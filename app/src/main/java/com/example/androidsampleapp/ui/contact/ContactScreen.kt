package com.example.androidsampleapp.ui.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidsampleapp.R
import com.example.androidsampleapp.domain.model.CallHistory
import com.example.androidsampleapp.domain.model.Contact
import com.example.androidsampleapp.ui.common.CenteredMessage
import com.example.androidsampleapp.ui.common.LoadingBox
import com.example.androidsampleapp.ui.common.PanelPreview
import com.example.androidsampleapp.ui.common.PreviewSurface
import com.example.androidsampleapp.ui.theme.TagAlert
import com.example.androidsampleapp.ui.theme.dimensions

/**
 * 連絡先画面の表示。電話帳と履歴を画面内のタブで出し分ける。
 *
 * この 2 つは遷移を伴わないので、下部バーのタブとは別物。どちらを出しているかは
 * [ContactState.selectedList] が持ち、切り替えても通信は起きない。
 *
 * 入口は [ContactRoute]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(
    state: ContactState,
    onIntent: (ContactIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = state.selectedList.ordinal) {
            ContactList.entries.forEach { list ->
                val badgeCount = if (list == ContactList.HISTORY) state.missedCallCount else 0
                Tab(
                    selected = list == state.selectedList,
                    onClick = { onIntent(ContactIntent.ListSelected(list)) },
                    text = {
                        BadgedBox(
                            badge = { if (badgeCount > 0) Badge { Text(badgeCount.toString()) } },
                        ) {
                            Text(stringResource(list.labelRes))
                        }
                    },
                )
            }
        }

        when {
            state.isLoading -> LoadingBox()

            state.loadFailed -> CenteredMessage(
                message = stringResource(R.string.contact_load_failed),
                color = MaterialTheme.colorScheme.error,
            )

            else -> when (state.selectedList) {
                ContactList.PHONEBOOK -> PhonebookList(state.contacts)
                ContactList.HISTORY -> CallHistoryList(state.histories)
            }
        }
    }
}

@Composable
private fun PhonebookList(contacts: List<Contact>) {
    if (contacts.isEmpty()) {
        CenteredMessage(message = stringResource(R.string.contact_phonebook_empty))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(contacts, key = { it.id }) { contact ->
            ListItem(
                headlineContent = { Text(contact.name) },
                supportingContent = { Text(contact.phoneNumber) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun CallHistoryList(histories: List<CallHistory>) {
    if (histories.isEmpty()) {
        CenteredMessage(message = stringResource(R.string.contact_history_empty))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(histories, key = { it.id }) { history ->
            ListItem(
                headlineContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            MaterialTheme.dimensions.spaceSmall,
                        ),
                    ) {
                        Text(history.name)
                        if (history.isMissed) MissedTag()
                    }
                },
                supportingContent = { Text(history.occurredAt) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun MissedTag() {
    Surface(color = TagAlert, shape = RoundedCornerShape(4.dp)) {
        Text(
            text = stringResource(R.string.contact_missed),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

private val previewContacts = listOf(
    Contact("1", "管理室", "0001"),
    Contact("2", "玄関", "0101"),
    Contact("3", "駐車場", "0102"),
)

private val previewHistories = listOf(
    CallHistory("1", "玄関", "9月11日 14:32", isMissed = true),
    CallHistory("2", "管理室", "9月11日 10:05", isMissed = false),
    CallHistory("3", "駐車場", "9月10日 08:12", isMissed = true),
)

@PanelPreview
@Composable
private fun ContactScreenPhonebookPreview() {
    PreviewSurface {
        ContactScreen(
            state = ContactState(
                selectedList = ContactList.PHONEBOOK,
                contacts = previewContacts,
                histories = previewHistories,
                missedCallCount = 2,
            ),
            onIntent = {},
        )
    }
}

@PanelPreview
@Composable
private fun ContactScreenHistoryPreview() {
    PreviewSurface {
        ContactScreen(
            state = ContactState(
                selectedList = ContactList.HISTORY,
                contacts = previewContacts,
                histories = previewHistories,
                missedCallCount = 2,
            ),
            onIntent = {},
        )
    }
}

@PanelPreview
@Composable
private fun ContactScreenLoadFailedPreview() {
    PreviewSurface {
        ContactScreen(state = ContactState(loadFailed = true), onIntent = {})
    }
}
