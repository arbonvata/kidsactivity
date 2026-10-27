package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.data.ActivityOccurrence
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity
import com.arbonvata.kidstrackeractivity.data.objectbox.Parent

@Composable
fun PickupDetails(activity: Activity) {
    activity.location?.takeIf { it.isNotBlank() }?.let {
        Text(stringResource(R.string.pickup_location_format, it), style = MaterialTheme.typography.bodySmall)
    }
    activity.pickupAdult?.takeIf { it.isNotBlank() }?.let {
        Text(stringResource(R.string.pickup_adult_format, it), style = MaterialTheme.typography.bodySmall)
    }
    activity.pickupNotes?.takeIf { it.isNotBlank() }?.let {
        Text(it, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * The "picked by" label for a delegated date, or null when the date is not
 * delegated. A name marked on the date wins; an unnamed delegation falls
 * back to the activity's usual pickup adult; otherwise a generic label.
 */
@Composable
fun delegatedPickupLabel(occurrence: ActivityOccurrence): String? {
    if (occurrence.delegatedTo == null) return null
    val name =
        occurrence.delegatedTo.takeIf { it.isNotBlank() }
            ?: occurrence.activity.pickupAdult?.takeIf { it.isNotBlank() }
    return if (name == null) {
        stringResource(R.string.dashboard_picked_by_someone_else)
    } else {
        stringResource(R.string.dashboard_picked_by_format, name)
    }
}

/**
 * The saved parent who covers this occurrence's pickup and has a phone — the
 * call button's target. A name marked on the date wins over the activity's
 * usual pickup adult; parents without a phone never match.
 */
fun pickupContactParent(
    occurrence: ActivityOccurrence,
    parents: List<Parent>,
): Parent? {
    val name =
        occurrence.delegatedTo?.takeIf { it.isNotBlank() }
            ?: occurrence.activity.pickupAdult?.takeIf { it.isNotBlank() }
            ?: return null
    return parents.firstOrNull {
        it.name.equals(name.trim(), ignoreCase = true) && it.phone.isNotBlank()
    }
}
