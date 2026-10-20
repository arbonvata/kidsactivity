package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.data.objectbox.Activity

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
