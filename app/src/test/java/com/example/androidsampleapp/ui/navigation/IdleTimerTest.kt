package com.example.androidsampleapp.ui.navigation

import com.example.androidsampleapp.config.AppConfig
import com.example.androidsampleapp.core.AppStateHolder
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IdleTimerTest {

    private val config = AppConfig(
        deviceHost = "127.0.0.1",
        tcpPort = 50100,
        udpPort = 50101,
        useFakeDevice = true,
        sleepTimeout = 30.seconds,
    )

    @Test
    fun `バックグラウンドに移ると無操作時間に関係なくスリープになる`() = runTest {
        val appStateHolder = AppStateHolder()
        val idleTimer = IdleTimer(appStateHolder, config, backgroundScope)
        runCurrent()
        assertFalse(appStateHolder.isSleeping.value)

        idleTimer.onEnteredBackground()

        assertTrue(appStateHolder.isSleeping.value)
    }

    @Test
    fun `画面が消えるとスリープになる`() = runTest {
        val appStateHolder = AppStateHolder()
        val idleTimer = IdleTimer(appStateHolder, config, backgroundScope)
        runCurrent()
        assertFalse(appStateHolder.isSleeping.value)

        idleTimer.onScreenOff()

        assertTrue(appStateHolder.isSleeping.value)
    }

    @Test
    fun `無操作がタイムアウトするとスリープになる`() = runTest {
        val appStateHolder = AppStateHolder()
        IdleTimer(appStateHolder, config, backgroundScope)
        runCurrent()

        advanceTimeBy(29.seconds)
        runCurrent()
        assertFalse(appStateHolder.isSleeping.value)

        advanceTimeBy(2.seconds)
        runCurrent()
        assertTrue(appStateHolder.isSleeping.value)
    }

    @Test
    fun `操作するとタイムアウトが測り直される`() = runTest {
        val appStateHolder = AppStateHolder()
        val idleTimer = IdleTimer(appStateHolder, config, backgroundScope)
        runCurrent()

        advanceTimeBy(25.seconds)
        idleTimer.onInteraction()
        runCurrent()

        // 起動からは 50 秒経っているが、最後の操作からはまだ 25 秒。
        advanceTimeBy(25.seconds)
        runCurrent()
        assertFalse(appStateHolder.isSleeping.value)

        advanceTimeBy(6.seconds)
        runCurrent()
        assertTrue(appStateHolder.isSleeping.value)
    }

    @Test
    fun `スリープ中に触れただけでは復帰しない`() = runTest {
        // 触れただけで復帰すると、スリープ画面の解除操作を定義した意味がなくなる。
        val appStateHolder = AppStateHolder()
        val idleTimer = IdleTimer(appStateHolder, config, backgroundScope)
        runCurrent()
        idleTimer.onEnteredBackground()

        idleTimer.onInteraction()
        runCurrent()

        assertTrue(appStateHolder.isSleeping.value)
    }

    @Test
    fun `wake で復帰する`() = runTest {
        val appStateHolder = AppStateHolder()
        val idleTimer = IdleTimer(appStateHolder, config, backgroundScope)
        runCurrent()
        idleTimer.onEnteredBackground()
        assertTrue(appStateHolder.isSleeping.value)

        idleTimer.wake()
        runCurrent()

        assertFalse(appStateHolder.isSleeping.value)
    }
}
