package com.usmanzade.timetable.ui.schedule

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.usmanzade.timetable.Graph
import com.usmanzade.timetable.data.Lesson
import com.usmanzade.timetable.data.Subject
import com.usmanzade.timetable.data.weekMonday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Stable
class ScheduleHolder(val subject: Subject?) {
    val weeks = mutableStateMapOf<LocalDate, List<Lesson>>()
    var loading by mutableIntStateOf(0)
        private set
    var offline by mutableStateOf(false)
        private set

    fun lessonsOn(date: LocalDate): List<Lesson> =
        weeks[date.weekMonday()].orEmpty()
            .filter { it.date == date }
            .sortedWith(compareByDescending<Lesson> { it.allDay }.thenBy { it.start })

    fun countOn(date: LocalDate): Int = weeks[date.weekMonday()].orEmpty().count { it.date == date && !it.cancelled }

    fun isLoaded(date: LocalDate): Boolean = weeks.containsKey(date.weekMonday())

    suspend fun ensure(mondays: Collection<LocalDate>, force: Boolean = false) {
        val s = subject ?: return
        val repo = Graph.repo

        withContext(Dispatchers.IO) {
            for (m in mondays) {
                if (weeks[m] == null) repo.cached(s, m)?.let { weeks[m] = it }
            }
        }

        val stale = mondays.filter { force || repo.ageMillis(s, it) > STALE_MS }
        if (stale.isEmpty()) return
        loading += 1
        try {
            var failed = false
            coroutineScope {
                stale.map { m ->
                    async {
                        try {
                            weeks[m] = repo.fetchWeek(s, m)
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            failed = true
                        }
                    }
                }.awaitAll()
            }
            offline = failed
        } finally {
            loading -= 1
        }
    }

    private companion object {
        const val STALE_MS = 15 * 60 * 1000L
    }
}
