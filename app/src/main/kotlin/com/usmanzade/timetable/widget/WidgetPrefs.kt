package com.usmanzade.timetable.widget

import android.content.Context
import com.usmanzade.timetable.Graph
import com.usmanzade.timetable.data.Subject
import com.usmanzade.timetable.data.weekMonday
import java.time.LocalDate

object WidgetPrefs {
    data class State(val subjectKey: String?, val week: Boolean, val offset: Int)

    private fun sp(c: Context) = c.getSharedPreferences("widgets", Context.MODE_PRIVATE)

    fun get(c: Context, id: Int): State {
        val p = sp(c)
        return State(p.getString("s$id", null), p.getBoolean("w$id", false), p.getInt("o$id", 0))
    }

    fun save(c: Context, id: Int, subjectKey: String?, week: Boolean, offset: Int = 0) {
        sp(c).edit().putString("s$id", subjectKey).putBoolean("w$id", week).putInt("o$id", offset).apply()
    }

    fun setOffset(c: Context, id: Int, offset: Int) {
        sp(c).edit().putInt("o$id", offset).apply()
    }

    fun clear(c: Context, id: Int) {
        sp(c).edit().remove("s$id").remove("w$id").remove("o$id").apply()
    }

    fun resolveSubject(state: State): Subject? {
        val favorites = Graph.settings.favorites.value
        return favorites.firstOrNull { it.key == state.subjectKey }
            ?: Graph.settings.activeSubject()
            ?: favorites.firstOrNull()
    }

    fun range(state: State): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return if (state.week) {
            val m = today.weekMonday().plusWeeks(state.offset.toLong())
            m to m.plusDays(6)
        } else {
            val d = today.plusDays(state.offset.toLong())
            d to d
        }
    }
}
