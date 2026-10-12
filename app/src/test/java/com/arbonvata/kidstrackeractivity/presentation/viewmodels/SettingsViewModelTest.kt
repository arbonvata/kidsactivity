package com.arbonvata.kidstrackeractivity.presentation.viewmodels

import com.arbonvata.kidstrackeractivity.data.repository.AlarmSettingsRepository
import com.arbonvata.kidstrackeractivity.testutil.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val alarmSettingsRepository = mockk<AlarmSettingsRepository>(relaxUnitFun = true)

    @Test
    fun exposesPersistedValuesFromTheRepository() =
        runTest {
            every { alarmSettingsRepository.schoolAlarmMinutes } returns flowOf(20)
            every { alarmSettingsRepository.activityAlarmMinutes } returns flowOf(45)
            val viewModel = SettingsViewModel(alarmSettingsRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.schoolAlarmMinutes.collect() }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.activityAlarmMinutes.collect() }

            assertEquals(20, viewModel.schoolAlarmMinutes.value)
            assertEquals(45, viewModel.activityAlarmMinutes.value)
            verify { alarmSettingsRepository.schoolAlarmMinutes }
            verify { alarmSettingsRepository.activityAlarmMinutes }
        }

    @Test
    fun sliderChangesPersistViaTheRepository() =
        runTest {
            every { alarmSettingsRepository.schoolAlarmMinutes } returns flowOf(10)
            every { alarmSettingsRepository.activityAlarmMinutes } returns flowOf(15)
            val viewModel = SettingsViewModel(alarmSettingsRepository)

            viewModel.onSchoolAlarmChange(25)
            viewModel.onActivityAlarmChange(40)

            coVerify { alarmSettingsRepository.setSchoolAlarmMinutes(25) }
            coVerify { alarmSettingsRepository.setActivityAlarmMinutes(40) }
        }
}
