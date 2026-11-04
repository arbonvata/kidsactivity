package com.arbonvata.kidstrackeractivity.presentation

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Both sharing entry points use the same UTF-8 encoding, MIME type and temporary URI permission. */
suspend fun shareCalendarFile(
    context: Context,
    calendar: String,
    chooserTitle: String,
    displayName: String,
) {
    val file =
        withContext(Dispatchers.IO) {
            File.createTempFile("calendar-", ".ics", context.cacheDir).apply { writeText(calendar, Charsets.UTF_8) }
        }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(displayName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    withContext(Dispatchers.Main) { context.startActivity(Intent.createChooser(send, chooserTitle)) }
}
