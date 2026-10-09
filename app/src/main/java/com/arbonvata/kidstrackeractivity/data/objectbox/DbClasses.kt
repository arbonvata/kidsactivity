package com.arbonvata.kidstrackeractivity.data.objectbox

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class Kid(
    @Id var id: Long = 0,
    var name: String = "",
    var defaultEndTime: String = "" // Format: "HH:mm" (e.g., "15:00")
)

@Entity
data class Activity(
    @Id var id: Long = 0,
    var kidId: Long = 0,             // Links this activity to a specific Kid
    var name: String = "",           // e.g., "Football", "Piano"
    var startTime: String = "",      // Format: "HH:mm" (e.g., "17:00")
    var endTime: String = "",        // Format: "HH:mm" (e.g., "18:30")
    var isEnabled: Boolean = true,   // Used to mute/disable the alarm without deleting it

    // ObjectBox natively supports primitive arrays!
    // No need for TypeConverters or Gson.
    // Values represent java.time.DayOfWeek (1 = Monday, 7 = Sunday)
    var daysOfWeek: IntArray = intArrayOf()
) {
    // Overriding equals and hashCode is highly recommended when using IntArray
    // in data classes, because arrays in Kotlin compare by reference by default.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Activity

        if (id != other.id) return false
        if (kidId != other.kidId) return false
        if (name != other.name) return false
        if (startTime != other.startTime) return false
        if (endTime != other.endTime) return false
        if (isEnabled != other.isEnabled) return false
        if (!daysOfWeek.contentEquals(other.daysOfWeek)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + kidId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + daysOfWeek.contentHashCode()
        return result
    }
}