package com.usmanzade.timetable.work

import com.usmanzade.timetable.data.ChangeType
import com.usmanzade.timetable.data.Lesson
import java.time.format.DateTimeFormatter

data class Change(val type: ChangeType, val lesson: Lesson, val detail: String)

object ChangeDiff {
    private val hm = DateTimeFormatter.ofPattern("HH:mm")

    private fun range(l: Lesson) = "${l.startDt.format(hm)}–${l.endDt.format(hm)}"

    fun diff(old: List<Lesson>, new: List<Lesson>): List<Change> {
        val result = ArrayList<Change>()
        val oldByDate = old.groupBy { it.date }
        val newByDate = new.groupBy { it.date }
        for (date in (oldByDate.keys + newByDate.keys).sorted()) {
            val o = oldByDate[date].orEmpty().toMutableList()
            val n = newByDate[date].orEmpty().toMutableList()

            val oi = o.iterator()
            while (oi.hasNext()) {
                val a = oi.next()
                val idx = n.indexOfFirst { it == a }
                if (idx >= 0) { n.removeAt(idx); oi.remove() }
            }

            val oi2 = o.iterator()
            while (oi2.hasNext()) {
                val a = oi2.next()
                val idx = n.indexOfFirst { it.subject == a.subject && it.start == a.start && it.end == a.end }
                if (idx < 0) continue
                val b = n.removeAt(idx)
                oi2.remove()
                compareFields(a, b, result)
            }

            val oi3 = o.iterator()
            while (oi3.hasNext()) {
                val a = oi3.next()
                val idx = n.indexOfFirst { it.subject == a.subject }
                if (idx < 0) continue
                val b = n.removeAt(idx)
                oi3.remove()
                if (a.start != b.start || a.end != b.end) {
                    result += Change(ChangeType.TIME, b, "${range(a)} → ${range(b)}")
                }
                compareFields(a, b, result)
            }

            for (a in o) if (!a.cancelled) result += Change(ChangeType.REMOVED, a, range(a))
            for (b in n) if (!b.cancelled) result += Change(ChangeType.ADDED, b, range(b))
        }
        return result
    }

    private fun compareFields(a: Lesson, b: Lesson, out: MutableList<Change>) {
        if (!a.cancelled && b.cancelled) out += Change(ChangeType.CANCELLED, b, range(b))
        if (a.locations != b.locations && b.locations.isNotBlank()) {
            out += Change(ChangeType.LOCATION, b, "${a.locations.ifBlank { "—" }} → ${b.locations}")
        }
        if (a.educators != b.educators && b.educators.isNotBlank()) {
            out += Change(ChangeType.EDUCATOR, b, "${a.educators.ifBlank { "—" }} → ${b.educators}")
        }
    }
}
