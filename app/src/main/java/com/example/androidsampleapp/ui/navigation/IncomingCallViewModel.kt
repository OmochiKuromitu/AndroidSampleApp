package com.example.androidsampleapp.ui.navigation

import androidx.lifecycle.ViewModel
import com.example.androidsampleapp.core.AppStateHolder
import com.example.androidsampleapp.domain.model.IncomingCall
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * 着信を Composable から見るための窓口。[IncomingCallRouter] が使う。
 *
 * スリープとは別の責務なので、まとめずに分けてある。
 */
@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    appStateHolder: AppStateHolder,
) : ViewModel() {

    val incomingCall: StateFlow<IncomingCall?> = appStateHolder.incomingCall
}
