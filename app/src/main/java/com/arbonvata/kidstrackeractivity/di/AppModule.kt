package com.arbonvata.kidstrackeractivity.di

import com.arbonvata.kidstrackeractivity.data.objectbox.MyObjectBox
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepositoryImpl
import com.arbonvata.kidstrackeractivity.data.repository.BackupRepository
import com.arbonvata.kidstrackeractivity.data.repository.BackupRepositoryImpl
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepositoryImpl
import com.arbonvata.kidstrackeractivity.data.repository.OccurrenceRepository
import com.arbonvata.kidstrackeractivity.data.repository.OccurrenceRepositoryImpl
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepository
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepositoryImpl
import io.objectbox.BoxStore
import org.koin.android.ext.koin.androidContext
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
        single<OccurrenceRepository> { OccurrenceRepositoryImpl(get(), get()) }
        single<BackupRepository> { BackupRepositoryImpl(get(), get(), get(), get()) }
    }
