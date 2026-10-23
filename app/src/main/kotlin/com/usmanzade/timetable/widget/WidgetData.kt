package com.usmanzade.timetable.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.RemoteViews
import com.usmanzade.timetable.Graph
import com.usmanzade.timetable.R
import com.usmanzade.timetable.data.Lesson
import com.usmanzade.timetable.data.weekMonday
import kotlinx.coroutines.runBlocking
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface WidgetItem {
    data class Header(val text: String) : WidgetItem
    data class Entry(val lesson: Lesson, val color: Int) : WidgetItem
}

object WidgetData {
    private const val STALE_MS = 30 * 60_000L
    private val hm = DateTimeFormatter.ofPattern("HH:mm")

    fun needsRefresh(ctx: Context, id: Int): Boolean {
        Graph.init(ctx)
        val subject = WidgetPrefs.resolveSubject(WidgetPrefs.get(ctx, id)) ?: return false
        val monday = WidgetPrefs.range(WidgetPrefs.get(ctx, id)).first.weekMonday()
        return Graph.repo.cached(subject, monday) == null || Graph.repo.ageMillis(subject, monday) > STALE_MS
    }

    fun items(ctx: Context, id: Int, allowNetwork: Boolean): List<WidgetItem> {
        Graph.init(ctx)
        val settings = Graph.settings
        val locale = if (settings.isRussian()) Locale.forLanguageTag("ru") else Locale.ENGLISH
        val st = WidgetPrefs.get(ctx, id)
        val subject = WidgetPrefs.resolveSubject(st) ?: return emptyList()
        val (from, to) = WidgetPrefs.range(st)
        val monday = from.weekMonday()
        val repo = Graph.repo

        var lessons: List<Lesson>? = repo.cached(subject, monday)
        if (allowNetwork && (lessons == null || repo.ageMillis(subject, monday) > STALE_MS)) {
            lessons = try {
                runBlocking { repo.fetchWeek(subject, monday) }
            } catch (e: Exception) {
                lessons
            }
        }

        val inRange = lessons.orEmpty().filter { it.date >= from && it.date <= to }.sortedBy { it.start }
        val out = ArrayList<WidgetItem>()
        if (st.week) {
            val dayFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)
            inRange.groupBy { it.date }.entries.sortedBy { it.key }.forEach { (date, day) ->
                out += WidgetItem.Header(date.format(dayFmt).replaceFirstChar { it.uppercase() })
                day.forEach { out += WidgetItem.Entry(it, colorOf(it)) }
            }
        } else {
            inRange.forEach { out += WidgetItem.Entry(it, colorOf(it)) }
        }
        return out
    }

    private fun colorOf(l: Lesson): Int =
        if (l.cancelled) Color.parseColor("#E94634") else Graph.settings.colorFor(l.category)

    fun view(ctx: Context, item: WidgetItem): RemoteViews {
        val rv = RemoteViews(ctx.packageName, R.layout.widget_item)
        when (item) {
            is WidgetItem.Header -> {
                rv.setViewVisibility(R.id.day_header, View.VISIBLE)
                rv.setViewVisibility(R.id.lesson_row, View.GONE)
                rv.setTextViewText(R.id.day_header, item.text)
            }
            is WidgetItem.Entry -> {
                val l = item.lesson
                rv.setViewVisibility(R.id.day_header, View.GONE)
                rv.setViewVisibility(R.id.lesson_row, View.VISIBLE)
                rv.setTextViewText(R.id.item_time, if (l.allDay) "—" else l.startDt.format(hm))
                rv.setTextColor(R.id.item_time, item.color)
                val title = SpannableString(l.title)
                if (l.cancelled) title.setSpan(StrikethroughSpan(), 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                rv.setTextViewText(R.id.item_title, title)
                rv.setTextViewText(
                    R.id.item_sub,
                    listOf(l.type.orEmpty(), l.locations, l.educators).filter { it.isNotBlank() }.joinToString(" · "),
                )
            }
        }
        rv.setOnClickFillInIntent(R.id.item_root, Intent())
        return rv
    }
}
