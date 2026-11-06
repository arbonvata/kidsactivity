package com.arbonvata.kidstrackeractivity.presentation.fixtures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.data.objectbox.Kid
import com.arbonvata.kidstrackeractivity.data.objectbox.MyObjectBox
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepositoryImpl
import com.arbonvata.kidstrackeractivity.data.repository.KidRepositoryImpl
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepositoryImpl
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.SchoolOverridesViewModel
import java.io.File
import java.time.LocalDate
import java.util.UUID

/** Each UI test gets a disposable database, independent of the application's data. */
class SchoolOverrideFixture : AutoCloseable {
    val day: LocalDate = LocalDate.now()
    val earlyEnd = "13:00"
    private val directory =
        File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "school-override-test-${UUID.randomUUID()}",
        )
    private val store = MyObjectBox.builder().directory(directory).build()
    private val models = ViewModelStore()
    private val child = Kid(name = "School test child")

    init {
        child.id = store.boxFor(Kid::class.java).put(child)
        store.boxFor(Activity::class.java).put(
            Activity(
                kidId = child.id,
                name = "School",
                startTime = "08:00",
                endTime = "15:00",
                daysOfWeek = intArrayOf(day.dayOfWeek.value),
                isSchoolActivity = true,
                isPeriodic = true,
            ),
        )
        store.boxFor(DailySchedule::class.java).put(
            DailySchedule(kidId = child.id, date = day.toString(), endTime = earlyEnd),
        )
    }

    val viewModel: SchoolOverridesViewModel =
        ViewModelProvider(
            models,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return SchoolOverridesViewModel(
                        KidRepositoryImpl(store),
                        ActivityRepositoryImpl(store),
                        ScheduleOverrideRepositoryImpl(store),
                    ) as T
                }
            },
        )[SchoolOverridesViewModel::class.java]

    fun savedDay(): DailySchedule = store.boxFor(DailySchedule::class.java).all.single()

    /** Clear the ViewModel on the UI thread before closing the database. */
    fun clearViewModel() = models.clear()

    override fun close() {
        store.close()
        directory.deleteRecursively()
    }
}
