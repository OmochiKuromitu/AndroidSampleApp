package com.example.androidsampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.androidsampleapp.service.MonitoringService
import com.example.androidsampleapp.ui.navigation.AppNavigation
import com.example.androidsampleapp.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 画面の入口。遷移に必要なものは AppNavigation が自分で取るので、ここでは渡さない。
 * @AndroidEntryPoint は hiltViewModel() がこの Activity を辿るために要る。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 前面にいるうちに起動する。Application からだと前面サービスの開始が制限される。
        MonitoringService.start(this)

        setContent {
            AppTheme {
                AppNavigation()
            }
        }
    }
}
