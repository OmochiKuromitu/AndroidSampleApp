package com.example.androidsampleapp

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
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
        observeScreenOff()
    }

    /**
     * バックグラウンドに移ったらスリープにする。他アプリへ移った場合を拾う。
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
     * 電源ボタンや画面消灯タイムアウトで画面が消えたらスリープにする。
     *
     * キオスク（LockTask）では消灯・復帰でアプリに戻るだけなので、
     * ProcessLifecycleOwner だけに頼ると取りこぼす。ON_STOP は構成変更を
     * 吸収するために約 700ms 遅れて飛び、その間に復帰すると打ち消されるため。
     * ACTION_SCREEN_OFF は遅延なく届く。
     *
     * ACTION_SCREEN_OFF は manifest 登録では受け取れないので、実行時に登録する。
     * 保護されたシステムブロードキャストなので受信は NOT_EXPORTED でよい。
     * Application と同じ寿命でよいため解除はしない。
     */
    private fun observeScreenOff() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                idleTimer.onScreenOff()
            }
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }
}
