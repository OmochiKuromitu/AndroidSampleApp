package com.example.androidsampleapp.ui.aircon

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.example.androidsampleapp.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * エアコン画面の配線。ここが AppNavigation から呼ばれる入口になる。
 *
 * ViewModel の取得、State の購読、Effect の受け取りを担い、[AirconScreen] には
 * State と操作のコールバックだけを渡す。操作を Intent に変えるのはここ。
 */
@Composable
fun AirconRoute(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: AirconViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var errorCode by rememberSaveable { mutableStateOf<Int?>(null) }

    // Effect は一回きりの出来事なので State とは別に受け取り、届いた順に 1 つずつ処理する。
    // 画面が裏に回っている間も受け取る（前面に戻るまで溜めることはしない）。
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AirconEffect.ShowErrorDialog -> errorCode = effect.resultCode
                is AirconEffect.ShowMessage ->
                    snackbarHostState.showSnackbar(effect.text ?: context.getString(effect.messageRes))
            }
        }
    }

    errorCode?.let { code ->
        AlertDialog(
            onDismissRequest = { errorCode = null },
            text = { Text(stringResource(R.string.aircon_error_result_code, code)) },
            confirmButton = {
                TextButton(onClick = { errorCode = null }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }

    AirconScreen(
        state = state,
        onPowerToggle = { viewModel.onIntent(AirconIntent.PowerToggled(it)) },
        onTemperatureDownClick = { viewModel.onIntent(AirconIntent.TemperatureDownClicked) },
        onTemperatureUpClick = { viewModel.onIntent(AirconIntent.TemperatureUpClicked) },
        onModeSelect = { viewModel.onIntent(AirconIntent.ModeSelected(it)) },
        modifier = modifier,
    )
}
