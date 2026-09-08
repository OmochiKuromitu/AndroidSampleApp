package com.example.androidsampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.androidsampleapp.core.AppStateHolder
import com.example.androidsampleapp.service.MonitoringService
import com.example.androidsampleapp.ui.navigation.AppNavigation
import com.example.androidsampleapp.ui.navigation.IdleTimer
import com.example.androidsampleapp.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appStateHolder: AppStateHolder

    @Inject
    lateinit var idleTimer: IdleTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 前面にいるうちに起動する。Application からだと前面サービスの開始が制限される。
        MonitoringService.start(this)

        setContent {
            AppTheme {
                AppNavigation(
                    appStateHolder = appStateHolder,
                    idleTimer = idleTimer,
                )
            }
        }
    }
}
