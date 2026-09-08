package com.example.androidsampleapp.feature.profile

import com.example.androidsampleapp.core.mvi.Reducer

class ProfileReducer : Reducer<ProfileState, ProfileIntent> {
    override fun reduce(state: ProfileState, intent: ProfileIntent): ProfileState = when (intent) {
        is ProfileIntent.NotificationsToggled -> state.copy(notificationsEnabled = intent.enabled)
        is ProfileIntent.DarkThemeToggled -> state.copy(darkThemeEnabled = intent.enabled)
        ProfileIntent.SaveClicked -> state.copy(isSaving = true)
        ProfileIntent.SaveCompleted -> state.copy(isSaving = false)
    }
}
