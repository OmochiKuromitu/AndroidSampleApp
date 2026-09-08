package com.example.androidsampleapp

data class ProfileState(
    val userName: String = "Sample User",
    val email: String = "sample@example.com",
    val notificationsEnabled: Boolean = true,
    val darkThemeEnabled: Boolean = false,
    val isSaving: Boolean = false,
) : UiState

sealed interface ProfileIntent : UiIntent {
    data class NotificationsToggled(val enabled: Boolean) : ProfileIntent
    data class DarkThemeToggled(val enabled: Boolean) : ProfileIntent
    data object SaveClicked : ProfileIntent
    data object SaveCompleted : ProfileIntent
}

sealed interface ProfileEffect : UiEffect {
    data class ShowMessage(val message: String) : ProfileEffect
}
