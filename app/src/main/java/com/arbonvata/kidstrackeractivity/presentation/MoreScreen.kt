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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onKidsClick: () -> Unit,
    onSchoolOverridesClick: () -> Unit,
    onConflictsClick: () -> Unit,
    onShareClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onKidSelectionClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.more_title)) },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Management Section
            Text(
                stringResource(R.string.more_section_management),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            MoreMenuItem(
                icon = Icons.Default.People,
                title = stringResource(R.string.more_kids_management_title),
                subtitle = stringResource(R.string.more_kids_management_subtitle),
                onClick = onKidsClick,
            )

            MoreMenuItem(
                icon = Icons.Default.Event,
                title = stringResource(R.string.more_school_overrides_title),
                subtitle = stringResource(R.string.more_school_overrides_subtitle),
                onClick = onSchoolOverridesClick,
            )

            MoreMenuItem(
                icon = Icons.Default.ViewDay,
                title = stringResource(R.string.more_daily_view_title),
                subtitle = stringResource(R.string.more_daily_view_subtitle),
                onClick = onKidSelectionClick,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tools Section
            Text(
                stringResource(R.string.more_section_tools),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            MoreMenuItem(
                icon = Icons.Default.Warning,
                title = stringResource(R.string.more_check_conflicts_title),
                subtitle = stringResource(R.string.more_check_conflicts_subtitle),
                onClick = onConflictsClick,
            )

            MoreMenuItem(
                icon = Icons.Default.Share,
                title = stringResource(R.string.more_share_export_title),
                subtitle = stringResource(R.string.more_share_export_subtitle),
                onClick = onShareClick,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // System Section
            Text(
                stringResource(R.string.more_section_system),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            MoreMenuItem(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.more_settings_title),
                subtitle = stringResource(R.string.more_settings_subtitle),
                onClick = onSettingsClick,
            )
        }
    }
}

@Composable
fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
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
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MoreScreenPreview() {
    MaterialTheme {
        MoreScreen(
            onKidsClick = {},
            onSchoolOverridesClick = {},
            onConflictsClick = {},
            onShareClick = {},
            onSettingsClick = {},
            onKidSelectionClick = {},
        )
    }
}
