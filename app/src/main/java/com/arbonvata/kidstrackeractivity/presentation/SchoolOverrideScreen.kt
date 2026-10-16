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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.data.objectbox.DailySchedule
import com.arbonvata.kidstrackeractivity.presentation.viewmodels.SchoolOverridesViewModel
import com.arbonvata.kidstrackeractivity.ui.theme.KidsTrackerActivityTheme
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class DayOverride(
    val date: LocalDate,
    val type: OverrideType,
    val customEndTime: LocalTime? = null,
)

enum class OverrideType { EARLY_RELEASE, NO_SCHOOL, CUSTOM_TIME }

private fun DailySchedule.toDayOverride(): DayOverride =
    DayOverride(
        date = LocalDate.parse(date),
        type = if (isNoSchool) OverrideType.NO_SCHOOL else OverrideType.CUSTOM_TIME,
        customEndTime = endTime.takeIf { it.isNotEmpty() }?.let(LocalTime::parse),
    )

/**
 * Stateful entry point of the School Overrides screen: kid picker on top,
 * the month calendar below, and a dialog per tapped day to set a custom
 * school end time or mark it "No School".
 */
@Composable
fun SchoolOverrideScreen(
    viewModel: SchoolOverridesViewModel = koinViewModel(),
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingDate by remember { mutableStateOf<LocalDate?>(null) }

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.kids.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.school_override_empty_state))
            }
        }

        else -> {
            val kidName =
                uiState.kids
                    .firstOrNull { it.id == uiState.selectedKidId }
                    ?.name
                    .orEmpty()
            SchoolOverrideContent(
                kidName = kidName,
                overrides = uiState.overrides.mapValues { (_, schedule) -> schedule.toDayOverride() },
                schoolWeekdays = uiState.schoolWeekdays,
                kidPicker = {
                    KidPickerField(
                        kids = uiState.kids,
                        selectedKidId = uiState.selectedKidId ?: 0L,
                        onKidSelected = viewModel::onKidSelected,
                    )
                },
                onDayClick = { editingDate = it },
                onBackClick = onBackClick,
            )
        }
    }

    editingDate?.let { date ->
        val existing = uiState.overrides[date]
        var noSchool by remember(date) { mutableStateOf(existing?.isNoSchool == true) }
        var customEnd by remember(date) { mutableStateOf(existing?.toDayOverride()?.customEndTime) }
        var showTimePicker by remember(date) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingDate = null },
            title = { Text(date.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = noSchool,
                            onCheckedChange = {
                                noSchool = it
                                if (it) customEnd = null
                            },
                        )
                        Text(stringResource(R.string.school_override_no_school_label))
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        enabled = !noSchool,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                R.string.school_override_school_ends,
                                customEnd?.format(DateTimeFormatter.ofPattern("HH:mm"))
                                    ?: stringResource(R.string.school_override_usual_time),
                            ),
                        )
                    }
                    if (existing != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.school_override_remove_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (date.dayOfWeek.value !in uiState.schoolWeekdays) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.school_override_no_activity_weekday_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setDayOverride(date, if (noSchool) null else customEnd, noSchool)
                        editingDate = null
                    },
                ) { Text(stringResource(R.string.school_override_save)) }
            },
            dismissButton = {
                TextButton(onClick = { editingDate = null }) { Text(stringResource(R.string.school_override_cancel)) }
            },
        )

        if (showTimePicker) {
            TimePickerDialog(
                initialTime = customEnd ?: LocalTime.of(13, 0),
                onConfirm = {
                    customEnd = it
                    showTimePicker = false
                },
                onDismiss = { showTimePicker = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolOverrideContent(
    kidName: String,
    overrides: Map<LocalDate, DayOverride>,
    schoolWeekdays: Set<Int> = emptySet(),
    kidPicker: @Composable () -> Unit = {},
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
                title = { Text(stringResource(R.string.school_override_title, kidName)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.school_override_back))
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            kidPicker()

            // Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.school_override_previous_month))
                }
                Text(
                    currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.school_override_next_month))
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LegendItem(color = MaterialTheme.colorScheme.tertiary, label = stringResource(R.string.school_override_type_early_release))
                LegendItem(color = MaterialTheme.colorScheme.error, label = stringResource(R.string.school_override_type_no_school))
                LegendItem(color = MaterialTheme.colorScheme.secondary, label = stringResource(R.string.school_override_type_custom))
            }
            Text(
                stringResource(R.string.school_override_dimmed_marks_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Weekday Headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf(
                    stringResource(R.string.school_override_weekday_monday),
                    stringResource(R.string.school_override_weekday_tuesday),
                    stringResource(R.string.school_override_weekday_wednesday),
                    stringResource(R.string.school_override_weekday_thursday),
                    stringResource(R.string.school_override_weekday_friday),
                    stringResource(R.string.school_override_weekday_saturday),
                    stringResource(R.string.school_override_weekday_sunday),
                ).forEach { day ->
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
                                hasSchoolActivity = date.dayOfWeek.value in schoolWeekdays,
                                modifier = Modifier.weight(1f),
                                onClick = { onDayClick(date) },
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }

            // Summary of overrides
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        pluralStringResource(R.plurals.school_override_month_summary, overrides.size, overrides.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (overrides.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        overrides.entries
                            .sortedBy { it.key }
                            .take(3)
                            .forEach { (date, override) ->
                                val detail =
                                    when (override.type) {
                                        OverrideType.NO_SCHOOL -> stringResource(R.string.school_override_type_no_school)
                                        OverrideType.EARLY_RELEASE -> stringResource(R.string.school_override_type_early_release)
                                        OverrideType.CUSTOM_TIME ->
                                            override.customEndTime?.let {
                                                stringResource(R.string.school_override_type_custom_with_time, it)
                                            } ?: stringResource(R.string.school_override_type_custom)
                                    }
                                Text(
                                    stringResource(
                                        R.string.school_override_summary_entry,
                                        date.format(DateTimeFormatter.ofPattern("MMM d")),
                                        detail,
                                    ),
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
    hasSchoolActivity: Boolean = true,
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
    // Marks on weekdays without a school activity are inert — dimmed dots.
    val markAlpha =
        when {
            override == null -> 0f
            hasSchoolActivity -> 0.3f
            else -> 0.12f
        }

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .padding(2.dp)
                .clip(CircleShape)
                .background(backgroundColor.copy(alpha = markAlpha))
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

@Preview(showBackground = true, name = "Overrides — with special days")
@Composable
fun SchoolOverrideContentPreview() {
    KidsTrackerActivityTheme {
        val today = LocalDate.now()
        val mockOverrides =
            mapOf(
                today.plusDays(3) to DayOverride(today.plusDays(3), OverrideType.EARLY_RELEASE),
                today.plusDays(7) to DayOverride(today.plusDays(7), OverrideType.NO_SCHOOL),
                today.plusDays(10) to DayOverride(today.plusDays(10), OverrideType.CUSTOM_TIME, LocalTime.of(13, 0)),
            )
        SchoolOverrideContent(
            kidName = "Leo",
            overrides = mockOverrides,
            // School runs only on the +3 day's weekday: that mark renders
            // solid, the other two dim (inert).
            schoolWeekdays = setOf(today.plusDays(3).dayOfWeek.value),
            onDayClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Overrides — no special days")
@Composable
fun SchoolOverrideContentEmptyPreview() {
    KidsTrackerActivityTheme {
        SchoolOverrideContent(
            kidName = "Mia",
            overrides = emptyMap(),
            schoolWeekdays = setOf(1, 2, 3, 4, 5),
            onDayClick = {},
            onBackClick = {},
        )
    }
}
