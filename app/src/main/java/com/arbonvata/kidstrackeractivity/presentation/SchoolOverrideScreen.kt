package com.arbonvata.kidstrackeractivity.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class DayOverride(
    val date: LocalDate,
    val type: OverrideType,
)

enum class OverrideType { EARLY_RELEASE, NO_SCHOOL, CUSTOM_TIME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolOverrideScreen(
    kidName: String,
    overrides: Map<LocalDate, DayOverride>,
    onDayClick: (LocalDate) -> Unit,
    onBackClick: () -> Unit,
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1=Mon, 7=Sun

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$kidName's School Schedule") },
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
            // Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                }
                Text(
                    currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LegendItem(color = MaterialTheme.colorScheme.tertiary, label = "Early Release")
                LegendItem(color = MaterialTheme.colorScheme.error, label = "No School")
                LegendItem(color = MaterialTheme.colorScheme.secondary, label = "Custom")
            }

            // Weekday Headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Calendar Grid
            val totalCells = firstDayOfWeek - 1 + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - (firstDayOfWeek - 1) + 1
                        val isValidDay = dayNumber in 1..daysInMonth

                        if (isValidDay) {
                            val date = currentMonth.atDay(dayNumber)
                            val override = overrides[date]
                            CalendarDay(
                                day = dayNumber,
                                override = override,
                                isToday = date == LocalDate.now(),
                                modifier = Modifier.weight(1f),
                                onClick = { onDayClick(date) },
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Summary of overrides
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "This month: ${overrides.size} special days",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (overrides.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        overrides.entries.take(3).forEach { (date, override) ->
                            Text(
                                "${date.format(DateTimeFormatter.ofPattern("MMM d"))}: ${
                                    when (override.type) {
                                        OverrideType.EARLY_RELEASE -> "Early Release"
                                        OverrideType.NO_SCHOOL -> "No School"
                                        OverrideType.CUSTOM_TIME -> "Custom Time"
                                    }
                                }",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDay(
    day: Int,
    override: DayOverride?,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val backgroundColor =
        when (override?.type) {
            OverrideType.EARLY_RELEASE -> MaterialTheme.colorScheme.tertiary
            OverrideType.NO_SCHOOL -> MaterialTheme.colorScheme.error
            OverrideType.CUSTOM_TIME -> MaterialTheme.colorScheme.secondary
            null -> Color.Transparent
        }

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .padding(2.dp)
                .clip(CircleShape)
                .background(backgroundColor.copy(alpha = if (override != null) 0.3f else 0f))
                .then(
                    if (isToday) {
                        Modifier.background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            CircleShape,
                        )
                    } else {
                        Modifier
                    },
                ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.5f)),
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(showBackground = true)
@Composable
fun SchoolOverrideScreenPreview() {
    MaterialTheme {
        val today = LocalDate.now()
        val mockOverrides =
            mapOf(
                today.plusDays(3) to DayOverride(today.plusDays(3), OverrideType.EARLY_RELEASE),
                today.plusDays(7) to DayOverride(today.plusDays(7), OverrideType.NO_SCHOOL),
                today.plusDays(10) to DayOverride(today.plusDays(10), OverrideType.CUSTOM_TIME),
            )
        SchoolOverrideScreen(
            kidName = "Leo",
            overrides = mockOverrides,
            onDayClick = {},
            onBackClick = {},
        )
    }
}
