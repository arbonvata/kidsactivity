package com.arbonvata.kidstrackeractivity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.arbonvata.kidstrackeractivity.presentation.navigation.KidsScheduleApp
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme

class MainActivity : ComponentActivity() {
    private val incomingIntent = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingIntent.value = intent?.takeIf { it.data != null }
        setContent {
            KidsTrackerActivityTheme {
                KidsScheduleApp(deepLinkIntent = incomingIntent.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Notification deep links while the app is already running.
        incomingIntent.value = intent.takeIf { it.data != null }
    }
}
