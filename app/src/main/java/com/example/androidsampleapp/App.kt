package com.example.androidsampleapp

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.androidsampleapp.domain.repository.DeviceRepository
import com.example.androidsampleapp.ui.navigation.IdleTimer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var idleTimer: IdleTimer

    @Inject
    lateinit var deviceRepository: DeviceRepository

    override fun onCreate() {
        super.onCreate()
        observeProcessLifecycle()
        monitorDeviceWhileForeground()
    }

    /**
     * バックグラウンドに移ったらスリープにする。他アプリへ移った場合や、画面が消えた場合を拾う。
     *
     * Activity の onStop ではなく ProcessLifecycleOwner を見るのは、
     * 構成変更（画面サイズ、ロケール、ダークテーマの切り替えなど）による
     * 再生成では ON_STOP が飛ばないため。Activity で拾うと、
     * 画面が変わっていないのにスリープへ落ちる。
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

    /**
     * アプリが前面にいる間だけ、機器の状態を定期的に取りに行く。
     *
     * 裏に回ると止まり、前面に戻ると再開する。画面が無い間に取り続けても見る人がいないので、
     * 常駐のサービスは置かない。どの画面が出ているかには関係なく動かしたいので、
     * 画面ではなくプロセスの寿命に合わせる。
     */
    private fun monitorDeviceWhileForeground() {
        val owner = ProcessLifecycleOwner.get()
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                deviceRepository.monitor()
            }
        }
    }
}
