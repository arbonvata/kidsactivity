package com.arbonvata.kidstrackeractivity.di

import com.arbonvata.kidstrackeractivity.alarms.AlarmManagerAlarmScheduler
import com.arbonvata.kidstrackeractivity.alarms.AlarmScheduler
import com.arbonvata.kidstrackeractivity.alarms.AlarmSyncCoordinator
import com.arbonvata.kidstrackeractivity.data.objectbox.MyObjectBox
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepositoryImpl
import com.arbonvata.kidstrackeractivity.data.repository.AlarmSettingsRepository
import com.arbonvata.kidstrackeractivity.data.repository.AlarmSettingsRepositoryImpl
import com.arbonvata.kidstrackeractivity.data.repository.BackupRepository
import com.arbonvata.kidstrackeractivity.data.repository.BackupRepositoryImpl
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepositoryImpl
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepository
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepositoryImpl
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddActivityViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.AddKidViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.ConflictViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.DailyScheduleViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.EditActivityViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.EditKidViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.KidDetailViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.KidsViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.MonthViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.SchoolOverridesViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.SettingsViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.ShareViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.TodayViewModel
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.WeekViewModel
import io.objectbox.BoxStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule =
    module {
        single<BoxStore> {
            MyObjectBox
                .builder()
                .androidContext(androidContext())
                .build()
        }

        single<KidRepository> { KidRepositoryImpl(get()) }
        single<ActivityRepository> { ActivityRepositoryImpl(get()) }
        single<ScheduleOverrideRepository> { ScheduleOverrideRepositoryImpl(get()) }
        single<BackupRepository> { BackupRepositoryImpl(get()) }
        single<AlarmSettingsRepository> { AlarmSettingsRepositoryImpl(androidContext()) }
        single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
        single<AlarmScheduler> { AlarmManagerAlarmScheduler(androidContext()) }
        single<AlarmSyncCoordinator> { AlarmSyncCoordinator(get(), get(), get(), get(), get(), get()) }
        viewModel { KidsViewModel(get()) }
        viewModel { AddKidViewModel(get()) }
        viewModel { TodayViewModel(get(), get(), get()) }
        viewModel { WeekViewModel(get(), get(), get()) }
        viewModel { MonthViewModel(get(), get(), get()) }
        viewModel { SettingsViewModel(get()) }
        viewModel { ShareViewModel(get()) }
        viewModel { SchoolOverridesViewModel(get(), get(), get()) }
        viewModel { ConflictViewModel(get(), get(), get()) }
        viewModel { (kidId: Long) ->
            AddActivityViewModel(
                kidId = kidId,
                activityRepository = get(),
                kidRepository = get(),
            )
        }
        viewModel { (kidId: Long) ->
            KidDetailViewModel(
                kidId = kidId,
                kidRepository = get(),
                activityRepository = get(),
            )
        }
        viewModel { (kidId: Long) ->
            EditKidViewModel(
                kidId = kidId,
                kidRepository = get(),
            )
        }
        viewModel { (activityId: Long) ->
            EditActivityViewModel(
                activityId = activityId,
                activityRepository = get(),
                kidRepository = get(),
            )
        }
        viewModel { (kidIds: Set<Long>) ->
            DailyScheduleViewModel(
                kidRepository = get(),
                activityRepository = get(),
                scheduleOverrideRepository = get(),
                selectedKidIds = kidIds,
            )
        }
    }
