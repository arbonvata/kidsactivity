package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    kidName: String,
    onShareWhatsApp: () -> Unit,
    onShareEmail: () -> Unit,
    onExportPdf: () -> Unit,
    onExportCalendar: () -> Unit,
    onCopyLink: () -> Unit,
    onBackClick: () -> Unit,
) {
    var selectedRange by remember { mutableStateOf("week") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share Schedule") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Share $kidName's Schedule",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Share with family, babysitters, or caregivers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }

            // Range Selection
            Text(
                "Select Time Range",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Today" to "today", "Week" to "week", "Month" to "month").forEach { (label, value) ->
                    FilterChip(
                        selected = selectedRange == value,
                        onClick = { selectedRange = value },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Share Options
            Text(
                "Share Via",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            ShareOptionRow(
                icon = Icons.AutoMirrored.Filled.Message,
                title = "WhatsApp",
                subtitle = "Send to contacts",
                onClick = onShareWhatsApp,
            )
            ShareOptionRow(
                icon = Icons.Default.Email,
                title = "Email",
                subtitle = "Send as attachment",
                onClick = onShareEmail,
            )
            ShareOptionRow(
                icon = Icons.Default.Link,
                title = "Copy Shareable Link",
                subtitle = "Read-only view for anyone",
                onClick = onCopyLink,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Export Options
            Text(
                "Export",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            ShareOptionRow(
                icon = Icons.Default.PictureAsPdf,
                title = "Export as PDF",
                subtitle = "Save as document",
                onClick = onExportPdf,
            )
            ShareOptionRow(
                icon = Icons.Default.CalendarMonth,
                title = "Export to Calendar",
                subtitle = "Google Calendar / ICS file",
                onClick = onExportCalendar,
            )
        }
    }
}

@Composable
fun ShareOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShareScreenPreview() {
    MaterialTheme {
        ShareScreen(
            kidName = "Leo",
            onShareWhatsApp = {},
            onShareEmail = {},
            onExportPdf = {},
            onExportCalendar = {},
            onCopyLink = {},
            onBackClick = {},
        )
    }
}
