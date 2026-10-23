package com.usmanzade.timetable.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.usmanzade.timetable.Graph
import com.usmanzade.timetable.R
import com.usmanzade.timetable.Tr
import com.usmanzade.timetable.data.ChangeRecord
import com.usmanzade.timetable.label
import com.usmanzade.timetable.ui.MainActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Notifier {
    private const val CHANNEL = "schedule_changes"

    fun createChannel(context: Context) {
        val tr = Tr(Graph.settings.isRussian())
        val ch = NotificationChannel(
            CHANNEL,
            tr("Изменения в расписании", "Schedule changes"),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun dateLabel(iso: String, ru: Boolean): String {
        val locale = if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH
        return runCatching {
            LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))
        }.getOrDefault(iso)
    }

    fun show(context: Context, records: List<ChangeRecord>) {
        if (records.isEmpty() || !canNotify(context)) return
        val ru = Graph.settings.isRussian()
        val tr = Tr(ru)
        val nm = NotificationManagerCompat.from(context)

        records.groupBy { it.subjectKey }.forEach { (key, list) ->
            val name = list.first().subjectName
            val lines = list.take(8).map {
                "${it.type.label(tr)}: ${it.title} · ${dateLabel(it.date, ru)}" +
                    if (it.detail.isNotBlank()) " (${it.detail})" else ""
            }
            val open = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_TAB, 2)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context, key.hashCode(), open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val style = NotificationCompat.InboxStyle().also { s ->
                lines.forEach(s::addLine)
                if (list.size > lines.size) s.setSummaryText("+${list.size - lines.size}")
            }
            val n = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(tr("Изменения · $name", "Changes · $name"))
                .setContentText(lines.first())
                .setStyle(style)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .build()
            runCatching { nm.notify(key.hashCode(), n) }
        }
    }
}
