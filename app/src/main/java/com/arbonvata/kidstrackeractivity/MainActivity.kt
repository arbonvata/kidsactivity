package com.arbonvata.kidstrackeractivity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.arbonvata.kidstrackeractivity.presentation.navigation.KidsScheduleApp
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.ScheduleViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel: ScheduleViewModel = koinViewModel()
                KidsScheduleApp(viewModel = viewModel)
            }
        }
    }
}
