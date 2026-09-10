package com.example.androidsampleapp.ui.navigation

import com.example.androidsampleapp.config.AppConfig
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
        val idleTimer = IdleTimer(config, backgroundScope)
        runCurrent()
        assertFalse(idleTimer.isSleeping.value)

        idleTimer.onEnteredBackground()

        assertTrue(idleTimer.isSleeping.value)
    }

    @Test
    fun `画面が消えるとスリープになる`() = runTest {
        val idleTimer = IdleTimer(config, backgroundScope)
        runCurrent()

        idleTimer.onScreenOff()

        assertTrue(idleTimer.isSleeping.value)
    }

    @Test
    fun `スリープタブが選ばれるとスリープになる`() = runTest {
        val idleTimer = IdleTimer(config, backgroundScope)
        runCurrent()

        idleTimer.onSleepRequested()

        assertTrue(idleTimer.isSleeping.value)
    }

    @Test
    fun `無操作がタイムアウトするとスリープになる`() = runTest {
        val idleTimer = IdleTimer(config, backgroundScope)
        runCurrent()

        advanceTimeBy(29.seconds)
        runCurrent()
        assertFalse(idleTimer.isSleeping.value)

        advanceTimeBy(2.seconds)
        runCurrent()
        assertTrue(idleTimer.isSleeping.value)
    }

    @Test
    fun `操作するとタイムアウトが測り直される`() = runTest {
        val idleTimer = IdleTimer(config, backgroundScope)
        runCurrent()

        advanceTimeBy(25.seconds)
        idleTimer.onInteraction()
        runCurrent()

        // 起動からは 50 秒経っているが、最後の操作からはまだ 25 秒。
        advanceTimeBy(25.seconds)
        runCurrent()
        assertFalse(idleTimer.isSleeping.value)

        advanceTimeBy(6.seconds)
        runCurrent()
        assertTrue(idleTimer.isSleeping.value)
    }

    @Test
    fun `スリープ中に触れただけでは復帰しない`() = runTest {
        // 触れただけで復帰すると、スリープ画面の解除操作を定義した意味がなくなる。
        val idleTimer = IdleTimer(config, backgroundScope)
        runCurrent()
        idleTimer.onEnteredBackground()

        idleTimer.onInteraction()
        runCurrent()

        assertTrue(idleTimer.isSleeping.value)
    }

    @Test
    fun `wake で復帰する`() = runTest {
        val idleTimer = IdleTimer(config, backgroundScope)
        runCurrent()
        idleTimer.onEnteredBackground()
        assertTrue(idleTimer.isSleeping.value)

        idleTimer.wake()
        runCurrent()

        assertFalse(idleTimer.isSleeping.value)
    }
}
