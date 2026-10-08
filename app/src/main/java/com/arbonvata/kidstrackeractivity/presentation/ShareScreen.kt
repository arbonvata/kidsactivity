package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    onBackClick: () -> Unit
) {
    var selectedRange by remember { mutableStateOf("week") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share Schedule") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Share ${kidName}'s Schedule",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Share with family, babysitters, or caregivers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // Range Selection
            Text(
                "Select Time Range",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Today" to "today", "Week" to "week", "Month" to "month").forEach { (label, value) ->
                    FilterChip(
                        selected = selectedRange == value,
                        onClick = { selectedRange = value },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Share Options
            Text(
                "Share Via",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            ShareOptionRow(
                icon = Icons.Default.Message,
                title = "WhatsApp",
                subtitle = "Send to contacts",
                onClick = onShareWhatsApp
            )
            ShareOptionRow(
                icon = Icons.Default.Email,
                title = "Email",
                subtitle = "Send as attachment",
                onClick = onShareEmail
            )
            ShareOptionRow(
                icon = Icons.Default.Link,
                title = "Copy Shareable Link",
                subtitle = "Read-only view for anyone",
                onClick = onCopyLink
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Export Options
            Text(
                "Export",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            ShareOptionRow(
                icon = Icons.Default.PictureAsPdf,
                title = "Export as PDF",
                subtitle = "Save as document",
                onClick = onExportPdf
            )
            ShareOptionRow(
                icon = Icons.Default.CalendarMonth,
                title = "Export to Calendar",
                subtitle = "Google Calendar / ICS file",
                onClick = onExportCalendar
            )
        }
    }
}

@Composable
fun ShareOptionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
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
            onBackClick = {}
        )
    }
}