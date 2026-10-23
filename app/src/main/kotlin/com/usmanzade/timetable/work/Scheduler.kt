package com.usmanzade.timetable.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.usmanzade.timetable.Graph
import java.util.concurrent.TimeUnit

object Scheduler {
    private const val NAME = "schedule-check"

    fun apply(context: Context) {
        val s = Graph.settings
        val wm = WorkManager.getInstance(context)
        if (!s.notifyEnabled.value || s.favorites.value.isEmpty()) {
            wm.cancelUniqueWork(NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<CheckWorker>(
            s.intervalMinutes.value.coerceAtLeast(15).toLong(), TimeUnit.MINUTES,
        ).setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
        ).build()
        wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun checkNow(context: Context) {
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<CheckWorker>().build())
    }
}
