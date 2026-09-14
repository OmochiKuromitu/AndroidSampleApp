package com.example.androidsampleapp.ui.aircon

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidsampleapp.core.mvi.CollectEffect

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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is AirconEffect.ShowMessage ->
                snackbarHostState.showSnackbar(context.getString(effect.messageRes))
        }
    }

    AirconScreen(
        state = state,
        onPowerToggle = { viewModel.dispatch(AirconIntent.PowerToggled(it)) },
        onTemperatureDownClick = { viewModel.dispatch(AirconIntent.TemperatureDownClicked) },
        onTemperatureUpClick = { viewModel.dispatch(AirconIntent.TemperatureUpClicked) },
        onModeSelect = { viewModel.dispatch(AirconIntent.ModeSelected(it)) },
        modifier = modifier,
    )
}
