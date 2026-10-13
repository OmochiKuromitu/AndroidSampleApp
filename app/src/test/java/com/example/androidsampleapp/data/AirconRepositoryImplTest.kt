package com.example.androidsampleapp.data

import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.model.AirconSettings
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AirconRepositoryImplTest {

    @Test
    fun `未設定なら機器の現在値に関係なく温度とモードはデフォルト`() = runTest {
        val fixture = AirconTestFixture()
        backgroundScope.launch { fixture.deviceRepository.monitor() }
        runCurrent()

        assertEquals(AirconSettings(), fixture.repository.settings.value)
        assertEquals(AirconMode.HEAT, fixture.repository.aircon.value.mode)
        assertEquals(22.5, fixture.repository.aircon.value.targetTemperature, 0.0)
        assertTrue(fixture.api.temperatures.isEmpty())
        assertTrue(fixture.api.modes.isEmpty())
    }

    @Test
    fun `温度だけ設定した場合はモードを未設定のデフォルトのまま残す`() = runTest {
        val fixture = AirconTestFixture()
        fixture.repository.setTargetTemperature(24.5)

        assertEquals(AirconSettings(targetTemperature = 24.5), fixture.repository.settings.value)
        assertEquals(AirconMode.HEAT, fixture.repository.aircon.value.mode)
        assertEquals("aircon-test", fixture.api.temperatures.single().deviceId)
    }

    @Test
    fun `モードだけ設定した場合は温度を未設定のデフォルトのまま残す`() = runTest {
        val fixture = AirconTestFixture()
        fixture.repository.setMode(AirconMode.DRY)

        assertEquals(AirconSettings(mode = AirconMode.DRY), fixture.repository.settings.value)
        assertEquals(22.5, fixture.repository.aircon.value.targetTemperature, 0.0)
        assertEquals("DRY", fixture.api.modes.single().mode)
        assertEquals("aircon-test", fixture.api.modes.single().deviceId)
    }

    @Test
    fun `温度とモードは互いの保存値を維持し、機器が受理した温度を保存する`() = runTest {
        val fixture = AirconTestFixture()
        fixture.repository.setMode(AirconMode.DRY)
        fixture.api.acceptedTemperature = 25.0
        fixture.repository.setTargetTemperature(25.5)

        assertEquals(25.5, fixture.api.temperatures.single().targetTemperature, 0.0)
        assertEquals(AirconSettings(25.0, AirconMode.DRY), fixture.repository.settings.value)
        fixture.repository.setMode(AirconMode.FAN)
        assertEquals(AirconSettings(25.0, AirconMode.FAN), fixture.repository.settings.value)
    }

    @Test
    fun `設定に失敗したら保存値と機器の現在値を変更しない`() = runTest {
        val fixture = AirconTestFixture()
        fixture.repository.setTargetTemperature(24.5)
        fixture.repository.setMode(AirconMode.DRY)
        val previousSettings = fixture.repository.settings.value
        val previousAircon = fixture.repository.aircon.value
        fixture.api.failCommands = true

        assertTrue(runCatching { fixture.repository.setTargetTemperature(28.0) }.exceptionOrNull() is IOException)
        assertTrue(runCatching { fixture.repository.setMode(AirconMode.HEAT) }.exceptionOrNull() is IOException)
        assertEquals(previousSettings, fixture.repository.settings.value)
        assertEquals(previousAircon, fixture.repository.aircon.value)
    }

    @Test
    fun `電源操作と定期取得では保存した温度とモードを上書きしない`() = runTest {
        val fixture = AirconTestFixture()
        fixture.repository.setTargetTemperature(24.5)
        fixture.repository.setMode(AirconMode.DRY)
        fixture.api.status = fixture.api.status.copy(mode = "FAN", targetTemperature = 19.0, roomTemperature = 18.0)
        fixture.repository.setPower(false)
        backgroundScope.launch { fixture.deviceRepository.monitor() }
        runCurrent()

        assertEquals(AirconSettings(24.5, AirconMode.DRY), fixture.repository.settings.value)
        assertEquals(AirconMode.FAN, fixture.repository.aircon.value.mode)
        assertEquals(19.0, fixture.repository.aircon.value.targetTemperature, 0.0)
        assertEquals(18.0, fixture.repository.aircon.value.roomTemperature, 0.0)
    }

    @Test
    fun `新しい Repository には以前のプロセスの保存値が残らない`() = runTest {
        val previousProcess = AirconTestFixture()
        previousProcess.repository.setTargetTemperature(24.5)
        previousProcess.repository.setMode(AirconMode.DRY)

        val newProcess = AirconTestFixture()
        newProcess.api.status = previousProcess.api.status
        backgroundScope.launch { newProcess.deviceRepository.monitor() }
        runCurrent()

        assertEquals(AirconSettings(), newProcess.repository.settings.value)
        assertEquals(24.5, newProcess.repository.aircon.value.targetTemperature, 0.0)
        assertEquals(AirconMode.DRY, newProcess.repository.aircon.value.mode)
    }

    @Test
    fun `キャンセル後に操作の応答が届いても保存値を変更しない`() = runTest {
        val fixture = AirconTestFixture()
        fixture.repository.setTargetTemperature(24.5)
        fixture.api.beforeResponse = { withContext(NonCancellable) { delay(100) } }
        val command = launch { fixture.repository.setTargetTemperature(28.0) }
        runCurrent()
        command.cancel()
        advanceUntilIdle()

        assertEquals(AirconSettings(targetTemperature = 24.5), fixture.repository.settings.value)
        assertEquals(24.5, fixture.repository.aircon.value.targetTemperature, 0.0)
    }
}
