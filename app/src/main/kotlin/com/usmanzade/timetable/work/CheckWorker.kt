package com.usmanzade.timetable.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.usmanzade.timetable.Graph
import com.usmanzade.timetable.data.ChangeRecord
import com.usmanzade.timetable.data.weekMonday
import com.usmanzade.timetable.widget.TodayWidgetProvider
import java.time.LocalDate

class CheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Graph.init(applicationContext)
        val settings = Graph.settings
        val repo = Graph.repo
        if (!settings.notifyEnabled.value) return Result.success()

        val today = LocalDate.now()
        val firstMonday = today.weekMonday()
        val enabled = settings.enabledTypes.value
        val records = ArrayList<ChangeRecord>()
        var anySuccess = false
        var anyFailure = false
        var seq = System.currentTimeMillis()

        for (subject in settings.favorites.value) {
            for (w in 0..3) {
                val monday = firstMonday.plusWeeks(w.toLong())
                val fresh = try {
                    repo.fetchWeek(subject, monday).also { anySuccess = true }
                } catch (e: Exception) {
                    anyFailure = true
                    continue
                }
                val old = repo.snapshot(subject, monday)
                repo.saveSnapshot(subject, monday, fresh)
                if (old == null) continue

                ChangeDiff.diff(
                    old.filter { it.date >= today },
                    fresh.filter { it.date >= today },
                ).filter { it.type in enabled }.forEach { c ->
                    records += ChangeRecord(
                        id = seq++,
                        at = System.currentTimeMillis(),
                        subjectKey = subject.key,
                        subjectName = subject.displayName,
                        type = c.type,
                        title = c.lesson.title,
                        date = c.lesson.date.toString(),
                        detail = c.detail,
                    )
                }
            }
        }

        if (records.isNotEmpty()) {
            settings.addChanges(records)
            Notifier.show(applicationContext, records)
        }
        TodayWidgetProvider.refreshAll(applicationContext)
        return if (!anySuccess && anyFailure) Result.retry() else Result.success()
    }
}
