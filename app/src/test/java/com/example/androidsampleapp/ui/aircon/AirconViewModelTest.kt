package com.example.androidsampleapp.ui.aircon

import androidx.lifecycle.ViewModelStore
import com.example.androidsampleapp.R
import com.example.androidsampleapp.data.AirconTestFixture
import com.example.androidsampleapp.domain.model.AirconMode
import com.example.androidsampleapp.domain.model.AirconSettings
import com.example.androidsampleapp.domain.usecase.ObserveAirconSettingsUseCase
import com.example.androidsampleapp.domain.usecase.ObserveAirconStateUseCase
import com.example.androidsampleapp.domain.usecase.ObserveConnectionStateUseCase
import com.example.androidsampleapp.domain.usecase.SetAirconModeUseCase
import com.example.androidsampleapp.domain.usecase.SetAirconPowerUseCase
import com.example.androidsampleapp.domain.usecase.SetAirconTemperatureUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AirconViewModelTest {
    private lateinit var store: ViewModelStore

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        store = ViewModelStore()
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun viewModel(fixture: AirconTestFixture): AirconViewModel {
        val viewModel = AirconViewModel(
            observeAircon = ObserveAirconStateUseCase(fixture.repository),
            observeSettings = ObserveAirconSettingsUseCase(fixture.repository),
            observeConnectionState = ObserveConnectionStateUseCase(fixture.deviceRepository),
            setPower = SetAirconPowerUseCase(fixture.repository),
            setMode = SetAirconModeUseCase(fixture.repository),
            setTemperature = SetAirconTemperatureUseCase(fixture.repository),
        )
        store.put("aircon", viewModel)
        return viewModel
    }

    @Test
    fun `初回表示はデフォルト設定を使い、電源と室温は機器の現在値を使う`() = runTest {
        val fixture = AirconTestFixture()
        val viewModel = viewModel(fixture)
        backgroundScope.launch { fixture.deviceRepository.monitor() }
        runCurrent()

        assertEquals(AirconSettings(), viewModel.uiState.value.settings)
        assertTrue(viewModel.uiState.value.aircon.isOn)
        assertEquals(20.0, viewModel.uiState.value.aircon.roomTemperature, 0.0)
        assertEquals(AirconMode.HEAT, viewModel.uiState.value.aircon.mode)
    }

    @Test
    fun `画面を作り直しても初期 State から保存値を復元し、設定を再送しない`() = runTest {
        val fixture = AirconTestFixture()
        fixture.repository.setTargetTemperature(24.5)
        fixture.repository.setMode(AirconMode.DRY)
        val first = viewModel(fixture)
        runCurrent()
        assertEquals(AirconSettings(24.5, AirconMode.DRY), first.uiState.value.settings)
        store.clear()

        val reopened = viewModel(fixture)
        assertEquals(AirconSettings(24.5, AirconMode.DRY), reopened.uiState.value.settings)
        runCurrent()
        assertEquals(1, fixture.api.temperatures.size)
        assertEquals(1, fixture.api.modes.size)
    }

    @Test
    fun `温度の上げ下げは機器の現在値でなく表示している保存値を基準にする`() = runTest {
        val fixture = AirconTestFixture()
        fixture.repository.setTargetTemperature(24.5)
        fixture.api.status = fixture.api.status.copy(targetTemperature = 18.0)
        backgroundScope.launch { fixture.deviceRepository.monitor() }
        val viewModel = viewModel(fixture)
        runCurrent()

        viewModel.onIntent(AirconIntent.TemperatureUpClicked)
        runCurrent()
        assertEquals(25.0, fixture.api.temperatures.last().targetTemperature, 0.0)
        assertEquals(25.0, viewModel.uiState.value.settings.targetTemperature, 0.0)
        viewModel.onIntent(AirconIntent.TemperatureDownClicked)
        runCurrent()
        assertEquals(24.5, viewModel.uiState.value.settings.targetTemperature, 0.0)
        assertFalse(viewModel.uiState.value.isSendingCommand)
    }

    @Test
    fun `モードを保存し、温度変更の失敗では保存値を維持してメッセージを出す`() = runTest {
        val fixture = AirconTestFixture()
        val viewModel = viewModel(fixture)
        viewModel.onIntent(AirconIntent.ModeSelected(AirconMode.DRY))
        runCurrent()
        assertEquals(AirconSettings(mode = AirconMode.DRY), viewModel.uiState.value.settings)

        fixture.api.failCommands = true
        viewModel.onIntent(AirconIntent.TemperatureUpClicked)
        runCurrent()
        assertEquals(AirconSettings(mode = AirconMode.DRY), viewModel.uiState.value.settings)
        assertFalse(viewModel.uiState.value.isSendingCommand)
        assertEquals(AirconEffect.ShowMessage(R.string.command_failed), viewModel.effect.first())
    }

    @Test
    fun `保存値から操作した場合も UseCase が温度の上下限を守る`() = runTest {
        val fixture = AirconTestFixture()
        fixture.repository.setTargetTemperature(30.0)
        val viewModel = viewModel(fixture)
        runCurrent()
        viewModel.onIntent(AirconIntent.TemperatureUpClicked)
        runCurrent()
        assertEquals(30.0, fixture.api.temperatures.last().targetTemperature, 0.0)

        fixture.repository.setTargetTemperature(16.0)
        runCurrent()
        viewModel.onIntent(AirconIntent.TemperatureDownClicked)
        runCurrent()
        assertEquals(16.0, fixture.api.temperatures.last().targetTemperature, 0.0)
        assertFalse(viewModel.uiState.value.isSendingCommand)
    }

    @Test
    fun `画面の破棄で設定をキャンセルし、保存値や失敗の Effect を変更しない`() = runTest {
        val fixture = AirconTestFixture()
        val viewModel = viewModel(fixture)
        val effects = mutableListOf<AirconEffect>()
        backgroundScope.launch { viewModel.effect.collect { effects += it } }
        fixture.api.beforeResponse = { awaitCancellation() }
        viewModel.onIntent(AirconIntent.ModeSelected(AirconMode.DRY))
        runCurrent()
        store.clear()
        runCurrent()

        assertEquals(AirconSettings(), fixture.repository.settings.value)
        assertTrue(effects.isEmpty())
        assertTrue(viewModel.uiState.value.isSendingCommand)
    }
}
