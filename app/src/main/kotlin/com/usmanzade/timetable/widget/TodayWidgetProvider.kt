package com.usmanzade.timetable.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.usmanzade.timetable.Graph
import com.usmanzade.timetable.R
import com.usmanzade.timetable.Tr
import com.usmanzade.timetable.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.util.Locale

class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Graph.init(context)
        launchUpdate(context, ids, goAsync())
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { WidgetPrefs.clear(context, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        Graph.init(context)
        val st = WidgetPrefs.get(context, id)
        when (intent.action) {
            ACTION_PREV -> WidgetPrefs.setOffset(context, id, st.offset - 1)
            ACTION_NEXT -> WidgetPrefs.setOffset(context, id, st.offset + 1)
            ACTION_TODAY -> WidgetPrefs.setOffset(context, id, 0)
            ACTION_MODE -> WidgetPrefs.save(context, id, st.subjectKey, !st.week, 0)
            ACTION_SUBJECT -> {
                val list = Graph.settings.favorites.value
                if (list.isNotEmpty()) {
                    val current = WidgetPrefs.resolveSubject(st)?.key
                    val next = list[(list.indexOfFirst { it.key == current } + 1).mod(list.size)]
                    WidgetPrefs.save(context, id, next.key, st.week, 0)
                }
            }
            else -> return
        }
        launchUpdate(context, intArrayOf(id), goAsync())
    }

    companion object {
        const val ACTION_PREV = "com.usmanzade.timetable.widget.PREV"
        const val ACTION_NEXT = "com.usmanzade.timetable.widget.NEXT"
        const val ACTION_TODAY = "com.usmanzade.timetable.widget.TODAY"
        const val ACTION_MODE = "com.usmanzade.timetable.widget.MODE"
        const val ACTION_SUBJECT = "com.usmanzade.timetable.widget.SUBJECT"
        private val ACCENT = android.graphics.Color.parseColor("#3482FF")
        private val SUB = android.graphics.Color.parseColor("#8A8A8A")

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        fun refreshAll(context: Context) {
            Graph.init(context)
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
            if (ids.isNotEmpty()) launchUpdate(context, ids)
        }

        fun launchUpdate(context: Context, ids: IntArray, pending: BroadcastReceiver.PendingResult? = null) {
            scope.launch {
                try {
                    val mgr = AppWidgetManager.getInstance(context)
                    ids.forEach { update(context, mgr, it) }
                } finally {
                    pending?.finish()
                }
            }
        }

        private suspend fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            Graph.init(ctx)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val cached = withContext(Dispatchers.IO) { WidgetData.items(ctx, id, allowNetwork = false) }
                push(ctx, mgr, id, cached)
                if (withContext(Dispatchers.IO) { WidgetData.needsRefresh(ctx, id) }) {
                    val fresh = withContext(Dispatchers.IO) { WidgetData.items(ctx, id, allowNetwork = true) }
                    push(ctx, mgr, id, fresh)
                }
            } else {
                push(ctx, mgr, id, null)
            }
        }

        private fun action(ctx: Context, id: Int, action: String, index: Int): PendingIntent {
            val intent = Intent(ctx, TodayWidgetProvider::class.java)
                .setAction(action)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .setData(Uri.parse("timetable://widget/$id/$action"))
            return PendingIntent.getBroadcast(
                ctx, id * 8 + index, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun push(ctx: Context, mgr: AppWidgetManager, id: Int, items: List<WidgetItem>?) {
            val tr = Tr(Graph.settings.isRussian())
            val locale = if (tr.ru) Locale.forLanguageTag("ru") else Locale.ENGLISH
            val st = WidgetPrefs.get(ctx, id)
            val subject = WidgetPrefs.resolveSubject(st)
            val (from, to) = WidgetPrefs.range(st)

            val rv = RemoteViews(ctx.packageName, R.layout.widget_today)
            val switchHint = if (Graph.settings.favorites.value.size > 1) " ▾" else ""
            rv.setTextViewText(R.id.widget_title, (subject?.displayName ?: tr("Расписание", "Timetable")) + switchHint)
            val dateText = if (st.week) {
                val f = DateTimeFormatter.ofPattern("d MMM", locale)
                "${from.format(f)} – ${to.format(f)}"
            } else {
                from.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)).replaceFirstChar { it.uppercase() }
            }

            val away = st.offset != 0
            rv.setTextViewText(R.id.widget_subtitle, if (away) "$dateText  ·  ↩ ${tr("сегодня", "today")}" else dateText)
            rv.setTextColor(R.id.widget_subtitle, if (away) ACCENT else SUB)
            rv.setTextViewText(R.id.btn_mode, if (st.week) tr("Неделя", "Week") else tr("День", "Day"))
            rv.setTextViewText(
                R.id.widget_empty,
                if (subject == null) tr("Добавьте группу в приложении", "Add a group in the app")
                else if (st.week) tr("На этой неделе занятий нет", "No classes this week")
                else tr("Занятий нет", "No classes"),
            )

            rv.setOnClickPendingIntent(R.id.btn_prev, action(ctx, id, ACTION_PREV, 0))
            rv.setOnClickPendingIntent(R.id.btn_next, action(ctx, id, ACTION_NEXT, 1))
            rv.setOnClickPendingIntent(R.id.widget_subtitle, action(ctx, id, ACTION_TODAY, 2))
            rv.setOnClickPendingIntent(R.id.btn_mode, action(ctx, id, ACTION_MODE, 3))
            rv.setOnClickPendingIntent(R.id.widget_title, action(ctx, id, ACTION_SUBJECT, 4))

            if (items != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val collection = RemoteViews.RemoteCollectionItems.Builder()
                    .setHasStableIds(false)
                    .setViewTypeCount(1)
                items.forEachIndexed { i, item -> collection.addItem(i.toLong(), WidgetData.view(ctx, item)) }
                rv.setRemoteAdapter(R.id.widget_list, collection.build())
            } else {
                setLegacyAdapter(ctx, rv, id)
            }
            rv.setEmptyView(R.id.widget_list, R.id.widget_empty)

            val open = PendingIntent.getActivity(
                ctx, id, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            rv.setPendingIntentTemplate(R.id.widget_list, open)

            mgr.updateAppWidget(id, rv)
            if (items == null) notifyLegacyList(mgr, id)
        }

        @Suppress("DEPRECATION")
        private fun notifyLegacyList(mgr: AppWidgetManager, id: Int) {
            mgr.notifyAppWidgetViewDataChanged(intArrayOf(id), R.id.widget_list)
        }

        @Suppress("DEPRECATION")
        private fun setLegacyAdapter(ctx: Context, rv: RemoteViews, id: Int) {
            val adapter = Intent(ctx, LessonsRemoteService::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .setData(Uri.parse("timetable://widget-list/$id"))
            rv.setRemoteAdapter(R.id.widget_list, adapter)
        }
    }
}
