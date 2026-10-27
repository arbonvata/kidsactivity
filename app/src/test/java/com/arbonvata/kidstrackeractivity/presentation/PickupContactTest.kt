package com.arbonvata.kidstrackeractivity.presentation

import com.arbonvata.kidstrackeractivity.data.ActivityOccurrence
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Parent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

/** Tests the name matching behind the Today row's call button. */
class PickupContactTest {
    private val monday = LocalDate.of(2026, 10, 19)
    private val parents =
        listOf(
            Parent(id = 1L, name = "Anna", relation = "mother", phone = "+46 70 111 22 33"),
            Parent(id = 2L, name = "Mark", relation = "father", phone = ""),
        )

    private fun occurrence(
        delegatedTo: String? = null,
        pickupAdult: String? = null,
    ): ActivityOccurrence {
        val activity = Activity(id = 1L, kidId = 1L, name = "Football", startTime = "17:00", endTime = "18:30", pickupAdult = pickupAdult)
        return ActivityOccurrence(activity, "Leo", monday, LocalTime.of(17, 0), LocalTime.of(18, 30), delegatedTo = delegatedTo)
    }

    @Test
    fun `delegated name wins over the activity's pickup adult`() {
        assertEquals(parents[0], pickupContactParent(occurrence(delegatedTo = "Anna", pickupAdult = "Mark"), parents))
    }

    @Test
    fun `falls back to the activity's pickup adult when not delegated`() {
        assertEquals(parents[0], pickupContactParent(occurrence(pickupAdult = "Anna"), parents))
    }

    @Test
    fun `matching is case-insensitive and ignores surrounding blanks`() {
        assertEquals(parents[0], pickupContactParent(occurrence(pickupAdult = "  anna "), parents))
    }

    @Test
    fun `parents without a phone never match`() {
        assertNull(pickupContactParent(occurrence(pickupAdult = "Mark"), parents))
    }

    @Test
    fun `unknown names and empty fields return null`() {
        assertNull(pickupContactParent(occurrence(pickupAdult = "Grandma"), parents))
        assertNull(pickupContactParent(occurrence(), parents))
    }
}
