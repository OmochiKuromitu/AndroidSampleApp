package com.example.androidsampleapp

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.androidsampleapp.ui.navigation.IdleTimer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var idleTimer: IdleTimer

    override fun onCreate() {
        super.onCreate()
        observeProcessLifecycle()
    }

    /**
     * バックグラウンドに移ったらスリープにする。
     *
     * Activity の onStop ではなく ProcessLifecycleOwner を見るのは、
     * 画面回転による再生成では ON_STOP が飛ばないため。Activity で拾うと
     * 回転しただけでスリープに落ちてしまう。
     */
    private fun observeProcessLifecycle() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    idleTimer.onEnteredBackground()
                }
            },
        )
    }
}
