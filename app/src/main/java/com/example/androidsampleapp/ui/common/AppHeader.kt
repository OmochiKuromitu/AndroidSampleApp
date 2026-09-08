package com.example.androidsampleapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.androidsampleapp.R
import com.example.androidsampleapp.domain.model.ConnectionState
import com.example.androidsampleapp.ui.theme.dimensions

/**
 * 全画面共通のヘッダー。タイトルと機器の接続状態を出す。
 */
@Composable
fun AppHeader(
    title: String,
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MaterialTheme.dimensions.headerHeight)
                .padding(horizontal = MaterialTheme.dimensions.spaceLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            ConnectionIndicator(connectionState)
        }
    }
}

@Composable
private fun ConnectionIndicator(connectionState: ConnectionState) {
    val (label, color) = when (connectionState) {
        ConnectionState.CONNECTED ->
            stringResource(R.string.connection_connected) to MaterialTheme.colorScheme.primary

        ConnectionState.CONNECTING ->
            stringResource(R.string.connection_connecting) to MaterialTheme.colorScheme.tertiary

        ConnectionState.DISCONNECTED ->
            stringResource(R.string.connection_disconnected) to MaterialTheme.colorScheme.error
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spaceSmall),
    ) {
        Surface(
            modifier = Modifier.size(MaterialTheme.dimensions.spaceSmall),
            shape = CircleShape,
            color = color,
            content = {},
        )
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}
