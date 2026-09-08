package com.example.androidsampleapp

import kotlinx.coroutines.delay

class ProfileViewModel : MviViewModel<ProfileState, ProfileIntent, ProfileEffect>(
    initialState = ProfileState(),
    reducer = ProfileReducer(),
) {
    override suspend fun handle(intent: ProfileIntent, previous: ProfileState, current: ProfileState) {
        when (intent) {
            ProfileIntent.SaveClicked -> {
                delay(SAVE_DELAY_MS) // 保存 API の代わり
                dispatch(ProfileIntent.SaveCompleted)
                sendEffect(ProfileEffect.ShowMessage("設定を保存しました"))
            }

            is ProfileIntent.NotificationsToggled,
            is ProfileIntent.DarkThemeToggled,
            ProfileIntent.SaveCompleted,
            -> Unit
        }
    }

    private companion object {
        const val SAVE_DELAY_MS = 500L
    }
}
