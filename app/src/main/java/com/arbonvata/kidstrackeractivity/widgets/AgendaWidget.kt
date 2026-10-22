package com.arbonvata.kidstrackeractivity.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.core.net.toUri
import com.arbonvata.kidstrackeractivity.MainActivity
import com.arbonvata.kidstrackeractivity.R
import com.arbonvata.kidstrackeractivity.data.expandActivitiesForDateRange
import com.arbonvata.kidstrackeractivity.data.repository.ActivityRepository
import com.arbonvata.kidstrackeractivity.data.repository.KidRepository
import com.arbonvata.kidstrackeractivity.data.repository.ScheduleOverrideRepository
import com.arbonvata.kidstrackeractivity.presentation.navigation.Screen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Uses RemoteViews so launchers can show the agenda without running an activity. */
class AgendaWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        refresh(context)
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            ACTION_REFRESH, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED, Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            -> refresh(context)
            else -> super.onReceive(context, intent)
        }
    }

    private fun refresh(context: Context) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                updateAgendaWidgets(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AgendaWidget", "Unable to refresh agenda", e)
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.arbonvata.kidstrackeractivity.widget.REFRESH"
    }
}

suspend fun updateAgendaWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, AgendaWidget::class.java))
    if (ids.isEmpty()) return
    val koin = GlobalContext.get()
    val now = LocalDateTime.now()
    val occurrences =
        expandActivitiesForDateRange(
            koin.get<KidRepository>().getAllKids(),
            koin.get<ActivityRepository>().getAllActivities(),
            now.toLocalDate(),
            now.toLocalDate().plusDays(7),
            includeDisabled = true,
            overrides = koin.get<ScheduleOverrideRepository>().getAll(),
        )
    val snapshot = agendaSnapshot(occurrences, now)
    val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
    val next = snapshot.next
    val nextText =
        next?.let {
            val time = it.agendaTime().toLocalTime().format(timeFormat)
            val action = if (it.activity.isSchoolActivity) context.getString(R.string.widget_pickup) else it.activity.name
            "${it.date.format(dateFormat)} $time\n${it.kidName} · $action"
        } ?: context.getString(R.string.widget_no_upcoming)
    val agenda =
        snapshot.remainingToday
            .take(4)
            .joinToString("\n") {
                val label = if (it.activity.isSchoolActivity) context.getString(R.string.widget_pickup) else it.activity.name
                val time = it.agendaTime().toLocalTime().format(timeFormat)
                "$time · ${it.kidName} · $label"
            }.ifBlank { context.getString(R.string.widget_today_empty) }
    val details =
        next
            ?.activity
            ?.let {
                listOfNotNull(
                    it.location?.takeIf(String::isNotBlank),
                    it.pickupAdult?.takeIf(String::isNotBlank)?.let { adult ->
                        context.getString(R.string.pickup_adult_format, adult)
                    },
                ).joinToString(" · ")
            }.orEmpty()
    val openApp =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    val openNext =
        if (next == null) {
            openApp
        } else {
            PendingIntent.getActivity(
                context,
                1,
                Intent(Intent.ACTION_VIEW, Screen.EditActivity.deepLink(next.activity.id).toUri(), context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    val refresh =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AgendaWidget::class.java).setAction(AgendaWidget.ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    val views =
        RemoteViews(context.packageName, R.layout.agenda_widget).apply {
            setTextViewText(R.id.widget_next, nextText)
            setTextViewText(R.id.widget_details, details)
            setTextViewText(R.id.widget_agenda, agenda)
            setTextViewText(R.id.widget_updated, context.getString(R.string.widget_updated, now.format(timeFormat)))
            setOnClickPendingIntent(R.id.widget_root, openApp)
            setOnClickPendingIntent(R.id.widget_next, openNext)
            setOnClickPendingIntent(R.id.widget_refresh, refresh)
        }
    manager.updateAppWidget(ids, views)
}
