package com.arbonvata.kidstrackeractivity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arbonvata.kidstrackeractivity.presentation.navigation.KidsScheduleApp
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.ScheduleViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel: ScheduleViewModel = viewModel()
                KidsScheduleApp(viewModel = viewModel)
            }
        }
    }
}
