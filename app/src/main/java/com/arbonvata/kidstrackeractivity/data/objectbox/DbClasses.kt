package com.arbonvata.kidstrackeractivity.data.objectbox

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class Kid(
    @Id var id: Long = 0,
    var name: String = "",
    var schoolName: String = "", // Name of the kid's school
)

@Entity
data class Activity(
    @Id var id: Long = 0,
    var kidId: Long = 0, // Links this activity to a specific Kid
    var name: String = "", // e.g., "Football", "Piano"
    var startTime: String = "", // Format: "HH:mm" (e.g., "17:00")
    var endTime: String = "", // Format: "HH:mm" (e.g., "18:30")
    var isEnabled: Boolean = true, // Used to mute/disable the alarm without deleting it
    // ObjectBox natively supports primitive arrays!
    // No need for TypeConverters or Gson.
    // Values represent java.time.DayOfWeek (1 = Monday, 7 = Sunday)
    var daysOfWeek: IntArray = intArrayOf(),
    // Periodic activities repeat on their weekdays indefinitely; one-offs
    // occur only during the week anchored at firstDate.
    var isPeriodic: Boolean = true,
    // ISO-8601 date, e.g. "2026-10-20". Null/blank means unbounded: rows
    // written before this column existed read null from the DB (ObjectBox
    // does not apply Kotlin defaults to stored rows).
    var firstDate: String? = null,
    // School activities remind AFTER they end (pickup); everything else
    // reminds before it starts. Consumed by the future alarm scheduler.
    var isSchoolActivity: Boolean = false,
    // Date-specific exceptions encoded by OccurrenceException; null for legacy rows.
    var occurrenceExceptions: String? = null,
) {
    // Overriding equals and hashCode is highly recommended when using IntArray
    // in data classes, because arrays in Kotlin compare by reference by default.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Activity

        if (occurrenceExceptions != other.occurrenceExceptions) return false
        if (id != other.id) return false
        if (kidId != other.kidId) return false
        if (name != other.name) return false
        if (startTime != other.startTime) return false
        if (endTime != other.endTime) return false
        if (isEnabled != other.isEnabled) return false
        if (!daysOfWeek.contentEquals(other.daysOfWeek)) return false
        if (isPeriodic != other.isPeriodic) return false
        if (firstDate != other.firstDate) return false
        if (isSchoolActivity != other.isSchoolActivity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (occurrenceExceptions?.hashCode() ?: 0)
        result = 31 * result + kidId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + daysOfWeek.contentHashCode()
        result = 31 * result + isPeriodic.hashCode()
        result = 31 * result + (firstDate?.hashCode() ?: 0)
        result = 31 * result + isSchoolActivity.hashCode()
        return result
    }
}

@Entity
data class DailySchedule(
    @Id var id: Long = 0,
    var kidId: Long = 0,
    // ISO-8601 date, e.g. "2026-10-18". Stored as a string so that
    // lexicographic order matches chronological order.
    var date: String = "",
    // "HH:mm" override for the school end time; empty means no override.
    var endTime: String = "",
    var isNoSchool: Boolean = false,
)
